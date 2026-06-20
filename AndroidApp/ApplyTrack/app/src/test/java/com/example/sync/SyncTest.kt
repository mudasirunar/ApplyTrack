package com.example.sync

import com.example.model.Attachment
import com.example.model.DeletedJob
import com.example.model.JobApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testSync_FullEntityDataMappingIntegrity() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val engine = SyncEngine(dao, remote)

        val complexJob = JobApplication(
            id = 1L,
            uuid = "complex-uuid-12345",
            companyName = "Tech Corp",
            role = "Software Engineer",
            platform = "LinkedIn",
            status = "Interview",
            jobDescription = "Build beautiful Android apps with Kotlin and Jetpack Compose.",
            notes = "Spoke with recruiter on Monday. Tech round scheduled for next week.",
            url = "https://example.com/tech-corp-job",
            email = "jobs@techcorp.com",
            statusHistory = listOf(
                com.example.model.StatusHistoryEntry("Applied", 1000L),
                com.example.model.StatusHistoryEntry("Interview", 2000L)
            ),
            resume = com.example.model.Attachment("resume_123.pdf", "MyResume.pdf"),
            coverLetter = com.example.model.Attachment("cover_letter_123.pdf", "MyCoverLetter.pdf"),
            additionalDocument = com.example.model.Attachment("cert_123.pdf", "Cert.pdf"),
            screenshots = listOf(
                com.example.model.Attachment("screen1.png", "Screenshot1.png"),
                com.example.model.Attachment("screen2.png", "Screenshot2.png")
            ),
            createdAt = 1000L,
            updatedAt = 3000L,
            lastSyncedAt = 0L // Dirty
        )

        // Insert locally
        dao.insertApplication(complexJob)

        // Run upload to remote
        val uploadResult = engine.uploadLocalChanges()
        assertTrue(uploadResult.isSuccess)

        // Verify remote got the full data structure correctly
        val remoteJob = remote.remoteApplications["complex-uuid-12345"]!!
        assertEquals(complexJob.uuid, remoteJob.uuid)
        assertEquals(complexJob.companyName, remoteJob.companyName)
        assertEquals(complexJob.role, remoteJob.role)
        assertEquals(complexJob.platform, remoteJob.platform)
        assertEquals(complexJob.status, remoteJob.status)
        assertEquals(complexJob.jobDescription, remoteJob.jobDescription)
        assertEquals(complexJob.notes, remoteJob.notes)
        assertEquals(complexJob.url, remoteJob.url)
        assertEquals(complexJob.email, remoteJob.email)
        
        assertEquals(complexJob.statusHistory?.size, remoteJob.statusHistory?.size)
        assertEquals("Applied", remoteJob.statusHistory?.get(0)?.status)
        assertEquals(1000L, remoteJob.statusHistory?.get(0)?.timestamp)
        assertEquals("Interview", remoteJob.statusHistory?.get(1)?.status)
        assertEquals(2000L, remoteJob.statusHistory?.get(1)?.timestamp)

        assertEquals(complexJob.resume?.fileName, remoteJob.resume?.fileName)
        assertEquals(complexJob.resume?.originalName, remoteJob.resume?.originalName)
        assertEquals(complexJob.coverLetter?.fileName, remoteJob.coverLetter?.fileName)
        assertEquals(complexJob.coverLetter?.originalName, remoteJob.coverLetter?.originalName)
        assertEquals(complexJob.additionalDocument?.fileName, remoteJob.additionalDocument?.fileName)
        assertEquals(complexJob.additionalDocument?.originalName, remoteJob.additionalDocument?.originalName)

        assertEquals(complexJob.screenshots?.size, remoteJob.screenshots?.size)
        assertEquals("screen1.png", remoteJob.screenshots?.get(0)?.fileName)
        assertEquals("screen2.png", remoteJob.screenshots?.get(1)?.fileName)

        assertEquals(complexJob.createdAt, remoteJob.createdAt)
        assertEquals(complexJob.updatedAt, remoteJob.updatedAt)

        // Clean local database to simulate another device
        dao.deleteAllApplications()
        assertEquals(0, dao.getAllApplicationsList().size)

        // Now run fetch/download to simulate syncing to other device
        val fetchResult = engine.fetchRemoteUpdates()
        assertTrue(fetchResult.isSuccess)

        // Verify local database now has the full structured object perfectly integrated
        val syncedJob = dao.getApplicationByUuid("complex-uuid-12345")!!
        assertEquals(complexJob.uuid, syncedJob.uuid)
        assertEquals(complexJob.companyName, syncedJob.companyName)
        assertEquals(complexJob.role, syncedJob.role)
        assertEquals(complexJob.platform, syncedJob.platform)
        assertEquals(complexJob.status, syncedJob.status)
        assertEquals(complexJob.jobDescription, syncedJob.jobDescription)
        assertEquals(complexJob.notes, syncedJob.notes)
        assertEquals(complexJob.url, syncedJob.url)
        assertEquals(complexJob.email, syncedJob.email)

        assertEquals(complexJob.statusHistory?.size, syncedJob.statusHistory?.size)
        assertEquals("Applied", syncedJob.statusHistory?.get(0)?.status)
        assertEquals("Interview", syncedJob.statusHistory?.get(1)?.status)

        assertEquals(complexJob.resume?.fileName, syncedJob.resume?.fileName)
        assertEquals(complexJob.coverLetter?.fileName, syncedJob.coverLetter?.fileName)
        assertEquals(complexJob.additionalDocument?.fileName, syncedJob.additionalDocument?.fileName)

        assertEquals(complexJob.screenshots?.size, syncedJob.screenshots?.size)
        assertEquals("screen1.png", syncedJob.screenshots?.get(0)?.fileName)
        assertEquals("screen2.png", syncedJob.screenshots?.get(1)?.fileName)
    }

    @Test
    fun testAuthTransition_GuestToCloudMigration() = runBlocking {
        val dao = FakeJobApplicationDao()
        for (i in 1..5) {
            dao.insertApplication(
                JobApplication(
                    uuid = "uuid_$i",
                    companyName = "Guest Job $i",
                    updatedAt = 1000L,
                    lastSyncedAt = 1000L
                )
            )
        }

        assertEquals(0, dao.getDirtyApplications().size)

        val localJobs = dao.getAllApplicationsList()
        for (job in localJobs) {
            dao.insertApplication(job.copy(lastSyncedAt = 0L))
        }

        assertEquals(5, dao.getDirtyApplications().size)
    }

    @Test
    fun testMediaSync_UploadBeforeFirestore_Success() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val supabase = FakeSupabaseStorage()
        val engine = EnhancedSyncEngine(dao, remote, supabase)

        val job = JobApplication(
            uuid = "job_with_media",
            companyName = "Success Corp",
            updatedAt = 2000L,
            lastSyncedAt = 0L,
            resume = Attachment("resume.pdf", "Resume.pdf"),
            screenshots = listOf(Attachment("screenshot1.png", "Screenshot1.png"))
        )
        dao.insertApplication(job)

        engine.uploadLocalChanges("user_123")

        assertTrue(supabase.files["user_123/resumes/resume.pdf"] == true)
        assertTrue(supabase.files["user_123/screenshots/screenshot1.png"] == true)

        assertTrue(remote.remoteApplications.containsKey("job_with_media"))
        assertEquals(2000L, dao.getApplicationByUuid("job_with_media")?.lastSyncedAt)
    }

    @Test
    fun testMediaSync_UploadBeforeFirestore_FailSupabase() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val supabase = FakeSupabaseStorage()
        val engine = EnhancedSyncEngine(dao, remote, supabase)

        val job = JobApplication(
            uuid = "job_with_media_fail",
            companyName = "Fail Corp",
            updatedAt = 2000L,
            lastSyncedAt = 0L,
            resume = Attachment("broken_resume.pdf", "Resume.pdf")
        )
        dao.insertApplication(job)

        supabase.throwsNetworkError = true

        engine.uploadLocalChanges("user_123")

        assertFalse(supabase.files.containsKey("user_123/resumes/broken_resume.pdf"))

        assertFalse(remote.remoteApplications.containsKey("job_with_media_fail"))
        assertEquals(0L, dao.getApplicationByUuid("job_with_media_fail")?.lastSyncedAt)
        assertEquals(1, dao.getDirtyApplications().size)
    }

    @Test
    fun testMediaSync_DownloadRetryBackoff_SuccessAfterRetries() = runBlocking {
        val supabase = FakeSupabaseStorage()
        val engine = EnhancedSyncEngine(FakeJobApplicationDao(), FakeRemoteService(), supabase)

        supabase.files["user_123/resumes/resume.pdf"] = true
        supabase.failAttemptsBeforeSuccess = 2

        val result = engine.downloadAttachmentWithRetry("user_123", "resumes", "resume.pdf")

        assertTrue(result)
        assertEquals(3, supabase.downloadCount)
    }

    @Test
    fun testMediaSync_DownloadRetryBackoff_PermanentFailure() = runBlocking {
        val supabase = FakeSupabaseStorage()
        val engine = EnhancedSyncEngine(FakeJobApplicationDao(), FakeRemoteService(), supabase)

        supabase.failAttemptsBeforeSuccess = 5

        val result = engine.downloadAttachmentWithRetry("user_123", "resumes", "missing.pdf")

        assertFalse(result)
        assertEquals(3, supabase.downloadCount)
    }

    @Test
    fun testAttachmentReplacement_CleansStorage() = runBlocking {
        val dao = FakeJobApplicationDao()
        val remote = FakeRemoteService()
        val supabase = FakeSupabaseStorage()
        val engine = EnhancedSyncEngine(dao, remote, supabase)

        val job = JobApplication(
            uuid = "job_1",
            companyName = "Test Corp",
            updatedAt = 1000L,
            lastSyncedAt = 1000L,
            resume = Attachment("old_resume.pdf", "OldResume.pdf")
        )
        dao.insertApplication(job)
        supabase.files["user_123/resumes/old_resume.pdf"] = true

        val updatedJob = job.copy(
            updatedAt = 2000L,
            lastSyncedAt = 0L,
            resume = Attachment("new_resume.pdf", "NewResume.pdf")
        )

        val deletedAttachments = mutableListOf<Pair<String, Attachment>>()
        if (job.resume != null && updatedJob.resume?.fileName != job.resume.fileName) {
            deletedAttachments.add("resumes" to job.resume)
        }

        deletedAttachments.forEach { (type, attachment) ->
            supabase.deleteFile("user_123", type, attachment.fileName)
        }
        dao.insertApplication(updatedJob)

        engine.uploadLocalChanges("user_123")

        assertTrue(supabase.files["user_123/resumes/new_resume.pdf"] == true)
        assertFalse(supabase.files.containsKey("user_123/resumes/old_resume.pdf"))
        assertEquals(1, supabase.deleteCount)
    }
}

