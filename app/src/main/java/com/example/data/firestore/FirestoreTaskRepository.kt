package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FirestoreTaskRepository(private val db: FirebaseFirestore) {
    // Secondary constructor resolving custom database ID from resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Real-time observation of user's tasks from Firestore
     */
    fun observeTasks(): Flow<List<Task>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        db.collection("users").document(uid).collection("tasks")
            .orderBy("dueDateTime")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Task::class.java)
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    /**
     * Get a single task by ID
     */
    suspend fun getTask(taskId: String): Task? {
        val uid = requireUserId()
        val path = "users/$uid/tasks/$taskId"
        return try {
            val doc = db.collection("users").document(uid).collection("tasks").document(taskId).get().await()
            doc.toObject(Task::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    /**
     * Create a new task document in Firestore
     */
    suspend fun createTask(task: Task): String {
        val uid = requireUserId()
        val tasksRef = db.collection("users").document(uid).collection("tasks")
        val taskWithUser = task.copy(
            userId = uid,
            createdAt = null, // @ServerTimestamp will populate on server
            updatedAt = null
        )

        return try {
            val docRef = tasksRef.add(taskWithUser).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, tasksRef.path)
            throw e
        }
    }

    /**
     * Update an existing task document in Firestore
     */
    suspend fun updateTask(task: Task) {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)

        try {
            // Setting updatedAt = null triggers @ServerTimestamp while preserving createdAt
            val updated = task.copy(
                userId = uid,
                updatedAt = null
            )
            docRef.set(updated).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Delete a task document from Firestore
     */
    suspend fun deleteTask(taskId: String) {
        val uid = requireUserId()
        require(taskId.isNotBlank()) { "Task ID must not be blank for delete" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)

        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    /**
     * Toggle completion status of a task
     */
    suspend fun toggleTaskComplete(task: Task) {
        val newStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
        val updated = task.copy(
            status = newStatus,
            completedAt = null // @ServerTimestamp if completed, or null
        )
        updateTask(updated)
    }
}
