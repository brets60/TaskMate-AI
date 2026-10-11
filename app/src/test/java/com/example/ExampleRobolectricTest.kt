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
}
