package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.TaskMateViewModel
import com.example.ui.components.CategoryBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTaskScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()

    val existingTask = remember(selectedTaskId, allTasks) {
        allTasks.firstOrNull { it.taskId == selectedTaskId }
    }

    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }
    var category by remember(existingTask) { mutableStateOf(TaskCategory.fromString(existingTask?.category ?: "GENERAL")) }
    var priority by remember(existingTask) { mutableStateOf(TaskPriority.fromString(existingTask?.priority ?: "MEDIUM")) }
    var dueDateTime by remember(existingTask) {
        val defaultTime = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
        mutableStateOf(existingTask?.dueDateTime ?: defaultTime)
    }
    var durationMinutes by remember(existingTask) { mutableStateOf(existingTask?.estimatedDurationMinutes ?: 30) }
    var reminderOffset by remember(existingTask) {
        mutableStateOf(ReminderOffset.fromMinutes(existingTask?.reminderMinutesBefore ?: 15))
    }
    var recurrence by remember(existingTask) {
        mutableStateOf(RecurrenceRule.fromString(existingTask?.recurrenceRule ?: "NONE"))
    }

    val calendar = remember(dueDateTime) {
        Calendar.getInstance().apply { timeInMillis = dueDateTime }
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingTask != null) "Edit Task" else "Create Task",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                placeholder = { Text("e.g. Finish client presentation", color = TextSecondaryLight) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceLight,
                    focusedContainerColor = SurfaceLight,
                    unfocusedBorderColor = BorderLight,
                    focusedBorderColor = PrimaryBlue
                )
            )

            // Task Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add key details or links...", color = TextSecondaryLight) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceLight,
                    focusedContainerColor = SurfaceLight,
                    unfocusedBorderColor = BorderLight,
                    focusedBorderColor = PrimaryBlue
                )
            )

            // Category Selection
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.values().take(3).forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PastelBlueBg else SurfaceLight,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.values().drop(3).forEach { cat ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PastelBlueBg else SurfaceLight,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Priority Selection
            Column {
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskPriority.values().forEach { prio ->
                        val isSelected = priority == prio
                        val (selBg, selColor) = when (prio) {
                            TaskPriority.URGENT -> Pair(PastelRoseBg, PastelRoseText)
                            TaskPriority.HIGH -> Pair(PastelPeachBg, PastelPeachText)
                            TaskPriority.MEDIUM -> Pair(PastelBlueBg, PastelBlueText)
                            TaskPriority.LOW -> Pair(PastelMintBg, PastelMintText)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) selBg else SurfaceLight,
                            border = BorderStroke(1.dp, if (isSelected) selColor else BorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { priority = prio }
                        ) {
                            Text(
                                text = prio.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) selColor else TextSecondaryLight,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Date & Time Picker Triggers
            Column {
                Text(
                    text = "Due Date & Time",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        dueDateTime = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceLight,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dateFormatter.format(Date(dueDateTime)), fontWeight = FontWeight.Medium, color = TextPrimaryLight, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        dueDateTime = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceLight,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(timeFormatter.format(Date(dueDateTime)), fontWeight = FontWeight.Medium, color = TextPrimaryLight, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Reminder Offset & Smart Notifications
            Column {
                Text(
                    text = "Reminder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val intervals = listOf(
                        ReminderOffset.AT_DUE_TIME,
                        ReminderOffset.MINS_10_BEFORE,
                        ReminderOffset.MINS_30_BEFORE,
                        ReminderOffset.HOUR_1_BEFORE
                    )
                    intervals.forEach { offset ->
                        val isSelected = reminderOffset == offset
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PastelBlueBg else SurfaceLight,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { reminderOffset = offset }
                        ) {
                            Text(
                                text = offset.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Estimated Duration
            Column {
                Text(
                    text = "Estimated Focus Time",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 90 to "1.5h").forEach { (mins, label) ->
                        val isSelected = durationMinutes == mins
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PastelBlueBg else SurfaceLight,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { durationMinutes = mins }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val task = TaskEntity(
                            taskId = existingTask?.taskId ?: UUID.randomUUID().toString(),
                            userId = existingTask?.userId ?: "user_default",
                            title = title.trim(),
                            description = description.trim(),
                            dueDate = dueDateTime,
                            isCompleted = existingTask?.isCompleted ?: (existingTask?.status == TaskStatus.COMPLETED.name),
                            category = category.name,
                            priority = priority.name,
                            status = existingTask?.status ?: TaskStatus.PENDING.name,
                            dueDateTime = dueDateTime,
                            estimatedDurationMinutes = durationMinutes,
                            recurrenceRule = recurrence.name,
                            reminderMinutesBefore = reminderOffset.minutesBefore
                        )
                        viewModel.saveTask(task)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingTask != null) "Save Changes" else "Save Task",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
