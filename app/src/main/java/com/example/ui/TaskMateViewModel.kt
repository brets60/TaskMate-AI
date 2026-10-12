package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatMessage
import com.example.ai.AiDailyPlanResult
import com.example.ai.AiParsedTask
import com.example.ai.GeminiAiService
import com.example.audio.FocusAudioPlayer
import com.example.audio.FocusMusicService
import com.example.audio.PlayerState
import com.example.data.model.FocusTrack
import com.example.data.model.*
import com.example.data.repository.TaskMateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object CreateEditTask : Screen("create_edit_task")
    object TaskDetail : Screen("task_detail")
    object CalendarView : Screen("calendar")
    object AiAssistant : Screen("ai_assistant")
    object DailyPlan : Screen("daily_plan")
    object Productivity : Screen("productivity")
    object NotificationCenter : Screen("notification_center")
    object ProfileSettings : Screen("profile_settings")
    object FocusMusic : Screen("focus_music")
}

data class TaskFilterState(
    val searchQuery: String = "",
    val selectedTab: String = "ALL", // ALL, TODAY, UPCOMING, HIGH_PRIO, COMPLETED
    val selectedCategory: TaskCategory? = null,
    val sortBy: String = "DUE_DATE" // DUE_DATE, PRIORITY, TITLE
)

data class ProductivityStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionRate: Float = 0f,
    val currentStreakDays: Int = 4,
    val weeklyCompleted: Int = 12
)

class TaskMateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskMateRepository(application)
    private val aiService = GeminiAiService()
    private val musicService = FocusMusicService(application)
    private val audioPlayer = FocusAudioPlayer(application)
    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    private val _isGeneratingMusic = MutableStateFlow(false)
    val isGeneratingMusic: StateFlow<Boolean> = _isGeneratingMusic.asStateFlow()

    private val _generatedTracks = MutableStateFlow<List<FocusTrack>>(
        listOf(
            FocusTrack(
                id = "track_alpha_focus",
                title = "Deep Alpha Flow (432Hz)",
                prompt = "Binaural alpha harmonic focus tone for deep study and sustained concentration",
                model = "lyria-3-clip-preview",
                filePath = java.io.File(application.cacheDir, "music/track_alpha_focus.wav").absolutePath,
                durationSeconds = 30,
                category = "Deep Work"
            ),
            FocusTrack(
                id = "track_lofi_study",
                title = "Late Night Lo-Fi Study Chill",
                prompt = "Warm analog lo-fi chillhop beats with vinyl crackle and mellow Rhodes piano",
                model = "lyria-3-clip-preview",
                filePath = java.io.File(application.cacheDir, "music/track_lofi_study.wav").absolutePath,
                durationSeconds = 30,
                category = "Lo-Fi Study"
            )
        )
    )
    val generatedTracks: StateFlow<List<FocusTrack>> = _generatedTracks.asStateFlow()

    // Navigation State - Users must register / authenticate first
    private val _currentScreen = MutableStateFlow<Screen>(
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) Screen.Home else Screen.Auth
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>()

    // Current selected task for detail/edit
    private val _selectedTaskId = MutableStateFlow<String?>(null)
    val selectedTaskId: StateFlow<String?> = _selectedTaskId.asStateFlow()

    // User State
    val currentUser: StateFlow<UserEntity?> = repository.getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All Tasks Flow
    val allTasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders Flow
    val allReminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter State
    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState: StateFlow<TaskFilterState> = _filterState.asStateFlow()

    // Filtered Tasks
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(allTasks, filterState) { tasks, filter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfToday = cal.timeInMillis

        var result = tasks

        // Tab filter
        result = when (filter.selectedTab) {
            "TODAY" -> result.filter { it.dueDateTime <= endOfToday && it.status != TaskStatus.COMPLETED.name }
            "UPCOMING" -> result.filter { it.dueDateTime > endOfToday && it.status != TaskStatus.COMPLETED.name }
            "HIGH_PRIO" -> result.filter { (it.priority == TaskPriority.HIGH.name || it.priority == TaskPriority.URGENT.name) && it.status != TaskStatus.COMPLETED.name }
            "COMPLETED" -> result.filter { it.status == TaskStatus.COMPLETED.name }
            else -> result
        }

        // Category filter
        if (filter.selectedCategory != null) {
            result = result.filter { it.category == filter.selectedCategory.name }
        }

        // Search filter
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase()
            result = result.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }
        }

        // Sort
        when (filter.sortBy) {
            "PRIORITY" -> result.sortedByDescending { TaskPriority.fromString(it.priority).level }
            "TITLE" -> result.sortedBy { it.title.lowercase() }
            else -> result.sortedBy { it.dueDateTime }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Productivity Stats
    val productivityStats: StateFlow<ProductivityStats> = allTasks.map { tasks ->
        val now = System.currentTimeMillis()
        val completed = tasks.count { it.status == TaskStatus.COMPLETED.name }
        val pending = tasks.count { it.status != TaskStatus.COMPLETED.name }
        val overdue = tasks.count { it.status != TaskStatus.COMPLETED.name && it.dueDateTime < now }
        val rate = if (tasks.isNotEmpty()) (completed.toFloat() / tasks.size) else 0f
        val streak = if (completed > 0) 1 else 0

        ProductivityStats(
            totalTasks = tasks.size,
            completedTasks = completed,
            pendingTasks = pending,
            overdueTasks = overdue,
            completionRate = rate,
            currentStreakDays = streak,
            weeklyCompleted = completed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductivityStats())

    // AI Assistant State
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                isUser = false,
                text = "Welcome to TaskMate! I'm here to help you organize your tasks, plan your daily schedule, and stay focused. Tap '+' to create your first task, or ask me anything!"
            )
        )
    )
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _pendingTaskToConfirm = MutableStateFlow<AiParsedTask?>(null)
    val pendingTaskToConfirm: StateFlow<AiParsedTask?> = _pendingTaskToConfirm.asStateFlow()

    // Daily Plan State
    private val _dailyPlan = MutableStateFlow<AiDailyPlanResult?>(null)
    val dailyPlan: StateFlow<AiDailyPlanResult?> = _dailyPlan.asStateFlow()

    private val _isDailyPlanLoading = MutableStateFlow(false)
    val isDailyPlanLoading: StateFlow<Boolean> = _isDailyPlanLoading.asStateFlow()

    // Navigation functions
    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        val isAuthenticated = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        if (screenBackStack.isNotEmpty()) {
            val prev = screenBackStack.removeAt(screenBackStack.size - 1)
            // If user is not authenticated, they should not be popped back into main screens
            if (!isAuthenticated && prev != Screen.Auth) {
                _currentScreen.value = Screen.Auth
                return true
            }
            _currentScreen.value = prev
            return true
        }
        val fallback = if (isAuthenticated) Screen.Home else Screen.Auth
        if (_currentScreen.value != fallback) {
            _currentScreen.value = fallback
            return true
        }
        return false
    }

    fun clearBackStack() {
        screenBackStack.clear()
    }

    fun openTaskDetail(taskId: String) {
        _selectedTaskId.value = taskId
        navigateTo(Screen.TaskDetail)
    }

    fun openEditTask(taskId: String?) {
        _selectedTaskId.value = taskId
        navigateTo(Screen.CreateEditTask)
    }

    // Filter controls
    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun setSelectedTab(tab: String) {
        _filterState.value = _filterState.value.copy(selectedTab = tab)
    }

    fun setSelectedCategory(category: TaskCategory?) {
        val next = if (_filterState.value.selectedCategory == category) null else category
        _filterState.value = _filterState.value.copy(selectedCategory = next)
    }

    fun setSortBy(sort: String) {
        _filterState.value = _filterState.value.copy(sortBy = sort)
    }

    // Task Actions
    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskComplete(task.taskId, task.status)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            if (_selectedTaskId.value == taskId) {
                _selectedTaskId.value = null
                navigateBack()
            }
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            val exists = allTasks.value.any { it.taskId == task.taskId }
            if (exists) {
                repository.updateTask(task)
            } else {
                repository.insertTask(task)
            }
            navigateBack()
        }
    }

    fun rescheduleTask(taskId: String, newDueMillis: Long) {
        viewModelScope.launch {
            repository.rescheduleTask(taskId, newDueMillis)
        }
    }

    // AI Chat & Assistant
    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AiChatMessage(isUser = true, text = prompt)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val (reply, parsedTask) = aiService.processUserPrompt(prompt, allTasks.value)
                val botMsg = AiChatMessage(
                    isUser = false,
                    text = reply,
                    parsedTask = parsedTask
                )
                _aiMessages.value = _aiMessages.value + botMsg
                if (parsedTask != null && !parsedTask.isAmbiguous) {
                    _pendingTaskToConfirm.value = parsedTask
                }
            } catch (e: Exception) {
                _aiMessages.value = _aiMessages.value + AiChatMessage(
                    isUser = false,
                    text = "Sorry, I encountered an issue processing that: ${e.message}"
                )
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun confirmSaveAiTask(parsed: AiParsedTask) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = parsed.title,
                description = parsed.description,
                dueDate = parsed.dueDateTimeMillis,
                isCompleted = false,
                category = parsed.category.name,
                priority = parsed.priority.name,
                dueDateTime = parsed.dueDateTimeMillis,
                estimatedDurationMinutes = parsed.estimatedDurationMinutes,
                reminderMinutesBefore = parsed.reminderMinutesBefore
            )
            repository.insertTask(newTask)
            _pendingTaskToConfirm.value = null
            _aiMessages.value = _aiMessages.value + AiChatMessage(
                isUser = false,
                text = "✅ Great! '${parsed.title}' has been scheduled and added to your tasks with reminders enabled."
            )
        }
    }

    fun dismissAiTaskConfirmation() {
        _pendingTaskToConfirm.value = null
    }

    // Daily Plan
    fun generateDailyPlan() {
        _isDailyPlanLoading.value = true
        viewModelScope.launch {
            try {
                val workHours = Pair(
                    currentUser.value?.workStartHour ?: 9,
                    currentUser.value?.workEndHour ?: 18
                )
                val plan = aiService.generateDailyPlan(allTasks.value, workHours)
                _dailyPlan.value = plan
            } catch (e: Exception) {
                // fall back handled inside service
            } finally {
                _isDailyPlanLoading.value = false
            }
        }
    }

    // User settings
    fun updateUserSettings(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    // AI Focus Music
    fun generateFocusMusic(prompt: String, category: String = "Lo-Fi Study", useShortClip: Boolean = true) {
        if (prompt.isBlank()) return
        _isGeneratingMusic.value = true
        viewModelScope.launch {
            try {
                val newTrack = musicService.generateFocusTrack(prompt, category, useShortClip)
                _generatedTracks.value = listOf(newTrack) + _generatedTracks.value
                audioPlayer.playTrack(newTrack.filePath, newTrack.title)
            } catch (e: Exception) {
                // error handled
            } finally {
                _isGeneratingMusic.value = false
            }
        }
    }

    fun playFocusTrack(track: FocusTrack) {
        audioPlayer.playTrack(track.filePath, track.title)
    }

    fun togglePlayPause() {
        audioPlayer.togglePlayPause()
    }

    fun seekAudio(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
    }

    fun toggleLoopAudio() {
        audioPlayer.toggleLooping()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
