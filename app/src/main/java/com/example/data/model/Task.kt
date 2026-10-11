package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "GENERAL",
    val priority: String = "MEDIUM",
    val status: String = "PENDING",
    val dueDateTime: Long = 0L,
    val estimatedDurationMinutes: Int = 30,
    val reminderMinutesBefore: Int = 15,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    @ServerTimestamp
    val completedAt: Timestamp? = null
)
