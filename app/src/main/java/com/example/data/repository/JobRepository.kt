package com.example.data.repository

import com.example.model.JobApplication
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getAllApplications(): Flow<List<JobApplication>>
    fun getApplicationById(id: Long): Flow<JobApplication?>
    suspend fun getApplicationByIdDirect(id: Long): JobApplication?
    suspend fun saveApplication(application: JobApplication): Long
    suspend fun deleteApplication(id: Long)
    
    // Sync Status check
    fun isFirebaseConfigured(): Boolean

    // Sync Operations
    suspend fun uploadLocalChanges(): Result<Unit>
    suspend fun fetchRemoteUpdates(): Result<Unit>
}
