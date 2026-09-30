package com.example.ui.applications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Compact summary bar displayed directly under the filter chips on the main screen.
 * Shows the upgraded tonal basis chip, date detail, and "Edit" action to trigger the Bottom Sheet.
 */
@Composable
fun DateFilterSummaryBar(
    dateFilterState: DateFilterState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val basisText =
        if (dateFilterState.basis == DateBasis.STATUS_UPDATED) "Status Updated" else "Date Added"
    val detailText = when (dateFilterState.mode) {
        DateFilterMode.MONTH -> "${monthNames.getOrNull(dateFilterState.month - 1) ?: ""} ${dateFilterState.year}"
        DateFilterMode.DAY -> {
            val cal = Calendar.getInstance().apply { timeInMillis = dateFilterState.specificDate }
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(cal.time)
        }
        DateFilterMode.RANGE -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            val sdfYear = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${sdf.format(Date(dateFilterState.startDate))} – ${sdfYear.format(Date(dateFilterState.endDate))}"
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                // Refined Tonal Basis Chip
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = basisText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Edit Date Filter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Bottom Sheet content for configuring the date filter.
 * Contains Date Basis selector, Filter Type selector, and date input controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterBottomSheetContent(
    dateFilterState: DateFilterState,
    onDismiss: () -> Unit,
    onUpdateFilter: (DateFilterState.() -> DateFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var draftState by remember(dateFilterState) { mutableStateOf(dateFilterState) }
    var showDayDatePicker by remember { mutableStateOf(false) }
    var showStartRangeDatePicker by remember { mutableStateOf(false) }
    var showEndRangeDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter by Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // Date Basis Section
        Text(
            text = "Date Basis",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = draftState.basis == DateBasis.DATE_ADDED,
                onClick = { draftState = draftState.copy(basis = DateBasis.DATE_ADDED) },
                label = { Text("Date Added") },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = draftState.basis == DateBasis.STATUS_UPDATED,
                onClick = { draftState = draftState.copy(basis = DateBasis.STATUS_UPDATED) },
                label = { Text("Status Updated") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Type Section
        Text(
            text = "Filter Type",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = draftState.mode == DateFilterMode.MONTH,
                onClick = { draftState = draftState.copy(mode = DateFilterMode.MONTH) },
                label = { Text("Month") },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = draftState.mode == DateFilterMode.DAY,
                onClick = { draftState = draftState.copy(mode = DateFilterMode.DAY) },
                label = { Text("Specific Day") },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = draftState.mode == DateFilterMode.RANGE,
                onClick = { draftState = draftState.copy(mode = DateFilterMode.RANGE) },
                label = { Text("Date Range") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Mode Inputs
        when (draftState.mode) {
            DateFilterMode.MONTH -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        var isMonthDropdownExpanded by remember { mutableStateOf(false) }
                        val monthNames = listOf(
                            "January", "February", "March", "April",
                            "May", "June", "July", "August",
                            "September", "October", "November", "December"
                        )

                        OutlinedTextField(
                            value = monthNames.getOrNull(draftState.month - 1) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Month") },
                            trailingIcon = {
                                IconButton(onClick = { isMonthDropdownExpanded = true }) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Open Month Dropdown"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isMonthDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isMonthDropdownExpanded,
                            onDismissRequest = { isMonthDropdownExpanded = false }
                        ) {
                            monthNames.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        draftState = draftState.copy(month = index + 1)
                                        isMonthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = draftState.year,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                draftState = draftState.copy(year = newValue)
                            }
                        },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            DateFilterMode.DAY -> {
                val formattedDate = remember(draftState.specificDate) {
                    val cal = Calendar.getInstance()
                        .apply { timeInMillis = draftState.specificDate }
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formattedDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Date") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showDayDatePicker = true
                            }
                    )
                }
            }

            DateFilterMode.RANGE -> {
                val formattedStart = remember(draftState.startDate) {
                    val cal = Calendar.getInstance()
                        .apply { timeInMillis = draftState.startDate }
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
                }
                val formattedEnd = remember(draftState.endDate) {
                    val cal = Calendar.getInstance()
                        .apply { timeInMillis = draftState.endDate }
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = formattedStart,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start Date") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showStartRangeDatePicker = true
                                }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = formattedEnd,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End Date") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showEndRangeDatePicker = true
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Done / Apply Action Button
        Button(
            onClick = {
                val finalStart = if (draftState.mode == DateFilterMode.RANGE) minOf(draftState.startDate, draftState.endDate) else draftState.startDate
                val finalEnd = if (draftState.mode == DateFilterMode.RANGE) maxOf(draftState.startDate, draftState.endDate) else draftState.endDate
                val appliedState = draftState.copy(startDate = finalStart, endDate = finalEnd)
                onUpdateFilter { appliedState }
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Apply Filter",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Date Picker Dialogs
    if (showDayDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(draftState.specificDate) {
                val localCal =
                    Calendar.getInstance().apply { timeInMillis = draftState.specificDate }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(
                        localCal.get(Calendar.YEAR),
                        localCal.get(Calendar.MONTH),
                        localCal.get(Calendar.DAY_OF_MONTH)
                    )
                }
                utcCal.timeInMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDayDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal =
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = utcMillis
                                }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            draftState = draftState.copy(specificDate = localCal.timeInMillis)
                        }
                        showDayDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDayDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartRangeDatePicker) {
        val maxEndUtcMillis = remember(draftState.endDate) {
            val localCal =
                Calendar.getInstance().apply { timeInMillis = draftState.endDate }
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH)
                )
            }
            utcCal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(draftState.startDate) {
                val localCal =
                    Calendar.getInstance().apply { timeInMillis = draftState.startDate }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(
                        localCal.get(Calendar.YEAR),
                        localCal.get(Calendar.MONTH),
                        localCal.get(Calendar.DAY_OF_MONTH)
                    )
                }
                utcCal.timeInMillis
            },
            selectableDates = remember(maxEndUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis <= maxEndUtcMillis
                    }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartRangeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal =
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = utcMillis
                                }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val newStart = localCal.timeInMillis
                            val newEnd = if (newStart > draftState.endDate) {
                                val endCal = (localCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                endCal.timeInMillis
                            } else {
                                draftState.endDate
                            }
                            draftState = draftState.copy(startDate = newStart, endDate = newEnd)
                        }
                        showStartRangeDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartRangeDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndRangeDatePicker) {
        val minStartUtcMillis = remember(draftState.startDate) {
            val localCal =
                Calendar.getInstance().apply { timeInMillis = draftState.startDate }
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH)
                )
            }
            utcCal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(draftState.endDate) {
                val localCal =
                    Calendar.getInstance().apply { timeInMillis = draftState.endDate }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(
                        localCal.get(Calendar.YEAR),
                        localCal.get(Calendar.MONTH),
                        localCal.get(Calendar.DAY_OF_MONTH)
                    )
                }
                utcCal.timeInMillis
            },
            selectableDates = remember(minStartUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= minStartUtcMillis
                    }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndRangeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal =
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = utcMillis
                                }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            val newEnd = localCal.timeInMillis
                            val newStart = if (newEnd < draftState.startDate) {
                                val startCal = (localCal.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                startCal.timeInMillis
                            } else {
                                draftState.startDate
                            }
                            draftState = draftState.copy(startDate = newStart, endDate = newEnd)
                        }
                        showEndRangeDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndRangeDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Backward compatibility wrapper
 */
@Composable
fun DateSubFilterPanel(
    dateFilterState: DateFilterState,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onUpdateFilter: (DateFilterState.() -> DateFilterState) -> Unit
) {
    DateFilterSummaryBar(
        dateFilterState = dateFilterState,
        onClick = onToggleExpand
    )
}