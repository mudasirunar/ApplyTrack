package com.example.data.sync

import android.content.Context
import com.example.auth.AuthManager
import com.example.data.local.JobApplicationDao
import com.example.data.repository.JobRepository
import com.example.model.Attachment
import com.example.model.JobApplication
import com.example.model.StatusHistoryEntry
import com.example.ui.SyncState
import com.example.utils.AppTheme
import com.example.utils.PreferencesHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import com.example.utils.AttachmentHelper

class SyncManagerImpl(
    private val context: Context,
    private val repository: JobRepository,
    private val dao: JobApplicationDao,
    private val preferencesHelper: PreferencesHelper,
    private val authManager: AuthManager
) : SyncManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var jobApplicationsListener: ListenerRegistration? = null
    
    private val supabaseStorageHelper = SupabaseStorageHelper()

    private val _downloadingFiles = MutableStateFlow<Set<String>>(emptySet())
    override val downloadingFiles: StateFlow<Set<String>> = _downloadingFiles.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    override val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    private var activeUserId: String? = null

    override fun startSync() {
        scope.launch {
            authManager.currentUserFlow.collectLatest { user ->
                val newUid = user?.uid
                if (newUid != activeUserId) {
                    stopActiveListeners()
                    activeUserId = newUid
                    
                    if (newUid != null && repository.isFirebaseConfigured()) {
                        startJobApplicationsListener(newUid)
                        runFullSync()
                    }
                }
            }
        }
    }

    override fun stopSync() {
        stopActiveListeners()
        scope.coroutineContext.cancelChildren()
    }

    private fun stopActiveListeners() {
        jobApplicationsListener?.remove()
        jobApplicationsListener = null
    }

    override fun triggerUpload() {
        if (preferencesHelper.isAutoSyncEnabled() && repository.isFirebaseConfigured()) {
            scope.launch {
                repository.uploadLocalChanges()
            }
        }
    }

    override suspend fun runFullSync(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!repository.isFirebaseConfigured()) {
            _syncState.value = SyncState.ERROR
            _syncErrorMessage.value = "Firestore is not configured. Add google-services.json to sync!"
            return@withContext Result.failure(IllegalStateException("Firestore is not configured."))
        }
        
        _syncState.value = SyncState.SYNCING
        _syncErrorMessage.value = null
        
        try {
            val uploadResult = repository.uploadLocalChanges()
            if (uploadResult.isFailure) {
                _syncState.value = SyncState.ERROR
                _syncErrorMessage.value = "Failed to upload local changes: ${uploadResult.exceptionOrNull()?.message}"
                return@withContext uploadResult
            }
            
            val fetchResult = repository.fetchRemoteUpdates()
            if (fetchResult.isFailure) {
                _syncState.value = SyncState.ERROR
                _syncErrorMessage.value = "Failed to fetch remote updates: ${fetchResult.exceptionOrNull()?.message}"
                return@withContext fetchResult
            }
            
            _syncState.value = SyncState.SUCCESS
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            _syncErrorMessage.value = e.localizedMessage ?: "Synchronization failed"
            Result.failure(e)
        }
    }

    override suspend fun migrateLocalDataToCloud(): Unit = withContext(Dispatchers.IO) {
        val userId = activeUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        if (!repository.isFirebaseConfigured()) return@withContext
        
        try {
            val firestore = FirebaseFirestore.getInstance()
            val localJobs = dao.getAllApplicationsList()
            
            for (job in localJobs) {
                val docRef = firestore.collection("users")
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
                docRef.set(data).await()

                // Upload files to Supabase Storage if they exist locally
                val uploads = listOfNotNull(
                    job.resume?.let { "resumes" to it.fileName },
                    job.coverLetter?.let { "cover_letters" to it.fileName },
                    job.additionalDocument?.let { "additional_documents" to it.fileName }
                ) + (job.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())

                uploads.forEach { (type, fileName) ->
                    val localFile = AttachmentHelper.getAttachmentFile(context, fileName)
                    if (localFile.exists()) {
                        supabaseStorageHelper.uploadFile(userId, type, fileName, localFile)
                    }
                }
            }
            
            runFullSync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startJobApplicationsListener(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val collectionRef = firestore.collection("users")
            .document(userId)
            .collection("job_applications")
            
        jobApplicationsListener = collectionRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshots == null) return@addSnapshotListener
            
            scope.launch(Dispatchers.IO) {
                val deletedUuids = dao.getAllDeletedJobs().map { it.uuid }.toSet()
                
                for (change in snapshots.documentChanges) {
                    val doc = change.document
                    val uuid = doc.getString("uuid") ?: doc.id
                    
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            if (deletedUuids.contains(uuid)) {
                                try {
                                    collectionRef.document(uuid).delete().await()
                                    dao.removeDeletedJobTracking(uuid)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                continue
                            }
                            
                            val companyName = doc.getString("companyName")
                            val role = doc.getString("role")
                            val platform = doc.getString("platform")
                            val status = doc.getString("status") ?: "Applied"
                            val jobDescription = doc.getString("jobDescription")
                            val notes = doc.getString("notes")
                            val url = doc.getString("url")
                            val email = doc.getString("email")
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            val remoteUpdatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            
                            val resume = parseAttachment(doc.get("resume"))
                            val coverLetter = parseAttachment(doc.get("coverLetter"))
                            val additionalDocument = parseAttachment(doc.get("additionalDocument"))
                            val screenshots = parseScreenshotsList(doc.get("screenshots"))
                            val statusHistory = parseStatusHistory(doc.get("statusHistory"))
                            
                            val localJob = dao.getApplicationByUuid(uuid)
                            var shouldDownloadFiles = false
                            if (localJob == null) {
                                val newJob = JobApplication(
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
                                    updatedAt = remoteUpdatedAt
                                )
                                dao.insertApplication(newJob)
                                shouldDownloadFiles = true
                            } else if (remoteUpdatedAt > localJob.updatedAt) {
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
                                    updatedAt = remoteUpdatedAt
                                )
                                dao.insertApplication(updatedJob)
                                shouldDownloadFiles = true
                            }

                            if (shouldDownloadFiles) {
                                val downloads = listOfNotNull(
                                    resume?.let { "resumes" to it.fileName },
                                    coverLetter?.let { "cover_letters" to it.fileName },
                                    additionalDocument?.let { "additional_documents" to it.fileName }
                                ) + (screenshots?.map { "screenshots" to it.fileName } ?: emptyList())

                                scope.launch(Dispatchers.IO) {
                                    downloads.forEach { (type, fileName) ->
                                        val destFile = AttachmentHelper.getAttachmentFile(context, fileName)
                                        if (!destFile.exists()) {
                                            _downloadingFiles.update { it + fileName }
                                            try {
                                                supabaseStorageHelper.downloadFile(userId, type, fileName, destFile)
                                            } finally {
                                                _downloadingFiles.update { it - fileName }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val localJob = dao.getApplicationByUuid(uuid)
                            if (localJob != null) {
                                val filesToDelete = listOfNotNull(
                                    localJob.resume?.let { "resumes" to it.fileName },
                                    localJob.coverLetter?.let { "cover_letters" to it.fileName },
                                    localJob.additionalDocument?.let { "additional_documents" to it.fileName }
                                ) + (localJob.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())

                                scope.launch(Dispatchers.IO) {
                                    filesToDelete.forEach { (type, fileName) ->
                                        supabaseStorageHelper.deleteFile(userId, type, fileName)
                                    }
                                }

                                dao.deleteApplication(localJob)
                                dao.removeDeletedJobTracking(uuid)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseAttachment(value: Any?): Attachment? {
        val map = value as? Map<*, *> ?: return null
        val fileName = map["fileName"] as? String
        val originalName = map["originalName"] as? String
        return if (fileName != null && originalName != null) Attachment(fileName, originalName) else null
    }

    private fun parseScreenshotsList(value: Any?): List<Attachment>? {
        val list = value as? List<*> ?: return null
        return list.mapNotNull { item ->
            val map = item as? Map<*, *>
            val fileName = map?.get("fileName") as? String
            val originalName = map?.get("originalName") as? String
            if (fileName != null && originalName != null) Attachment(fileName, originalName) else null
        }
    }

    private fun parseStatusHistory(value: Any?): List<StatusHistoryEntry>? {
        val list = value as? List<*> ?: return null
        return list.mapNotNull { item ->
            val map = item as? Map<*, *>
            val status = map?.get("status") as? String
            val timestamp = map?.get("timestamp") as? Long
            if (status != null && timestamp != null) StatusHistoryEntry(status, timestamp) else null
        }
    }
}
