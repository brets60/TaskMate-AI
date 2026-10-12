package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    fun getAllTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND taskId = :taskId")
    suspend fun getTaskById(userId: String, taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    suspend fun getTaskByIdDirect(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    fun getTaskByIdFlow(taskId: String): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY dueDate ASC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 ORDER BY dueDate ASC")
    fun getCompletedTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getPendingTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted ORDER BY dueDate ASC")
    fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = :status, completionStatus = :status, isCompleted = CASE WHEN :status = 'COMPLETED' THEN 1 ELSE 0 END, completedAt = :completedAt, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isCompleted = :isCompleted, status = CASE WHEN :isCompleted = 1 THEN 'COMPLETED' ELSE 'PENDING' END, completionStatus = CASE WHEN :isCompleted = 1 THEN 'COMPLETED' ELSE 'PENDING' END, completedAt = :completedAt, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun updateCompletion(taskId: String, isCompleted: Boolean, completedAt: Long? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND dueDate < :thresholdTime")
    suspend fun getOverdueTasks(userId: String, thresholdTime: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate BETWEEN :startTime AND :endTime")
    suspend fun getTasksBetween(userId: String, startTime: Long, endTime: Long): List<TaskEntity>
}

typealias TaskItemDao = TaskDao

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY scheduledAt ASC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getRemindersForTask(taskId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status WHERE reminderId = :reminderId")
    suspend fun updateReminderStatus(reminderId: String, status: String)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: String)

    @Query("SELECT * FROM reminders WHERE scheduledAt > :now AND status = 'SCHEDULED' ORDER BY scheduledAt ASC")
    suspend fun getActiveFutureReminders(now: Long): List<ReminderEntity>
}

@Dao
interface DailyPlanDao {
    @Query("SELECT * FROM daily_plans WHERE userId = :userId AND dateString = :dateString LIMIT 1")
    fun getPlanForDate(userId: String, dateString: String): Flow<DailyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DailyPlanEntity)
}

@Dao
interface ProductivityLogDao {
    @Query("SELECT * FROM productivity_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(userId: String): Flow<List<ProductivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProductivityLogEntity)

    @Query("SELECT COUNT(*) FROM productivity_logs WHERE userId = :userId AND action = 'COMPLETED' AND timestamp >= :sinceTimestamp")
    suspend fun getCompletedCountSince(userId: String, sinceTimestamp: Long): Int
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserDirect(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)
}
