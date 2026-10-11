package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.TaskMateDatabase
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device rebooted, restoring scheduled task reminders...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TaskMateDatabase.getDatabase(context)
                    val futureReminders = db.reminderDao().getActiveFutureReminders(System.currentTimeMillis())
                    for (reminder in futureReminders) {
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            taskId = reminder.taskId,
                            scheduledTimeMillis = reminder.scheduledAt,
                            notificationId = reminder.notificationId
                        )
                    }
                    Log.d(TAG, "Restored ${futureReminders.size} reminders successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore reminders on boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
