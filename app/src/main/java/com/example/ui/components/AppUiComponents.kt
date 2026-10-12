package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiParsedTask
import com.example.data.model.*
import com.example.ui.Screen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriorityBadge(priority: TaskPriority, modifier: Modifier = Modifier) {
    val (color, bg) = when (priority) {
        TaskPriority.URGENT -> Pair(PastelRoseText, PastelRoseBg)
        TaskPriority.HIGH -> Pair(PastelPeachText, PastelPeachBg)
        TaskPriority.MEDIUM -> Pair(PastelBlueText, PastelBlueBg)
        TaskPriority.LOW -> Pair(PastelMintText, PastelMintBg)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = priority.title,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CategoryBadge(category: TaskCategory, modifier: Modifier = Modifier) {
    val (icon, color, bg) = when (category) {
        TaskCategory.WORK -> Triple(Icons.Default.Work, PastelBlueText, PastelBlueBg)
        TaskCategory.PERSONAL -> Triple(Icons.Default.Person, PastelPurpleText, PastelPurpleBg)
        TaskCategory.STUDY -> Triple(Icons.Default.School, PastelMintText, PastelMintBg)
        TaskCategory.HEALTH -> Triple(Icons.Default.Favorite, PastelRoseText, PastelRoseBg)
        TaskCategory.FINANCE -> Triple(Icons.Default.AccountBalanceWallet, PastelAmberText, PastelAmberBg)
        TaskCategory.GENERAL -> Triple(Icons.Default.FormatListBulleted, TextSecondaryLight, SurfaceVariantLight)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = category.title,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val taskStatus = TaskStatus.fromString(status)
    val (label, color, bg, icon) = when (taskStatus) {
        TaskStatus.COMPLETED -> listOf(
            "Completed",
            AccentEmerald,
            PastelMintBg,
            Icons.Default.CheckCircle
        )
        TaskStatus.IN_PROGRESS -> listOf(
            "In Progress",
            PrimaryBlueDark,
            PastelBlueBg,
            Icons.Default.Timelapse
        )
        TaskStatus.CANCELLED -> listOf(
            "Cancelled",
            PastelRoseText,
            PastelRoseBg,
            Icons.Default.Cancel
        )
        TaskStatus.PENDING -> listOf(
            "Pending",
            PastelAmberText,
            PastelAmberBg,
            Icons.Default.Schedule
        )
    }

    Surface(
        color = bg as Color,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag("status_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = null,
                tint = color as Color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label as String,
                color = color as Color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("task_status_text")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissTaskItem(
    task: TaskEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("swipe_to_dismiss_${task.taskId}"),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PriorityUrgent)
                    .padding(horizontal = 20.dp)
                    .testTag("swipe_delete_background_${task.taskId}"),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Delete",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        content = {
            content()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItemCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = task.status == TaskStatus.COMPLETED.name
    val isOverdue = !isCompleted && task.dueDateTime < System.currentTimeMillis()
    val priority = TaskPriority.fromString(task.priority)
    val category = TaskCategory.fromString(task.category)

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.taskId}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) SurfaceVariantLight.copy(alpha = 0.6f) else SurfaceLight
        ),
        border = BorderStroke(1.dp, if (isCompleted) Color.Transparent else BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Friendly circular checkbox
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onToggleComplete() }
                    .testTag("task_toggle_${task.taskId}"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = if (isCompleted) PastelMintBg else Color.Transparent,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isCompleted) AccentEmerald else if (isOverdue) PriorityUrgent else BorderLight.copy(alpha = 1.5f)
                    )
                ) {
                    if (isCompleted) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark incomplete",
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title and Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompleted) TextSecondaryLight else TextPrimaryLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("task_title")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        status = task.status,
                        modifier = Modifier.testTag("task_status_${task.taskId}")
                    )
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Badges row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PriorityBadge(priority)
                    CategoryBadge(category)

                    // Due time text
                    val dateFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(task.dueDateTime))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.ErrorOutline else Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (isOverdue) PriorityUrgent else TextSecondaryLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isOverdue) "Overdue" else dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) PriorityUrgent else TextSecondaryLight,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // More Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Task options",
                        tint = TextSecondaryLight
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Details") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Task", color = PriorityUrgent) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PriorityUrgent) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskMateBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar"),
        containerColor = SurfaceLight,
        tonalElevation = 3.dp
    ) {
        val navItems = listOf(
            Triple(Screen.Home, "Home", Icons.Default.Home to Icons.Outlined.Home),
            Triple(Screen.Tasks, "Tasks", Icons.Default.Checklist to Icons.Outlined.Checklist),
            Triple(Screen.CalendarView, "Calendar", Icons.Default.CalendarMonth to Icons.Outlined.CalendarMonth),
            Triple(Screen.AiAssistant, "Assistant", Icons.Default.AutoAwesome to Icons.Outlined.AutoAwesome),
            Triple(Screen.ProfileSettings, "Profile", Icons.Default.Person to Icons.Outlined.Person)
        )

        navItems.forEach { (screen, label, icons) ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlueDark,
                    selectedTextColor = PrimaryBlueDark,
                    indicatorColor = PastelBlueBg,
                    unselectedIconColor = TextSecondaryLight,
                    unselectedTextColor = TextSecondaryLight
                )
            )
        }
    }
}

@Composable
fun AiTaskConfirmationCard(
    parsedTask: AiParsedTask,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_task_confirmation_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PastelBlueBg),
        border = BorderStroke(1.dp, PastelBlueBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryBlueDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Task Ready to Add",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryBlueDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = parsedTask.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )

            if (parsedTask.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parsedTask.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val timeStr = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(parsedTask.dueDateTimeMillis))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SurfaceLight,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimaryLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                PriorityBadge(parsedTask.priority)
                CategoryBadge(parsedTask.category)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondaryLight)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlueDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_ai_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to My Tasks", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
