package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TaskCategory
import com.example.ui.Screen
import com.example.ui.TaskMateViewModel
import com.example.ui.components.SwipeToDismissTaskItem
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskMateViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val filterState by viewModel.filterState.collectAsState()
    val tasks by viewModel.filteredTasks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Tasks (${tasks.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_tasks_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort tasks", tint = TextPrimaryLight)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Due Date") },
                                onClick = {
                                    viewModel.setSortBy("DUE_DATE")
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Priority") },
                                onClick = {
                                    viewModel.setSortBy("PRIORITY")
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PriorityHigh, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Title") },
                                onClick = {
                                    viewModel.setSortBy("TITLE")
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openEditTask(null) },
                        modifier = Modifier.testTag("add_task_header_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Task", tint = PrimaryBlue)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Light Clean Search Bar
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search tasks or notes...", color = TextSecondaryLight) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryLight) },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextSecondaryLight)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceLight,
                    focusedContainerColor = SurfaceLight,
                    unfocusedBorderColor = BorderLight,
                    focusedBorderColor = PrimaryBlue
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Friendly Filter Tabs Row
            val tabs = listOf(
                "ALL" to "All",
                "TODAY" to "Today",
                "UPCOMING" to "Upcoming",
                "HIGH_PRIO" to "Priority",
                "COMPLETED" to "Done"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tabs) { (tabKey, tabLabel) ->
                    val isSelected = filterState.selectedTab == tabKey
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PastelBlueBg else SurfaceLight,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) PrimaryBlue else BorderLight
                        ),
                        modifier = Modifier.clickable { viewModel.setSelectedTab(tabKey) }
                    ) {
                        Text(
                            text = tabLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAll = filterState.selectedCategory == null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAll) SurfaceVariantLight else SurfaceLight,
                        border = BorderStroke(1.dp, if (isAll) PrimaryBlueLight else BorderLight),
                        modifier = Modifier.clickable { viewModel.setSelectedCategory(null) }
                    ) {
                        Text(
                            text = "All Categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAll) PrimaryBlueDark else TextSecondaryLight,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                items(TaskCategory.values()) { cat ->
                    val isSelected = filterState.selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PastelBlueBg else SurfaceLight,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                        modifier = Modifier.clickable { viewModel.setSelectedCategory(cat) }
                    ) {
                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PrimaryBlueDark else TextSecondaryLight,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Task List or Empty State
            if (tasks.isEmpty()) {
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
                                color = PastelBlueBg,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (filterState.searchQuery.isNotBlank()) "No tasks match '${filterState.searchQuery}'" else "No tasks found here",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a task to get started.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.openEditTask(null) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Task", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("task_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tasks, key = { it.taskId }) { task ->
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
            }
        }
    }
}

/**
 * Stateless scrollable task list displaying tasks with title and status for each.
 */
@Composable
fun TaskListScreen(
    tasks: List<com.example.data.model.TaskEntity>,
    onToggleComplete: (com.example.data.model.TaskEntity) -> Unit = {},
    onClick: (com.example.data.model.TaskEntity) -> Unit = {},
    onDelete: (com.example.data.model.TaskEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Tasks Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your task list is empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("task_list")
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(tasks, key = { it.taskId }) { task ->
                SwipeToDismissTaskItem(
                    task = task,
                    onDelete = { onDelete(task) }
                ) {
                    TaskItemCard(
                        task = task,
                        onToggleComplete = { onToggleComplete(task) },
                        onClick = { onClick(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }
        }
    }
}
