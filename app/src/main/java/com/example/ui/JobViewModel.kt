package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.JobRepository
import com.example.model.JobApplication
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.example.model.StatusHistoryEntry
import com.example.model.Attachment
import com.example.utils.PreferencesHelper
import com.example.utils.AppTheme
import com.example.utils.BackupHelper
import com.example.utils.AttachmentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import android.content.Context
import java.util.Calendar

enum class SyncState {
    IDLE, SYNCING, SUCCESS, ERROR
}

class JobViewModel(
    private val repository: JobRepository,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {

    // Theme & Preferences State
    val appTheme = preferencesHelper.themeFlow
    val autoSyncEnabled = preferencesHelper.autoSyncFlow

    fun setAppTheme(theme: AppTheme) {
        preferencesHelper.setTheme(theme)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        preferencesHelper.setAutoSyncEnabled(enabled)
    }

    // Current Search & Filter states
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("All") // "All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Month"
    val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1..12
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString()) // e.g. "2026"
    val sortByLatest = MutableStateFlow(true) // true: latest first, false: oldest first
    val isSearchFocused = MutableStateFlow(false)
    val isFabVisible = MutableStateFlow(true)

    // Detailed application state (for detail/edit view)
    private val _selectedApplication = MutableStateFlow<JobApplication?>(null)
    val selectedApplication: StateFlow<JobApplication?> = _selectedApplication.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _pendingDeleteJob = MutableStateFlow<JobApplication?>(null)
    val pendingDeleteJob: StateFlow<JobApplication?> = _pendingDeleteJob.asStateFlow()

    private val permanentlyDeletedIds = MutableStateFlow<Set<Long>>(emptySet())

    // Helper data class to bypass Kotlin's 5-flow combine limitation in a type-safe manner
    private data class FilterParams(
        val query: String,
        val status: String,
        val month: Int,
        val year: String,
        val sortByLatest: Boolean
    )

    private val filterParamsFlow: Flow<FilterParams> = combine(
        searchQuery,
        statusFilter,
        selectedMonth,
        selectedYear,
        sortByLatest
    ) { query, status, month, year, latest ->
        FilterParams(query, status, month, year, latest)
    }

    // Reactive State: All UI Job Applications list combined with filters, searches, months, years & sorting
    val filteredApplications: StateFlow<List<JobApplication>> = combine(
        repository.getAllApplications(),
        filterParamsFlow,
        _pendingDeleteJob,
        permanentlyDeletedIds
    ) { apps, params, pendingDelete, permanentlyDeleted ->
        val missingIds = permanentlyDeleted.filter { id -> apps.none { it.id == id } }
        if (missingIds.isNotEmpty()) {
            viewModelScope.launch {
                permanentlyDeletedIds.value = permanentlyDeletedIds.value - missingIds.toSet()
            }
        }

        var result = apps.filter { it.id != pendingDelete?.id && !permanentlyDeleted.contains(it.id) }

        // Apply Search (Search by company, role, job description, or notes)
        if (params.query.isNotBlank()) {
            result = result.filter {
                it.companyName?.contains(params.query, ignoreCase = true) == true ||
                it.role?.contains(params.query, ignoreCase = true) == true ||
                it.jobDescription?.contains(params.query, ignoreCase = true) == true ||
                it.notes?.contains(params.query, ignoreCase = true) == true
            }
        }

        // Apply Status or Month/Year Filter
        if (params.status == "Month") {
            val targetYear = params.year.toIntOrNull()
            if (targetYear != null) {
                val cal = Calendar.getInstance()
                result = result.filter { app ->
                    cal.timeInMillis = app.createdAt
                    val appMonth = cal.get(Calendar.MONTH) + 1 // 1..12
                    val appYear = cal.get(Calendar.YEAR)
                    appMonth == params.month && appYear == targetYear
                }
            }
        } else if (params.status != "All") {
            result = result.filter {
                it.status.equals(params.status, ignoreCase = true)
            }
        }

        // Apply Sort
        result = if (params.sortByLatest) {
            result.sortedByDescending { it.createdAt }
        } else {
            result.sortedBy { it.createdAt }
        }

        result
    }.onEach {
        _isInitialLoading.value = false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary Statistics Cards derived dynamically
    val dashboardStats: StateFlow<DashboardStats> = combine(
        repository.getAllApplications(),
        _pendingDeleteJob,
        permanentlyDeletedIds
    ) { apps, pendingDelete, permanentlyDeleted ->
        val filteredApps = apps.filter { it.id != pendingDelete?.id && !permanentlyDeleted.contains(it.id) }
        DashboardStats(
            total = filteredApps.size,
            interviews = filteredApps.count { it.status.equals("Interview", ignoreCase = true) },
            rejected = filteredApps.count { it.status.equals("Rejected", ignoreCase = true) },
            offers = filteredApps.count { it.status.equals("Offer", ignoreCase = true) },
            saved = filteredApps.count { it.status.equals("Saved", ignoreCase = true) || it.status.equals("Applied", ignoreCase = true) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    // Sync Status States
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    val isFirebaseConfigured = repository.isFirebaseConfigured()

    private var loadJob: Job? = null

    // Fetch and observe direct updates for detail view
    fun loadApplicationById(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getApplicationById(id).collect {
                _selectedApplication.value = it
            }
        }
    }

    fun clearSelectedApplication() {
        loadJob?.cancel()
        loadJob = null
        _selectedApplication.value = null
    }

    // Save/Insert operations
    fun saveJobApplication(
        companyName: String,
        role: String,
        platform: String,
        status: String,
        jobDescription: String,
        notes: String,
        url: String?,
        email: String?,
        timeApplied: Long,
        resume: Attachment?,
        coverLetter: Attachment?,
        additionalDocument: Attachment?,
        screenshots: List<Attachment>?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val baseApp = _selectedApplication.value ?: JobApplication()
            val oldHistory = baseApp.statusHistory ?: emptyList()
            val newHistory = if (baseApp.id == 0L) {
                listOf(StatusHistoryEntry(status, timeApplied))
            } else {
                if (baseApp.status != status) {
                    oldHistory + StatusHistoryEntry(status, System.currentTimeMillis())
                } else {
                    oldHistory.ifEmpty {
                        listOf(StatusHistoryEntry(status, baseApp.createdAt))
                    }
                }
            }
            val finalApp = baseApp.copy(
                companyName = companyName.trim().ifEmpty { null },
                role = role.trim().ifEmpty { null },
                platform = platform.trim().ifEmpty { null },
                status = status,
                jobDescription = jobDescription.trim().ifEmpty { null },
                notes = notes.trim().ifEmpty { null },
                url = url?.trim()?.ifEmpty { null },
                email = email?.trim()?.ifEmpty { null },
                statusHistory = newHistory,
                resume = resume,
                coverLetter = coverLetter,
                additionalDocument = additionalDocument,
                screenshots = screenshots,
                createdAt = if (baseApp.id == 0L) timeApplied else baseApp.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveApplication(finalApp)
            // Trigger auto upload sync if initialized
            triggerUploadSync()
            onSuccess()
        }
    }

    fun requestDeleteApplication(job: JobApplication) {
        viewModelScope.launch {
            // Commit any existing pending delete first
            _pendingDeleteJob.value?.let { commitDelete(it) }
            _pendingDeleteJob.value = job
        }
    }

    fun undoDelete() {
        _pendingDeleteJob.value = null
    }

    fun commitPendingDelete() {
        val jobToCommit = _pendingDeleteJob.value
        if (jobToCommit != null) {
            permanentlyDeletedIds.value = permanentlyDeletedIds.value + jobToCommit.id
            _pendingDeleteJob.value = null
            commitDelete(jobToCommit)
        }
    }

    private fun commitDelete(job: JobApplication) {
        viewModelScope.launch {
            repository.deleteApplication(job.id)
            triggerUploadSync()
        }
    }

    fun deleteSelectedApplication(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            permanentlyDeletedIds.value = permanentlyDeletedIds.value + id
            repository.deleteApplication(id)
            // Trigger auto upload sync if initialized
            triggerUploadSync()
            onSuccess()
        }
    }

    // --- Dynamic Background Serialization Sync Layer ---
    fun runFullSync() {
        if (!isFirebaseConfigured) {
            _syncState.value = SyncState.ERROR
            _syncErrorMessage.value = "Firebase Firestore is not configured. Drop google-services.json to sync!"
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            _syncErrorMessage.value = null

            // 1. Fetch remote changes to synchronize local db representation
            val fetchResult = repository.fetchRemoteUpdates()
            if (fetchResult.isFailure) {
                _syncState.value = SyncState.ERROR
                _syncErrorMessage.value = "Failed to fetch remote updates: ${fetchResult.exceptionOrNull()?.message}"
                return@launch
            }

            // 2. Upload local updates with merged last-write-wins priority override
            val uploadResult = repository.uploadLocalChanges()
            if (uploadResult.isFailure) {
                _syncState.value = SyncState.ERROR
                _syncErrorMessage.value = "Failed to upload local changes: ${uploadResult.exceptionOrNull()?.message}"
            } else {
                _syncState.value = SyncState.SUCCESS
            }
        }
    }

    private fun triggerUploadSync() {
        if (isFirebaseConfigured && preferencesHelper.isAutoSyncEnabled()) {
            viewModelScope.launch {
                repository.uploadLocalChanges()
            }
        }
    }

    fun clearAllLocalData(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteAllApplications()
                AttachmentHelper.clearAllAttachments(context)
            }
            // Trigger upload sync immediately to propagate the delete remotely if online
            triggerUploadSync()
            onSuccess()
        }
    }

    fun exportBackup(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val apps = repository.getAllApplications().first()
                    BackupHelper.exportBackupToZip(context, apps, uri)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun checkBackupConflicts(
        context: Context,
        uri: Uri,
        onResult: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val conflicts = withContext(Dispatchers.IO) {
                    val apps = BackupHelper.readBackupApplicationsOnly(context, uri)
                    val existing = repository.getAllApplications().first()
                    var conflictCount = 0
                    for (imported in apps) {
                        val match = existing.find { it.uuid == imported.uuid }
                        if (match != null) {
                            val importedWithLocalId = imported.copy(id = match.id)
                            if (match != importedWithLocalId) {
                                conflictCount++
                            }
                        }
                    }
                    conflictCount
                }
                onResult(conflicts)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun importBackup(
        context: Context,
        uri: Uri,
        overwrite: Boolean,
        onProgress: (String) -> Unit,
        onSuccess: (Int, Int, Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var importedCount = 0
                var updatedCount = 0
                var ignoredCount = 0
                withContext(Dispatchers.IO) {
                    withContext(Dispatchers.Main) { onProgress("Reading backup file...") }
                    val apps = BackupHelper.importBackupFromZip(context, uri)
                    
                    withContext(Dispatchers.Main) { onProgress("Restoring records...") }
                    val result = repository.importBackup(apps, overwrite)
                    importedCount = result.importedCount
                    updatedCount = result.updatedCount
                    ignoredCount = result.ignoredCount
                }
                // Sync additions
                triggerUploadSync()
                onSuccess(importedCount, updatedCount, ignoredCount)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

// Stats Holder
data class DashboardStats(
    val total: Int = 0,
    val interviews: Int = 0,
    val rejected: Int = 0,
    val offers: Int = 0,
    val saved: Int = 0
)

// ViewModel Factory Creator
class JobViewModelFactory(
    private val repository: JobRepository,
    private val preferencesHelper: PreferencesHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JobViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JobViewModel(repository, preferencesHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class representation")
    }
}
