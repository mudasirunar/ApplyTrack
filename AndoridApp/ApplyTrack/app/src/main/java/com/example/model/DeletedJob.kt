package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_jobs")
data class DeletedJob(
    @PrimaryKey val uuid: String,
    val deletedAt: Long = System.currentTimeMillis()
)
