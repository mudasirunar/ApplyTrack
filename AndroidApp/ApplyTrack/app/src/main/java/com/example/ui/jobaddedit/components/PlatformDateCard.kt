package com.example.ui.jobaddedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDateCard(
    platform: String,
    onPlatformChange: (String) -> Unit,
    platformExpanded: Boolean,
    onPlatformExpandedChange: (Boolean) -> Unit,
    customPlatformName: String,
    onCustomPlatformNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    status: String,
    onStatusChange: (String) -> Unit,
    statusExpanded: Boolean,
    onStatusExpandedChange: (Boolean) -> Unit,
    formattedDateString: String,
    showDatePicker: Boolean,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val platformOptions = listOf("LinkedIn", "Indeed", "Email", "Website", "Other")
    val statusOptions = listOf("Applied", "Interview", "Rejected", "Offer", "Saved")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = platformExpanded,
                onExpandedChange = onPlatformExpandedChange
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = platform,
                    onValueChange = {},
                    label = { Text("Application Platform") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("dropdown_platform_trigger"),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = platformExpanded,
                    onDismissRequest = { onPlatformExpandedChange(false) }
                ) {
                    platformOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onPlatformChange(selectionOption)
                                onPlatformExpandedChange(false)
                            },
                            modifier = Modifier.testTag("platform_option_$selectionOption")
                        )
                    }
                }
            }

            if (platform == "Other") {
                OutlinedTextField(
                    value = customPlatformName,
                    onValueChange = onCustomPlatformNameChange,
                    label = { Text("Platform Name") },
                    placeholder = { Text("e.g. Glassdoor, CareerBuilder") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_platform"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            if (platform == "Email") {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("e.g. recruiter@company.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("URL") },
                placeholder = { Text("e.g. company.com/careers or posting link") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_url"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = onStatusExpandedChange
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = status,
                    onValueChange = {},
                    label = { Text("Current Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("dropdown_status_trigger"),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { onStatusExpandedChange(false) }
                ) {
                    statusOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onStatusChange(selectionOption)
                                onStatusExpandedChange(false)
                            },
                            modifier = Modifier.testTag("status_option_$selectionOption")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formattedDateString,
                onValueChange = {},
                readOnly = true,
                label = { Text("Status Date") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Trigger Calendar Picker",
                        tint = if (showDatePicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onDateClick()
                        }
                    }
                    .testTag("input_date_applied"),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
