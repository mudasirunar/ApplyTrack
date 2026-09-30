package com.example.ui.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardMessageProviderTest {

    @Test
    fun testGetDashboardMessage_ZeroCountReturnsNonEmpty() {
        val message = DashboardMessageProvider.getDashboardMessage(0)
        assertNotNull(message)
        assertTrue(message.isNotBlank())
    }

    @Test
    fun testGetDashboardMessage_NegativeCountReturnsNonEmpty() {
        val message = DashboardMessageProvider.getDashboardMessage(-10)
        assertNotNull(message)
        assertTrue(message.isNotBlank())
    }

    @Test
    fun testGetDashboardMessage_LowCountBoundaries() {
        for (count in listOf(1, 2, 3)) {
            val message = DashboardMessageProvider.getDashboardMessage(count)
            assertNotNull(message)
            assertTrue("Message for count $count should not be blank", message.isNotBlank())
        }
    }

    @Test
    fun testGetDashboardMessage_MediumCountBoundaries() {
        for (count in listOf(4, 5, 8, 9)) {
            val message = DashboardMessageProvider.getDashboardMessage(count)
            assertNotNull(message)
            assertTrue("Message for count $count should not be blank", message.isNotBlank())
        }
    }

    @Test
    fun testGetDashboardMessage_HighCountBoundaries() {
        for (count in listOf(10, 14, 19)) {
            val message = DashboardMessageProvider.getDashboardMessage(count)
            assertNotNull(message)
            assertTrue("Message for count $count should not be blank", message.isNotBlank())
        }
    }

    @Test
    fun testGetDashboardMessage_VeryHighCountBoundaries() {
        for (count in listOf(20, 35, 49)) {
            val message = DashboardMessageProvider.getDashboardMessage(count)
            assertNotNull(message)
            assertTrue("Message for count $count should not be blank", message.isNotBlank())
        }
    }

    @Test
    fun testGetDashboardMessage_UltraHighCountBoundaries() {
        for (count in listOf(50, 75, 99)) {
            val message = DashboardMessageProvider.getDashboardMessage(count)
            assertNotNull(message)
            assertTrue("Message for count $count should not be blank", message.isNotBlank())
        }
    }

    @Test
    fun testGetDashboardMessage_LegendaryCountBoundaries() {
        for (count in listOf(100, 150, 500, 1000)) {
            val message = DashboardMessageProvider.getDashboardMessage(count)
            assertNotNull(message)
            assertTrue("Message for count $count should not be blank", message.isNotBlank())
        }
    }

    @Test
    fun testGetDashboardMessage_RandomnessReturnsVaryingMessages() {
        // Across 50 calls for a given count, it should hit multiple distinct quotes
        val messages = (1..50).map { DashboardMessageProvider.getDashboardMessage(15) }.toSet()
        assertTrue("Should produce multiple unique messages across iterations", messages.size > 1)
    }
}
