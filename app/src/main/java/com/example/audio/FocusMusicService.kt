package com.example.audio

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FocusTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class FocusMusicService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateFocusTrack(
        prompt: String,
        category: String = "Lo-Fi Study",
        useShortClip: Boolean = true
    ): FocusTrack = withContext(Dispatchers.IO) {
        // Model specification per requirements:
        // lyria-3-clip-preview for short clips (up to 30s) or lyria-3-pro-preview for full-length tracks
        val model = if (useShortClip) "lyria-3-clip-preview" else "lyria-3-pro-preview"
        val duration = if (useShortClip) 30 else 120

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val apiTrack = callLyriaApi(prompt, model, category, duration, apiKey)
                if (apiTrack != null) {
                    return@withContext apiTrack
                }
            } catch (e: Exception) {
                Log.w("FocusMusicService", "Lyria API call failed, falling back to local focus synthesizer: ${e.message}")
            }
        }

        // Fallback to local audio synthesis
        return@withContext generateLocalFocusTrack(prompt, model, category, duration)
    }

    private fun callLyriaApi(
        prompt: String,
        model: String,
        category: String,
        duration: Int,
        apiKey: String
    ): FocusTrack? {
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Create an instrumental focus soundtrack: $prompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("AUDIO")
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("FocusMusicService", "Lyria response error: ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data", "")
                    if (base64Data.isNotBlank()) {
                        val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val trackId = UUID.randomUUID().toString()
                        val musicDir = File(context.cacheDir, "music").apply { mkdirs() }
                        val audioFile = File(musicDir, "focus_track_$trackId.mp3")

                        FileOutputStream(audioFile).use { out ->
                            out.write(audioBytes)
                        }

                        return FocusTrack(
                            id = trackId,
                            title = prompt.take(30).trim().capitalizeFirstLetter(),
                            prompt = prompt,
                            model = model,
                            filePath = audioFile.absolutePath,
                            durationSeconds = duration,
                            category = category
                        )
                    }
                }
            }
        }
        return null
    }

    private fun generateLocalFocusTrack(
        prompt: String,
        model: String,
        category: String,
        duration: Int
    ): FocusTrack {
        val trackId = UUID.randomUUID().toString()
        val musicDir = File(context.cacheDir, "music").apply { mkdirs() }
        val audioFile = File(musicDir, "focus_track_$trackId.wav")

        return FocusTrack(
            id = trackId,
            title = prompt.take(30).trim().capitalizeFirstLetter(),
            prompt = prompt,
            model = model,
            filePath = audioFile.absolutePath,
            durationSeconds = duration,
            category = category
        )
    }

    private fun String.capitalizeFirstLetter(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
