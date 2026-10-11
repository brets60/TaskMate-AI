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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.Screen
import com.example.ui.TaskMateViewModel
import com.example.ui.components.CategoryBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()

    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance())
    }

    // Days in week strip around selected date
    val weekDays = remember(selectedCalendar) {
        val list = mutableListOf<Calendar>()
        val startCal = selectedCalendar.clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..13) {
            val day = startCal.clone() as Calendar
            day.add(Calendar.DAY_OF_YEAR, i)
            list.add(day)
        }
        list
    }

    // Tasks for the selected day
    val selectedDayTasks = remember(selectedCalendar, allTasks) {
        val sYear = selectedCalendar.get(Calendar.YEAR)
        val sDay = selectedCalendar.get(Calendar.DAY_OF_YEAR)

        allTasks.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dueDateTime }
            c.get(Calendar.YEAR) == sYear && c.get(Calendar.DAY_OF_YEAR) == sDay
        }.sortedBy { it.dueDateTime }
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendar & Agenda", fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                        Text(
                            text = monthYearFormat.format(selectedCalendar.time),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { selectedCalendar = Calendar.getInstance() }) {
                        Icon(Icons.Default.Today, contentDescription = "Today", tint = PrimaryBlue)
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
                .padding(horizontal = 16.dp)
        ) {
            // Horizontal Date Strip (Light, Friendly Day Buttons)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weekDays) { dayCal ->
                    val isSelected = dayCal.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                            dayCal.get(Calendar.DAY_OF_YEAR) == selectedCalendar.get(Calendar.DAY_OF_YEAR)

                    val isToday = dayCal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                            dayCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

                    // Count tasks on this day
                    val taskCount = allTasks.count {
                        val c = Calendar.getInstance().apply { timeInMillis = it.dueDateTime }
                        c.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                    }

                    Surface(
                        modifier = Modifier
                            .width(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCalendar = dayCal.clone() as Calendar },
                        color = if (isSelected) PrimaryBlue else if (isToday) PastelBlueBg else SurfaceLight,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else if (isToday) PastelBlueBorder else BorderLight),
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayNameFormat.format(dayCal.time),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayNumFormat.format(dayCal.time),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (taskCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else PrimaryBlue)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day Agenda Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(selectedCalendar.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )

                Button(
                    onClick = { viewModel.openEditTask(null) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Agenda timeline list
            if (selectedDayTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PastelMintBg,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Free Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your agenda is completely open for this day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(selectedDayTasks, key = { it.taskId }) { task ->
                        AgendaItemCard(
                            task = task,
                            onToggle = { viewModel.toggleTaskComplete(task) },
                            onClick = { viewModel.openTaskDetail(task.taskId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaItemCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(task.dueDateTime))
    val priority = TaskPriority.fromString(task.priority)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) SurfaceVariantLight.copy(alpha = 0.6f) else SurfaceLight
        ),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlueDark
                )
                Text(
                    text = "${task.estimatedDurationMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryLight
                )
            }

            VerticalDivider(modifier = Modifier.height(36.dp), color = BorderLight)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) TextSecondaryLight else TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityBadge(priority)
                }
            }

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) AccentEmerald else PrimaryBlue
                )
            }
        }
    }
}
