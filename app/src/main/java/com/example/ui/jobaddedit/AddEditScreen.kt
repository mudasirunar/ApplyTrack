package com.example.ui.jobaddedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.JobViewModel
import com.example.model.JobApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: JobViewModel,
    jobId: Long?,
    onNavigateBack: () -> Unit
) {
    val selectedApp by viewModel.selectedApplication.collectAsStateWithLifecycle()

    // Load job data if editing
    LaunchedEffect(jobId) {
        if (jobId != null) {
            viewModel.loadApplicationById(jobId)
        } else {
            viewModel.clearSelectedApplication()
        }
    }

    // Input States
    var companyName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("LinkedIn") }
    var status by remember { mutableStateOf("Applied") }
    var jobDescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var appliedDateEpoch by remember { mutableStateOf(System.currentTimeMillis()) }

    // Dropdown expanded states
    var platformExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    // Date Picker show state
    var showDatePicker by remember { mutableStateOf(false) }

    // Sync state when loading existing values
    LaunchedEffect(selectedApp) {
        selectedApp?.let { app ->
            companyName = app.companyName ?: ""
            role = app.role ?: ""
            platform = app.platform ?: "LinkedIn"
            status = app.status
            jobDescription = app.jobDescription ?: ""
            notes = app.notes ?: ""
            appliedDateEpoch = app.createdAt
        }
    }

    val platformOptions = listOf("LinkedIn", "Email", "Website", "Other")
    val statusOptions = listOf("Applied", "Interview", "Rejected", "Offer", "Saved")

    val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val formattedDateString = remember(appliedDateEpoch) { sdf.format(Date(appliedDateEpoch)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (jobId == null) "New Application" else "Edit Application",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Scrollable central input fields column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .padding(bottom = 90.dp), // space for fixed save button
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Job Core Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Job Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Company name input
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name") },
                            placeholder = { Text("e.g. Acme Corp") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_company_name"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Role / Position input
                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role / Position") },
                            placeholder = { Text("e.g. Senior Frontend Engineer") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_role"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Section 2: Platform and Calendar Info metadata
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Platform dropdown box selector
                        ExposedDropdownMenuBox(
                            expanded = platformExpanded,
                            onExpandedChange = { platformExpanded = !platformExpanded }
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
                                onDismissRequest = { platformExpanded = false }
                            ) {
                                platformOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            platform = selectionOption
                                            platformExpanded = false
                                        },
                                        modifier = Modifier.testTag("platform_option_$selectionOption")
                                    )
                                }
                            }
                        }

                        // Status dropdown box selector
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded }
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
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                statusOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            status = selectionOption
                                            statusExpanded = false
                                        },
                                        modifier = Modifier.testTag("status_option_$selectionOption")
                                    )
                                }
                            }
                        }

                        // Interactive Date Picker Trigger Field
                        OutlinedTextField(
                            value = formattedDateString,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date Applied") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Trigger Calendar Picker"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .testTag("input_date_applied"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Section 3: Large textareas descriptions & notes
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Job Description Body
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Job Description / Posting Link") },
                            placeholder = { Text("Paste the job description or link here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .testTag("input_job_description"),
                            maxLines = 10,
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Personal Notes Body
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Personal Notes") },
                            placeholder = { Text("Any thoughts, recruiter names, or specific things to remember...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .testTag("input_job_notes"),
                            maxLines = 10,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Fixed Bottom Action container supporting Save actions
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveJobApplication(
                                companyName = companyName,
                                role = role,
                                platform = platform,
                                status = status,
                                jobDescription = jobDescription,
                                notes = notes,
                                timeApplied = appliedDateEpoch,
                                onSuccess = onNavigateBack
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_application_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Save Application",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    // Material 3 Native DatePickerDialog Overlay Handler State
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = appliedDateEpoch
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            appliedDateEpoch = it
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("datepicker_confirm")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag("datepicker_dismiss")
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
