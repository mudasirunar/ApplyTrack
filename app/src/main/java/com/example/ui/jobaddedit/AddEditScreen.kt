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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

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
    var customPlatformName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
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
    LaunchedEffect(selectedApp, jobId) {
        if (jobId != null) {
            selectedApp?.let { app ->
                if (app.id == jobId) {
                    companyName = app.companyName ?: ""
                    role = app.role ?: ""
                    val rawPlatform = app.platform ?: "LinkedIn"
                    
                    val standardPlatforms = listOf("LinkedIn", "Indeed", "Email", "Website", "Other")
                    if (rawPlatform in standardPlatforms) {
                        platform = rawPlatform
                        customPlatformName = ""
                    } else {
                        platform = "Other"
                        customPlatformName = rawPlatform
                    }
                    
                    url = app.url ?: ""
                    email = app.email ?: ""
                    status = app.status
                    jobDescription = app.jobDescription ?: ""
                    notes = app.notes ?: ""
                    appliedDateEpoch = app.createdAt
                }
            }
        } else {
            companyName = ""
            role = ""
            platform = "LinkedIn"
            customPlatformName = ""
            url = ""
            email = ""
            status = "Applied"
            jobDescription = ""
            notes = ""
            appliedDateEpoch = System.currentTimeMillis()
        }
    }

    val platformOptions = listOf("LinkedIn", "Indeed", "Email", "Website", "Other")
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
                .imePadding()
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
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(8.dp)
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
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
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

                        // Conditional custom text input fields based on selected platform (only for Other)
                        if (platform == "Other") {
                            OutlinedTextField(
                                value = customPlatformName,
                                onValueChange = { customPlatformName = it },
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

                        // Conditional email address input field (only for Email platform)
                        if (platform == "Email") {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
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

                        // Permanent URL input field (always visible, not mandatory)
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") },
                            placeholder = { Text("e.g. company.com/careers or posting link") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_url"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

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
                            val finalPlatform = when (platform) {
                                "Other" -> customPlatformName.trim().ifEmpty { "Other" }
                                else -> platform
                            }
                            
                            val finalUrl = url.trim().ifEmpty { null }
                            val finalEmail = if (platform == "Email") email.trim().ifEmpty { null } else null

                            viewModel.saveJobApplication(
                                companyName = companyName,
                                role = role,
                                platform = finalPlatform,
                                status = status,
                                jobDescription = jobDescription,
                                notes = notes,
                                url = finalUrl,
                                email = finalEmail,
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
                            text = if (jobId == null) "Save Application" else "Update Application",
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
            initialSelectedDateMillis = remember(appliedDateEpoch) {
                val localCal = Calendar.getInstance().apply { timeInMillis = appliedDateEpoch }
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
                }
                utcCal.timeInMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
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
                            appliedDateEpoch = localCal.timeInMillis
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
