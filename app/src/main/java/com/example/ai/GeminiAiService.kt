package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processUserPrompt(
        prompt: String,
        currentTasks: List<TaskEntity>
    ): Pair<String, AiParsedTask?> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val apiResult = callGeminiApi(prompt, currentTasks, apiKey)
                if (apiResult != null) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.w("GeminiAiService", "API call failed, falling back to local NLP: ${e.message}")
            }
        }

        // Local Smart NLP Fallback
        return@withContext fallbackLocalNlp(prompt, currentTasks)
    }

    suspend fun generateDailyPlan(
        tasks: List<TaskEntity>,
        userWorkingHours: Pair<Int, Int> = Pair(9, 18)
    ): AiDailyPlanResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val plan = callGeminiForDailyPlan(tasks, userWorkingHours, apiKey)
                if (plan != null) return@withContext plan
            } catch (e: Exception) {
                Log.w("GeminiAiService", "Daily plan API call failed: ${e.message}")
            }
        }

        return@withContext fallbackDailyPlan(tasks, userWorkingHours)
    }

    private fun callGeminiApi(
        prompt: String,
        currentTasks: List<TaskEntity>,
        apiKey: String
    ): Pair<String, AiParsedTask?>? {
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm (EEEE)", Locale.getDefault()).format(Date())

        val tasksContext = currentTasks.take(10).joinToString("\n") {
            "- ${it.title} [Priority: ${it.priority}, Due: ${SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(it.dueDateTime))}]"
        }

        val systemInstruction = """
            You are TaskMate AI, an intelligent personal productivity assistant.
            Current time: $nowStr.
            User's active tasks:
            $tasksContext

            If the user asks to create, schedule, or remind a task:
            Extract details and reply in this EXACT JSON structure inside a markdown codeblock ```json:
            {
               "is_task_creation": true,
               "is_ambiguous": false,
               "clarification_question": "",
               "title": "Clean task title",
               "description": "Helpful notes or sub-steps",
               "category": "WORK|PERSONAL|STUDY|HEALTH|FINANCE|GENERAL",
               "priority": "URGENT|HIGH|MEDIUM|LOW",
               "due_datetime_iso": "YYYY-MM-DDTHH:mm:00",
               "estimated_duration_minutes": 30,
               "reminder_minutes_before": 15,
               "chat_reply": "Friendly response explaining what was scheduled."
            }
            If the user request is ambiguous (e.g. 'study' with no time or date specified), set "is_ambiguous": true and supply a "clarification_question".
            If the user asks general questions, prioritization advice, or chat:
            {
               "is_task_creation": false,
               "chat_reply": "Your helpful, motivational advice."
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nUser request: $prompt"))
                    })
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val responseBody = response.body?.string() ?: return null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            return parseAiResponseText(text)
        }
    }

    private fun parseAiResponseText(rawText: String): Pair<String, AiParsedTask?> {
        val jsonPattern = "```json([\\s\\S]*?)```".toRegex()
        val match = jsonPattern.find(rawText)
        val jsonString = match?.groups?.get(1)?.value?.trim() ?: rawText.trim()

        try {
            val obj = JSONObject(jsonString)
            val chatReply = obj.optString("chat_reply", rawText)
            val isTask = obj.optBoolean("is_task_creation", false)

            if (isTask) {
                val isAmbiguous = obj.optBoolean("is_ambiguous", false)
                val clarify = obj.optString("clarification_question", "")
                val title = obj.optString("title", "New Task")
                val desc = obj.optString("description", "")
                val cat = TaskCategory.fromString(obj.optString("category", "GENERAL"))
                val prio = TaskPriority.fromString(obj.optString("priority", "MEDIUM"))
                val duration = obj.optInt("estimated_duration_minutes", 30)
                val reminderMins = obj.optInt("reminder_minutes_before", 15)

                val isoDate = obj.optString("due_datetime_iso", "")
                val dueMillis = parseIsoOrFallback(isoDate)

                val parsed = AiParsedTask(
                    title = title,
                    description = desc,
                    category = cat,
                    priority = prio,
                    dueDateTimeMillis = dueMillis,
                    estimatedDurationMinutes = duration,
                    reminderMinutesBefore = reminderMins,
                    isAmbiguous = isAmbiguous,
                    clarificationQuestion = clarify
                )
                return Pair(chatReply, parsed)
            } else {
                return Pair(chatReply, null)
            }
        } catch (e: Exception) {
            return Pair(rawText, null)
        }
    }

    private fun parseIsoOrFallback(iso: String): Long {
        if (iso.isNotBlank()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val parsed = format.parse(iso)
                if (parsed != null) return parsed.time
            } catch (e: Exception) {
                // fall through
            }
        }
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 2)
        cal.set(Calendar.MINUTE, 0)
        return cal.timeInMillis
    }

    private fun callGeminiForDailyPlan(
        tasks: List<TaskEntity>,
        workingHours: Pair<Int, Int>,
        apiKey: String
    ): AiDailyPlanResult? {
        val taskListStr = tasks.joinToString("\n") {
            "- ${it.title} [Priority: ${it.priority}, Est: ${it.estimatedDurationMinutes}m]"
        }
        val prompt = """
            Generate an optimized daily time-block schedule for today between ${workingHours.first}:00 and ${workingHours.second}:00.
            Tasks:
            $taskListStr

            Return JSON inside ```json:
            {
               "summary": "Brief 1-2 sentence overview of the focus for today",
               "focusTip": "Actionable productivity tip",
               "timeBlocks": [
                  {"timeSlot": "09:00 - 09:45", "taskTitle": "Task Name", "isBreak": false, "notes": "Deep work session"},
                  {"timeSlot": "09:45 - 10:00", "taskTitle": "Rest & Hydrate", "isBreak": true, "notes": "15 min break"}
               ]
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().put("text", prompt)) })
                })
            })
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonMatch = "```json([\\s\\S]*?)```".toRegex().find(text)
            val jsonStr = jsonMatch?.groups?.get(1)?.value?.trim() ?: text.trim()
            val obj = JSONObject(jsonStr)

            val summary = obj.optString("summary", "Personalized focus plan for today.")
            val focusTip = obj.optString("focusTip", "Work in 45-minute blocks to sustain peak concentration.")
            val blocksArray = obj.optJSONArray("timeBlocks") ?: JSONArray()
            val blocks = mutableListOf<AiDailyScheduleBlock>()
            for (i in 0 until blocksArray.length()) {
                val b = blocksArray.getJSONObject(i)
                blocks.add(
                    AiDailyScheduleBlock(
                        timeSlot = b.optString("timeSlot", "09:00 - 09:30"),
                        taskTitle = b.optString("taskTitle", "Task"),
                        isBreak = b.optBoolean("isBreak", false),
                        notes = b.optString("notes", "")
                    )
                )
            }
            return AiDailyPlanResult(summary, blocks, focusTip)
        }
    }

    private fun fallbackLocalNlp(
        prompt: String,
        currentTasks: List<TaskEntity>
    ): Pair<String, AiParsedTask?> {
        val lower = prompt.lowercase()

        // Check if user is asking prioritization
        if (lower.contains("which should i do first") || lower.contains("what to do first") || lower.contains("prioritize")) {
            val pending = currentTasks.filter { it.status != "COMPLETED" }
                .sortedWith(compareByDescending<TaskEntity> { TaskPriority.fromString(it.priority).level }.thenBy { it.dueDateTime })

            val topTask = pending.firstOrNull()
            val reply = if (topTask != null) {
                "Based on priority and deadlines, start with '${topTask.title}' (${topTask.priority} priority). Tackling your most critical item early builds great momentum!"
            } else {
                "You currently have no pending urgent tasks! You can review your roadmap or add new goals."
            }
            return Pair(reply, null)
        }

        // Check for task creation keywords
        val isCreateCommand = lower.contains("remind me") || lower.contains("create") || lower.contains("add task") || lower.contains("schedule")

        if (isCreateCommand) {
            val titleClean = prompt
                .replace(Regex("(?i)remind me to|create a task to|create task|add task to|add task|schedule"), "")
                .replace(Regex("(?i)tomorrow|at \\d+(:\\d+)? ?(am|pm)?|on friday|on monday|priority \\w+"), "")
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val finalTitle = if (titleClean.length > 2) titleClean else "Scheduled Task"

            val cal = Calendar.getInstance()
            if (lower.contains("tomorrow")) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            } else if (lower.contains("friday")) {
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else if (lower.contains("monday")) {
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Extract time
            val timeMatch = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(lower)
            if (timeMatch != null) {
                var hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
                val min = timeMatch.groupValues[3].toIntOrNull() ?: 0
                val ampm = timeMatch.groupValues[4].lowercase()
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 18)
                cal.set(Calendar.MINUTE, 0)
            }

            val category = when {
                lower.contains("study") || lower.contains("homework") || lower.contains("exam") || lower.contains("assignment") -> TaskCategory.STUDY
                lower.contains("bill") || lower.contains("pay") || lower.contains("bank") || lower.contains("finance") -> TaskCategory.FINANCE
                lower.contains("gym") || lower.contains("workout") || lower.contains("doctor") || lower.contains("health") -> TaskCategory.HEALTH
                lower.contains("meeting") || lower.contains("email") || lower.contains("work") || lower.contains("sprint") -> TaskCategory.WORK
                else -> TaskCategory.PERSONAL
            }

            val priority = when {
                lower.contains("urgent") || lower.contains("asap") -> TaskPriority.URGENT
                lower.contains("high") || lower.contains("important") -> TaskPriority.HIGH
                lower.contains("low") -> TaskPriority.LOW
                else -> TaskPriority.MEDIUM
            }

            val parsed = AiParsedTask(
                title = finalTitle,
                description = "Created automatically via TaskMate AI natural language assistant.",
                category = category,
                priority = priority,
                dueDateTimeMillis = cal.timeInMillis,
                estimatedDurationMinutes = 45,
                reminderMinutesBefore = 15,
                isAmbiguous = false
            )

            val timeStr = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault()).format(Date(cal.timeInMillis))
            return Pair("I parsed your task '$finalTitle' for $timeStr with a 15-minute reminder. Please confirm to save it to your schedule!", parsed)
        }

        // Generic helpful reply
        return Pair("I'm here to help you stay organized! You can say 'Remind me to study tomorrow at 7 PM', 'What task should I do first?', or 'Plan my schedule for today'.", null)
    }

    private fun fallbackDailyPlan(
        tasks: List<TaskEntity>,
        workingHours: Pair<Int, Int>
    ): AiDailyPlanResult {
        val pending = tasks.filter { it.status != "COMPLETED" }
            .sortedWith(compareByDescending<TaskEntity> { TaskPriority.fromString(it.priority).level }.thenBy { it.dueDateTime })

        val blocks = mutableListOf<AiDailyScheduleBlock>()
        var currentHour = workingHours.first
        var currentMinute = 0

        for (task in pending.take(5)) {
            val duration = task.estimatedDurationMinutes.coerceIn(15, 90)
            val startStr = String.format("%02d:%02d", currentHour, currentMinute)

            val endTotalMinutes = currentHour * 60 + currentMinute + duration
            val endHour = endTotalMinutes / 60
            val endMin = endTotalMinutes % 60
            val endStr = String.format("%02d:%02d", endHour, endMin)

            blocks.add(
                AiDailyScheduleBlock(
                    timeSlot = "$startStr - $endStr",
                    taskTitle = task.title,
                    taskId = task.taskId,
                    isBreak = false,
                    notes = "Priority: ${task.priority} • Focus time"
                )
            )

            // Add 15 min rest break
            val breakEndTotal = endTotalMinutes + 15
            val bEndHour = breakEndTotal / 60
            val bEndMin = breakEndTotal % 60
            blocks.add(
                AiDailyScheduleBlock(
                    timeSlot = String.format("%02d:%02d - %02d:%02d", endHour, endMin, bEndHour, bEndMin),
                    taskTitle = "Recharge & Stretch",
                    isBreak = true,
                    notes = "Step away, hydrate, and relax your eyes."
                )
            )

            currentHour = bEndHour
            currentMinute = bEndMin
            if (currentHour >= workingHours.second) break
        }

        if (blocks.isEmpty()) {
            blocks.add(
                AiDailyScheduleBlock(
                    timeSlot = String.format("%02d:00 - %02d:00", workingHours.first, workingHours.first + 1),
                    taskTitle = "Strategic Goal Planning",
                    isBreak = false,
                    notes = "Set upcoming weekly milestones."
                )
            )
        }

        return AiDailyPlanResult(
            summary = "Optimized daily plan with balanced deep-work intervals and built-in recharge breaks.",
            timeBlocks = blocks,
            focusTip = "Eliminate notifications during your first 45-minute block to enter deep focus faster."
        )
    }
}
