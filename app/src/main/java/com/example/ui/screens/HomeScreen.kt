package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.ui.Screen
import com.example.ui.TaskMateViewModel
import com.example.ui.components.SwipeToDismissTaskItem
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val stats by viewModel.productivityStats.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedQuickFilter by remember { mutableStateOf("TODAY") }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Filter tasks based on selected filter
    val displayedTasks = remember(allTasks, selectedQuickFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endOfToday = cal.timeInMillis

        when (selectedQuickFilter) {
            "TODAY" -> allTasks.filter {
                it.dueDateTime <= endOfToday && it.status != TaskStatus.COMPLETED.name
            }.sortedBy { it.dueDateTime }
            "URGENT" -> allTasks.filter {
                (it.priority == TaskPriority.URGENT.name || it.priority == TaskPriority.HIGH.name) &&
                        it.status != TaskStatus.COMPLETED.name
            }.sortedBy { it.dueDateTime }
            "WORK" -> allTasks.filter {
                it.category == TaskCategory.WORK.name && it.status != TaskStatus.COMPLETED.name
            }.sortedBy { it.dueDateTime }
            "PERSONAL" -> allTasks.filter {
                it.category == TaskCategory.PERSONAL.name && it.status != TaskStatus.COMPLETED.name
            }.sortedBy { it.dueDateTime }
            else -> allTasks.filter { it.status != TaskStatus.COMPLETED.name }.sortedBy { it.dueDateTime }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$greetingText, ${user?.displayName?.split(" ")?.firstOrNull() ?: "there"} 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.NotificationCenter) },
                        modifier = Modifier.testTag("notification_center_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (stats.overdueTasks > 0) {
                                    Badge(containerColor = PriorityUrgent) {
                                        Text("${stats.overdueTasks}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = TextPrimaryLight)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Productivity) },
                        modifier = Modifier.testTag("stats_header_button")
                    ) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Productivity Insights", tint = TextPrimaryLight)
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Auth) },
                        modifier = Modifier.testTag("auth_header_button")
                    ) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Account & Security", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openEditTask(null) },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("create_task_fab"),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Task", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Light, Friendly Daily Schedule Card (Clean & User-friendly, not an AI dark banner)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_briefing_card")
                        .clickable { viewModel.navigateTo(Screen.DailyPlan) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelBlueBg),
                    border = BorderStroke(1.dp, PastelBlueBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TODAY'S SCHEDULE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PastelBlueText,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PastelBlueBorder)
                            ) {
                                Text(
                                    text = "${displayedTasks.size} tasks",
                                    color = PastelBlueText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (stats.overdueTasks > 0) {
                                "You have ${stats.overdueTasks} overdue tasks. Let's get back on track!"
                            } else if (displayedTasks.isEmpty()) {
                                "You're all clear for today! Tap below to plan your week or add a new goal."
                            } else {
                                "Organize your day with smooth time blocks and scheduled breaks."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryLight,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.DailyPlan) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("View Smart Schedule", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(Screen.AiAssistant) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PrimaryBlueDark
                                ),
                                border = BorderStroke(1.dp, PastelBlueBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlueDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Assistant", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Quick Stats Row (Light, Gentle Pastel Containers)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LightStatCard(
                        title = "To Do",
                        count = stats.pendingTasks,
                        bgColor = PastelBlueBg,
                        textColor = PastelBlueText,
                        icon = Icons.Default.PendingActions,
                        modifier = Modifier.weight(1f)
                    )
                    LightStatCard(
                        title = "Done",
                        count = stats.completedTasks,
                        bgColor = PastelMintBg,
                        textColor = PastelMintText,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                    LightStatCard(
                        title = "Overdue",
                        count = stats.overdueTasks,
                        bgColor = PastelRoseBg,
                        textColor = PastelRoseText,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    LightStatCard(
                        title = "Streak",
                        count = stats.currentStreakDays,
                        suffix = "d",
                        bgColor = PastelAmberBg,
                        textColor = PastelAmberText,
                        icon = Icons.Default.LocalFireDepartment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Filter Chips Row (All, Today, Urgent, Work, Personal)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )

                        TextButton(onClick = { viewModel.navigateTo(Screen.Tasks) }) {
                            Text("See All", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf(
                            "TODAY" to "Today",
                            "URGENT" to "Important",
                            "WORK" to "Work",
                            "PERSONAL" to "Personal",
                            "ALL" to "All Active"
                        )
                        items(filters) { (key, label) ->
                            val isSelected = selectedQuickFilter == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PastelBlueBg else SurfaceLight,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                ),
                                modifier = Modifier.clickable { selectedQuickFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Task List for Selected Filter
            if (displayedTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_state_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PastelBlueBg,
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddTask,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (allTasks.isEmpty()) "No Tasks Yet" else "All Caught Up!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (allTasks.isEmpty()) {
                                    "Your task list is empty. Tap the button below to add your very first task or schedule."
                                } else {
                                    "No tasks matching this filter. Switch filters or create a new task."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.openEditTask(null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Task", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                items(displayedTasks, key = { it.taskId }) { task ->
                    SwipeToDismissTaskItem(
                        task = task,
                        onDelete = {
                            viewModel.deleteTask(task.taskId)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "\"${task.title}\" deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreTask(task)
                                }
                            }
                        }
                    ) {
                        TaskItemCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskComplete(task) },
                            onClick = { viewModel.openTaskDetail(task.taskId) },
                            onDelete = { viewModel.deleteTask(task.taskId) }
                        )
                    }
                }
            }

            // Quick Helpful Assistant Shortcuts (Friendly, light pills)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Assistant Prompts",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val prompts = listOf(
                            "Plan my day with breaks" to Icons.Default.Schedule,
                            "What should I do first?" to Icons.Default.HelpOutline,
                            "Remind me to study tomorrow at 7 PM" to Icons.Default.School
                        )

                        prompts.forEach { (prompt, icon) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        viewModel.navigateTo(Screen.AiAssistant)
                                        viewModel.sendAiPrompt(prompt)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceVariantLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimaryLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LightStatCard(
    title: String,
    count: Int,
    suffix: String = "",
    bgColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$count$suffix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
