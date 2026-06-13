package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.StatusHistoryEntry
import com.example.model.Attachment
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStatusHistoryList(value: List<StatusHistoryEntry>?): String? {
        if (value == null) return null
        return value.joinToString(",") { "${it.status}|${it.timestamp}" }
    }

    @TypeConverter
    fun toStatusHistoryList(value: String?): List<StatusHistoryEntry>? {
        if (value.isNullOrBlank()) return null
        return value.split(",").mapNotNull { item ->
            val parts = item.split("|")
            if (parts.size == 2) {
                val status = parts[0]
                val timestamp = parts[1].toLongOrNull()
                if (timestamp != null) {
                    StatusHistoryEntry(status, timestamp)
                } else null
            } else null
        }
    }

    @TypeConverter
    fun fromAttachment(value: Attachment?): String? {
        if (value == null) return null
        val obj = JSONObject()
        obj.put("fileName", value.fileName)
        obj.put("originalName", value.originalName)
        return obj.toString()
    }

    @TypeConverter
    fun toAttachment(value: String?): Attachment? {
        if (value.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(value)
            Attachment(
                fileName = obj.getString("fileName"),
                originalName = obj.getString("originalName")
            )
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromAttachmentList(value: List<Attachment>?): String? {
        if (value == null) return null
        val array = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("fileName", item.fileName)
            obj.put("originalName", item.originalName)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toAttachmentList(value: String?): List<Attachment>? {
        if (value.isNullOrBlank()) return null
        val list = mutableListOf<Attachment>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Attachment(
                        fileName = obj.getString("fileName"),
                        originalName = obj.getString("originalName")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
