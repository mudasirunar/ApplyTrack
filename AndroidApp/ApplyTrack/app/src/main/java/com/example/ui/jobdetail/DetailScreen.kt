package com.example.ui.jobdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JobViewModel

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
                        IconButton(
                            onClick = { onNavigateToEdit(jobId) },
                            modifier = Modifier.testTag("edit_job_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Job"
                            )
                        }
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
                    DetailHeaderCard(job = job)

                    CollapsibleDescriptionCard(
                        jobDescription = job.jobDescription ?: "",
                        isExpanded = isDescriptionExpanded,
                        onToggleExpand = { isDescriptionExpanded = !isDescriptionExpanded }
                    )

                    PersonalNotesCard(notes = job.notes ?: "")

                    PlatformDetailsCard(job = job)

                    val hasAttachments = job.resume != null || job.coverLetter != null || job.additionalDocument != null || !job.screenshots.isNullOrEmpty()
                    if (hasAttachments) {
                        DetailAttachmentsCard(
                            job = job,
                            downloadingFiles = downloadingFiles,
                            onViewPdf = onNavigateToPdfViewer,
                            onViewImage = { index -> onNavigateToImageViewer(jobId, index) }
                        )
                    }

                    TimelineCard(job = job)

                    DetailMetadataCard(job = job)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                viewModel.requestDeleteApplication(selectedApp!!)
                onNavigateBack()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (hasLoadedOnce && selectedApp == null) {
        DeletedFallbackDialog(
            onConfirm = onNavigateBack
        )
    }
}
