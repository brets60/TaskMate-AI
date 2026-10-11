package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentTrackTitle: String = "",
    val isLooping: Boolean = true
)

class FocusAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playTrack(filePath: String, title: String) {
        stop()

        try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                // Generate ambient focus audio wave if local synthetic track
                generateSyntheticFocusToneFile(file)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                isLooping = _playerState.value.isLooping
                prepare()
                start()
            }

            val duration = mediaPlayer?.duration ?: 0
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                currentTrackTitle = title,
                durationMs = duration,
                currentPositionMs = 0
            )

            startProgressTracking()

            mediaPlayer?.setOnCompletionListener {
                if (!_playerState.value.isLooping) {
                    _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0)
                    stopProgressTracking()
                }
            }
        } catch (e: Exception) {
            Log.e("FocusAudioPlayer", "Error playing audio file: ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            stopProgressTracking()
        } else {
            player.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleLooping() {
        val newLoop = !_playerState.value.isLooping
        mediaPlayer?.isLooping = newLoop
        _playerState.value = _playerState.value.copy(isLooping = newLoop)
    }

    fun stop() {
        stopProgressTracking()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0)
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                _playerState.value = _playerState.value.copy(currentPositionMs = current)
                delay(300)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Generates a warm binaural harmonic focus tone wave file (WAV) for instant offline focus audio
     */
    private fun generateSyntheticFocusToneFile(file: File) {
        try {
            file.parentFile?.mkdirs()
            val sampleRate = 44100
            val durationSeconds = 30
            val numSamples = sampleRate * durationSeconds
            val sample = DoubleArray(numSamples)

            // Warm chord with 432Hz focus harmonic frequency and alpha waves (10Hz difference)
            val freq1 = 216.0 // A3
            val freq2 = 226.0 // Alpha wave difference of 10Hz for deep focus
            val freq3 = 324.0 // E4 perfect fifth
            val freq4 = 432.0 // A4 fundamental

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Ambient pulsating envelope
                val envelope = 0.5 * (1.0 + sin(2 * Math.PI * 0.1 * t))
                val wave = 0.3 * sin(2 * Math.PI * freq1 * t) +
                        0.25 * sin(2 * Math.PI * freq2 * t) +
                        0.25 * sin(2 * Math.PI * freq3 * t) +
                        0.2 * sin(2 * Math.PI * freq4 * t)
                sample[i] = wave * envelope
            }

            val generatedSound = ByteArray(2 * numSamples)
            var idx = 0
            for (dVal in sample) {
                val clamped = (dVal * 32767).toInt().coerceIn(-32768, 32767)
                generatedSound[idx++] = (clamped and 0x00ff).toByte()
                generatedSound[idx++] = ((clamped and 0xff00) ushr 8).toByte()
            }

            // Write standard 16-bit PCM WAV header
            val totalDataLen = generatedSound.size + 36
            val byteRate = sampleRate * 2

            FileOutputStream(file).use { out ->
                val header = ByteArray(44)
                header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
                header[4] = (totalDataLen and 0xff).toByte()
                header[5] = ((totalDataLen shr 8) and 0xff).toByte()
                header[6] = ((totalDataLen shr 16) and 0xff).toByte()
                header[7] = ((totalDataLen shr 24) and 0xff).toByte()
                header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
                header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
                header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // PCM format
                header[20] = 1; header[21] = 0 // Mono
                header[22] = (sampleRate and 0xff).toByte()
                header[23] = ((sampleRate shr 8) and 0xff).toByte()
                header[24] = ((sampleRate shr 16) and 0xff).toByte()
                header[25] = ((sampleRate shr 24) and 0xff).toByte()
                header[26] = (byteRate and 0xff).toByte()
                header[27] = ((byteRate shr 8) and 0xff).toByte()
                header[28] = ((byteRate shr 16) and 0xff).toByte()
                header[29] = ((byteRate shr 24) and 0xff).toByte()
                header[30] = 2; header[31] = 0 // block align
                header[32] = 16; header[33] = 0 // bits per sample
                header[34] = 'd'.code.toByte(); header[35] = 'a'.code.toByte(); header[36] = 't'.code.toByte(); header[37] = 'a'.code.toByte()
                header[38] = (generatedSound.size and 0xff).toByte()
                header[39] = ((generatedSound.size shr 8) and 0xff).toByte()
                header[40] = ((generatedSound.size shr 16) and 0xff).toByte()
                header[41] = ((generatedSound.size shr 24) and 0xff).toByte()

                out.write(header)
                out.write(generatedSound)
            }
        } catch (e: Exception) {
            Log.e("FocusAudioPlayer", "Error creating synthetic focus tone: ${e.message}", e)
        }
    }
}
