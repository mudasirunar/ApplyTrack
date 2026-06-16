package com.example.sync

import com.example.model.DeletedJob
import com.example.model.JobApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTest {

    @Test
    fun testSync_UploadDirtyAndCleanIgnore() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        // Create 20 dirty apps and 20 clean apps using generator helper
        val fakeJobs = SyncTestDataGenerator.generateFakeJobs(count = 40, dirtyRatio = 0.5)
        for (app in fakeJobs) {
            dao.insertApplication(app)
        }

        assertEquals(20, dao.getDirtyApplications().size)

        // Run upload
        val result = engine.uploadLocalChanges()
        assertTrue(result.isSuccess)

        // Verify only 20 dirty ones uploaded and marked clean
        assertEquals(0, dao.getDirtyApplications().size)
        assertEquals(20, remote.remoteApplications.size)

        for (i in 1..40) {
            val app = dao.getApplicationByUuid("uuid_$i")!!
            assertEquals(1000L, app.lastSyncedAt)
            if (i <= 20) {
                // Was dirty, now uploaded to remote
                assertTrue(remote.remoteApplications.containsKey("uuid_$i"))
            } else {
                // Was clean, should not be on remote (was never dirty)
                assertTrue(!remote.remoteApplications.containsKey("uuid_$i"))
            }
        }
    }

    @Test
    fun testSync_DeletionsOverrideRemote() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        // Setup remote with 5 apps
        for (i in 1..5) {
            remote.remoteApplications["uuid_$i"] = SyncTestDataGenerator.generateFakeJob(i)
        }

        // Setup local with 4 apps (app_5 is deleted)
        for (i in 1..4) {
            dao.insertApplication(SyncTestDataGenerator.generateFakeJob(i))
        }
        dao.insertDeletedJob(DeletedJob(uuid = "uuid_5"))

        // Run upload
        val result = engine.uploadLocalChanges()
        assertTrue(result.isSuccess)

        // uuid_5 should be deleted remotely, and deleted tracking cleaned
        assertTrue(!remote.remoteApplications.containsKey("uuid_5"))
        assertEquals(0, dao.getAllDeletedJobs().size)
    }

    @Test
    fun testSync_LastWriteWins_ConflictResolution() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        // Scenario A: Local is newer (upload)
        dao.insertApplication(
            JobApplication(uuid = "app_newer_local", companyName = "Company A", updatedAt = 2000L, lastSyncedAt = 0L)
        )
        remote.remoteApplications["app_newer_local"] =
            JobApplication(uuid = "app_newer_local", companyName = "Old Company A", updatedAt = 1000L)

        // Scenario B: Remote is newer (download)
        dao.insertApplication(
            JobApplication(uuid = "app_newer_remote", companyName = "Old Company B", updatedAt = 1000L, lastSyncedAt = 1000L)
        )
        remote.remoteApplications["app_newer_remote"] =
            JobApplication(uuid = "app_newer_remote", companyName = "New Company B", updatedAt = 3000L)

        // Scenario C: Equal timestamps, local not marked clean
        dao.insertApplication(
            JobApplication(uuid = "app_equal", companyName = "Company C", updatedAt = 1500L, lastSyncedAt = 1000L)
        )
        remote.remoteApplications["app_equal"] =
            JobApplication(uuid = "app_equal", companyName = "Company C", updatedAt = 1500L)

        // Perform synchronization
        engine.uploadLocalChanges()
        engine.fetchRemoteUpdates()

        // Verify Scenario A: Local (newer) uploaded to remote and marked clean locally
        assertEquals("Company A", remote.remoteApplications["app_newer_local"]?.companyName)
        assertEquals(2000L, dao.getApplicationByUuid("app_newer_local")?.lastSyncedAt)

        // Verify Scenario B: Remote (newer) downloaded to local
        assertEquals("New Company B", dao.getApplicationByUuid("app_newer_remote")?.companyName)
        assertEquals(3000L, dao.getApplicationByUuid("app_newer_remote")?.lastSyncedAt)

        // Verify Scenario C: Equal timestamps -> marked clean local
        assertEquals(1500L, dao.getApplicationByUuid("app_equal")?.lastSyncedAt)
        assertEquals(0, dao.getDirtyApplications().size)
    }

    @Test
    fun testSync_OfflinePersistenceAndRecovery() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        // Create a dirty app
        dao.insertApplication(
            JobApplication(uuid = "uuid_1", companyName = "Local offline edit", updatedAt = 2000L, lastSyncedAt = 1000L)
        )

        // Connection is offline!
        remote.throwsNetworkError = true

        // Attempt upload changes
        engine.uploadLocalChanges()

        // Sync should fail/skip gracefully and KEEP the application dirty
        assertEquals(1, dao.getDirtyApplications().size)
        assertEquals(0, remote.remoteApplications.size)

        // Connection recovers!
        remote.throwsNetworkError = false

        // Attempt upload changes again
        val successResult = engine.uploadLocalChanges()
        assertTrue(successResult.isSuccess)

        // Should successfully upload and clear dirty flag
        assertEquals(0, dao.getDirtyApplications().size)
        assertEquals(1, remote.remoteApplications.size)
        assertEquals("Local offline edit", remote.remoteApplications["uuid_1"]?.companyName)
    }

    @Test
    fun testSync_WipePriority() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        // User had 5 apps local and remote
        for (i in 1..5) {
            dao.insertApplication(SyncTestDataGenerator.generateFakeJob(i))
            remote.remoteApplications["uuid_$i"] = SyncTestDataGenerator.generateFakeJob(i)
        }

        // User executes reset / delete all local data
        val localApps = dao.getAllApplicationsList()
        for (app in localApps) {
            dao.insertDeletedJob(DeletedJob(uuid = app.uuid))
        }
        dao.deleteAllApplications()

        assertEquals(0, dao.getAllApplicationsList().size)
        assertEquals(5, dao.getAllDeletedJobs().size)

        // Run upload changes to propagate reset
        engine.uploadLocalChanges()

        // Remote database must be completely empty now
        assertEquals(0, remote.remoteApplications.size)
        assertEquals(0, dao.getAllDeletedJobs().size)
    }
}
