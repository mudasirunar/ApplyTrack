package com.example.ui.jobaddedit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Attachment
import com.example.ui.JobViewModel
import com.example.utils.AttachmentHelper
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .padding(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                JobDetailsCard(
                    role = role,
                    onRoleChange = { role = it },
                    companyName = companyName,
                    onCompanyNameChange = { companyName = it }
                )

                PlatformDateCard(
                    platform = platform,
                    onPlatformChange = { platform = it },
                    platformExpanded = platformExpanded,
                    onPlatformExpandedChange = { platformExpanded = it },
                    customPlatformName = customPlatformName,
                    onCustomPlatformNameChange = { customPlatformName = it },
                    email = email,
                    onEmailChange = { email = it },
                    url = url,
                    onUrlChange = { url = it },
                    status = status,
                    onStatusChange = { status = it },
                    statusExpanded = statusExpanded,
                    onStatusExpandedChange = { statusExpanded = it },
                    formattedDateString = formattedDateString,
                    showDatePicker = showDatePicker,
                    onDateClick = { showDatePicker = true }
                )

                NotesDescriptionCard(
                    jobDescription = jobDescription,
                    onJobDescriptionChange = { jobDescription = it },
                    notes = notes,
                    onNotesChange = { notes = it }
                )

                AttachmentsCard(
                    resumeState = resumeState,
                    onPickResume = { resumeLauncher.launch("application/pdf") },
                    onRemoveResume = { resumeState = null },
                    coverLetterState = coverLetterState,
                    onPickCoverLetter = { coverLetterLauncher.launch("application/pdf") },
                    onRemoveCoverLetter = { coverLetterState = null },
                    additionalDocumentState = additionalDocumentState,
                    onPickAdditionalDoc = { additionalDocLauncher.launch("application/pdf") },
                    onRemoveAdditionalDoc = { additionalDocumentState = null },
                    screenshotsList = screenshotsList,
                    onAddScreenshot = { screenshotsLauncher.launch("image/*") },
                    onRemoveScreenshot = { index ->
                        screenshotsList = screenshotsList.toMutableList().apply { removeAt(index) }
                    }
                )
            }

            SaveButtonContainer(
                isSaving = isSaving,
                jobId = jobId,
                onSave = {
                    isSaving = true
                    scope.launch {
                        try {
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

                            withContext(Dispatchers.IO) {
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

                                if (currentJobVal != null) {
                                    val deletedAttachments = mutableListOf<Pair<String, Attachment>>()
                                    
                                    if (currentJobVal.resume != null && (finalResume == null || finalResume?.fileName != currentJobVal.resume.fileName)) {
                                        deletedAttachments.add("resumes" to currentJobVal.resume)
                                    }
                                    if (currentJobVal.coverLetter != null && (finalCoverLetter == null || finalCoverLetter?.fileName != currentJobVal.coverLetter.fileName)) {
                                        deletedAttachments.add("cover_letters" to currentJobVal.coverLetter)
                                    }
                                    if (currentJobVal.additionalDocument != null && (finalAdditionalDoc == null || finalAdditionalDoc?.fileName != currentJobVal.additionalDocument.fileName)) {
                                        deletedAttachments.add("additional_documents" to currentJobVal.additionalDocument)
                                    }
                                    val currentScreenshots = currentJobVal.screenshots ?: emptyList()
                                    for (oldScreenshot in currentScreenshots) {
                                        if (finalScreenshots.none { it.fileName == oldScreenshot.fileName }) {
                                            deletedAttachments.add("screenshots" to oldScreenshot)
                                        }
                                    }
                                    
                                    if (deletedAttachments.isNotEmpty()) {
                                        deletedAttachments.forEach { (_, attachment) ->
                                            AttachmentHelper.deleteFile(context, attachment.fileName)
                                        }
                                        
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showDatePicker) {
        AddEditDatePickerDialog(
            appliedDateEpoch = appliedDateEpoch,
            onDateSelected = { appliedDateEpoch = it },
            onDismiss = {
                showDatePicker = false
                focusManager.clearFocus()
            }
        )
    }

    if (jobId != null && hasLoadedOnce && selectedApp == null) {
        DeletedAlertDialog(
            onConfirm = onNavigateBack
        )
    }
}
