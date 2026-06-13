package com.example.data.local

import androidx.room.*
import com.example.model.DeletedJob
import com.example.model.JobApplication
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {

    @Query("SELECT * FROM job_applications ORDER BY createdAt DESC")
    fun getAllApplicationsFlow(): Flow<List<JobApplication>>

    @Query("SELECT * FROM job_applications WHERE id = :id LIMIT 1")
    fun getApplicationFlowById(id: Long): Flow<JobApplication?>

    @Query("SELECT * FROM job_applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: Long): JobApplication?

    @Query("SELECT * FROM job_applications WHERE uuid = :uuid LIMIT 1")
    suspend fun getApplicationByUuid(uuid: String): JobApplication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: JobApplication): Long

    @Update
    suspend fun updateApplication(application: JobApplication)

    @Delete
    suspend fun deleteApplication(application: JobApplication)

    @Query("DELETE FROM job_applications WHERE id = :id")
    suspend fun deleteApplicationById(id: Long)

    @Query("SELECT * FROM job_applications")
    suspend fun getAllApplicationsList(): List<JobApplication>

    // --- Deleted Jobs Tracking Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedJob(deletedJob: DeletedJob)

    @Query("SELECT * FROM deleted_jobs")
    suspend fun getAllDeletedJobs(): List<DeletedJob>

    @Query("DELETE FROM deleted_jobs WHERE uuid = :uuid")
    suspend fun removeDeletedJobTracking(uuid: String)

    @Query("DELETE FROM deleted_jobs")
    suspend fun clearDeletedJobs()

    @Query("DELETE FROM job_applications")
    suspend fun deleteAllApplications()
}
