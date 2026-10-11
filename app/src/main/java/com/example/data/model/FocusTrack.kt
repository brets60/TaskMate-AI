package com.example.data.model

import java.util.UUID

data class FocusTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val prompt: String,
    val model: String, // "lyria-3-clip-preview" or "lyria-3-pro-preview"
    val filePath: String,
    val durationSeconds: Int = 30,
    val category: String = "Lo-Fi Study",
    val createdAt: Long = System.currentTimeMillis()
)
