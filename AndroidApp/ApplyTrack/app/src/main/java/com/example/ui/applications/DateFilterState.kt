package com.example.ui.applications

import java.util.Calendar

data class DateFilterState(
    val mode: DateFilterMode = DateFilterMode.MONTH,
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, // 1..12
    val year: String = Calendar.getInstance().get(Calendar.YEAR).toString(),
    val specificDate: Long = System.currentTimeMillis(),
    val startDate: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis,
    val endDate: Long = System.currentTimeMillis()
)

enum class DateFilterMode {
    MONTH,
    DAY,
    RANGE
}
