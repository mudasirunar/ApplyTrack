package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "job_applications")
data class JobApplication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val companyName: String? = null,
    val role: String? = null,
    val platform: String? = null,
    val status: String = "Applied",
    val jobDescription: String? = null,
    val notes: String? = null,
    val url: String? = null,
    val email: String? = null,
    val statusHistory: List<StatusHistoryEntry>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
