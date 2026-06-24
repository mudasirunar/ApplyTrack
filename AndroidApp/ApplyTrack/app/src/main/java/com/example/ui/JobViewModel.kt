package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.example.data.repository.JobRepository
import com.example.model.JobApplication
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.model.StatusHistoryEntry
import com.example.model.Attachment
import com.example.utils.PreferencesHelper
import com.example.auth.AuthManager
import com.example.utils.AppTheme
import com.example.utils.BackupHelper
import com.example.utils.AttachmentHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import android.content.Context
import java.util.Calendar

enum class SortOption {
    STATUS_LATEST,
    STATUS_OLDEST,
    CREATION_LATEST,
    CREATION_OLDEST
}

enum class SyncState {
    IDLE, SYNCING, SUCCESS, ERROR
}

class JobViewModel(
    private val repository: JobRepository,
    private val preferencesHelper: PreferencesHelper,
    private val syncManager: com.example.data.sync.SyncManager,
    val authManager: AuthManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    // Theme & Preferences State
    val appTheme = preferencesHelper.themeFlow

    fun setAppTheme(theme: AppTheme) {
        preferencesHelper.setTheme(theme)
    }

    // Current Search & Filter states
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("All") // "All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Month"
    val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1..12
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString()) // e.g. "2026"
    val dashboardYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString()) // e.g. "2026"
    val selectedResume = MutableStateFlow("Select---")
    val resumeSearchQuery = MutableStateFlow("")
    val selectedPlatform = MutableStateFlow("LinkedIn")

    fun setDashboardYear(year: String) {
        dashboardYear.value = year.trim()
    }

    val sortOption = MutableStateFlow(SortOption.STATUS_LATEST)
    val isSearchFocused = MutableStateFlow(false)
    val isFabVisible = MutableStateFlow(true)
    val shouldScrollToFilter = MutableStateFlow(false)

    val dashboardScrollToTop = MutableStateFlow(0)
    val applicationsScrollToTop = MutableStateFlow(0)
    val settingsScrollToTop = MutableStateFlow(0)

    fun triggerScrollToTop(route: String) {
        when (route) {
            "dashboard" -> dashboardScrollToTop.update { it + 1 }
            "applications" -> applicationsScrollToTop.update { it + 1 }
            "settings" -> settingsScrollToTop.update { it + 1 }
        }
    }

    // Detailed application state (for detail/edit view)
    private val _selectedApplication = MutableStateFlow<JobApplication?>(null)
    val selectedApplication: StateFlow<JobApplication?> = _selectedApplication.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _pendingDeleteJobs = MutableStateFlow<List<JobApplication>>(emptyList())
    val pendingDeleteJobs: StateFlow<List<JobApplication>> = _pendingDeleteJobs.asStateFlow()

    private val applicationScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val _inFlightDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

    val isSelectionModeActive = MutableStateFlow(false)
    val selectedJobIds = MutableStateFlow<Set<Long>>(emptySet())

    fun enterSelectionMode(id: Long) {
        isSelectionModeActive.value = true
        selectedJobIds.value = setOf(id)
    }

    fun exitSelectionMode() {
        isSelectionModeActive.value = false
        selectedJobIds.value = emptySet()
    }

    fun toggleJobSelection(id: Long) {
        selectedJobIds.update { current ->
            val isCurrentlySelected = current.contains(id)
            val newSelection = if (isCurrentlySelected) current - id else current + id
            if (isCurrentlySelected && newSelection.isEmpty()) {
                isSelectionModeActive.value = false
            } else if (!isCurrentlySelected && current.isEmpty()) {
                isSelectionModeActive.value = true
            }
            newSelection
        }
    }

    fun clearJobSelection() {
        selectedJobIds.value = emptySet()
    }

    fun selectJobs(ids: List<Long>) {
        selectedJobIds.value = selectedJobIds.value + ids
    }

    fun deselectJobs(ids: List<Long>) {
        selectedJobIds.value = selectedJobIds.value - ids.toSet()
    }

    private val permanentlyDeletedIds = MutableStateFlow<Set<Long>>(emptySet())

    // Helper data class to bypass Kotlin's 5-flow combine limitation in a type-safe manner
    private data class FilterParams(
        val query: String,
        val status: String,
        val month: Int,
        val year: String,
        val sortOption: SortOption,
        val selectedResume: String,
        val selectedPlatform: String
    )

    private val filterParamsFlow: Flow<FilterParams> = combine(
        listOf<Flow<Any>>(
            searchQuery,
            statusFilter,
            selectedMonth,
            selectedYear,
            sortOption,
            selectedResume,
            selectedPlatform
        )
    ) { array ->
        FilterParams(
            query = array[0] as String,
            status = array[1] as String,
            month = array[2] as Int,
            year = array[3] as String,
            sortOption = array[4] as SortOption,
            selectedResume = array[5] as String,
            selectedPlatform = array[6] as String
        )
    }

    val isListCalculating = MutableStateFlow(false)
    private var calculationDelayJob: Job? = null

    init {
        viewModelScope.launch {
            filterParamsFlow.collect {
                startCalculationTracking()
            }
        }
    }

    private fun startCalculationTracking() {
        calculationDelayJob?.cancel()
        calculationDelayJob = viewModelScope.launch {
            delay(100L) // Wait 100ms before showing loader
            isListCalculating.value = true
        }
    }

    private fun stopCalculationTracking() {
        calculationDelayJob?.cancel()
        isListCalculating.value = false
    }

    // Reactive State: All UI Job Applications list combined with filters, searches, months, years & sorting
    val filteredApplications: StateFlow<List<JobApplication>> = combine(
        repository.getAllApplications(),
        filterParamsFlow,
        _pendingDeleteJobs,
        _inFlightDeleteIds
    ) { apps, params, pendingDelete, inFlightIds ->
        val pendingIds = pendingDelete.map { it.id }.toSet()
        var result = apps.filter { it.id !in pendingIds && it.id !in inFlightIds }

        // Apply Search (Search by company, role, job description, notes, attachment names, urls, or emails)
        if (params.query.isNotBlank()) {
            result = result.filter {
                it.companyName?.contains(params.query, ignoreCase = true) == true ||
                it.role?.contains(params.query, ignoreCase = true) == true ||
                it.jobDescription?.contains(params.query, ignoreCase = true) == true ||
                it.notes?.contains(params.query, ignoreCase = true) == true ||
                it.resume?.originalName?.contains(params.query, ignoreCase = true) == true ||
                it.coverLetter?.originalName?.contains(params.query, ignoreCase = true) == true ||
                it.additionalDocument?.originalName?.contains(params.query, ignoreCase = true) == true ||
                it.url?.contains(params.query, ignoreCase = true) == true ||
                it.email?.contains(params.query, ignoreCase = true) == true
            }
        }

        // Apply Status, Month/Year, Resume, or Platform Filter
        when (params.status) {
            "Month" -> {
                val targetYear = params.year.toIntOrNull()
                if (targetYear != null) {
                    val cal = Calendar.getInstance()
                    result = result.filter { app ->
                        val statusTimestamp = app.statusHistory?.lastOrNull()?.timestamp ?: app.createdAt
                        cal.timeInMillis = statusTimestamp
                        val appMonth = cal.get(Calendar.MONTH) + 1 // 1..12
                        val appYear = cal.get(Calendar.YEAR)
                        appMonth == params.month && appYear == targetYear
                    }
                }
            }
            "Resume" -> {
                if (params.selectedResume == "Select---") {
                    result = emptyList()
                } else {
                    result = result.filter { app ->
                        app.resume?.originalName == params.selectedResume
                    }
                }
            }
            "Platform" -> {
                val standardPlatforms = listOf("LinkedIn", "Indeed", "Email", "Website")
                if (params.selectedPlatform == "Other") {
                    result = result.filter { app ->
                        val platformName = app.platform ?: ""
                        platformName !in standardPlatforms
                    }
                } else {
                    result = result.filter { app ->
                        app.platform.equals(params.selectedPlatform, ignoreCase = true)
                    }
                }
            }
            "All" -> {
                // Show all
            }
            else -> {
                result = result.filter {
                    it.status.equals(params.status, ignoreCase = true)
                }
            }
        }

        // Apply Sort
        result = when (params.sortOption) {
            SortOption.STATUS_LATEST -> result.sortedByDescending { it.statusHistory?.lastOrNull()?.timestamp ?: it.createdAt }
            SortOption.STATUS_OLDEST -> result.sortedBy { it.statusHistory?.lastOrNull()?.timestamp ?: it.createdAt }
            SortOption.CREATION_LATEST -> result.sortedByDescending { it.createdAt }
            SortOption.CREATION_OLDEST -> result.sortedBy { it.createdAt }
        }

        result
    }.flowOn(defaultDispatcher)
    .onEach {
        _isInitialLoading.value = false
        stopCalculationTracking()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Summary Statistics Cards derived dynamically
    val dashboardAnalytics: StateFlow<DashboardAnalytics> = combine(
        repository.getAllApplications(),
        dashboardYear,
        _pendingDeleteJobs,
        _inFlightDeleteIds
    ) { apps, dbYear, pendingDelete, inFlightIds ->
        val pendingIds = pendingDelete.map { it.id }.toSet()
        val filteredApps = apps.filter { it.id !in pendingIds && it.id !in inFlightIds }
        computeDashboardAnalytics(filteredApps, dbYear)
    }.flowOn(defaultDispatcher)
    .onEach {
        _isInitialLoading.value = false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DashboardAnalytics()
    )

    private fun computeDashboardAnalytics(apps: List<JobApplication>, year: String): DashboardAnalytics {
        val total = apps.size
        val applied = apps.count { it.status.equals("Applied", ignoreCase = true) }
        val saved = apps.count { it.status.equals("Saved", ignoreCase = true) }
        val interviews = apps.count { it.status.equals("Interview", ignoreCase = true) }
        val offers = apps.count { it.status.equals("Offer", ignoreCase = true) }
        val rejected = apps.count { it.status.equals("Rejected", ignoreCase = true) }
        val responses = interviews + offers + rejected

        val successRate = if (total > 0) (offers.toFloat() / total * 100f) else 0f
        val rejectionRate = if (total > 0) (rejected.toFloat() / total * 100f) else 0f
        val interviewRate = if (total > 0) (interviews.toFloat() / total * 100f) else 0f
        val responseRate = if (total > 0) ((interviews + offers + rejected).toFloat() / total * 100f) else 0f

        // Platform analytics (group custom platforms under "Other" to align with dropdown options)
        val standardPlatforms = listOf("LinkedIn", "Indeed", "Email", "Website")
        val platforms = apps.filter { !it.platform.isNullOrBlank() }
            .groupBy { app ->
                val trimmed = app.platform!!.trim()
                standardPlatforms.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: "Other"
            }
            .map { (platform, platformApps) ->
                PlatformStat(
                    platform = platform,
                    count = platformApps.size,
                    interviewCount = platformApps.count { it.status.equals("Interview", ignoreCase = true) },
                    offerCount = platformApps.count { it.status.equals("Offer", ignoreCase = true) }
                )
            }
            .sortedByDescending { it.count }
            .take(5)

        // Resume effectiveness
        val resumeStats = apps.filter { it.resume != null }
            .groupBy { it.resume!!.originalName }
            .map { (name, resumeApps) ->
                ResumeStat(
                    resumeName = name,
                    totalUsed = resumeApps.size,
                    interviewCount = resumeApps.count { it.status.equals("Interview", ignoreCase = true) },
                    offerCount = resumeApps.count { it.status.equals("Offer", ignoreCase = true) },
                    rejectedCount = resumeApps.count { it.status.equals("Rejected", ignoreCase = true) }
                )
            }
            .sortedByDescending { it.totalUsed }

        // Monthly activity (all 12 months for the selected year)
        val cal = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val targetYear = year.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val monthlyActivity = (0 until 12).map { monthIndex ->
            val monthApps = apps.filter { app ->
                val appCal = Calendar.getInstance().apply { timeInMillis = app.createdAt }
                appCal.get(Calendar.MONTH) == monthIndex && appCal.get(Calendar.YEAR) == targetYear
            }
            val count = monthApps.size
            val appliedCount = monthApps.count { it.status.equals("Applied", ignoreCase = true) }
            val savedCount = monthApps.count { it.status.equals("Saved", ignoreCase = true) }
            val interviewCount = monthApps.count { it.status.equals("Interview", ignoreCase = true) }
            val offerCount = monthApps.count { it.status.equals("Offer", ignoreCase = true) }
            val rejectedCount = monthApps.count { it.status.equals("Rejected", ignoreCase = true) }

            MonthActivity(
                label = monthNames[monthIndex],
                count = count,
                appliedCount = appliedCount,
                savedCount = savedCount,
                interviewCount = interviewCount,
                offerCount = offerCount,
                rejectedCount = rejectedCount
            )
        }

        // Status distribution
        val statusDistribution = listOf(
            StatusSlice("Applied", applied, 0xFFFFB300),
            StatusSlice("Saved", saved, 0xFF78909C),
            StatusSlice("Interview", interviews, 0xFF4CAF50),
            StatusSlice("Offer", offers, 0xFF1E88E5),
            StatusSlice("Rejected", rejected, 0xFFE53935)
        ).filter { it.count > 0 }

        // Weekly/monthly activity counts
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7L * 24 * 60 * 60 * 1000
        val applicationsThisWeek = apps.count { it.createdAt >= oneWeekAgo }

        cal.timeInMillis = now
        val thisMonth = cal.get(Calendar.MONTH)
        val thisYear = cal.get(Calendar.YEAR)
        val applicationsThisMonth = apps.count { app ->
            val appCal = Calendar.getInstance().apply { timeInMillis = app.createdAt }
            appCal.get(Calendar.MONTH) == thisMonth && appCal.get(Calendar.YEAR) == thisYear
        }

        return DashboardAnalytics(
            total = total,
            applied = applied,
            saved = saved,
            interviews = interviews,
            offers = offers,
            rejected = rejected,
            responses = responses,
            successRate = successRate,
            rejectionRate = rejectionRate,
            interviewRate = interviewRate,
            responseRate = responseRate,
            platforms = platforms,
            resumeStats = resumeStats,
            monthlyActivity = monthlyActivity,
            statusDistribution = statusDistribution,
            applicationsThisWeek = applicationsThisWeek,
            applicationsThisMonth = applicationsThisMonth
        )
    }

    // Sync Status States delegated to SyncManager
    val syncState: StateFlow<SyncState> = syncManager.syncState
    val downloadingFiles: StateFlow<Set<String>> = syncManager.downloadingFiles

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
                    oldHistory + StatusHistoryEntry(status, timeApplied)
                } else {
                    if (oldHistory.isNotEmpty()) {
                        oldHistory.dropLast(1) + oldHistory.last().copy(timestamp = timeApplied)
                    } else {
                        listOf(StatusHistoryEntry(status, timeApplied))
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
                createdAt = if (baseApp.id == 0L) System.currentTimeMillis() else baseApp.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveApplication(finalApp)
            // Trigger auto upload sync if initialized
            triggerUploadSync()
            onSuccess()
        }
    }

    fun requestDeleteApplication(job: JobApplication) {
        commitPendingDelete()
        _pendingDeleteJobs.value = listOf(job)
    }

    fun requestDeleteApplications(jobs: List<JobApplication>) {
        commitPendingDelete()
        _pendingDeleteJobs.value = jobs
    }

    fun undoDelete() {
        _pendingDeleteJobs.value = emptyList()
    }

    fun commitPendingDelete() {
        val jobsToDelete = _pendingDeleteJobs.value
        if (jobsToDelete.isEmpty()) return
        
        val idsToDelete = jobsToDelete.map { it.id }.toSet()
        _inFlightDeleteIds.update { it + idsToDelete }
        _pendingDeleteJobs.value = emptyList()

        applicationScope.launch {
            try {
                repository.deleteApplications(idsToDelete.toList())
                triggerUploadSync()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _inFlightDeleteIds.update { it - idsToDelete }
            }
        }
    }

    fun commitPendingDeleteBlocking() {
        val jobsToDelete = _pendingDeleteJobs.value
        if (jobsToDelete.isEmpty()) return
        
        val idsToDelete = jobsToDelete.map { it.id }.toSet()
        _inFlightDeleteIds.update { it + idsToDelete }
        _pendingDeleteJobs.value = emptyList()

        kotlinx.coroutines.runBlocking(ioDispatcher) {
            try {
                repository.deleteApplications(idsToDelete.toList())
                triggerUploadSync()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _inFlightDeleteIds.update { it - idsToDelete }
            }
        }
    }
    // --- Dynamic Background Serialization Sync Layer ---
    private fun triggerUploadSync() {
        syncManager.triggerUpload()
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

// Analytics Holder
@Immutable
data class DashboardAnalytics(
    val total: Int = 0,
    val applied: Int = 0,
    val saved: Int = 0,
    val interviews: Int = 0,
    val offers: Int = 0,
    val rejected: Int = 0,
    val responses: Int = 0,
    val successRate: Float = 0f,
    val rejectionRate: Float = 0f,
    val interviewRate: Float = 0f,
    val responseRate: Float = 0f,
    val platforms: List<PlatformStat> = emptyList(),
    val resumeStats: List<ResumeStat> = emptyList(),
    val monthlyActivity: List<MonthActivity> = emptyList(),
    val statusDistribution: List<StatusSlice> = emptyList(),
    val applicationsThisWeek: Int = 0,
    val applicationsThisMonth: Int = 0
)

@Immutable
data class PlatformStat(val platform: String, val count: Int, val interviewCount: Int, val offerCount: Int)
@Immutable
data class ResumeStat(val resumeName: String, val totalUsed: Int, val interviewCount: Int, val offerCount: Int, val rejectedCount: Int)
@Immutable
data class MonthActivity(
    val label: String,
    val count: Int,
    val appliedCount: Int = 0,
    val savedCount: Int = 0,
    val interviewCount: Int = 0,
    val offerCount: Int = 0,
    val rejectedCount: Int = 0
)
@Immutable
data class StatusSlice(val status: String, val count: Int, val color: Long)

// ViewModel Factory Creator
class JobViewModelFactory(
    private val repository: JobRepository,
    private val preferencesHelper: PreferencesHelper,
    private val syncManager: com.example.data.sync.SyncManager,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JobViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JobViewModel(repository, preferencesHelper, syncManager, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class representation")
    }
}
