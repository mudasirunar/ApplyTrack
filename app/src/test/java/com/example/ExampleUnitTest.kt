package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.model.JobApplication
import com.example.ui.DashboardStats

class ExampleUnitTest {

  @Test
  fun testDashboardStatsCalculation() {
    val apps = listOf(
      JobApplication(status = "Applied"),
      JobApplication(status = "Saved"),
      JobApplication(status = "Interview"),
      JobApplication(status = "Interview"),
      JobApplication(status = "Rejected"),
      JobApplication(status = "Offer")
    )

    // Replicate mapping logic from ViewModel
    val stats = DashboardStats(
      total = apps.size,
      interviews = apps.count { it.status.equals("Interview", ignoreCase = true) },
      rejected = apps.count { it.status.equals("Rejected", ignoreCase = true) },
      offers = apps.count { it.status.equals("Offer", ignoreCase = true) },
      saved = apps.count { it.status.equals("Saved", ignoreCase = true) || it.status.equals("Applied", ignoreCase = true) }
    )

    assertEquals(6, stats.total)
    assertEquals(2, stats.interviews)
    assertEquals(1, stats.rejected)
    assertEquals(1, stats.offers)
    assertEquals(2, stats.saved)
  }

  @Test
  fun testDashboardStatsEmpty() {
    val apps = emptyList<JobApplication>()
    val stats = DashboardStats(
      total = apps.size,
      interviews = apps.count { it.status.equals("Interview", ignoreCase = true) },
      rejected = apps.count { it.status.equals("Rejected", ignoreCase = true) },
      offers = apps.count { it.status.equals("Offer", ignoreCase = true) },
      saved = apps.count { it.status.equals("Saved", ignoreCase = true) || it.status.equals("Applied", ignoreCase = true) }
    )

    assertEquals(0, stats.total)
    assertEquals(0, stats.interviews)
    assertEquals(0, stats.rejected)
    assertEquals(0, stats.offers)
    assertEquals(0, stats.saved)
  }
}
