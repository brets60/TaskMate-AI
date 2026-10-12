package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        DailyPlanEntity::class,
        ProductivityLogEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TaskMateDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun productivityLogDao(): ProductivityLogDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: TaskMateDatabase? = null

        fun getDatabase(context: Context): TaskMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskMateDatabase::class.java,
                    "taskmate_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: TaskMateDatabase) {
            val user = UserEntity(
                userId = "user_default",
                displayName = "TaskMate User",
                email = "",
                timezone = "America/Los_Angeles",
                notificationsEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                defaultReminderMinutes = 15,
                workStartHour = 9,
                workEndHour = 18,
                themeMode = "LIGHT"
            )
            database.userDao().insertOrUpdateUser(user)
            // App initialized in a completely clean empty state: 0 tasks, 0 reminders, 0 logs
        }
    }
}

typealias AppDatabase = TaskMateDatabase
typealias TaskDatabase = TaskMateDatabase

