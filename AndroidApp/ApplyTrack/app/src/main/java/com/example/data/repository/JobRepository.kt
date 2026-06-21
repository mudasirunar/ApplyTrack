package com.example.data.repository

import com.example.model.JobApplication
import kotlinx.coroutines.flow.Flow

data class ImportResult(
    val importedCount: Int,
    val updatedCount: Int,
    val ignoredCount: Int
)

interface JobRepository {
    fun getAllApplications(): Flow<List<JobApplication>>
    fun getApplicationById(id: Long): Flow<JobApplication?>
    suspend fun getApplicationByIdDirect(id: Long): JobApplication?
    suspend fun saveApplication(application: JobApplication): Long
    suspend fun deleteApplication(id: Long)
    suspend fun deleteApplications(ids: List<Long>)
    
    // Sync Status check
    fun isFirebaseConfigured(): Boolean

    // Sync Operations
    suspend fun uploadLocalChanges(): Result<Unit>
    suspend fun fetchRemoteUpdates(): Result<Int>

    suspend fun deleteAllApplications()
    suspend fun importBackup(applications: List<JobApplication>, overwriteConflicts: Boolean): ImportResult
    suspend fun restoreApplication(application: JobApplication)
}
