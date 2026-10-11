package com.example

import android.app.Application
import com.example.notification.NotificationHelper

class TaskMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)
    }
}
