package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ai.AiDailyScheduleBlock
import com.example.ui.TaskMateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPlanScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val plan by viewModel.dailyPlan.collectAsState()
    val isLoading by viewModel.isDailyPlanLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (plan == null) {
            viewModel.generateDailyPlan()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Daily Schedule", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateDailyPlan() },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("regenerate_plan_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-plan", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Structuring your schedule and break times...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryLight
                    )
                }
            }
        } else {
            val currentPlan = plan
            if (currentPlan == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.generateDailyPlan() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Today's Schedule", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Plan Summary card (Gentle light pastel blue)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PastelBlueBg),
                            border = BorderStroke(1.dp, PastelBlueBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Today's Focus", fontWeight = FontWeight.Bold, color = PastelBlueText)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentPlan.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryLight
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = SurfaceLight,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderLight)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = currentPlan.focusTip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryLight
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Schedule Timeline header
                    item {
                        Text(
                            text = "Time-Blocked Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    }

                    // Blocks
                    items(currentPlan.timeBlocks) { block ->
                        ScheduleBlockCard(block)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleBlockCard(block: AiDailyScheduleBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (block.isBreak) PastelMintBg else SurfaceLight
        ),
        border = BorderStroke(1.dp, if (block.isBreak) PastelMintBorder else BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (block.isBreak) PastelMintBg else PastelBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (block.isBreak) Icons.Default.Coffee else Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = if (block.isBreak) AccentEmerald else PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.taskTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryLight
                )
                if (block.notes.isNotBlank()) {
                    Text(
                        text = block.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
            }

            Surface(
                color = if (block.isBreak) PastelMintBg else PastelBlueBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (block.isBreak) PastelMintBorder else PastelBlueBorder)
            ) {
                Text(
                    text = block.timeSlot,
                    color = if (block.isBreak) PastelMintText else PastelBlueText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
