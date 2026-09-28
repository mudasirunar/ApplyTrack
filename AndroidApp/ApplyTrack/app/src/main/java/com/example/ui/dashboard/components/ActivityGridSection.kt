package com.example.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** yyyyMMdd key for a timestamp, in the device's local time zone. */
fun activityDayKey(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
}

private data class ActivityCell(val timeMillis: Long, val inRange: Boolean, val future: Boolean, val count: Int)
private data class MonthLabel(val week: Int, val text: String)
private data class ActivityGridData(
    val weeks: List<List<ActivityCell>>,
    val monthLabels: List<MonthLabel>,
    val total: Int,
    val max: Int,
    val todayWeek: Int
)

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun startOfDay(c: Calendar): Calendar = (c.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

private fun buildGrid(selectedYear: Int?, counts: Map<Int, Int>): ActivityGridData {
    val today = startOfDay(Calendar.getInstance())
    val start: Calendar
    val end: Calendar
    if (selectedYear == null) {
        end = today.clone() as Calendar
        start = (today.clone() as Calendar).apply { add(Calendar.YEAR, -1); add(Calendar.DAY_OF_YEAR, 1) }
    } else {
        start = Calendar.getInstance().apply { clear(); set(selectedYear, Calendar.JANUARY, 1) }
        end = Calendar.getInstance().apply { clear(); set(selectedYear, Calendar.DECEMBER, 31) }
    }

    // Grid starts on the Monday on/before `start`
    val dow = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon = 0 ... Sun = 6
    val cursor = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -dow) }
    val spanDays = ((end.timeInMillis - cursor.timeInMillis) / 86_400_000.0).roundToInt() + 1
    val weekCount = ceil(spanDays / 7.0).toInt()

    val weeks = ArrayList<List<ActivityCell>>(weekCount)
    val labels = ArrayList<MonthLabel>()
    var total = 0
    var maxCount = 0
    var lastMonth = -1
    var todayWeek = weekCount - 1

    for (w in 0 until weekCount) {
        val days = ArrayList<ActivityCell>(7)
        var firstVisibleMonth = -1
        for (d in 0 until 7) {
            val millis = cursor.timeInMillis
            val inRange = millis >= start.timeInMillis && millis <= end.timeInMillis
            val future = millis > today.timeInMillis
            val count = if (inRange) counts[activityDayKey(millis)] ?: 0 else 0
            if (inRange) {
                total += count
                maxCount = max(maxCount, count)
                if (firstVisibleMonth == -1) firstVisibleMonth = cursor.get(Calendar.MONTH)
            }
            if (millis == today.timeInMillis) todayWeek = w
            days.add(ActivityCell(millis, inRange, future, count))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        weeks.add(days)
        if (firstVisibleMonth != -1 && firstVisibleMonth != lastMonth) {
            lastMonth = firstVisibleMonth
            labels.add(MonthLabel(w, MONTHS[firstVisibleMonth]))
        }
    }
    // Drop a label that would collide with the next one
    val monthLabels = labels.filterIndexed { i, l -> i == labels.lastIndex || labels[i + 1].week - l.week >= 3 }
    return ActivityGridData(weeks, monthLabels, total, maxCount, todayWeek)
}

private fun levelFor(count: Int, max: Int): Int {
    if (count <= 0) return 0
    return min(4, ceil(count.toDouble() / max(max, 4) * 4).toInt())
}

private val CELL: Dp = 12.dp
private val GAP: Dp = 3.dp
private val MONTH_LABEL_HEIGHT: Dp = 16.dp

