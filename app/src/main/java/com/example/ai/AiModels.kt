package com.example.ai

import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority

data class AiParsedTask(
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.GENERAL,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateTimeMillis: Long,
    val estimatedDurationMinutes: Int = 30,
    val reminderMinutesBefore: Int = 15,
    val isAmbiguous: Boolean = false,
    val clarificationQuestion: String = ""
)

data class AiDailyScheduleBlock(
    val timeSlot: String, // e.g. "09:00 - 09:45"
    val taskTitle: String,
    val taskId: String? = null,
    val isBreak: Boolean = false,
    val notes: String = ""
)

data class AiDailyPlanResult(
    val summary: String,
    val timeBlocks: List<AiDailyScheduleBlock>,
    val focusTip: String
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val parsedTask: AiParsedTask? = null,
    val timestamp: Long = System.currentTimeMillis()
)
