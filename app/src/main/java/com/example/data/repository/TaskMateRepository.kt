package com.example.data.repository

import android.content.Context
import com.example.data.local.TaskMateDatabase
import com.example.data.model.*
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskMateRepository(
    private val context: Context,
    private val database: TaskMateDatabase = TaskMateDatabase.getDatabase(context)
) {
    private val taskDao = database.taskDao()
    private val reminderDao = database.reminderDao()
    private val dailyPlanDao = database.dailyPlanDao()
    private val logDao = database.productivityLogDao()
    private val userDao = database.userDao()

    fun getAllTasks(userId: String = "user_default"): Flow<List<TaskEntity>> =
        taskDao.getAllTasks(userId)

    suspend fun getTaskById(taskId: String, userId: String = "user_default"): TaskEntity? =
        taskDao.getTaskById(userId, taskId)

    suspend fun insertTask(task: TaskEntity, scheduleReminder: Boolean = true): String {
        taskDao.insertTask(task)

        // Log creation
        logDao.insertLog(
            ProductivityLogEntity(
                userId = task.userId,
                taskId = task.taskId,
                taskTitle = task.title,
                action = "CREATED"
            )
        )

        if (scheduleReminder && task.status != TaskStatus.COMPLETED.name) {
            val reminderTime = task.dueDateTime - (task.reminderMinutesBefore * 60 * 1000L)
            val notifId = (Math.random() * 90000 + 10000).toInt()
            val reminder = ReminderEntity(
                userId = task.userId,
                taskId = task.taskId,
                scheduledAt = reminderTime,
                reminderType = ReminderOffset.fromMinutes(task.reminderMinutesBefore).name,
                status = "SCHEDULED",
                notificationId = notifId
            )
            reminderDao.insertReminder(reminder)
            ReminderScheduler.scheduleReminder(context, task.taskId, reminderTime, notifId)
        }

        return task.taskId
    }

    suspend fun updateTask(task: TaskEntity) {
        val updatedTask = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updatedTask)

        // Cancel previous reminders and schedule new one if pending
        val existingReminders = reminderDao.getRemindersForTask(task.taskId)
        for (r in existingReminders) {
            ReminderScheduler.cancelReminder(context, r.notificationId)
        }
        reminderDao.deleteRemindersForTask(task.taskId)

        if (task.status != TaskStatus.COMPLETED.name && task.status != TaskStatus.CANCELLED.name) {
            val reminderTime = task.dueDateTime - (task.reminderMinutesBefore * 60 * 1000L)
            val notifId = (Math.random() * 90000 + 10000).toInt()
            val reminder = ReminderEntity(
                userId = task.userId,
                taskId = task.taskId,
                scheduledAt = reminderTime,
                reminderType = ReminderOffset.fromMinutes(task.reminderMinutesBefore).name,
                status = "SCHEDULED",
                notificationId = notifId
            )
            reminderDao.insertReminder(reminder)
            ReminderScheduler.scheduleReminder(context, task.taskId, reminderTime, notifId)
        }
    }

    suspend fun toggleTaskComplete(taskId: String, currentStatus: String) {
        val newStatus = if (currentStatus == TaskStatus.COMPLETED.name) TaskStatus.PENDING.name else TaskStatus.COMPLETED.name
        val completedAt = if (newStatus == TaskStatus.COMPLETED.name) System.currentTimeMillis() else null

        taskDao.updateTaskStatus(taskId, newStatus, completedAt)

        val task = taskDao.getTaskByIdDirect(taskId)
        if (task != null) {
            logDao.insertLog(
                ProductivityLogEntity(
                    userId = task.userId,
                    taskId = taskId,
                    taskTitle = task.title,
                    action = if (newStatus == TaskStatus.COMPLETED.name) "COMPLETED" else "RESCHEDULED"
                )
            )

            if (newStatus == TaskStatus.COMPLETED.name) {
                // Cancel active reminders
                val existing = reminderDao.getRemindersForTask(taskId)
                for (r in existing) {
                    ReminderScheduler.cancelReminder(context, r.notificationId)
                    reminderDao.updateReminderStatus(r.reminderId, "DISMISSED")
                }
            } else {
                // Rescheduled - set alarm again if future
                val reminderTime = task.dueDateTime - (task.reminderMinutesBefore * 60 * 1000L)
                val notifId = (Math.random() * 90000 + 10000).toInt()
                reminderDao.insertReminder(
                    ReminderEntity(
                        userId = task.userId,
                        taskId = task.taskId,
                        scheduledAt = reminderTime,
                        reminderType = ReminderOffset.fromMinutes(task.reminderMinutesBefore).name,
                        status = "SCHEDULED",
                        notificationId = notifId
                    )
                )
                ReminderScheduler.scheduleReminder(context, task.taskId, reminderTime, notifId)
            }
        }
    }

    suspend fun deleteTask(taskId: String) {
        val task = taskDao.getTaskByIdDirect(taskId)
        val existing = reminderDao.getRemindersForTask(taskId)
        for (r in existing) {
            ReminderScheduler.cancelReminder(context, r.notificationId)
        }
        reminderDao.deleteRemindersForTask(taskId)
        taskDao.deleteTaskById(taskId)

        if (task != null) {
            logDao.insertLog(
                ProductivityLogEntity(
                    userId = task.userId,
                    taskId = taskId,
                    taskTitle = task.title,
                    action = "DELETED"
                )
            )
        }
    }

    suspend fun rescheduleTask(taskId: String, newDueDateTime: Long) {
        val task = taskDao.getTaskByIdDirect(taskId) ?: return
        val updated = task.copy(dueDateTime = newDueDateTime, status = TaskStatus.PENDING.name, completedAt = null)
        updateTask(updated)
        logDao.insertLog(
            ProductivityLogEntity(
                userId = task.userId,
                taskId = taskId,
                taskTitle = task.title,
                action = "RESCHEDULED"
            )
        )
    }

    fun getAllReminders(userId: String = "user_default"): Flow<List<ReminderEntity>> =
        reminderDao.getAllReminders(userId)

    fun getDailyPlan(userId: String, dateString: String): Flow<DailyPlanEntity?> =
        dailyPlanDao.getPlanForDate(userId, dateString)

    suspend fun saveDailyPlan(plan: DailyPlanEntity) {
        dailyPlanDao.insertPlan(plan)
    }

    fun getProductivityLogs(userId: String = "user_default"): Flow<List<ProductivityLogEntity>> =
        logDao.getRecentLogs(userId)

    suspend fun getCompletedCountSince(userId: String, sinceTimestamp: Long): Int =
        logDao.getCompletedCountSince(userId, sinceTimestamp)

    fun getUser(userId: String = "user_default"): Flow<UserEntity?> =
        userDao.getUser(userId)

    suspend fun updateUser(user: UserEntity) {
        userDao.insertOrUpdateUser(user)
    }
}
