package com.example.data.local

import com.example.model.Attachment
import com.example.model.StatusHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    // =========================================================================
    // Status History Tests
    // =========================================================================

    @Test
    fun testStatusHistory_NullAndEmptyHandling() {
        assertNull(converters.fromStatusHistoryList(null))
        assertNull(converters.toStatusHistoryList(null))
        assertNull(converters.toStatusHistoryList(""))
        assertNull(converters.toStatusHistoryList("   "))
    }

    @Test
    fun testStatusHistory_RoundTripConversion() {
        val original = listOf(
            StatusHistoryEntry(status = "Saved", timestamp = 1700000000000L),
            StatusHistoryEntry(status = "Applied", timestamp = 1700000100000L),
            StatusHistoryEntry(status = "Interview", timestamp = 1700000200000L),
            StatusHistoryEntry(status = "Offer", timestamp = 1700000300000L)
        )

        val serialized = converters.fromStatusHistoryList(original)
        assertNotNull(serialized)
        assertEquals("Saved|1700000000000,Applied|1700000100000,Interview|1700000200000,Offer|1700000300000", serialized)

        val deserialized = converters.toStatusHistoryList(serialized)
        assertNotNull(deserialized)
        assertEquals(4, deserialized!!.size)
        assertEquals("Saved", deserialized[0].status)
        assertEquals(1700000000000L, deserialized[0].timestamp)
        assertEquals("Offer", deserialized[3].status)
        assertEquals(1700000300000L, deserialized[3].timestamp)
    }

    @Test
    fun testStatusHistory_MalformedStringGracefulHandling() {
        // Missing pipe, non-numeric timestamp, and empty item should be skipped safely
        val malformed = "Saved,Applied|not_a_number,Interview|1700000500000,,BadData"
        val result = converters.toStatusHistoryList(malformed)

        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals("Interview", result[0].status)
        assertEquals(1700000500000L, result[0].timestamp)
    }

    // =========================================================================
    // Single Attachment Tests
    // =========================================================================

    @Test
    fun testSingleAttachment_NullAndEmptyHandling() {
        assertNull(converters.fromAttachment(null))
        assertNull(converters.toAttachment(null))
        assertNull(converters.toAttachment(""))
        assertNull(converters.toAttachment("invalid_json_string"))
    }

    @Test
    fun testSingleAttachment_RoundTrip() {
        val attachment = Attachment(
            fileName = "resume_uuid_12345.pdf",
            originalName = "Software_Engineer_Resume.pdf"
        )

        val json = converters.fromAttachment(attachment)
        assertNotNull(json)

        val restored = converters.toAttachment(json)
        assertNotNull(restored)
        assertEquals("resume_uuid_12345.pdf", restored!!.fileName)
        assertEquals("Software_Engineer_Resume.pdf", restored.originalName)
    }

    // =========================================================================
    // Attachment List Tests
    // =========================================================================

    @Test
    fun testAttachmentList_NullAndEmptyHandling() {
        assertNull(converters.fromAttachmentList(null))
        assertNull(converters.toAttachmentList(null))
        assertNull(converters.toAttachmentList(""))
    }

    @Test
    fun testAttachmentList_RoundTrip() {
        val attachments = listOf(
            Attachment(fileName = "file_1.pdf", originalName = "Resume_2026.pdf"),
            Attachment(fileName = "file_2.png", originalName = "Assessment_Result.png"),
            Attachment(fileName = "file_3.pdf", originalName = "Cover_Letter.pdf")
        )

        val jsonArray = converters.fromAttachmentList(attachments)
        assertNotNull(jsonArray)

        val restoredList = converters.toAttachmentList(jsonArray)
        assertNotNull(restoredList)
        assertEquals(3, restoredList!!.size)
        assertEquals("file_1.pdf", restoredList[0].fileName)
        assertEquals("Resume_2026.pdf", restoredList[0].originalName)
        assertEquals("file_3.pdf", restoredList[2].fileName)
        assertEquals("Cover_Letter.pdf", restoredList[2].originalName)
    }

    @Test
    fun testAttachmentList_CorruptJsonReturnsEmpty() {
        val corrupt = "{ not an array }"
        val result = converters.toAttachmentList(corrupt)
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }
}
