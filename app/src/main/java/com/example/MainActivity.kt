package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.Screen
import com.example.ui.TaskMateViewModel
import com.example.ui.components.TaskMateBottomNavigation
import com.example.ui.screens.*
import com.example.ui.theme.TaskMateTheme

import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import com.example.auth.FirebaseAuthManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val extraTaskId = intent?.getStringExtra("EXTRA_TASK_ID")

        // Attempt silent background Google Sign-In with Credential Manager
        val credentialManager = CredentialManager.create(this)
        FirebaseAuthManager.attemptAutoSignIn(
            context = this,
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                android.util.Log.d("MainActivity", "Silent auto-sign-in succeeded for ${user.uid}")
            },
            onUnauthenticated = {
                android.util.Log.d("MainActivity", "User unauthenticated or first launch")
            },
            scope = lifecycleScope
        )

        setContent {
            val viewModel: TaskMateViewModel = viewModel()

            LaunchedEffect(extraTaskId) {
                if (!extraTaskId.isNullOrBlank()) {
                    viewModel.openTaskDetail(extraTaskId)
                }
            }

            val currentUser by viewModel.currentUser.collectAsState()
            val themeMode = currentUser?.themeMode ?: "LIGHT"
            val darkTheme = when (themeMode) {
                "DARK" -> true
                else -> false
            }

            TaskMateTheme(darkTheme = darkTheme) {
                val currentScreen by viewModel.currentScreen.collectAsState()

                val showBottomNav = currentScreen in listOf(
                    Screen.Home,
                    Screen.Tasks,
                    Screen.CalendarView,
                    Screen.AiAssistant,
                    Screen.ProfileSettings
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            TaskMateBottomNavigation(
                                currentScreen = currentScreen,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = if (showBottomNav) innerPadding.calculateBottomPadding() else 0.dp
                            )
                    ) {
                        when (currentScreen) {
                            Screen.Splash, Screen.Home -> HomeScreen(viewModel)
                            Screen.Tasks -> TaskListScreen(viewModel)
                            Screen.CreateEditTask -> CreateEditTaskScreen(viewModel)
                            Screen.TaskDetail -> TaskDetailScreen(viewModel)
                            Screen.CalendarView -> CalendarScreen(viewModel)
                            Screen.AiAssistant -> AiAssistantScreen(viewModel)
                            Screen.DailyPlan -> DailyPlanScreen(viewModel)
                            Screen.Productivity -> ProductivityScreen(viewModel)
                            Screen.NotificationCenter -> NotificationCenterScreen(viewModel)
                            Screen.ProfileSettings -> ProfileSettingsScreen(viewModel)
                            Screen.Auth -> AuthScreen(viewModel)
                            Screen.FocusMusic -> FocusMusicScreen(viewModel)
                            Screen.Onboarding -> HomeScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
