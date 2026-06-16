package com.example.sync

import com.example.data.local.JobApplicationDao
import com.example.model.DeletedJob
import com.example.model.JobApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

// --- Fake DAO Implementation ---
class FakeJobApplicationDao : JobApplicationDao {
    val applications = mutableMapOf<String, JobApplication>()
    val deletedJobs = mutableListOf<DeletedJob>()

    override fun getAllApplicationsFlow(): Flow<List<JobApplication>> = flowOf(applications.values.toList())
    override fun getApplicationFlowById(id: Long): Flow<JobApplication?> = flowOf(applications.values.find { it.id == id })
    override suspend fun getApplicationById(id: Long): JobApplication? = applications.values.find { it.id == id }
    override suspend fun getApplicationByUuid(uuid: String): JobApplication? = applications[uuid]

    override suspend fun insertApplication(application: JobApplication): Long {
        val id = if (application.id == 0L) (applications.size + 1).toLong() else application.id
        val appWithId = application.copy(id = id)
        applications[application.uuid] = appWithId
        return id
    }

    override suspend fun updateApplication(application: JobApplication) {
        applications[application.uuid] = application
    }

    override suspend fun deleteApplication(application: JobApplication) {
        applications.remove(application.uuid)
    }

    override suspend fun deleteApplicationById(id: Long) {
        val app = applications.values.find { it.id == id }
        if (app != null) applications.remove(app.uuid)
    }

    override suspend fun getAllApplicationsList(): List<JobApplication> = applications.values.toList()

    override suspend fun insertDeletedJob(deletedJob: DeletedJob) {
        deletedJobs.removeAll { it.uuid == deletedJob.uuid }
        deletedJobs.add(deletedJob)
    }

    override suspend fun getAllDeletedJobs(): List<DeletedJob> = deletedJobs.toList()

    override suspend fun removeDeletedJobTracking(uuid: String) {
        deletedJobs.removeAll { it.uuid == uuid }
    }

    override suspend fun clearDeletedJobs() {
        deletedJobs.clear()
    }

    override suspend fun deleteAllApplications() {
        applications.clear()
    }

    override suspend fun getDirtyApplications(): List<JobApplication> {
        return applications.values.filter { it.updatedAt > it.lastSyncedAt }
    }

    override suspend fun updateLastSyncedAt(uuid: String, lastSyncedAt: Long) {
        val app = applications[uuid]
        if (app != null) {
            applications[uuid] = app.copy(lastSyncedAt = lastSyncedAt)
        }
    }
}

// --- Fake Remote Service (Simulating Firestore collection) ---
class FakeRemoteService {
    val remoteApplications = mutableMapOf<String, JobApplication>()
    var throwsNetworkError = false

    suspend fun upload(job: JobApplication) {
        if (throwsNetworkError) throw IOException("Failed to connect to Firestore")
        remoteApplications[job.uuid] = job
    }

    suspend fun delete(uuid: String) {
        if (throwsNetworkError) throw IOException("Failed to connect to Firestore")
        remoteApplications.remove(uuid)
    }

    suspend fun fetchAll(): List<JobApplication> {
        if (throwsNetworkError) throw IOException("Failed to connect to Firestore")
        return remoteApplications.values.toList()
    }
}

// --- Replicated Sync Execution Logic (Core Business Logic) ---
class SyncEngine(
    private val dao: JobApplicationDao,
    private val remoteService: FakeRemoteService
) {
    suspend fun uploadLocalChanges(): Result<Unit> = kotlin.runCatching {
        // 1. Process deletions
        val deletedJobs = dao.getAllDeletedJobs()
        for (deletedJob in deletedJobs) {
            try {
                remoteService.delete(deletedJob.uuid)
                dao.removeDeletedJobTracking(deletedJob.uuid)
            } catch (e: Exception) {
                // Let it retry on next sync
            }
        }

        // 2. Upload dirty updates
        val dirtyJobs = dao.getDirtyApplications()
        for (job in dirtyJobs) {
            try {
                remoteService.upload(job)
                dao.updateLastSyncedAt(job.uuid, job.updatedAt)
            } catch (e: Exception) {
                // Let it retry on next sync
            }
        }
    }

    suspend fun fetchRemoteUpdates(): Result<Unit> = kotlin.runCatching {
        val remoteList = remoteService.fetchAll()
        val deletedUuids = dao.getAllDeletedJobs().map { it.uuid }.toSet()

        for (remoteJob in remoteList) {
            val uuid = remoteJob.uuid

            if (deletedUuids.contains(uuid)) {
                try {
                    remoteService.delete(uuid)
                    dao.removeDeletedJobTracking(uuid)
                } catch (e: Exception) {}
                continue
            }

            val localJob = dao.getApplicationByUuid(uuid)

            if (localJob == null) {
                val incomingJob = remoteJob.copy(lastSyncedAt = remoteJob.updatedAt)
                dao.insertApplication(incomingJob)
            } else {
                if (remoteJob.updatedAt > localJob.updatedAt) {
                    val updatedJob = remoteJob.copy(
                        id = localJob.id,
                        lastSyncedAt = remoteJob.updatedAt
                    )
                    dao.insertApplication(updatedJob)
                } else if (remoteJob.updatedAt == localJob.updatedAt && localJob.lastSyncedAt < localJob.updatedAt) {
                    dao.updateLastSyncedAt(uuid, localJob.updatedAt)
                }
            }
        }
    }
}

// --- Test Data Generator ---
object SyncTestDataGenerator {
    fun generateFakeJob(
        index: Int,
        updatedAt: Long = 1000L,
        lastSyncedAt: Long = 0L,
        companyName: String? = null,
        role: String? = null
    ): JobApplication {
        return JobApplication(
            id = index.toLong(),
            uuid = "uuid_$index",
            companyName = companyName ?: "Company $index",
            role = role ?: "Role $index",
            platform = "Platform $index",
            status = "Applied",
            createdAt = 1000L,
            updatedAt = updatedAt,
            lastSyncedAt = lastSyncedAt
        )
    }

    fun generateFakeJobs(
        count: Int,
        dirtyRatio: Double = 0.5,
        baseTime: Long = 1000L
    ): List<JobApplication> {
        val list = mutableListOf<JobApplication>()
        for (i in 1..count) {
            val isDirty = (i / count.toDouble()) <= dirtyRatio
            val lastSynced = if (isDirty) 0L else baseTime
            list.add(
                generateFakeJob(
                    index = i,
                    updatedAt = baseTime,
                    lastSyncedAt = lastSynced
                )
            )
        }
        return list
    }
}