// --- Fakes and Helpers supporting the new Sync & Storage behavior ---

class FakeSupabaseStorage {
    val files = mutableMapOf<String, Boolean>()
    var throwsNetworkError = false
    var checkCount = 0
    var uploadCount = 0
    var downloadCount = 0
    var deleteCount = 0
    var failAttemptsBeforeSuccess = 0

    fun checkFileExists(userId: String, type: String, fileName: String): Boolean {
        checkCount++
        return files["$userId/$type/$fileName"] == true
    }

    fun uploadFile(userId: String, type: String, fileName: String): Boolean {
        uploadCount++
        if (throwsNetworkError) return false
        files["$userId/$type/$fileName"] = true
        return true
    }

    fun downloadFile(userId: String, type: String, fileName: String): Boolean {
        downloadCount++
        if (failAttemptsBeforeSuccess > 0) {
            failAttemptsBeforeSuccess--
            return false
        }
        if (throwsNetworkError) return false
        return files["$userId/$type/$fileName"] == true
    }

    fun deleteFile(userId: String, type: String, fileName: String): Boolean {
        deleteCount++
        if (throwsNetworkError) return false
        files.remove("$userId/$type/$fileName")
        return true
    }
}

class EnhancedSyncEngine(
    private val dao: FakeJobApplicationDao,
    private val remoteService: FakeRemoteService,
    private val supabaseStorage: FakeSupabaseStorage
) {
    suspend fun uploadLocalChanges(userId: String): Result<Unit> = kotlin.runCatching {
        val deletedJobs = dao.getAllDeletedJobs()
        val dirtyJobs = dao.getDirtyApplications()

        if (deletedJobs.isEmpty() && dirtyJobs.isEmpty()) {
            return@runCatching
        }

        val successfulJobs = mutableListOf<JobApplication>()
        for (job in dirtyJobs) {
            val jobUploads = listOfNotNull(
                job.resume?.let { "resumes" to it.fileName },
                job.coverLetter?.let { "cover_letters" to it.fileName },
                job.additionalDocument?.let { "additional_documents" to it.fileName }
            ) + (job.screenshots?.map { "screenshots" to it.fileName } ?: emptyList())

            var allUploadsSucceeded = true
            for ((type, fileName) in jobUploads) {
                val existsOnCloud = supabaseStorage.checkFileExists(userId, type, fileName)
                if (!existsOnCloud) {
                    val uploadResult = supabaseStorage.uploadFile(userId, type, fileName)
                    if (!uploadResult) {
                        allUploadsSucceeded = false
                    }
                }
            }

            if (allUploadsSucceeded) {
                successfulJobs.add(job)
            }
        }

        for (deletedJob in deletedJobs) {
            remoteService.delete(deletedJob.uuid)
            dao.removeDeletedJobTracking(deletedJob.uuid)
        }

        for (job in successfulJobs) {
            remoteService.upload(job)
            dao.updateLastSyncedAt(job.uuid, job.updatedAt)
        }
    }

    suspend fun downloadAttachmentWithRetry(
        userId: String,
        type: String,
        fileName: String,
        retryDelayMs: Long = 10L
    ): Boolean {
        var success = false
        var attempt = 1
        val maxAttempts = 3
        var delayMs = retryDelayMs

        while (!success && attempt <= maxAttempts) {
            success = supabaseStorage.downloadFile(userId, type, fileName)
            if (!success) {
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                }
                attempt++
            }
        }
        return success
    }
}

