package com.example.data.repository

import com.example.data.local.JobApplicationDao
import com.example.model.DeletedJob
import com.example.model.JobApplication
import com.example.model.StatusHistoryEntry
import com.example.model.Attachment
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.UUID

// Custom task await implementation to avoid direct dependency on play-services-coroutines
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Firestore Task failed"))
        }
    }
}

class JobRepositoryImpl(
    private val dao: JobApplicationDao
) : JobRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    override fun getAllApplications(): Flow<List<JobApplication>> {
        return dao.getAllApplicationsFlow()
    }

    override fun getApplicationById(id: Long): Flow<JobApplication?> {
        return dao.getApplicationFlowById(id)
    }

    override suspend fun getApplicationByIdDirect(id: Long): JobApplication? {
        return dao.getApplicationById(id)
    }

    override suspend fun saveApplication(application: JobApplication): Long {
        val updatedApp = application.copy(updatedAt = System.currentTimeMillis())
        return dao.insertApplication(updatedApp)
    }

    override suspend fun deleteApplication(id: Long) {
        val app = dao.getApplicationById(id)
        if (app != null) {
            // Track deletion for remote sync
            dao.insertDeletedJob(DeletedJob(uuid = app.uuid))
            // Delete from Room table
            dao.deleteApplication(app)
        }
    }

    override fun isFirebaseConfigured(): Boolean {
        return firestore != null
    }

    // --- Core Sync Optimization Layer: Last-write-wins & Delete-Overrides ---
    override suspend fun uploadLocalChanges(): Result<Unit> = kotlin.runCatching {
        val fs = firestore ?: throw IllegalStateException("Firebase Firestore is not configured or initialized.")
        
        // 1. Process local deletions first (Deletes always override remote)
        val deletedJobs = dao.getAllDeletedJobs()
        for (deletedJob in deletedJobs) {
            try {
                fs.collection("job_applications")
                    .document(deletedJob.uuid)
                    .delete()
                    .await()
                // Successfully deleted remotely, clean local tracking
                dao.removeDeletedJobTracking(deletedJob.uuid)
            } catch (e: Exception) {
                // If deletion fails (e.g. no internet), keep local tracking to retry later
            }
        }

        // 2. Upload local updates (Last-write-wins)
        val localJobs = dao.getAllApplicationsList()
        for (job in localJobs) {
            try {
                // Check if document exists and get its update timestamp
                val docRef = fs.collection("job_applications").document(job.uuid)
                val docSnapshot = docRef.get().await()
                
                var shouldUpload = true
                if (docSnapshot.exists()) {
                    val remoteUpdatedAt = docSnapshot.getLong("updatedAt") ?: 0L
                    if (remoteUpdatedAt > job.updatedAt) {
                        shouldUpload = false // Remote is newer, fetchRemoteUpdates will sync it
                    }
                }

                if (shouldUpload) {
                    val data = hashMapOf(
                        "uuid" to job.uuid,
                        "companyName" to job.companyName,
                        "role" to job.role,
                        "platform" to job.platform,
                        "status" to job.status,
                        "jobDescription" to job.jobDescription,
                        "notes" to job.notes,
                        "url" to job.url,
                        "email" to job.email,
                        "statusHistory" to job.statusHistory?.map { entry ->
                            hashMapOf(
                                "status" to entry.status,
                                "timestamp" to entry.timestamp
                            )
                        },
                        "resume" to job.resume?.let {
                            hashMapOf("fileName" to it.fileName, "originalName" to it.originalName)
                        },
                        "coverLetter" to job.coverLetter?.let {
                            hashMapOf("fileName" to it.fileName, "originalName" to it.originalName)
                        },
                        "additionalDocument" to job.additionalDocument?.let {
                            hashMapOf("fileName" to it.fileName, "originalName" to it.originalName)
                        },
                        "screenshots" to job.screenshots?.map {
                            hashMapOf("fileName" to it.fileName, "originalName" to it.originalName)
                        },
                        "createdAt" to job.createdAt,
                        "updatedAt" to job.updatedAt
                    )
                    docRef.set(data).await()
                }
            } catch (e: Exception) {
                // Ignore individual document failure and continue sync process
            }
        }
    }

    override suspend fun fetchRemoteUpdates(): Result<Unit> = kotlin.runCatching {
        val fs = firestore ?: throw IllegalStateException("Firebase Firestore is not configured or initialized.")
        
        // 1. Fetch all applications from Firestore
        val querySnapshot = fs.collection("job_applications").get().await()
        val deletedUuids = dao.getAllDeletedJobs().map { it.uuid }.toSet()

        for (doc in querySnapshot.documents) {
            val uuid = doc.getString("uuid") ?: doc.id
            
            // If deleted locally, local delete always overrides remote database
            if (deletedUuids.contains(uuid)) {
                try {
                    fs.collection("job_applications").document(uuid).delete().await()
                    dao.removeDeletedJobTracking(uuid)
                } catch (e: Exception) {
                    // Ignore deletion errors, tracking remains for retry
                }
                continue
            }

            // Extract remote values defensively
            val companyName = doc.getString("companyName")
            val role = doc.getString("role")
            val platform = doc.getString("platform")
            val status = doc.getString("status") ?: "Applied"
            val jobDescription = doc.getString("jobDescription")
            val notes = doc.getString("notes")
            val url = doc.getString("url")
            val email = doc.getString("email")
            val rawHistory = doc.get("statusHistory")
            val statusHistory = when (rawHistory) {
                is List<*> -> {
                    rawHistory.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        val status = map?.get("status") as? String
                        val timestamp = map?.get("timestamp") as? Long
                        if (status != null && timestamp != null) {
                            StatusHistoryEntry(status, timestamp)
                        } else null
                    }
                }
                is String -> {
                    rawHistory.split(",").mapNotNull { item ->
                        val parts = item.split("|")
                        if (parts.size == 2) {
                            val status = parts[0]
                            val timestamp = parts[1].toLongOrNull()
                            if (timestamp != null) {
                                StatusHistoryEntry(status, timestamp)
                            } else null
                        } else null
                    }
                }
                else -> null
            }
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

            val resume = (doc.get("resume") as? Map<*, *>)?.let {
                val fileName = it["fileName"] as? String
                val originalName = it["originalName"] as? String
                if (fileName != null && originalName != null) Attachment(fileName, originalName) else null
            }
            val coverLetter = (doc.get("coverLetter") as? Map<*, *>)?.let {
                val fileName = it["fileName"] as? String
                val originalName = it["originalName"] as? String
                if (fileName != null && originalName != null) Attachment(fileName, originalName) else null
            }
            val additionalDocument = (doc.get("additionalDocument") as? Map<*, *>)?.let {
                val fileName = it["fileName"] as? String
                val originalName = it["originalName"] as? String
                if (fileName != null && originalName != null) Attachment(fileName, originalName) else null
            }
            val rawScreenshots = doc.get("screenshots")
            val screenshots = when (rawScreenshots) {
                is List<*> -> {
                    rawScreenshots.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        val fileName = map?.get("fileName") as? String
                        val originalName = map?.get("originalName") as? String
                        if (fileName != null && originalName != null) {
                            Attachment(fileName, originalName)
                        } else null
                    }
                }
                else -> null
            }

            val localJob = dao.getApplicationByUuid(uuid)
            
            if (localJob == null) {
                // Insert new remote job application
                val incomingJob = JobApplication(
                    uuid = uuid,
                    companyName = companyName,
                    role = role,
                    platform = platform,
                    status = status,
                    jobDescription = jobDescription,
                    notes = notes,
                    url = url,
                    email = email,
                    statusHistory = statusHistory,
                    resume = resume,
                    coverLetter = coverLetter,
                    additionalDocument = additionalDocument,
                    screenshots = screenshots,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
                dao.insertApplication(incomingJob)
            } else {
                // Check last-write-wins comparison
                if (updatedAt > localJob.updatedAt) {
                    val updatedJob = localJob.copy(
                        companyName = companyName,
                        role = role,
                        platform = platform,
                        status = status,
                        jobDescription = jobDescription,
                        notes = notes,
                        url = url,
                        email = email,
                        statusHistory = statusHistory,
                        resume = resume,
                        coverLetter = coverLetter,
                        additionalDocument = additionalDocument,
                        screenshots = screenshots,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    dao.insertApplication(updatedJob)
                }
            }
        }
    }
}
