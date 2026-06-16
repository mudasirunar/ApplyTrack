package com.example.sync

import com.example.model.DeletedJob
import com.example.model.JobApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class SyncStressTest {

    private fun runScaleTest(size: Int) = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        // Generate and insert fake jobs
        val fakeJobs = SyncTestDataGenerator.generateFakeJobs(count = size, dirtyRatio = 0.6, baseTime = 1000L)
        for (job in fakeJobs) {
            dao.insertApplication(job)
        }

        // Expected counts
        val expectedDirtyCount = (size * 0.6).toInt()
        val expectedCleanCount = size - expectedDirtyCount

        assertEquals(expectedDirtyCount, dao.getDirtyApplications().size)

        // 1. Measure upload changes time
        val uploadTime = measureTimeMillis {
            val result = engine.uploadLocalChanges()
            assertTrue(result.isSuccess)
        }
        println("Scale $size: Uploaded $expectedDirtyCount dirty apps in ${uploadTime}ms")

        // Verify all are now clean and remote has the uploaded ones
        assertEquals(0, dao.getDirtyApplications().size)
        assertEquals(expectedDirtyCount, remote.remoteApplications.size)

        // 2. Perform a sync where remote has updates for the clean apps (newer timestamp)
        for (i in 1..size) {
            val uuid = "uuid_$i"
            val isCleanInitially = i > expectedDirtyCount
            if (isCleanInitially) {
                // Make remote newer
                remote.remoteApplications[uuid] = SyncTestDataGenerator.generateFakeJob(
                    index = i,
                    updatedAt = 2000L, // newer
                    companyName = "Updated Company $i"
                )
            }
        }

        val fetchTime = measureTimeMillis {
            val result = engine.fetchRemoteUpdates()
            assertTrue(result.isSuccess)
        }
        println("Scale $size: Fetched remote updates in ${fetchTime}ms")

        // Verify that all initially clean apps have been updated locally
        for (i in 1..size) {
            val uuid = "uuid_$i"
            val isCleanInitially = i > expectedDirtyCount
            val localJob = dao.getApplicationByUuid(uuid)!!
            if (isCleanInitially) {
                assertEquals("Updated Company $i", localJob.companyName)
                assertEquals(2000L, localJob.updatedAt)
                assertEquals(2000L, localJob.lastSyncedAt)
            } else {
                assertEquals("Company $i", localJob.companyName)
                assertEquals(1000L, localJob.updatedAt)
            }
        }

        // 3. Stress deletions: Delete 50% of the elements locally
        val deleteCount = size / 2
        val deletedUuids = mutableSetOf<String>()
        for (i in 1..deleteCount) {
            val uuid = "uuid_$i"
            deletedUuids.add(uuid)
            dao.insertDeletedJob(DeletedJob(uuid = uuid))
            dao.deleteApplicationById(i.toLong())
        }

        assertEquals(size - deleteCount, dao.getAllApplicationsList().size)
        assertEquals(deleteCount, dao.getAllDeletedJobs().size)

        val deleteSyncTime = measureTimeMillis {
            val result = engine.uploadLocalChanges()
            assertTrue(result.isSuccess)
        }
        println("Scale $size: Propagated $deleteCount deletions in ${deleteSyncTime}ms")

        // Verify deletions were synced to remote and local tracker is cleared
        assertEquals(0, dao.getAllDeletedJobs().size)
        for (uuid in deletedUuids) {
            assertTrue(!remote.remoteApplications.containsKey(uuid))
        }
    }

    @Test
    fun testSync_Scale_0() {
        runScaleTest(0)
    }

    @Test
    fun testSync_Scale_1() {
        runScaleTest(1)
    }

    @Test
    fun testSync_Scale_10() {
        runScaleTest(10)
    }

    @Test
    fun testSync_Scale_50() {
        runScaleTest(50)
    }

    @Test
    fun testSync_Scale_500() {
        runScaleTest(500)
    }

    @Test
    fun testSync_Scale_1000() {
        runScaleTest(1000)
    }
}
