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
    version = 1,
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
                displayName = "Alex Rivera",
                email = "alex.rivera@example.com",
                timezone = "America/Los_Angeles",
                notificationsEnabled = true,
                soundEnabled = true,
                vibrateEnabled = true,
                defaultReminderMinutes = 15,
                workStartHour = 9,
                workEndHour = 18,
                themeMode = "SYSTEM"
            )
            database.userDao().insertOrUpdateUser(user)

            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            // Sample tasks for a vibrant first experience
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 30)
            val todayMorning = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 14)
            cal.set(Calendar.MINUTE, 0)
            val todayAfternoon = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 17)
            cal.set(Calendar.MINUTE, 30)
            val todayEvening = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 11)
            val tomorrow = cal.timeInMillis

            val sampleTasks = listOf(
                TaskEntity(
                    taskId = "task_1",
                    userId = "user_default",
                    title = "Review Q4 Product Roadmap & Milestones",
                    description = "Sync with engineering and design leads on the sprint priorities and timeline.",
                    category = TaskCategory.WORK.name,
                    priority = TaskPriority.HIGH.name,
                    status = TaskStatus.PENDING.name,
                    dueDateTime = todayMorning,
                    estimatedDurationMinutes = 45,
                    reminderMinutesBefore = 15
                ),
                TaskEntity(
                    taskId = "task_2",
                    userId = "user_default",
                    title = "Prepare AI Task Assistant Presentation",
                    description = "Finalize slides highlighting Gemini-powered schedule optimization and time blocking.",
                    category = TaskCategory.STUDY.name,
                    priority = TaskPriority.URGENT.name,
                    status = TaskStatus.IN_PROGRESS.name,
                    dueDateTime = todayAfternoon,
                    estimatedDurationMinutes = 60,
                    reminderMinutesBefore = 30
                ),
                TaskEntity(
                    taskId = "task_3",
                    userId = "user_default",
                    title = "Gym Workout & 30-min Cardio Session",
                    description = "Core stretching, interval training, and hydration check.",
                    category = TaskCategory.HEALTH.name,
                    priority = TaskPriority.MEDIUM.name,
                    status = TaskStatus.PENDING.name,
                    dueDateTime = todayEvening,
                    estimatedDurationMinutes = 45,
                    reminderMinutesBefore = 10
                ),
                TaskEntity(
                    taskId = "task_4",
                    userId = "user_default",
                    title = "Pay Monthly Electricity & Utility Bills",
                    description = "Review consumption statement and confirm autopay confirmation number.",
                    category = TaskCategory.FINANCE.name,
                    priority = TaskPriority.MEDIUM.name,
                    status = TaskStatus.PENDING.name,
                    dueDateTime = tomorrow,
                    estimatedDurationMinutes = 15,
                    reminderMinutesBefore = 60
                ),
                TaskEntity(
                    taskId = "task_5",
                    userId = "user_default",
                    title = "Morning Mindfulness & Journaling",
                    description = "Completed 10-minute guided reflection and priority setting.",
                    category = TaskCategory.PERSONAL.name,
                    priority = TaskPriority.LOW.name,
                    status = TaskStatus.COMPLETED.name,
                    dueDateTime = now - 3600000 * 2,
                    estimatedDurationMinutes = 15,
                    completedAt = now - 3600000 * 2
                )
            )

            for (task in sampleTasks) {
                database.taskDao().insertTask(task)
                if (task.status != TaskStatus.COMPLETED.name) {
                    val reminderTime = task.dueDateTime - (task.reminderMinutesBefore * 60 * 1000L)
                    val reminder = ReminderEntity(
                        reminderId = "rem_${task.taskId}",
                        userId = "user_default",
                        taskId = task.taskId,
                        scheduledAt = reminderTime,
                        reminderType = ReminderOffset.fromMinutes(task.reminderMinutesBefore).name,
                        status = "SCHEDULED"
                    )
                    database.reminderDao().insertReminder(reminder)
                }
            }

            // Seed sample logs
            database.productivityLogDao().insertLog(
                ProductivityLogEntity(
                    userId = "user_default",
                    taskId = "task_5",
                    taskTitle = "Morning Mindfulness & Journaling",
                    action = "COMPLETED",
                    timestamp = now - 3600000 * 2
                )
            )
        }
    }
}
