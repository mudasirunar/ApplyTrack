package com.example.ui.jobdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JobApplication
import com.example.model.StatusHistoryEntry
import com.example.ui.JobViewModel
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.LinkBlue
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.model.Attachment
import com.example.utils.AttachmentHelper
import java.io.File
import com.example.ui.components.StatusPill
import com.example.ui.components.InfoRow
import com.example.ui.components.TimelineNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: JobViewModel,
    jobId: Long,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToPdfViewer: (String, String) -> Unit,
    onNavigateToImageViewer: (Long, Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedApp by viewModel.selectedApplication.collectAsStateWithLifecycle()
    val downloadingFiles by viewModel.downloadingFiles.collectAsStateWithLifecycle()
    var isDescriptionExpanded by remember { mutableStateOf(true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    var hasLoadedOnce by remember(jobId) { mutableStateOf(false) }
    LaunchedEffect(selectedApp, jobId) {
        if (selectedApp != null && selectedApp?.id == jobId) {
            hasLoadedOnce = true
        }
    }

    val context = LocalContext.current

    val sdf = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }
    val sdfApplied = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Load state
    LaunchedEffect(jobId) {
        if (viewModel.selectedApplication.value?.id != jobId) {
            viewModel.clearSelectedApplication()
        }
        viewModel.loadApplicationById(jobId)
    }

    Scaffold(
        topBar = {
            key(MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    title = {
                        Text(
                            text = selectedApp?.role ?: "Job Detail",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    actions = {
                        // Edit Trigger
                        IconButton(
                            onClick = { onNavigateToEdit(jobId) },
                            modifier = Modifier.testTag("edit_job_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Job"
                            )
                        }
                        // Delete Trigger
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_job_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Job",
                                tint = MaterialTheme.colorScheme.error
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            val job = selectedApp

            if (job == null) {
                // Loading / Empty fallback if item is deleted or fetching
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    
                    // Header Block: Company visual, title & primary stats info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini company avatar profile
                            Surface(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = "Work Icon Indicator",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.role ?: "Position unassigned",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = job.companyName ?: "Unknown Company",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Display Custom Status Pill inside matching colors
                                    StatusPill(status = job.status)
                                }
                            }
                        }
                    }

                    // Two columns dashboard panel if needed, represented sequentially in layout:
                    // Section 1: Collapsible Job Description Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("description_panel")
                    ) {
                        val rotationState by animateFloatAsState(
                            targetValue = if (isDescriptionExpanded) 180f else 0f, label = "arrowRotation"
                        )

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Description Symbol",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Job Description",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand description panel",
                                    modifier = Modifier
                                        .rotate(rotationState)
                                        .testTag("expand_arrow")
                                )
                            }

                            AnimatedVisibility(
                                visible = isDescriptionExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val isDescUrl = remember(job.jobDescription) {
                                        val desc = job.jobDescription?.trim() ?: ""
                                        desc.startsWith("http://", ignoreCase = true) || 
                                        desc.startsWith("https://", ignoreCase = true)
                                    }
                                    if (isDescUrl) {
                                        val context = LocalContext.current
                                        Text(
                                            text = job.jobDescription ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = LinkBlue,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                            ),
                                            lineHeight = 22.sp,
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(job.jobDescription?.trim()))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // no-op
                                                }
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = job.jobDescription?.takeIf { it.isNotBlank() }
                                                ?: "No description recorded for this application. Paste screenshots, links or core skills here to remember.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Personal Notes Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = "Notes icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Personal Notes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = job.notes?.takeIf { it.isNotBlank() }
                                    ?: "Add personal notes here (recruiter names, callback phone numbers, question lists or test parameters).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Section 3: Extra static info bento (Platform, applied date, metrics)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Platform Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            InfoRow(label = "Platform", value = job.platform ?: "Unrecorded (Direct)")
                            
                            val context = LocalContext.current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                val hasUrl = !job.url.isNullOrBlank()
                                Text(
                                    text = if (hasUrl) job.url!! else "Not specified",
                                    style = if (hasUrl) {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = LinkBlue,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(start = 16.dp)
                                        .then(
                                            if (hasUrl) {
                                                Modifier.clickable {
                                                    try {
                                                        val rawUrl = job.url!!
                                                        val intentUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) && 
                                                                           !rawUrl.startsWith("https://", ignoreCase = true)) {
                                                            "https://$rawUrl"
                                                        } else {
                                                            rawUrl
                                                        }
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // no-op
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                 )
                            }

                            val hasEmail = !job.email.isNullOrBlank()
                            if (hasEmail) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Contact Email",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = job.email!!,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = LinkBlue,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(start = 16.dp)
                                            .clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:${job.email!!}")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // no-op
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Section 3.5: Attachments Card
                    val hasAttachments = job.resume != null || job.coverLetter != null || job.additionalDocument != null || !job.screenshots.isNullOrEmpty()
                    if (hasAttachments) {
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
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                // Documents
                                listOf(
                                    "Resume / CV" to job.resume,
                                    "Cover Letter" to job.coverLetter,
                                    "Additional Document" to job.additionalDocument
                                ).forEach { (label, docAttachment) ->
                                    if (docAttachment != null) {
                                        val exists = remember(downloadingFiles, docAttachment.fileName) {
                                            AttachmentHelper.fileExists(context, docAttachment.fileName)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = docAttachment.originalName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            if (exists) {
                                                Button(
                                                    onClick = { onNavigateToPdfViewer(docAttachment.fileName, docAttachment.originalName) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("View", style = MaterialTheme.typography.bodySmall)
                                                }
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    Text("Syncing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }

                                // Screenshots
                                if (!job.screenshots.isNullOrEmpty()) {
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
                                            job.screenshots.forEachIndexed { index, screenshot ->
                                                val file = remember(screenshot.fileName) {
                                                    AttachmentHelper.getAttachmentFile(context, screenshot.fileName)
                                                }
                                                val exists = remember(downloadingFiles, screenshot.fileName) {
                                                    file.exists()
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                        .then(
                                                            if (exists) {
                                                                Modifier.clickable { onNavigateToImageViewer(jobId, index) }
                                                            } else Modifier
                                                        )
                                                ) {
                                                    if (exists) {
                                                        AsyncImage(
                                                            model = file,
                                                            contentDescription = "Screenshot",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Timeline Visualization (Mimicking delivery status tracker)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom interactive vertical timeline
                            Column {
                                val historyEntries = remember(job.statusHistory, job.createdAt, job.updatedAt, job.status) {
                                    val parsed = job.statusHistory
                                    if (!parsed.isNullOrEmpty()) {
                                        parsed
                                    } else {
                                        if (job.updatedAt > job.createdAt + 1000L) {
                                            listOf(
                                                StatusHistoryEntry("Saved", job.createdAt),
                                                StatusHistoryEntry(job.status, job.updatedAt)
                                            )
                                        } else {
                                            listOf(
                                                StatusHistoryEntry(job.status, job.createdAt)
                                            )
                                        }
                                    }
                                }

                                historyEntries.asReversed().forEachIndexed { index, entry ->
                                    val isCurrent = index == 0
                                    val isFirst = index == 0
                                    val isLast = index == historyEntries.size - 1
                                    
                                    val stageLabel = entry.status.replaceFirstChar { it.uppercase() }
                                    
                                    TimelineNode(
                                        stage = stageLabel,
                                        date = sdfApplied.format(Date(entry.timestamp)),
                                        isCurrent = isCurrent,
                                        isFirst = isFirst,
                                        isLast = isLast
                                    )
                                }
                            }
                        }
                    }

                    // Section 5: System Metadata Card (Created and Last Updated Timestamps)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Metadata",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            InfoRow(label = "Created Time", value = sdf.format(Date(job.createdAt)))
                            InfoRow(label = "Last Updated", value = sdf.format(Date(job.updatedAt)))
                        }
                    }
                }
            }
        }
    }

    // Interactive confirm delete dialog overlay
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Delete Application") },
            text = { Text(text = "Are you sure you want to delete this job application? This action cannot be undone and will overwrite remote backups.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestDeleteApplication(selectedApp!!)
                        onNavigateBack()
                        showDeleteConfirmDialog = false
                    },
                    modifier = Modifier.testTag("delete_dialog_confirm")
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("delete_dialog_cancel")
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (hasLoadedOnce && selectedApp == null) {
        AlertDialog(
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
