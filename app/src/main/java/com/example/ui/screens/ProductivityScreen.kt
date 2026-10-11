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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.ui.TaskMateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val stats by viewModel.productivityStats.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    val completedPercentage = (stats.completionRate * 100).toInt()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Productivity Insights", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
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
            // Main Score Card (Light Pastel Blue)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PastelBlueBg),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TASK COMPLETION RATE",
                        style = MaterialTheme.typography.labelMedium,
                        color = PastelBlueText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$completedPercentage%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${stats.completedTasks} of ${stats.totalTasks} total tasks completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryLight
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { stats.completionRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentEmerald,
                        trackColor = Color.White
                    )
                }
            }

            // Streak & Weekly Completion Stats (Light pastel cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelAmberBg),
                    border = BorderStroke(1.dp, PastelAmberBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AccentAmber)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stats.currentStreakDays} Days",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PastelAmberText
                        )
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = PastelAmberText.copy(alpha = 0.8f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelMintBg),
                    border = BorderStroke(1.dp, PastelMintBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentEmerald)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stats.weeklyCompleted}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PastelMintText
                        )
                        Text(
                            text = "Completed This Week",
                            style = MaterialTheme.typography.labelSmall,
                            color = PastelMintText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Category Distribution breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tasks by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TaskCategory.values().forEach { cat ->
                        val count = allTasks.count { it.category == cat.name }
                        val fraction = if (allTasks.isNotEmpty()) count.toFloat() / allTasks.size else 0f

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryLight,
                                modifier = Modifier.width(75.dp)
                            )
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = PrimaryBlue,
                                trackColor = SurfaceVariantLight
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryLight
                            )
                        }
                    }
                }
            }

            // Friendly Productivity Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelPurpleBg),
                border = BorderStroke(1.dp, PastelPurpleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Productivity Tip",
                            fontWeight = FontWeight.Bold,
                            color = PastelPurpleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (stats.overdueTasks > 0) {
                                "You have ${stats.overdueTasks} tasks awaiting action. Rescheduling them to realistic time blocks relieves stress and maintains focus."
                            } else {
                                "Great momentum! Tackling high-priority tasks in the morning keeps your productivity streak alive."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryLight
                        )
                    }
                }
            }
        }
    }
}