@Composable
fun ActivityGridSection(
    counts: Map<Int, Int>,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selection by rememberSaveable { mutableStateOf<Int?>(null) } // null = last year
    val years = remember(counts) { counts.keys.map { it / 10000 }.distinct().sortedDescending() }
    val active = selection?.takeIf { it in years }
    val data = remember(active, counts) { buildGrid(active, counts) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = remember(isDark) {
        if (isDark) listOf(
            Color.White.copy(alpha = 0.08f), Color(0xFF0E4429), Color(0xFF006D32), Color(0xFF26A641), Color(0xFF39D353)
        ) else listOf(
            Color.Gray.copy(alpha = 0.16f), Color(0xFF9BE9A8), Color(0xFF40C463), Color(0xFF30A14E), Color(0xFF216E39)
        )
    }

    val scrollState = rememberScrollState()
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    // Last year -> latest weeks, current year -> around today, other years -> January
    LaunchedEffect(active, data.weeks.size) {
        withFrameNanos { }
        val maxScroll = scrollState.maxValue
        val target = when (active) {
            null -> maxScroll
            currentYear -> (maxScroll * (data.todayWeek.toFloat() / data.weeks.size)).roundToInt()
            else -> 0
        }
        scrollState.scrollTo(target)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Application Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                YearDropdown(selected = active, years = years, onSelect = { selection = it })
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                // Fixed day labels
                Column(
                    modifier = Modifier.width(32.dp).padding(top = MONTH_LABEL_HEIGHT),
                    verticalArrangement = Arrangement.spacedBy(GAP)
                ) {
                    val dayLabels = mapOf(0 to "Mon", 2 to "Wed", 4 to "Fri", 6 to "Sun")
                    repeat(7) { d ->
                        Box(Modifier.height(CELL), contentAlignment = Alignment.CenterStart) {
                            dayLabels[d]?.let {
                                Text(
                                    text = it,
                                    fontSize = 9.sp,
                                    lineHeight = 9.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Scrollable grid
                Box(Modifier.weight(1f).horizontalScroll(scrollState)) {
                    ActivityCanvas(data = data, palette = palette, onDayClick = onDayClick)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${data.total} application${if (data.total == 1) "" else "s"} " +
                            if (active == null) "in the last year" else "in $active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    palette.forEach { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(it)) }
                    Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActivityCanvas(
    data: ActivityGridData,
    palette: List<Color>,
    onDayClick: (Long) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val weekCount = data.weeks.size

    Canvas(
        modifier = Modifier
            .width((CELL + GAP) * weekCount)
            .height(MONTH_LABEL_HEIGHT + (CELL + GAP) * 7)
            .pointerInput(data) {
                val stepPx = (CELL + GAP).toPx()
                val labelPx = MONTH_LABEL_HEIGHT.toPx()
                detectTapGestures { offset ->
                    val w = (offset.x / stepPx).toInt()
                    val d = ((offset.y - labelPx) / stepPx).toInt()
                    if (offset.y < labelPx || w !in 0 until weekCount || d !in 0..6) return@detectTapGestures
                    val cell = data.weeks[w][d]
                    if (cell.inRange && !cell.future && cell.count > 0) onDayClick(cell.timeMillis)
                }
            }
    ) {
        val cellPx = CELL.toPx()
        val stepPx = (CELL + GAP).toPx()
        val labelPx = MONTH_LABEL_HEIGHT.toPx()
        val radius = CornerRadius(2.dp.toPx())

        data.monthLabels.forEach { l ->
            drawText(textMeasurer, l.text, topLeft = Offset(l.week * stepPx, 0f), style = labelStyle)
        }

        data.weeks.forEachIndexed { w, days ->
            days.forEachIndexed { d, cell ->
                if (!cell.inRange) return@forEachIndexed
                val base = palette[levelFor(cell.count, data.max)]
                val color = if (cell.future) base.copy(alpha = base.alpha * 0.5f) else base
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * stepPx, labelPx + d * stepPx),
                    size = Size(cellPx, cellPx),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
private fun YearDropdown(selected: Int?, years: List<Int>, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable { expanded = true }
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected?.toString() ?: "Last year",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Last year") }, onClick = { onSelect(null); expanded = false })
            years.forEach { y ->
                DropdownMenuItem(text = { Text(y.toString()) }, onClick = { onSelect(y); expanded = false })
            }
        }
    }
}