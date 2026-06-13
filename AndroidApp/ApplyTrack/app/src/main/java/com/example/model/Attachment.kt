package com.example.model

data class Attachment(
    val fileName: String,      // Unique name stored locally (e.g. UUID + extension)
    val originalName: String  // Original filename uploaded (e.g. resume_2026.pdf)
)
