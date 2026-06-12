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
import java.util.Calendar

enum class SyncState {
    IDLE, SYNCING, SUCCESS, ERROR
}

class JobViewModel(private val repository: JobRepository) : ViewModel() {

    // Current Search & Filter states
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("All") // "All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Month"
    val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1..12
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString()) // e.g. "2026"
    val sortByLatest = MutableStateFlow(true) // true: latest first, false: oldest first
    val isSearchFocused = MutableStateFlow(false)

    // Detailed application state (for detail/edit view)
    private val _selectedApplication = MutableStateFlow<JobApplication?>(null)
    val selectedApplication: StateFlow<JobApplication?> = _selectedApplication.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

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
        filterParamsFlow
    ) { apps, params ->
        var result = apps

        // Apply Search (Search by company or role)
        if (params.query.isNotBlank()) {
            result = result.filter {
                it.companyName?.contains(params.query, ignoreCase = true) == true ||
                it.role?.contains(params.query, ignoreCase = true) == true
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
    val dashboardStats: StateFlow<DashboardStats> = repository.getAllApplications()
        .map { apps ->
            DashboardStats(
                total = apps.size,
                interviews = apps.count { it.status.equals("Interview", ignoreCase = true) },
                rejected = apps.count { it.status.equals("Rejected", ignoreCase = true) },
                offers = apps.count { it.status.equals("Offer", ignoreCase = true) },
                saved = apps.count { it.status.equals("Saved", ignoreCase = true) || it.status.equals("Applied", ignoreCase = true) }
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

    fun deleteSelectedApplication(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
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
        if (isFirebaseConfigured) {
            viewModelScope.launch {
                repository.uploadLocalChanges()
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
    private val repository: JobRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JobViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JobViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class representation")
    }
}
