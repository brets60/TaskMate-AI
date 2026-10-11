package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.TaskMateDatabase
import com.example.data.model.ProductivityLogEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskStatus
import com.example.notification.NotificationHelper
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MARK_DONE = "com.example.taskmate.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.taskmate.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val TAG = "TaskActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val action = intent.action

        Log.d(TAG, "Received action: $action for taskId: $taskId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskMateDatabase.getDatabase(context)
                val task = db.taskDao().getTaskByIdDirect(taskId)

                when (action) {
                    ACTION_MARK_DONE -> {
                        NotificationHelper.dismissNotification(context, notifId)
                        if (task != null) {
                            db.taskDao().updateTaskStatus(
                                taskId = taskId,
                                status = TaskStatus.COMPLETED.name,
                                completedAt = System.currentTimeMillis()
                            )
                            db.productivityLogDao().insertLog(
                                ProductivityLogEntity(
                                    userId = task.userId,
                                    taskId = taskId,
                                    taskTitle = task.title,
                                    action = "COMPLETED"
                                )
                            )
                        }
                    }
                    ACTION_SNOOZE -> {
                        NotificationHelper.dismissNotification(context, notifId)
                        // Snooze 10 minutes
                        val snoozeMillis = System.currentTimeMillis() + (10 * 60 * 1000L)
                        val newNotifId = (Math.random() * 90000 + 10000).toInt()
                        ReminderScheduler.scheduleReminder(context, taskId, snoozeMillis, newNotifId)

                        if (task != null) {
                            db.reminderDao().insertReminder(
                                ReminderEntity(
                                    userId = task.userId,
                                    taskId = taskId,
                                    scheduledAt = snoozeMillis,
                                    reminderType = "SNOOZE_10M",
                                    status = "SNOOZED",
                                    notificationId = newNotifId
                                )
                            )
                            db.productivityLogDao().insertLog(
                                ProductivityLogEntity(
                                    userId = task.userId,
                                    taskId = taskId,
                                    taskTitle = task.title,
                                    action = "SNOOZED"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling task action $action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
