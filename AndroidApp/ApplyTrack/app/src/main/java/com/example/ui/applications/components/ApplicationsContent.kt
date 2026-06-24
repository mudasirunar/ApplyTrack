package com.example.ui.applications

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.JobApplication


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApplicationsContent(
    isInitialLoading: Boolean,
    isListCalculating: Boolean,
    apps: List<JobApplication>,
    selectedJobIds: Set<Long>,
    isSelectionModeActive: Boolean,
    lazyListState: LazyListState,
    isFabVisible: Boolean,
    isSearchFocused: Boolean,
    fabBottomPadding: Dp,
    statusFilter: String,
    selectedResume: String,
    totalAppsCount: Int,
    resumeStatsEmpty: Boolean,
    onJobClick: (JobApplication) -> Unit,
    onJobLongClick: (JobApplication) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (JobApplication) -> Unit,
    onAddClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isInitialLoading) {
            ApplicationsShimmerScreen()
        } else if (isListCalculating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (apps.isEmpty()) {
            val isSearchOrFilterActive = totalAppsCount > 0

            val emptyIcon = when {
                statusFilter == "Resume" && totalAppsCount > 0 && resumeStatsEmpty -> Icons.Default.AttachFile
                statusFilter == "Resume" && totalAppsCount > 0 && selectedResume == "Select---" -> Icons.Default.Info
                isSearchOrFilterActive -> Icons.Default.Search
                else -> Icons.Default.Info
            }

            val emptyTitle = when {
                statusFilter == "Resume" && totalAppsCount > 0 && resumeStatsEmpty -> "No resumes found"
                statusFilter == "Resume" && totalAppsCount > 0 && selectedResume == "Select---" -> "Select a Resume"
                isSearchOrFilterActive -> "No results found"
                else -> "No applications saved yet"
            }

            val emptyDescription = when {
                statusFilter == "Resume" && totalAppsCount > 0 && resumeStatsEmpty ->
                    "Attach a CV/resume (PDF) to your applications to track and filter them."
                statusFilter == "Resume" && totalAppsCount > 0 && selectedResume == "Select---" ->
                    "Choose a resume from the dropdown above to filter your applications."
                isSearchOrFilterActive ->
                    "No applications match your current search terms or active filters. Try adjusting them!"
                else ->
                    "Tap the '+' button in the bottom right corner to start!"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = emptyIcon,
                        contentDescription = emptyTitle,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = emptyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = emptyDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps, key = { it.id }) { job ->
                    val isSelected = selectedJobIds.contains(job.id)
                    JobCard(
                        job = job,
                        onClick = { onJobClick(job) },
                        onEditClick = { onEditClick(job.id) },
                        onDeleteClick = { onDeleteClick(job) },
                        isSelectionModeActive = isSelectionModeActive,
                        isSelected = isSelected,
                        onLongClick = { onJobLongClick(job) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        AddJobFab(
            visible = isFabVisible && !isSearchFocused && !isInitialLoading && !isListCalculating,
            bottomPadding = fabBottomPadding,
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
