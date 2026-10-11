package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun TaskDetailScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allReminders by viewModel.allReminders.collectAsState()

    val task = remember(selectedTaskId, allTasks) {
        allTasks.firstOrNull { it.taskId == selectedTaskId }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (task == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Task not found or has been removed.", color = TextSecondaryLight)
        }
        return
    }

    val isCompleted = task.status == TaskStatus.COMPLETED.name
    val isOverdue = !isCompleted && task.dueDateTime < System.currentTimeMillis()
    val priority = TaskPriority.fromString(task.priority)
    val category = TaskCategory.fromString(task.category)

    val reminder = allReminders.firstOrNull { it.taskId == task.taskId }

    val formattedDueDate = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(task.dueDateTime))

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Task Details", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openEditTask(task.taskId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Task", tint = PrimaryBlue)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = PriorityUrgent)
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
            // Status and Badges Card (Light Pastel Header)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) PastelMintBg else if (isOverdue) PastelRoseBg else SurfaceLight
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCompleted) PastelMintBorder else if (isOverdue) PastelRoseBorder else BorderLight
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = if (isCompleted) PastelMintText else if (isOverdue) PastelRoseText else PrimaryBlue,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "COMPLETED" else if (isOverdue) "OVERDUE" else "PENDING",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriorityBadge(priority)
                            CategoryBadge(category)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryLight
                        )
                    }
                }
            }

            // Schedule & Reminder info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Schedule & Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelBlueBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Due Date & Time", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                            Text(formattedDueDate, fontWeight = FontWeight.SemiBold, color = TextPrimaryLight)
                        }
                    }

                    HorizontalDivider(color = BorderLight)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelMintBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Scheduled Reminder", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                            val reminderText = if (reminder != null) {
                                val remDate = SimpleDateFormat("h:mm a (MMM d)", Locale.getDefault()).format(Date(reminder.scheduledAt))
                                "Active • Alerts at $remDate (${task.reminderMinutesBefore}m before)"
                            } else {
                                "Direct alarm not set"
                            }
                            Text(reminderText, fontWeight = FontWeight.Medium, color = TextPrimaryLight)
                        }
                    }

                    HorizontalDivider(color = BorderLight)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PastelAmberBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Timelapse, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Estimated Focus Duration", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                            Text("${task.estimatedDurationMinutes} minutes", fontWeight = FontWeight.Medium, color = TextPrimaryLight)
                        }
                    }
                }
            }

            // Quick Reschedule actions
            if (!isCompleted) {
                Text(
                    text = "Quick Reschedule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                            viewModel.rescheduleTask(task.taskId, cal.timeInMillis)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = PrimaryBlueDark),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Text("+1 Hour", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                            viewModel.rescheduleTask(task.taskId, cal.timeInMillis)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = PrimaryBlueDark),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Text("Tomorrow 9AM", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                            viewModel.rescheduleTask(task.taskId, cal.timeInMillis)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = PrimaryBlueDark),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Text("+1 Week", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Primary Toggle Complete button
            Button(
                onClick = { viewModel.toggleTaskComplete(task) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) SurfaceVariantLight else AccentEmerald,
                    contentColor = if (isCompleted) TextPrimaryLight else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("detail_toggle_complete_button")
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Replay else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) "Mark as Incomplete" else "Mark as Completed",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
            text = { Text("Are you sure you want to delete '${task.title}'? This will also cancel any scheduled reminder alarms.", color = TextSecondaryLight) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTask(task.taskId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityUrgent)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondaryLight)
                }
            }
        )
    }
}
