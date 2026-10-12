package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.UserEntity
import com.example.ui.Screen
import com.example.ui.TaskMateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    viewModel: TaskMateViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val user by viewModel.currentUser.collectAsState()

    var notificationsEnabled by remember(user) { mutableStateOf(user?.notificationsEnabled ?: true) }
    var soundEnabled by remember(user) { mutableStateOf(user?.soundEnabled ?: true) }
    var vibrateEnabled by remember(user) { mutableStateOf(user?.vibrateEnabled ?: true) }
    var defaultReminder by remember(user) { mutableStateOf(user?.defaultReminderMinutes ?: 15) }
    var themeMode by remember(user) { mutableStateOf(user?.themeMode ?: "SYSTEM") }

    fun saveUserChanges() {
        user?.let {
            val updated = it.copy(
                notificationsEnabled = notificationsEnabled,
                soundEnabled = soundEnabled,
                vibrateEnabled = vibrateEnabled,
                defaultReminderMinutes = defaultReminder,
                themeMode = themeMode
            )
            viewModel.updateUserSettings(updated)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold, color = TextPrimaryLight) },
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
            // Profile Card (Light surface)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = PastelBlueBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val name = firebaseUser?.displayName ?: user?.displayName ?: "TU"
                            Text(
                                text = name.take(2).uppercase(),
                                color = PastelBlueText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = firebaseUser?.displayName ?: user?.displayName ?: "TaskMate User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = firebaseUser?.email ?: user?.email?.ifEmpty { "Not signed in" } ?: "Not signed in",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                        Text(
                            text = if (firebaseUser != null) "✓ Account Synced" else "Local Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (firebaseUser != null) AccentEmerald else PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Notification Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reminder Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = TextPrimaryLight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Smart Task Reminders", fontWeight = FontWeight.Medium, color = TextPrimaryLight)
                            Text("Scheduled alarm notifications", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                saveUserChanges()
                            }
                        )
                    }

                    HorizontalDivider(color = BorderLight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sound Alerts", color = TextPrimaryLight)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                saveUserChanges()
                            }
                        )
                    }

                    HorizontalDivider(color = BorderLight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vibration", color = TextPrimaryLight)
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = {
                                vibrateEnabled = it
                                saveUserChanges()
                            }
                        )
                    }
                }
            }

            // Theme Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = TextPrimaryLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PastelBlueBg else SurfaceLight,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        themeMode = mode
                                        saveUserChanges()
                                    }
                            ) {
                                Text(
                                    text = label,
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
            }

            // Navigation to other sections
            Button(
                onClick = { viewModel.navigateTo(Screen.Productivity) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextPrimaryLight),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Productivity Insights & Charts", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = { viewModel.navigateTo(Screen.NotificationCenter) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight, contentColor = TextPrimaryLight),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notification Center", fontWeight = FontWeight.Medium)
            }

            // Account & Sign Out button
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Auth) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_screen_nav_button")
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Account Settings & Sign Out", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
