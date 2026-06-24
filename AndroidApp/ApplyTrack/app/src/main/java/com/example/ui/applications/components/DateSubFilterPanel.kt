package com.example.ui.applications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSubFilterPanel(
    dateFilterState: DateFilterState,
    onUpdateFilter: (DateFilterState.() -> DateFilterState) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showDayDatePicker by remember { mutableStateOf(false) }
    var showStartRangeDatePicker by remember { mutableStateOf(false) }
    var showEndRangeDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var showInfoDialog by remember { mutableStateOf(false) }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Text(
                        text = "How Date Filtering Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "To help you track active timelines, date filters match the date of your most recent status change (such as when you originally applied, or when the role moved to Interview or Offer).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Note: This can differ from the Dashboard, which counts application volumes strictly based on the date they were first added to the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text(
                            text = "Got it",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            )
        }

        // Row of Mode Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Filter Type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Filtering Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            FilterChip(
                selected = dateFilterState.mode == DateFilterMode.MONTH,
                onClick = { onUpdateFilter { copy(mode = DateFilterMode.MONTH) } },
                label = { Text("Month") },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = dateFilterState.mode == DateFilterMode.DAY,
                onClick = { onUpdateFilter { copy(mode = DateFilterMode.DAY) } },
                label = { Text("Specific Day") },
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = dateFilterState.mode == DateFilterMode.RANGE,
                onClick = { onUpdateFilter { copy(mode = DateFilterMode.RANGE) } },
                label = { Text("Date Range") },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Inputs based on selected mode
        when (dateFilterState.mode) {
            DateFilterMode.MONTH -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Dropdown Trigger Button
                    Box(modifier = Modifier.weight(1f)) {
                        var isMonthDropdownExpanded by remember { mutableStateOf(false) }
                        val monthNames = listOf(
                            "January", "February", "March", "April", "May", "June",
                            "July", "August", "September", "October", "November", "December"
                        )

                        OutlinedTextField(
                            value = monthNames.getOrNull(dateFilterState.month - 1) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Month") },
                            trailingIcon = {
                                IconButton(onClick = { isMonthDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open Month Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Transparent clickable overlay
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
                                        onUpdateFilter { copy(month = index + 1) }
                                        isMonthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year Input Field
                    OutlinedTextField(
                        value = dateFilterState.year,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                onUpdateFilter { copy(year = newValue) }
                            }
                        },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
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
                val formattedDate = remember(dateFilterState.specificDate) {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateFilterState.specificDate }
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
                val formattedStart = remember(dateFilterState.startDate) {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateFilterState.startDate }
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
                }
                val formattedEnd = remember(dateFilterState.endDate) {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateFilterState.endDate }
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start Date
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

                    // End Date
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
    }

    // Material 3 DatePickerDialog Overlays
    if (showDayDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(dateFilterState.specificDate) {
                val localCal = Calendar.getInstance().apply { timeInMillis = dateFilterState.specificDate }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
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
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onUpdateFilter { copy(specificDate = localCal.timeInMillis) }
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(dateFilterState.startDate) {
                val localCal = Calendar.getInstance().apply { timeInMillis = dateFilterState.startDate }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
                }
                utcCal.timeInMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartRangeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
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
                            onUpdateFilter { copy(startDate = localCal.timeInMillis) }
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(dateFilterState.endDate) {
                val localCal = Calendar.getInstance().apply { timeInMillis = dateFilterState.endDate }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
                }
                utcCal.timeInMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndRangeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
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
                            onUpdateFilter { copy(endDate = localCal.timeInMillis) }
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
