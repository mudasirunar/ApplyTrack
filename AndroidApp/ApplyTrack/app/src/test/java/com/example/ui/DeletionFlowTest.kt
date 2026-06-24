package com.example.ui

import com.example.auth.AuthManager
import com.example.auth.AuthState
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ImportResult
import com.example.data.repository.JobRepository
import com.example.data.sync.SyncManager
import com.example.model.JobApplication
import com.example.utils.AppTheme
import com.example.utils.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DeletionFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeJobRepository
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var syncManager: SyncManager
    private lateinit var authManager: AuthManager
    private lateinit var viewModel: JobViewModel

    // Fake JobRepository implementation
    class FakeJobRepository : JobRepository {
        val applicationsFlow = MutableStateFlow<Map<Long, JobApplication>>(emptyMap())
        var deleteCallCount = 0
        val deletedIds = mutableListOf<Long>()

        override fun getAllApplications(): Flow<List<JobApplication>> {
            return applicationsFlow.map { it.values.toList() }
        }

        override fun getApplicationById(id: Long): Flow<JobApplication?> {
            return applicationsFlow.map { it[id] }
        }

        override suspend fun getApplicationByIdDirect(id: Long): JobApplication? {
            return applicationsFlow.value[id]
        }

        override suspend fun saveApplication(application: JobApplication): Long {
            val id = if (application.id == 0L) (applicationsFlow.value.size + 1).toLong() else application.id
            val app = application.copy(id = id)
            applicationsFlow.value = applicationsFlow.value + (id to app)
            return id
        }

        override suspend fun deleteApplication(id: Long) {
            deleteCallCount++
            deletedIds.add(id)
            applicationsFlow.value = applicationsFlow.value - id
        }

        override suspend fun deleteApplications(ids: List<Long>) {
            ids.forEach { id ->
                deleteCallCount++
                deletedIds.add(id)
                applicationsFlow.value = applicationsFlow.value - id
            }
        }

        override fun isFirebaseConfigured(): Boolean = false
        override suspend fun uploadLocalChanges(): Result<Unit> = Result.success(Unit)
        override suspend fun fetchRemoteUpdates(): Result<Int> = Result.success(0)
        override suspend fun deleteAllApplications() {
            applicationsFlow.value = emptyMap()
        }
        override suspend fun importBackup(applications: List<JobApplication>, overwriteConflicts: Boolean): ImportResult {
            return ImportResult(0, 0, 0)
        }
        override suspend fun restoreApplication(application: JobApplication) {
            applicationsFlow.value = applicationsFlow.value + (application.id to application)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = FakeJobRepository()
        
        // Mock PreferencesHelper
        preferencesHelper = mock()
        whenever(preferencesHelper.themeFlow).thenReturn(MutableStateFlow(AppTheme.SYSTEM))

        // Mock SyncManager
        syncManager = mock()
        whenever(syncManager.syncState).thenReturn(MutableStateFlow(SyncState.IDLE))
        whenever(syncManager.downloadingFiles).thenReturn(MutableStateFlow(emptySet()))

        // Mock AuthManager
        authManager = mock()
        whenever(authManager.authState).thenReturn(MutableStateFlow(AuthState.GUEST))
        whenever(authManager.currentUserFlow).thenReturn(MutableStateFlow(null))

        // Setup mock database applications
        repository.applicationsFlow.value = mapOf(
            1L to JobApplication(id = 1L, uuid = "uuid_1", role = "Android Dev", companyName = "Google", status = "Applied"),
            2L to JobApplication(id = 2L, uuid = "uuid_2", role = "iOS Dev", companyName = "Apple", status = "Interview"),
            3L to JobApplication(id = 3L, uuid = "uuid_3", role = "Web Dev", companyName = "Meta", status = "Offer")
        )

        viewModel = JobViewModel(repository, preferencesHelper, syncManager, authManager, testDispatcher, testDispatcher)
    }

    private fun TestScope.startCollecting() {
        backgroundScope.launch { viewModel.filteredApplications.collect {} }
        backgroundScope.launch { viewModel.dashboardAnalytics.collect {} }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_hasApplications() = runTest(testDispatcher) {
        startCollecting()
        testDispatcher.scheduler.advanceUntilIdle()
        val apps = viewModel.filteredApplications.value
        assertEquals(3, apps.size)
    }

    @Test
    fun testSingleDeleteRequest_updatesPendingDeleteAndHidesFromUI() = runTest(testDispatcher) {
        startCollecting()
        val targetJob = repository.applicationsFlow.value[1L]!!
        viewModel.requestDeleteApplication(targetJob)

        // Verify the job immediately disappears from UI (filteredApplications)
        // Wait for flow collection to compute
        testDispatcher.scheduler.advanceUntilIdle()

        val uiApps = viewModel.filteredApplications.value
        assertEquals(2, uiApps.size)
        assertFalse(uiApps.any { it.id == 1L })

        // Verify the job is in the pending delete list
        assertEquals(1, viewModel.pendingDeleteJobs.value.size)
        assertEquals(targetJob, viewModel.pendingDeleteJobs.value.first())

        // Verify it is NOT yet deleted in the repository (DB write is deferred)
        assertEquals(0, repository.deleteCallCount)
        assertTrue(repository.applicationsFlow.value.containsKey(1L))
    }

    @Test
    fun testSingleDeleteUndo_restoresJobToUIInstantly() = runTest(testDispatcher) {
        startCollecting()
        val targetJob = repository.applicationsFlow.value[1L]!!
        viewModel.requestDeleteApplication(targetJob)
        testDispatcher.scheduler.advanceUntilIdle()

        // Perform undo
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // UI list must instantly contain the target job again
        val uiApps = viewModel.filteredApplications.value
        assertEquals(3, uiApps.size)
        assertTrue(uiApps.any { it.id == 1L })

        // Pending list must be empty
        assertTrue(viewModel.pendingDeleteJobs.value.isEmpty())

        // Repository should still have 0 deletes
        assertEquals(0, repository.deleteCallCount)
    }

    @Test
    fun testSingleDeleteCommit_deletesFromRepository() = runTest(testDispatcher) {
        startCollecting()
        val targetJob = repository.applicationsFlow.value[1L]!!
        viewModel.requestDeleteApplication(targetJob)
        testDispatcher.scheduler.advanceUntilIdle()

        // Perform commit
        viewModel.commitPendingDelete()
        
        // Wait for background coroutine on applicationScope to finish
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify repository deletion is executed
        assertEquals(1, repository.deleteCallCount)
        assertEquals(1L, repository.deletedIds.first())
        assertFalse(repository.applicationsFlow.value.containsKey(1L))

        // Verify UI has 2 jobs and doesn't contain the deleted job
        val uiApps = viewModel.filteredApplications.value
        assertEquals(2, uiApps.size)
        assertFalse(uiApps.any { it.id == 1L })

        // Pending list must be cleared
        assertTrue(viewModel.pendingDeleteJobs.value.isEmpty())
    }

    @Test
    fun testBulkDeleteRequest_updatesPendingDeleteAndHidesAllFromUI() = runTest(testDispatcher) {
        startCollecting()
        val targetJobs = listOf(
            repository.applicationsFlow.value[1L]!!,
            repository.applicationsFlow.value[2L]!!
        )
        viewModel.requestDeleteApplications(targetJobs)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify both disappear instantly from UI
        val uiApps = viewModel.filteredApplications.value
        assertEquals(1, uiApps.size)
        assertEquals(3L, uiApps.first().id)

        // Verify both are in pending list
        assertEquals(2, viewModel.pendingDeleteJobs.value.size)

        // Verify DB write is deferred
        assertEquals(0, repository.deleteCallCount)
    }

    @Test
    fun testBulkDeleteUndo_restoresAllToUIInstantly() = runTest(testDispatcher) {
        startCollecting()
        val targetJobs = listOf(
            repository.applicationsFlow.value[1L]!!,
            repository.applicationsFlow.value[2L]!!
        )
        viewModel.requestDeleteApplications(targetJobs)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all 3 are in the UI again
        val uiApps = viewModel.filteredApplications.value
        assertEquals(3, uiApps.size)

        // Pending list is cleared
        assertTrue(viewModel.pendingDeleteJobs.value.isEmpty())
        assertEquals(0, repository.deleteCallCount)
    }

    @Test
    fun testBulkDeleteCommit_deletesAllFromRepository() = runTest(testDispatcher) {
        startCollecting()
        val targetJobs = listOf(
            repository.applicationsFlow.value[1L]!!,
            repository.applicationsFlow.value[2L]!!
        )
        viewModel.requestDeleteApplications(targetJobs)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.commitPendingDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify deletions executed on repository
        assertEquals(2, repository.deleteCallCount)
        assertTrue(repository.deletedIds.contains(1L))
        assertTrue(repository.deletedIds.contains(2L))
        assertFalse(repository.applicationsFlow.value.containsKey(1L))
        assertFalse(repository.applicationsFlow.value.containsKey(2L))

        // Verify UI has 1 job
        val uiApps = viewModel.filteredApplications.value
        assertEquals(1, uiApps.size)
        assertEquals(3L, uiApps.first().id)
    }

    @Test
    fun testConsecutiveDeletes_commitsFirstDeleteAutomatically() = runTest(testDispatcher) {
        startCollecting()
        val firstJob = repository.applicationsFlow.value[1L]!!
        val secondJob = repository.applicationsFlow.value[2L]!!

        // Request first delete
        viewModel.requestDeleteApplication(firstJob)
        testDispatcher.scheduler.advanceUntilIdle()

        // Request second delete (this should trigger commit for firstJob)
        viewModel.requestDeleteApplication(secondJob)
        testDispatcher.scheduler.advanceUntilIdle()

        // First job must be deleted from repository now
        assertEquals(1, repository.deleteCallCount)
        assertEquals(1L, repository.deletedIds.first())
        assertFalse(repository.applicationsFlow.value.containsKey(1L))

        // Second job is currently pending and hidden from UI
        val uiApps = viewModel.filteredApplications.value
        assertEquals(1, uiApps.size)
        assertEquals(3L, uiApps.first().id)
        
        assertEquals(1, viewModel.pendingDeleteJobs.value.size)
        assertEquals(secondJob, viewModel.pendingDeleteJobs.value.first())
    }
}
