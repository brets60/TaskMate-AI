package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.FirestoreTaskRepository
import com.example.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface FirestoreTasksUiState {
    object Loading : FirestoreTasksUiState
    data class Success(val tasks: List<Task>) : FirestoreTasksUiState
    data class Error(val message: String) : FirestoreTasksUiState
}

class FirestoreTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirestoreTaskRepository(application)
    private val auth = FirebaseAuth.getInstance()

    // Internal operation loading state
    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

    // Error message banner state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Real-time observed tasks from Firestore
    val tasksState: StateFlow<FirestoreTasksUiState> = flow {
        if (auth.currentUser == null) {
            emit(FirestoreTasksUiState.Error("Please sign in with Google to access Firestore tasks."))
            return@flow
        }
        repository.observeTasks()
            .map<List<Task>, FirestoreTasksUiState> { tasks ->
                FirestoreTasksUiState.Success(tasks)
            }
            .catch { error ->
                emit(FirestoreTasksUiState.Error(error.localizedMessage ?: "Failed to observe tasks from Firestore"))
            }
            .collect { emit(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FirestoreTasksUiState.Loading
    )

    /**
     * Create a new task in Firestore
     */
    fun createTask(
        title: String,
        description: String = "",
        category: String = "GENERAL",
        priority: String = "MEDIUM",
        dueDateTime: Long = System.currentTimeMillis() + 3600000L,
        durationMinutes: Int = 30,
        reminderMinutes: Int = 15,
        onSuccess: (String) -> Unit = {}
    ) {
        if (title.isBlank()) {
            _errorMessage.value = "Task title cannot be empty"
            return
        }

        viewModelScope.launch {
            _isOperating.value = true
            _errorMessage.value = null
            try {
                val newTask = Task(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    status = "PENDING",
                    dueDateTime = dueDateTime,
                    estimatedDurationMinutes = durationMinutes,
                    reminderMinutesBefore = reminderMinutes
                )
                val newId = repository.createTask(newTask)
                onSuccess(newId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to create task"
            } finally {
                _isOperating.value = false
            }
        }
    }

    /**
     * Update an existing task in Firestore
     */
    fun updateTask(task: Task, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isOperating.value = true
            _errorMessage.value = null
            try {
                repository.updateTask(task)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update task"
            } finally {
                _isOperating.value = false
            }
        }
    }

    /**
     * Delete a task from Firestore
     */
    fun deleteTask(taskId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isOperating.value = true
            _errorMessage.value = null
            try {
                repository.deleteTask(taskId)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete task"
            } finally {
                _isOperating.value = false
            }
        }
    }

    /**
     * Toggle completion status of a task
     */
    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                repository.toggleTaskComplete(task)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to toggle task status"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
