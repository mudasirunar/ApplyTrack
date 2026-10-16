package com.mudasir.applytrack.utils

import com.mudasir.applytrack.model.Attachment
import com.mudasir.applytrack.model.JobApplication
import com.mudasir.applytrack.model.StatusHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobMergerTest {

    @Test
    fun testMerge_NonConflictingFieldsPreserved() {
        val localJob = JobApplication(
            id = 1L,
            uuid = "job-uuid-1",
            companyName = "Acme Corp",
            notes = "Local note",
            updatedAt = 2000L
        )

        val remoteJob = JobApplication(
            id = 1L,
            uuid = "job-uuid-1",
            companyName = "Acme Corp",
            notes = null,
            role = "Senior Android Engineer",
            updatedAt = 1000L
        )

        val merged = JobMerger.merge(localJob, remoteJob)
        assertEquals("Senior Android Engineer", merged.role)
        assertEquals("Local note", merged.notes)
    }

    @Test
    fun testMerge_SameFieldCollision_LastWriteWins() {
        // Case 1: Local is newer -> local wins
        val localNewer = JobApplication(
            id = 2L,
            uuid = "job-uuid-2",
            status = "Interviewing",
            updatedAt = 2000L
        )
        val remoteOlder = JobApplication(
            id = 2L,
            uuid = "job-uuid-2",
            status = "Screening",
            updatedAt = 1000L
        )
        assertEquals("Interviewing", JobMerger.merge(localNewer, remoteOlder).status)

        // Case 2: Remote is newer -> remote wins
        val localOlder = JobApplication(
            id = 2L,
            uuid = "job-uuid-2",
            status = "Applied",
            updatedAt = 1000L
        )
        val remoteNewer = JobApplication(
            id = 2L,
            uuid = "job-uuid-2",
            status = "Offer",
            updatedAt = 3000L
        )
        assertEquals("Offer", JobMerger.merge(localOlder, remoteNewer).status)
    }

    @Test
    fun testMerge_StatusHistoryUnion_NoMilestonesLost() {
        val localJob = JobApplication(
            id = 3L,
            uuid = "job-uuid-3",
            status = "Interviewing",
            updatedAt = 3000L,
            statusHistory = listOf(
                StatusHistoryEntry("Applied", 1000L),
                StatusHistoryEntry("Interviewing", 3000L)
            )
        )

        val remoteJob = JobApplication(
            id = 3L,
            uuid = "job-uuid-3",
            status = "Screening",
            updatedAt = 2000L,
            statusHistory = listOf(
                StatusHistoryEntry("Applied", 1000L),
                StatusHistoryEntry("Screening", 2000L)
            )
        )

        val merged = JobMerger.merge(localJob, remoteJob)
        val history = merged.statusHistory ?: emptyList()
        assertEquals(3, history.size)
        assertEquals("Applied", history[0].status)
        assertEquals("Screening", history[1].status)
        assertEquals("Interviewing", history[2].status)
    }

    @Test
    fun testMerge_AttachmentsAndScreenshotsPreserved() {
        val localJob = JobApplication(
            id = 4L,
            uuid = "job-uuid-4",
            resume = Attachment("resume.pdf", "resume.pdf"),
            screenshots = listOf(Attachment("screen1.png", "screen1.png")),
            updatedAt = 2000L
        )

        val remoteJob = JobApplication(
            id = 4L,
            uuid = "job-uuid-4",
            coverLetter = Attachment("cover.pdf", "cover.pdf"),
            screenshots = listOf(Attachment("screen2.png", "screen2.png")),
            updatedAt = 1000L
        )

        val merged = JobMerger.merge(localJob, remoteJob)
        assertEquals("resume.pdf", merged.resume?.fileName)
        assertEquals("cover.pdf", merged.coverLetter?.fileName)
        assertEquals(2, merged.screenshots?.size)
    }
}
