package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ReminderOffset
import com.example.data.model.Task
import com.example.data.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TaskMate AI", appName)
  }

  @Test
  fun `test task priority ordering and parsing`() {
    val urgent = TaskPriority.fromString("urgent")
    val low = TaskPriority.fromString("low")
    assertEquals(TaskPriority.URGENT, urgent)
    assertEquals(TaskPriority.LOW, low)
    assert(urgent.level > low.level)
  }

  @Test
  fun `test reminder offset mapping`() {
    val tenMins = ReminderOffset.fromMinutes(10)
    assertEquals(ReminderOffset.MINS_10_BEFORE, tenMins)
  }

  @Test
  fun `test firestore task data class defaults and construction`() {
    val task = Task(
      id = "test_doc_1",
      userId = "user_123",
      title = "Launch Sprint Review",
      description = "Prepare product slides and roadmap updates",
      category = "WORK",
      priority = "HIGH",
      status = "PENDING",
      dueDateTime = 1700000000000L,
      estimatedDurationMinutes = 45,
      reminderMinutesBefore = 15
    )

    assertEquals("test_doc_1", task.id)
    assertEquals("user_123", task.userId)
    assertEquals("Launch Sprint Review", task.title)
    assertEquals("WORK", task.category)
    assertEquals("HIGH", task.priority)
    assertEquals("PENDING", task.status)
    assertEquals(45, task.estimatedDurationMinutes)
    assertEquals(15, task.reminderMinutesBefore)

    // Test zero-arg synthetic constructor for Firestore deserialization
    val defaultTask = Task()
    assertEquals("", defaultTask.id)
    assertEquals("", defaultTask.userId)
    assertEquals("", defaultTask.title)
    assertEquals("PENDING", defaultTask.status)
    assertEquals("GENERAL", defaultTask.category)
  }

  @Test
  fun `test room task schema entity creation and field persistence`() {
    val task = TaskEntity(
      title = "Complete Product Presentation",
      description = "Prepare detailed slide deck for stakeholders",
      dueDate = 1750000000000L,
      isCompleted = false
    )

    assertEquals("Complete Product Presentation", task.title)
    assertEquals("Prepare detailed slide deck for stakeholders", task.description)
    assertEquals(1750000000000L, task.dueDate)
    org.junit.Assert.assertFalse(task.isCompleted)
    assertEquals("PENDING", task.status)
    assertEquals("PENDING", task.completionStatus)
  }

  @Test
  fun `test room database dao crud operations`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.TaskMateDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.taskDao()

    val task1 = TaskEntity(
      taskId = "local_task_1",
      userId = "test_user",
      title = "Buy groceries",
      description = "Milk, eggs, and sourdough bread",
      dueDate = 1700000000000L,
      isCompleted = false
    )
    dao.insert(task1)

    val retrieved = dao.getTaskById("local_task_1")
    assertNotNull(retrieved)
    assertEquals("Buy groceries", retrieved?.title)
    assertEquals("Milk, eggs, and sourdough bread", retrieved?.description)
    assertEquals(1700000000000L, retrieved?.dueDate)
    assertEquals(false, retrieved?.isCompleted)

    // Update completion status
    dao.updateCompletion("local_task_1", isCompleted = true)
    val completedTask = dao.getTaskById("local_task_1")
    assertNotNull(completedTask)
    assertEquals(true, completedTask?.isCompleted)
    assertEquals("COMPLETED", completedTask?.status)

    // Observe via Flow
    val allTasks = kotlinx.coroutines.flow.first(dao.getAllTasks())
    assertEquals(1, allTasks.size)

    // Delete
    dao.deleteTaskById("local_task_1")
    val deleted = dao.getTaskById("local_task_1")
    assertEquals(null, deleted)

    db.close()
  }

  @Test
  fun `test querying room database tasks showing title and status`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.TaskMateDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.taskDao()

    val task1 = TaskEntity(
      taskId = "task_alpha",
      title = "Refactor Database Module",
      description = "Optimize queries and indexing",
      dueDate = System.currentTimeMillis() + 86400000L,
      isCompleted = false
    )
    val task2 = TaskEntity(
      taskId = "task_beta",
      title = "Submit Weekly Report",
      description = "Summarize user metrics",
      dueDate = System.currentTimeMillis() + 172800000L,
      isCompleted = true
    )
    dao.insert(task1)
    dao.insert(task2)

    val queriedTasks = kotlinx.coroutines.flow.first(dao.getAllTasks())
    assertEquals(2, queriedTasks.size)

    // Verify each task contains title and status
    val retrieved1 = queriedTasks.first { it.taskId == "task_alpha" }
    assertEquals("Refactor Database Module", retrieved1.title)
    assertEquals("PENDING", retrieved1.status)

    val retrieved2 = queriedTasks.first { it.taskId == "task_beta" }
    assertEquals("Submit Weekly Report", retrieved2.title)
    assertEquals("COMPLETED", retrieved2.status)

    db.close()
  }

  @Test
  fun `test deleting task from room database on swipe-to-dismiss action`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.TaskMateDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.taskDao()

    val taskToDismiss = TaskEntity(
      taskId = "task_swipe_delete",
      title = "Swipe To Dismiss Test Task",
      description = "Should be removed from Room database",
      dueDate = System.currentTimeMillis() + 3600000L,
      isCompleted = false
    )
    dao.insert(taskToDismiss)

    // Verify task exists in Room DB
    val initialTasks = kotlinx.coroutines.flow.first(dao.getAllTasks())
    assertEquals(1, initialTasks.size)
    assertEquals("task_swipe_delete", initialTasks[0].taskId)

    // Action: Simulate swipe-to-dismiss deletion
    dao.deleteTaskById(taskToDismiss.taskId)

    // Verify task is removed from Room DB
    val afterDeleteTasks = kotlinx.coroutines.flow.first(dao.getAllTasks())
    assertEquals(0, afterDeleteTasks.size)
    assertNull(dao.getTaskById("task_swipe_delete"))

    db.close()
  }

  @Test
  fun `test strong password evaluation requirements`() {
    // Weak password - only lowercase
    val weak = com.example.ui.screens.evaluatePasswordStrength("password")
    org.junit.Assert.assertFalse(weak.isStrong)
    org.junit.Assert.assertFalse(weak.hasUppercase)
    org.junit.Assert.assertFalse(weak.hasDigit)
    org.junit.Assert.assertFalse(weak.hasSpecialChar)

    // Missing special character and uppercase
    val med = com.example.ui.screens.evaluatePasswordStrength("password123")
    org.junit.Assert.assertFalse(med.isStrong)
    org.junit.Assert.assertFalse(med.hasUppercase)
    org.junit.Assert.assertFalse(med.hasSpecialChar)

    // Strong password meeting all 5 requirements: >= 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special symbol
    val strong = com.example.ui.screens.evaluatePasswordStrength("P@ssw0rdSecure!")
    org.junit.Assert.assertTrue(strong.isStrong)
    org.junit.Assert.assertTrue(strong.hasMinLength)
    org.junit.Assert.assertTrue(strong.hasUppercase)
    org.junit.Assert.assertTrue(strong.hasLowercase)
    org.junit.Assert.assertTrue(strong.hasDigit)
    org.junit.Assert.assertTrue(strong.hasSpecialChar)
    org.junit.Assert.assertEquals(5, strong.score)
    org.junit.Assert.assertEquals("Strong & Secure", strong.label)
  }
}

