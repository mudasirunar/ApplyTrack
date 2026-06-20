package com.example.ui.jobaddedit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.Attachment
import com.example.ui.JobViewModel
import com.example.utils.AttachmentHelper
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import com.example.ui.components.AttachmentRow
import com.example.ui.components.ScreenAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: JobViewModel,
    jobId: Long?,
    onNavigateBack: () -> Unit
) {
    val selectedApp by viewModel.selectedApplication.collectAsStateWithLifecycle()

    var hasLoadedOnce by remember(jobId) { mutableStateOf(false) }
    LaunchedEffect(selectedApp, jobId) {
        if (jobId != null && selectedApp != null && selectedApp?.id == jobId) {
            hasLoadedOnce = true
        }
    }

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

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var resumeState by remember { mutableStateOf<ScreenAttachment?>(null) }
    var coverLetterState by remember { mutableStateOf<ScreenAttachment?>(null) }
    var additionalDocumentState by remember { mutableStateOf<ScreenAttachment?>(null) }
    var screenshotsList by remember { mutableStateOf<List<ScreenAttachment>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    BackHandler(enabled = isSaving) {
        // Prevent system/gesture back navigation while saving
    }

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
                    appliedDateEpoch = app.statusHistory?.lastOrNull()?.timestamp ?: app.createdAt

                    resumeState = app.resume?.let { ScreenAttachment(attachment = it) }
                    coverLetterState = app.coverLetter?.let { ScreenAttachment(attachment = it) }
                    additionalDocumentState = app.additionalDocument?.let { ScreenAttachment(attachment = it) }
                    screenshotsList = app.screenshots?.map { ScreenAttachment(attachment = it) } ?: emptyList()
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

            resumeState = null
            coverLetterState = null
            additionalDocumentState = null
            screenshotsList = emptyList()
        }
    }

    val platformOptions = listOf("LinkedIn", "Indeed", "Email", "Website", "Other")
    val statusOptions = listOf("Applied", "Interview", "Rejected", "Offer", "Saved")

    val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val formattedDateString = remember(appliedDateEpoch) { sdf.format(Date(appliedDateEpoch)) }

    val resumeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { resumeState = ScreenAttachment(uri = it) }
    }

    val coverLetterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { coverLetterState = ScreenAttachment(uri = it) }
    }

    val additionalDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { additionalDocumentState = ScreenAttachment(uri = it) }
    }

    val screenshotsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (screenshotsList.size < 3) {
                screenshotsList = screenshotsList + ScreenAttachment(uri = it)
            } else {
                Toast.makeText(context, "Maximum 3 screenshots allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            key(MaterialTheme.colorScheme.surface) {
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
                            enabled = !isSaving,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                                tint = if (isSaving) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
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
                                        showDatePicker = true
                                    }
                                }
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

                // Section 4: Attachments Bento Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Documents & Attachments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AttachmentRow(
                            label = "Resume / CV",
                            attachment = resumeState,
                            onPickFile = { resumeLauncher.launch("application/pdf") },
                            onRemove = { resumeState = null }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        AttachmentRow(
                            label = "Cover Letter",
                            attachment = coverLetterState,
                            onPickFile = { coverLetterLauncher.launch("application/pdf") },
                            onRemove = { coverLetterState = null }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        AttachmentRow(
                            label = "Additional Document",
                            attachment = additionalDocumentState,
                            onPickFile = { additionalDocLauncher.launch("application/pdf") },
                            onRemove = { additionalDocumentState = null }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        // Screenshots Grid
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Screenshots / Additional Images",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                screenshotsList.forEachIndexed { index, item ->
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    ) {
                                        if (item.attachment != null) {
                                            val file = AttachmentHelper.getAttachmentFile(context, item.attachment.fileName)
                                            AsyncImage(
                                                model = file,
                                                contentDescription = "Screenshot",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            AsyncImage(
                                                model = item.uri,
                                                contentDescription = "Screenshot",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Delete badge
                                        IconButton(
                                            onClick = {
                                                screenshotsList = screenshotsList.toMutableList().apply { removeAt(index) }
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                .padding(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete screenshot",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                if (screenshotsList.size < 3) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable { screenshotsLauncher.launch("image/*") }
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Add Screenshot",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Add (${screenshotsList.size}/3)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                            if (isSaving) return@Button
                            isSaving = true
                            scope.launch {
                                try {
                                    // Capture Compose states on the Main thread
                                    val resumeVal = resumeState
                                    val coverLetterVal = coverLetterState
                                    val additionalDocVal = additionalDocumentState
                                    val screenshotsVal = screenshotsList
                                    val currentJobVal = selectedApp
                                    val companyNameVal = companyName
                                    val roleVal = role
                                    val platformVal = platform
                                    val customPlatformNameVal = customPlatformName
                                    val urlVal = url
                                    val emailVal = email
                                    val statusVal = status
                                    val jobDescriptionVal = jobDescription
                                    val notesVal = notes
                                    val appliedDateEpochVal = appliedDateEpoch

                                    var finalResume: Attachment? = null
                                    var finalCoverLetter: Attachment? = null
                                    var finalAdditionalDoc: Attachment? = null
                                    var finalScreenshots: List<Attachment> = emptyList()

                                    // Switch to Dispatchers.IO for background copying and cleaning
                                    withContext(Dispatchers.IO) {
                                        // 1. Copy Resume
                                        finalResume = resumeVal?.let { item ->
                                            if (item.attachment != null) {
                                                item.attachment
                                            } else {
                                                val uri = item.uri!!
                                                val origName = AttachmentHelper.getFileName(context, uri)
                                                val ext = AttachmentHelper.getFileExtension(origName)
                                                val uniqueName = "${UUID.randomUUID()}.$ext"
                                                AttachmentHelper.copyUriToInternalStorage(context, uri, uniqueName)
                                                Attachment(uniqueName, origName)
                                            }
                                        }

                                        // 2. Copy Cover Letter
                                        finalCoverLetter = coverLetterVal?.let { item ->
                                            if (item.attachment != null) {
                                                item.attachment
                                            } else {
                                                val uri = item.uri!!
                                                val origName = AttachmentHelper.getFileName(context, uri)
                                                val ext = AttachmentHelper.getFileExtension(origName)
                                                val uniqueName = "${UUID.randomUUID()}.$ext"
                                                AttachmentHelper.copyUriToInternalStorage(context, uri, uniqueName)
                                                Attachment(uniqueName, origName)
                                            }
                                        }

                                        // 3. Copy Additional Document
                                        finalAdditionalDoc = additionalDocVal?.let { item ->
                                            if (item.attachment != null) {
                                                item.attachment
                                            } else {
                                                val uri = item.uri!!
                                                val origName = AttachmentHelper.getFileName(context, uri)
                                                val ext = AttachmentHelper.getFileExtension(origName)
                                                val uniqueName = "${UUID.randomUUID()}.$ext"
                                                AttachmentHelper.copyUriToInternalStorage(context, uri, uniqueName)
                                                Attachment(uniqueName, origName)
                                            }
                                        }

                                        // 4. Copy Screenshots
                                        finalScreenshots = screenshotsVal.map { item ->
                                            if (item.attachment != null) {
                                                item.attachment
                                            } else {
                                                val uri = item.uri!!
                                                val origName = AttachmentHelper.getFileName(context, uri)
                                                val ext = AttachmentHelper.getFileExtension(origName)
                                                val uniqueName = "${UUID.randomUUID()}.$ext"
                                                AttachmentHelper.copyUriToInternalStorage(context, uri, uniqueName)
                                                Attachment(uniqueName, origName)
                                            }
                                        }

                                        // 5. Clean up old deleted files on disk & Supabase storage
                                        if (currentJobVal != null) {
                                            val deletedAttachments = mutableListOf<Pair<String, Attachment>>()
                                            
                                            // Check resume
                                            if (currentJobVal.resume != null && (finalResume == null || finalResume?.fileName != currentJobVal.resume.fileName)) {
                                                deletedAttachments.add("resumes" to currentJobVal.resume)
                                            }
                                            // Check coverLetter
                                            if (currentJobVal.coverLetter != null && (finalCoverLetter == null || finalCoverLetter?.fileName != currentJobVal.coverLetter.fileName)) {
                                                deletedAttachments.add("cover_letters" to currentJobVal.coverLetter)
                                            }
                                            // Check additionalDocument
                                            if (currentJobVal.additionalDocument != null && (finalAdditionalDoc == null || finalAdditionalDoc?.fileName != currentJobVal.additionalDocument.fileName)) {
                                                deletedAttachments.add("additional_documents" to currentJobVal.additionalDocument)
                                            }
                                            // Check screenshots
                                            val currentScreenshots = currentJobVal.screenshots ?: emptyList()
                                            for (oldScreenshot in currentScreenshots) {
                                                if (finalScreenshots.none { it.fileName == oldScreenshot.fileName }) {
                                                    deletedAttachments.add("screenshots" to oldScreenshot)
                                                }
                                            }
                                            
                                            if (deletedAttachments.isNotEmpty()) {
                                                // Delete locally
                                                deletedAttachments.forEach { (_, attachment) ->
                                                    AttachmentHelper.deleteFile(context, attachment.fileName)
                                                }
                                                
                                                // Delete from Supabase Storage
                                                val userId = viewModel.authManager.currentUser?.uid
                                                if (userId != null) {
                                                    val supabaseHelper = com.example.data.sync.SupabaseStorageHelper()
                                                    deletedAttachments.forEach { (type, attachment) ->
                                                        try {
                                                            supabaseHelper.deleteFile(userId, type, attachment.fileName)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val finalPlatform = when (platformVal) {
                                        "Other" -> customPlatformNameVal.trim().ifEmpty { "Other" }
                                        else -> platformVal
                                    }
                                    
                                    val finalUrl = urlVal.trim().ifEmpty { null }
                                    val finalEmail = if (platformVal == "Email") emailVal.trim().ifEmpty { null } else null

                                    viewModel.saveJobApplication(
                                        companyName = companyNameVal,
                                        role = roleVal,
                                        platform = finalPlatform,
                                        status = statusVal,
                                        jobDescription = jobDescriptionVal,
                                        notes = notesVal,
                                        url = finalUrl,
                                        email = finalEmail,
                                        timeApplied = appliedDateEpochVal,
                                        resume = finalResume,
                                        coverLetter = finalCoverLetter,
                                        additionalDocument = finalAdditionalDoc,
                                        screenshots = finalScreenshots,
                                        onSuccess = onNavigateBack
                                    )
                                } catch (e: Exception) {
                                    isSaving = false
                                    Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_application_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (jobId == null) "Saving..." else "Updating...",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
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
            onDismissRequest = {
                showDatePicker = false
                focusManager.clearFocus()
            },
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
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.testTag("datepicker_confirm")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.testTag("datepicker_dismiss")
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (jobId != null && hasLoadedOnce && selectedApp == null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Application Deleted", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This job application has been deleted from another device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onNavigateBack()
                    }
                ) {
                    Text("Return to Dashboard", fontWeight = FontWeight.Bold)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}
