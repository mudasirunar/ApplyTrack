package com.example.ui.applications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JobApplication
import com.example.ui.JobViewModel
import com.example.ui.SortOption
import com.example.ui.SyncState
import com.example.ui.components.JobCard
import com.example.ui.dashboard.ApplicationsShimmerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    viewModel: JobViewModel,
    onNavigateToAddEdit: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val apps by viewModel.filteredApplications.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val spacing = 8.dp
    val fabBottomPadding = navigationBarsPadding + 80.dp + spacing
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    
    var jobToDelete by remember { mutableStateOf<JobApplication?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var previousIndex by rememberSaveable { mutableStateOf(lazyListState.firstVisibleItemIndex) }
    var previousScrollOffset by rememberSaveable { mutableStateOf(lazyListState.firstVisibleItemScrollOffset) }
    val isFabVisible by viewModel.isFabVisible.collectAsStateWithLifecycle()
    val isSearchFocused by viewModel.isSearchFocused.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchFocused) {
        focusManager.clearFocus()
    }

    DisposableEffect(Unit) {
        viewModel.isFabVisible.value = true
        onDispose {}
    }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        val currentIndex = lazyListState.firstVisibleItemIndex
        val currentOffset = lazyListState.firstVisibleItemScrollOffset
        
        if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset)) {
            // Scrolling down
            viewModel.isFabVisible.value = false
        } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousScrollOffset)) {
            // Scrolling up
            viewModel.isFabVisible.value = true
        }
        
        previousIndex = currentIndex
        previousScrollOffset = currentOffset
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Filter Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search...") },
                    leadingIcon = {
                        if (isSearchFocused) {
                            IconButton(onClick = { focusManager.clearFocus() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            viewModel.isSearchFocused.value = focusState.isFocused
                        }
                        .testTag("search_bar"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Static Filter section without expand/collapse animation
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Filter Chips row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statuses = listOf("All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Month")
                        items(statuses) { status ->
                            val isSelected = statusFilter == status
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.statusFilter.value = status
                                    if (status == "All") {
                                        viewModel.sortOption.value = SortOption.STATUS_LATEST
                                    }
                                },
                                label = { Text(status) },
                                modifier = Modifier.testTag("filter_chip_$status"),
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // Month/Year Sub-filter controls
                    AnimatedVisibility(
                        visible = statusFilter == "Month",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                                    value = monthNames.getOrNull(selectedMonth - 1) ?: "",
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

                                // Transparent clickable overlay to open the dropdown when clicking anywhere on the field
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { isMonthDropdownExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = isMonthDropdownExpanded,
                                    onDismissRequest = { isMonthDropdownExpanded = false }
                                ) {
                                    monthNames.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                viewModel.selectedMonth.value = index + 1
                                                isMonthDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Year Input Field
                            OutlinedTextField(
                                value = selectedYear,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                        viewModel.selectedYear.value = newValue
                                    }
                                },
                                label = { Text("Year") },
                                singleLine = true,
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sorting Info Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSortBottomSheet = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val sortLabel = when (sortOption) {
                            SortOption.STATUS_LATEST -> "Sorted: Latest status update"
                            SortOption.STATUS_OLDEST -> "Sorted: Oldest status update"
                            SortOption.CREATION_LATEST -> "Sorted: Latest created"
                            SortOption.CREATION_OLDEST -> "Sorted: Oldest created"
                        }
                        Text(
                            text = sortLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Job Applications List with Loader and Empty State Handler
            val isListCalculating = apps.isEmpty() && stats.total > 0 && searchQuery.isEmpty() && statusFilter == "All"
            if (isInitialLoading || isListCalculating) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ApplicationsShimmerScreen()
                }
            } else if (apps.isEmpty()) {
                val isSearchOrFilterActive = stats.total > 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchOrFilterActive) Icons.Default.Search else Icons.Default.Info,
                            contentDescription = if (isSearchOrFilterActive) "No search results" else "Information empty local database",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isSearchOrFilterActive) "No results found" else "No applications saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSearchOrFilterActive) {
                                "No applications match your current search terms or active filters. Try adjusting them!"
                            } else {
                                "Tap the '+' button in the bottom right corner to start!"
                            },
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
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            onClick = { onNavigateToDetail(job.id) },
                            onEditClick = { onNavigateToAddEdit(job.id) },
                            onDeleteClick = {
                                jobToDelete = job
                                showDeleteConfirmDialog = true
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isFabVisible && !isSearchFocused && !isInitialLoading,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabBottomPadding)
        ) {
            val isSyncing = syncState == SyncState.SYNCING
            val fabBgColor = if (isSyncing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
            val fabContentColor = if (isSyncing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onPrimary

            FloatingActionButton(
                onClick = {
                    if (!isSyncing) {
                        onNavigateToAddEdit(null)
                    }
                },
                containerColor = fabBgColor,
                contentColor = fabContentColor,
                modifier = Modifier.testTag("add_job_fab"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Job Application")
            }
        }
    }
    }

    if (showDeleteConfirmDialog && jobToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                jobToDelete = null
            },
            title = { Text(text = "Delete Application") },
            text = { Text(text = "Are you sure you want to delete this job application? This action cannot be undone and will overwrite remote backups.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestDeleteApplication(jobToDelete!!)
                        showDeleteConfirmDialog = false
                        jobToDelete = null
                    },
                    modifier = Modifier.testTag("delete_dialog_confirm")
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        jobToDelete = null
                    },
                    modifier = Modifier.testTag("delete_dialog_cancel")
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showSortBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sort Applications By",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val options = listOf(
                    SortOption.STATUS_LATEST to "Status Date: Latest first",
                    SortOption.STATUS_OLDEST to "Status Date: Oldest first",
                    SortOption.CREATION_LATEST to "Creation Date: Latest first",
                    SortOption.CREATION_OLDEST to "Creation Date: Oldest first"
                )
                
                options.forEach { (option, label) ->
                    val isSelected = sortOption == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val currentIndex = lazyListState.firstVisibleItemIndex
                                val currentOffset = lazyListState.firstVisibleItemScrollOffset
                                
                                viewModel.sortOption.value = option
                                showSortBottomSheet = false
                                
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(10)
                                    lazyListState.scrollToItem(currentIndex, currentOffset)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}