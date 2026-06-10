package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.StatusHistoryEntry

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
}
