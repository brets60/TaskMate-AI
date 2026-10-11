package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.TaskMateDatabase
import com.example.data.model.TaskStatus
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.taskmate.ACTION_TRIGGER_REMINDER"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, taskId.hashCode())

        Log.d(TAG, "Alarm triggered for taskId=$taskId, notifId=$notifId")

        // Run in IO scope to check database and user preferences
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskMateDatabase.getDatabase(context)
                val task = db.taskDao().getTaskByIdDirect(taskId)
                val user = db.userDao().getUserDirect(task?.userId ?: "user_default")

                if (task != null && task.status != TaskStatus.COMPLETED.name && task.status != TaskStatus.CANCELLED.name) {
                    val notificationsAllowed = user?.notificationsEnabled != false
                    if (notificationsAllowed) {
                        val isOverdue = System.currentTimeMillis() > task.dueDateTime
                        NotificationHelper.showTaskReminderNotification(context, task, notifId, isOverdue)
                    }
                } else {
                    Log.d(TAG, "Task $taskId completed or deleted, skipping notification.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm broadcast: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
