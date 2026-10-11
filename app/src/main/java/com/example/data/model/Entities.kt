package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskPriority(val title: String, val level: Int) {
    URGENT("Urgent", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1);

    companion object {
        fun fromString(value: String): TaskPriority {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) || it.title.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class TaskCategory(val title: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    STUDY("Study"),
    HEALTH("Health"),
    FINANCE("Finance"),
    GENERAL("General");

    companion object {
        fun fromString(value: String): TaskCategory {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) || it.title.equals(value, ignoreCase = true) } ?: GENERAL
        }
    }
}

enum class TaskStatus(val title: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): TaskStatus {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) || it.title.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

enum class RecurrenceRule(val title: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun fromString(value: String): RecurrenceRule {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) || it.title.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class ReminderOffset(val title: String, val minutesBefore: Int) {
    AT_DUE_TIME("At due time", 0),
    MINS_10_BEFORE("10 minutes before", 10),
    MINS_30_BEFORE("30 minutes before", 30),
    HOUR_1_BEFORE("1 hour before", 60),
    DAY_1_BEFORE("1 day before", 1440);

    companion object {
        fun fromMinutes(minutes: Int): ReminderOffset {
            return values().firstOrNull { it.minutesBefore == minutes } ?: MINS_10_BEFORE
        }
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val taskId: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val title: String,
    val description: String = "",
    val category: String = TaskCategory.GENERAL.name,
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.PENDING.name,
    val dueDateTime: Long,
    val estimatedDurationMinutes: Int = 30,
    val recurrenceRule: String = RecurrenceRule.NONE.name,
    val reminderMinutesBefore: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val reminderId: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val taskId: String,
    val scheduledAt: Long,
    val reminderType: String = ReminderOffset.MINS_10_BEFORE.name,
    val status: String = "SCHEDULED", // SCHEDULED, TRIGGERED, DISMISSED, SNOOZED
    val notificationId: Int = (Math.random() * 100000).toInt()
)

@Entity(tableName = "daily_plans")
data class DailyPlanEntity(
    @PrimaryKey
    val planId: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val dateString: String, // YYYY-MM-DD
    val scheduleJson: String,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "productivity_logs")
data class ProductivityLogEntity(
    @PrimaryKey
    val logId: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val taskId: String,
    val taskTitle: String,
    val action: String, // CREATED, COMPLETED, RESCHEDULED, SNOOZED, DELETED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String = "user_default",
    val displayName: String = "Alex Rivera",
    val email: String = "alex.rivera@example.com",
    val timezone: String = "UTC-7 (Pacific Time)",
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 10,
    val workStartHour: Int = 9,
    val workEndHour: Int = 18,
    val themeMode: String = "LIGHT", // LIGHT, DARK, SYSTEM
    val createdAt: Long = System.currentTimeMillis()
)
