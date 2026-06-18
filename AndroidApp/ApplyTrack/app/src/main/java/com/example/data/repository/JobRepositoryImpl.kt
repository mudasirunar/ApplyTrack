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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import android.content.Context
import com.example.utils.AttachmentHelper
import com.example.data.sync.SupabaseStorageHelper
import java.io.File
import java.util.UUID
import androidx.room.withTransaction

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
    private val context: Context,
    private val dao: JobApplicationDao
) : JobRepository {

    private val supabaseStorageHelper = SupabaseStorageHelper()

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
        val updatedApp = application.copy(
            updatedAt = System.currentTimeMillis(),
            lastSyncedAt = 0L
        )
        return dao.insertApplication(updatedApp)
    }

    override suspend fun deleteApplication(id: Long) {
        val app = dao.getApplicationById(id)
        if (app != null) {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val filesToDelete = listOfNotNull(
                    app.resume?.let { "resumes" to it.fileName },
                    app.coverLetter?.let { "cover_letters" to it.fileName },
                    app.additionalDocument?.let { "additional_documents" to it.fileName }
                ) + (app.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())
                
                kotlin.runCatching {
                    filesToDelete.forEach { (type, fileName) ->
                        supabaseStorageHelper.deleteFile(userId, type, fileName)
                    }
                }
            }
            
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
        
        // Await Firebase user session restoration (up to 3 seconds)
        var firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            for (i in 1..30) {
                kotlinx.coroutines.delay(100)
                firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) break
            }
        }
        if (firebaseUser == null || firebaseUser.isAnonymous) {
            return@runCatching
        }
        val userId = firebaseUser.uid
        
        val deletedJobs = dao.getAllDeletedJobs()
        val dirtyJobs = dao.getDirtyApplications()
        
        if (deletedJobs.isEmpty() && dirtyJobs.isEmpty()) {
            return@runCatching
        }

        // Group operations into a Firestore WriteBatch
        val batch = fs.batch()

        // 1. Process local deletions first
        for (deletedJob in deletedJobs) {
            val docRef = fs.collection("users")
                .document(userId)
                .collection("job_applications")
                .document(deletedJob.uuid)
            batch.delete(docRef)
        }

        // 2. Upload local updates (Last-write-wins dirty tracking)
        for (job in dirtyJobs) {
            val docRef = fs.collection("users")
                .document(userId)
                .collection("job_applications")
                .document(job.uuid)
            
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
            batch.set(docRef, data)
        }

        // Commit batch atomically to Firestore
        batch.commit().await()

        // Commit local SQLite updates in a transaction
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        db.withTransaction {
            for (job in dirtyJobs) {
                dao.updateLastSyncedAt(job.uuid, job.updatedAt)
            }
            for (deletedJob in deletedJobs) {
                dao.removeDeletedJobTracking(deletedJob.uuid)
            }
        }
        // Upload attachments in parallel and await completion so background workers don't get terminated early
        kotlinx.coroutines.coroutineScope {
            val uploadTasks = mutableListOf<Deferred<Unit>>()
            for (job in dirtyJobs) {
                val jobUploads = listOfNotNull(
                    job.resume?.let { "resumes" to it.fileName },
                    job.coverLetter?.let { "cover_letters" to it.fileName },
                    job.additionalDocument?.let { "additional_documents" to it.fileName }
                ) + (job.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())

                for ((type, fileName) in jobUploads) {
                    val task = async(Dispatchers.IO) {
                        try {
                            val localFile = AttachmentHelper.getAttachmentFile(context, fileName)
                            if (localFile.exists()) {
                                val existsOnCloud = supabaseStorageHelper.checkFileExists(userId, type, fileName)
                                if (!existsOnCloud) {
                                    supabaseStorageHelper.uploadFile(userId, type, fileName, localFile)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    uploadTasks.add(task)
                }
            }
            uploadTasks.awaitAll()
        }
    }

    override suspend fun fetchRemoteUpdates(): Result<Int> = kotlin.runCatching {
        val fs = firestore ?: throw IllegalStateException("Firebase Firestore is not configured or initialized.")
        
        // Await Firebase user session restoration (up to 3 seconds)
        var firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            for (i in 1..30) {
                kotlinx.coroutines.delay(100)
                firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) break
            }
        }
        if (firebaseUser == null || firebaseUser.isAnonymous) {
            return@runCatching 0
        }
        val userId = firebaseUser.uid
        
        // Fetch all applications from Firestore under the user's subcollection
        val querySnapshot = fs.collection("users")
            .document(userId)
            .collection("job_applications")
            .get()
            .await()
        
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        db.withTransaction {
            val deletedUuids = dao.getAllDeletedJobs().map { it.uuid }.toSet()
            var changesCount = 0

            for (doc in querySnapshot.documents) {
                val uuid = doc.getString("uuid") ?: doc.id
                
                // If deleted locally, local delete always overrides remote database
                if (deletedUuids.contains(uuid)) {
                    try {
                        fs.collection("users")
                            .document(userId)
                            .collection("job_applications")
                            .document(uuid)
                            .delete()
                            .await()
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
                        updatedAt = updatedAt,
                        lastSyncedAt = updatedAt
                    )
                    dao.insertApplication(incomingJob)
                    changesCount++
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
                            updatedAt = updatedAt,
                            lastSyncedAt = updatedAt
                        )
                        dao.insertApplication(updatedJob)
                        changesCount++
                    } else if (updatedAt == localJob.updatedAt && localJob.lastSyncedAt < localJob.updatedAt) {
                        dao.updateLastSyncedAt(uuid, localJob.updatedAt)
                    }
                }
            }
            changesCount
        }
    }
    override suspend fun deleteAllApplications() = withContext(Dispatchers.IO) {
        // Collect all existing local application records first
        val allApps = dao.getAllApplicationsList()
        
        // Record their deletions in the deleted_jobs table first so we propagate this wipe to Firestore on next sync
        for (app in allApps) {
            dao.insertDeletedJob(DeletedJob(uuid = app.uuid))
        }
        
        // Wipes local database table instantly
        dao.deleteAllApplications()
        
        // Clean up remote attachments asynchronously so it returns instantly and doesn't block UI thread
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            kotlin.runCatching {
                val filesToDelete = allApps.flatMap { app ->
                    listOfNotNull(
                        app.resume?.let { "resumes" to it.fileName },
                        app.coverLetter?.let { "cover_letters" to it.fileName },
                        app.additionalDocument?.let { "additional_documents" to it.fileName }
                    ) + (app.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())
                }
                
                // Launch background IO operations
                CoroutineScope(Dispatchers.IO).launch {
                    filesToDelete.forEach { (type, fileName) ->
                        supabaseStorageHelper.deleteFile(userId, type, fileName)
                    }
                }
            }
        }
    }

    override suspend fun importBackup(
        applications: List<JobApplication>,
        overwriteConflicts: Boolean
    ): ImportResult {
        val existingApps = dao.getAllApplicationsList()
        var importedCount = 0
        var updatedCount = 0
        var ignoredCount = 0
        for (importedApp in applications) {
            val match = existingApps.find { it.uuid == importedApp.uuid }
            if (match == null) {
                // New record -> insert and mark dirty for sync
                dao.insertApplication(importedApp.copy(
                    id = 0L,
                    lastSyncedAt = 0L,
                    updatedAt = System.currentTimeMillis()
                ))
                importedCount++
            } else {
                // Duplicate UUID exists -> check if there are actual content changes
                // Reset transient fields (id, sync timestamps) for content comparison
                val contentMatch = match.copy(id = 0, lastSyncedAt = 0, updatedAt = 0)
                val contentImported = importedApp.copy(id = 0, lastSyncedAt = 0, updatedAt = 0)
                
                if (contentMatch == contentImported) {
                    // Identical content -> ignore
                    ignoredCount++
                } else {
                    // Conflict detected (changes found)
                    if (overwriteConflicts) {
                        // Overwrite with backup version and mark dirty
                        dao.insertApplication(importedApp.copy(
                            id = match.id,
                            lastSyncedAt = 0L,
                            updatedAt = System.currentTimeMillis()
                        ))
                        updatedCount++
                    } else {
                        // Ignore the changes
                        ignoredCount++
                    }
                }
            }
        }
        return ImportResult(importedCount, updatedCount, ignoredCount)
    }
}
