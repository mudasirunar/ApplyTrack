package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.model.JobApplication
import com.example.ui.DashboardAnalytics

class ExampleUnitTest {

  @Test
  fun testDashboardAnalyticsCalculation() {
    val apps = listOf(
      JobApplication(status = "Applied"),
      JobApplication(status = "Saved"),
      JobApplication(status = "Interview"),
      JobApplication(status = "Interview"),
      JobApplication(status = "Rejected"),
      JobApplication(status = "Offer")
    )

    // Replicate mapping logic from ViewModel
    val stats = DashboardAnalytics(
      total = apps.size,
      applied = apps.count { it.status.equals("Applied", ignoreCase = true) },
      saved = apps.count { it.status.equals("Saved", ignoreCase = true) },
      interviews = apps.count { it.status.equals("Interview", ignoreCase = true) },
      rejected = apps.count { it.status.equals("Rejected", ignoreCase = true) },
      offers = apps.count { it.status.equals("Offer", ignoreCase = true) },
      responses = apps.count { it.status.equals("Interview", ignoreCase = true) } +
          apps.count { it.status.equals("Offer", ignoreCase = true) } +
          apps.count { it.status.equals("Rejected", ignoreCase = true) }
    )

    assertEquals(6, stats.total)
    assertEquals(1, stats.applied)
    assertEquals(1, stats.saved)
    assertEquals(2, stats.interviews)
    assertEquals(1, stats.rejected)
    assertEquals(1, stats.offers)
    assertEquals(4, stats.responses)
  }

  @Test
  fun testDashboardAnalyticsEmpty() {
    val stats = DashboardAnalytics()

    assertEquals(0, stats.total)
    assertEquals(0, stats.applied)
    assertEquals(0, stats.saved)
    assertEquals(0, stats.interviews)
    assertEquals(0, stats.rejected)
    assertEquals(0, stats.offers)
    assertEquals(0, stats.responses)
  }
}
