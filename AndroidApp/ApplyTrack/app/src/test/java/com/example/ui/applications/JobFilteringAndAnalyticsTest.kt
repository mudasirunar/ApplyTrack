package com.example.ui.applications

import com.example.model.Attachment
import com.example.model.JobApplication
import com.example.model.StatusHistoryEntry
import com.example.ui.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JobFilteringAndAnalyticsTest {

    private lateinit var sampleJobs: List<JobApplication>

    @Before
    fun setUp() {
        sampleJobs = listOf(
            JobApplication(
                id = 1,
                companyName = "Google",
                role = "Android Software Engineer",
                platform = "LinkedIn",
                status = "Interview",
                createdAt = 1700001000000L,
                statusHistory = listOf(
                    StatusHistoryEntry("Applied", 1700001000000L),
                    StatusHistoryEntry("Interview", 1700003000000L)
                ),
                notes = "Completed technical screen with hiring manager",
                resume = Attachment("resume_v1.pdf", "Senior_Resume.pdf")
            ),
            JobApplication(
                id = 2,
                companyName = "Apple",
                role = "Compose UI Engineer",
                platform = "LinkedIn",
                status = "Offer",
                createdAt = 1700000500000L,
                statusHistory = listOf(
                    StatusHistoryEntry("Applied", 1700000500000L),
                    StatusHistoryEntry("Interview", 1700002500000L),
                    StatusHistoryEntry("Offer", 1700004000000L)
                ),
                notes = "Received verbal offer",
                resume = Attachment("resume_v1.pdf", "Senior_Resume.pdf")
            ),
            JobApplication(
                id = 3,
                companyName = "Netflix",
                role = "Full Stack Engineer",
                platform = "Indeed",
                status = "Rejected",
                createdAt = 1700000800000L,
                statusHistory = listOf(
                    StatusHistoryEntry("Applied", 1700000800000L),
                    StatusHistoryEntry("Rejected", 1700002000000L)
                ),
                notes = "Role closed internally",
                resume = Attachment("resume_v2.pdf", "General_Resume.pdf")
            ),
            JobApplication(
                id = 4,
                companyName = "Stripe",
                role = "Backend Kotlin Engineer",
                platform = "Company Website",
                status = "Applied",
                createdAt = 1700001200000L,
                statusHistory = listOf(
                    StatusHistoryEntry("Applied", 1700001200000L)
                ),
                notes = "Applied via referral link",
                resume = Attachment("resume_v2.pdf", "General_Resume.pdf")
            ),
            JobApplication(
                id = 5,
                companyName = "Meta",
                role = "Mobile Systems Architect",
                platform = "LinkedIn",
                status = "Saved",
                createdAt = 1700000100000L,
                statusHistory = null,
                notes = "Need to tailor resume before applying",
                resume = null
            )
        )
    }

    // =========================================================================
    // Search Filtering Tests (Matches JobViewModel Search Logic)
    // =========================================================================

    @Test
    fun testSearchQuery_MatchesCompanyCaseInsensitive() {
        val query = "google"
        val filtered = sampleJobs.filter {
            it.companyName?.contains(query, ignoreCase = true) == true ||
            it.role?.contains(query, ignoreCase = true) == true ||
            it.notes?.contains(query, ignoreCase = true) == true
        }

        assertEquals(1, filtered.size)
        assertEquals("Google", filtered[0].companyName)
    }

    @Test
    fun testSearchQuery_MatchesRole() {
        val query = "engineer"
        val filtered = sampleJobs.filter {
            it.companyName?.contains(query, ignoreCase = true) == true ||
            it.role?.contains(query, ignoreCase = true) == true
        }

        assertEquals(4, filtered.size) // Google, Apple, Netflix, Stripe
    }

    @Test
    fun testSearchQuery_MatchesNotesContent() {
        val query = "referral"
        val filtered = sampleJobs.filter {
            it.notes?.contains(query, ignoreCase = true) == true
        }

        assertEquals(1, filtered.size)
        assertEquals("Stripe", filtered[0].companyName)
    }

    // =========================================================================
    // Status Filtering Tests (Matches JobViewModel Status Logic)
    // =========================================================================

    @Test
    fun testStatusFilter_SpecificStatus() {
        val interviews = sampleJobs.filter { it.status.equals("Interview", ignoreCase = true) }
        assertEquals(1, interviews.size)
        assertEquals("Google", interviews[0].companyName)

        val offers = sampleJobs.filter { it.status.equals("Offer", ignoreCase = true) }
        assertEquals(1, offers.size)
        assertEquals("Apple", offers[0].companyName)

        val applied = sampleJobs.filter { it.status.equals("Applied", ignoreCase = true) }
        assertEquals(1, applied.size)
        assertEquals("Stripe", applied[0].companyName)
    }

    @Test
    fun testStatusFilter_ResponseBucket() {
        // "Response" filter in JobViewModel collects interview, offer, and rejected
        val responseJobs = sampleJobs.filter {
            val s = it.status.lowercase()
            s == "interview" || s == "offer" || s == "rejected"
        }

        assertEquals(3, responseJobs.size)
        assertTrue(responseJobs.any { it.companyName == "Google" })
        assertTrue(responseJobs.any { it.companyName == "Apple" })
        assertTrue(responseJobs.any { it.companyName == "Netflix" })
    }

    // =========================================================================
    // Sorting Tests (Matches JobViewModel SortOption Logic)
    // =========================================================================

    @Test
    fun testSort_StatusLatest() {
        val sorted = sampleJobs.sortedByDescending { it.statusHistory?.lastOrNull()?.timestamp ?: it.createdAt }
        assertEquals("Apple", sorted[0].companyName)      // Offer at 1700004000000L
        assertEquals("Google", sorted[1].companyName)     // Interview at 1700003000000L
        assertEquals("Meta", sorted.last().companyName)   // Saved (no history, createdAt 1700000100000L)
    }

    @Test
    fun testSort_StatusOldest() {
        val sorted = sampleJobs.sortedBy { it.statusHistory?.lastOrNull()?.timestamp ?: it.createdAt }
        assertEquals("Meta", sorted[0].companyName)
        assertEquals("Apple", sorted.last().companyName)
    }

    @Test
    fun testSort_CreationLatest() {
        val sorted = sampleJobs.sortedByDescending { it.createdAt }
        assertEquals("Stripe", sorted[0].companyName)     // 1700001200000L
        assertEquals("Google", sorted[1].companyName)     // 1700001000000L
    }

    // =========================================================================
    // Analytics & Funnel Conversion Math Tests
    // =========================================================================

    @Test
    fun testAnalytics_StatusDistribution() {
        val statusCounts = sampleJobs.groupingBy { it.status }.eachCount()

        assertEquals(1, statusCounts["Interview"] ?: 0)
        assertEquals(1, statusCounts["Offer"] ?: 0)
        assertEquals(1, statusCounts["Rejected"] ?: 0)
        assertEquals(1, statusCounts["Applied"] ?: 0)
        assertEquals(1, statusCounts["Saved"] ?: 0)
    }

    @Test
    fun testAnalytics_FunnelRates() {
        // Active pipeline: Applied + Interview + Offer + Rejected (excluding Saved draft jobs)
        val activePipeline = sampleJobs.filter { it.status != "Saved" }
        val totalActive = activePipeline.size // 4
        val interviewOrBeyond = sampleJobs.count { it.status == "Interview" || it.status == "Offer" } // 2
        val offerCount = sampleJobs.count { it.status == "Offer" } // 1

        val callbackRate = if (totalActive > 0) (interviewOrBeyond.toDouble() / totalActive) * 100.0 else 0.0
        val offerConversionRate = if (interviewOrBeyond > 0) (offerCount.toDouble() / interviewOrBeyond) * 100.0 else 0.0

        assertEquals(50.0, callbackRate, 0.01)       // 2 / 4 = 50%
        assertEquals(50.0, offerConversionRate, 0.01) // 1 / 2 = 50%
    }

    @Test
    fun testAnalytics_ZeroApplicationsGracefulHandling() {
        val emptyList = emptyList<JobApplication>()
        val total = emptyList.size
        val callbackRate = if (total > 0) (0.0 / total) * 100.0 else 0.0

        assertEquals(0.0, callbackRate, 0.001)
        assertTrue("Callback rate must be a valid finite number, not NaN", !callbackRate.isNaN())
    }

    @Test
    fun testAnalytics_PlatformPerformanceRanking() {
        val platformStats = sampleJobs
            .filter { it.status != "Saved" }
            .groupBy { it.platform ?: "Other" }
            .mapValues { (_, jobs) ->
                val total = jobs.size
                val positiveResponses = jobs.count { it.status == "Interview" || it.status == "Offer" }
                val rate = (positiveResponses.toDouble() / total) * 100.0
                Pair(total, rate)
            }

        // LinkedIn: 2 active (Google=Interview, Apple=Offer) -> 100% response rate
        val linkedInStats = platformStats["LinkedIn"]
        assertEquals(2, linkedInStats?.first)
        assertEquals(100.0, linkedInStats?.second ?: 0.0, 0.01)

        // Indeed: 1 active (Netflix=Rejected) -> 0% response rate
        val indeedStats = platformStats["Indeed"]
        assertEquals(1, indeedStats?.first)
        assertEquals(0.0, indeedStats?.second ?: 0.0, 0.01)
    }
}
