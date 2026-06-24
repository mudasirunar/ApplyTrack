package com.example.ui.applications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JobApplication
import com.example.ui.JobViewModel
import com.example.ui.SortOption
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
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val spacing = 8.dp
    val fabBottomPadding = navigationBarsPadding + 80.dp + spacing
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val dateFilterState by viewModel.dateFilterState.collectAsStateWithLifecycle()
    val shouldScrollToFilter by viewModel.shouldScrollToFilter.collectAsStateWithLifecycle()
    val isListCalculating by viewModel.isListCalculating.collectAsStateWithLifecycle()
    val selectedResume by viewModel.selectedResume.collectAsStateWithLifecycle()
    val resumeSearchQuery by viewModel.resumeSearchQuery.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    
    var jobToDelete by remember { mutableStateOf<JobApplication?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    var jobToDeleteList by remember { mutableStateOf<List<JobApplication>>(emptyList()) }
    var showDeleteListConfirmDialog by remember { mutableStateOf(false) }

    val selectedJobIds by viewModel.selectedJobIds.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()
    val filterLazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val scrollToTopEvent by viewModel.applicationsScrollToTop.collectAsStateWithLifecycle()
    var lastScrollTrigger by remember { mutableStateOf(scrollToTopEvent) }

    LaunchedEffect(scrollToTopEvent) {
        if (scrollToTopEvent > lastScrollTrigger) {
            if (lazyListState.firstVisibleItemIndex > 10) {
                lazyListState.scrollToItem(10)
            }
            lazyListState.animateScrollToItem(0)
        }
        lastScrollTrigger = scrollToTopEvent
    }
    var previousIndex by rememberSaveable { mutableStateOf(lazyListState.firstVisibleItemIndex) }
    var previousScrollOffset by rememberSaveable { mutableStateOf(lazyListState.firstVisibleItemScrollOffset) }
    val isFabVisible by viewModel.isFabVisible.collectAsStateWithLifecycle()
    val isSearchFocused by viewModel.isSearchFocused.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchFocused || isSelectionModeActive) {
        if (isSelectionModeActive) {
            viewModel.exitSelectionMode()
        } else if (isSearchFocused) {
            focusManager.clearFocus()
        }
    }

    DisposableEffect(Unit) {
        viewModel.isFabVisible.value = true
        onDispose {}
    }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        val currentIndex = lazyListState.firstVisibleItemIndex
        val currentOffset = lazyListState.firstVisibleItemScrollOffset
        
        if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset)) {
            viewModel.isFabVisible.value = false
        } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousScrollOffset)) {
            viewModel.isFabVisible.value = true
        }
        
        previousIndex = currentIndex
        previousScrollOffset = currentOffset
    }

    LaunchedEffect(shouldScrollToFilter) {
        if (shouldScrollToFilter) {
            val statuses = listOf("All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Resume", "Platform", "Date")
            val selectedIndex = statuses.indexOf(statusFilter)
            if (selectedIndex >= 0) {
                filterLazyListState.animateScrollToItem(selectedIndex)
            }
            viewModel.shouldScrollToFilter.value = false
        }
    }

    val currentFilters = listOf(searchQuery, statusFilter, selectedResume, selectedPlatform, dateFilterState, sortOption)
    var lastFilters by remember { mutableStateOf(currentFilters) }

    LaunchedEffect(currentFilters) {
        if (lastFilters != currentFilters) {
            lazyListState.scrollToItem(0)
            lastFilters = currentFilters
        }
    }

    Scaffold(
        topBar = {
            ApplicationsTopBar(
                isSelectionModeActive = isSelectionModeActive,
                selectedCount = selectedJobIds.size,
                onCloseClick = { viewModel.exitSelectionMode() }
            )
        },
        bottomBar = {
            val displayedIds = apps.map { it.id }
            ApplicationsBottomBar(
                isSelectionModeActive = isSelectionModeActive,
                displayedIds = displayedIds,
                selectedJobIds = selectedJobIds,
                onSelectAllToggle = { select ->
                    if (select) {
                        viewModel.selectJobs(displayedIds)
                    } else {
                        viewModel.deselectJobs(displayedIds)
                    }
                },
                onDeleteClick = {
                    if (selectedJobIds.isNotEmpty()) {
                        jobToDeleteList = apps.filter { selectedJobIds.contains(it.id) }
                        showDeleteListConfirmDialog = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = !isSelectionModeActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        ApplicationsSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.searchQuery.value = it },
                            isFocused = isSearchFocused,
                            onFocusChange = { viewModel.isSearchFocused.value = it },
                            onBackClick = { focusManager.clearFocus() }
                        )

                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            ApplicationsFilterChips(
                                selectedStatus = statusFilter,
                                onStatusClick = { status ->
                                    viewModel.statusFilter.value = status
                                    if (status == "All") {
                                        viewModel.sortOption.value = SortOption.STATUS_LATEST
                                    }
                                },
                                lazyListState = filterLazyListState
                            )

                            // Resume & Platform Sub-filters
                            PlatformSubFilterPanel(
                                statusFilter = statusFilter,
                                selectedResume = selectedResume,
                                onResumeChange = { viewModel.selectedResume.value = it },
                                selectedPlatform = selectedPlatform,
                                onPlatformChange = { viewModel.selectedPlatform.value = it },
                                resumeSearchQuery = resumeSearchQuery,
                                onResumeSearchQueryChange = { viewModel.resumeSearchQuery.value = it },
                                resumeNames = stats.resumeStats.map { it.resumeName }
                            )

                            // Date Sub-filter
                            AnimatedVisibility(
                                visible = statusFilter == "Date",
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                DateSubFilterPanel(
                                    dateFilterState = dateFilterState,
                                    onUpdateFilter = { update -> viewModel.updateDateFilter(update) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sort Info Row
                SortInfoRow(
                    sortOption = sortOption,
                    onClick = { showSortBottomSheet = true }
                )

                // Main Content List / Shimmers / Empty screens
                ApplicationsContent(
                    isInitialLoading = isInitialLoading,
                    isListCalculating = isListCalculating,
                    apps = apps,
                    selectedJobIds = selectedJobIds,
                    isSelectionModeActive = isSelectionModeActive,
                    lazyListState = lazyListState,
                    isFabVisible = isFabVisible,
                    isSearchFocused = isSearchFocused,
                    fabBottomPadding = fabBottomPadding,
                    statusFilter = statusFilter,
                    selectedResume = selectedResume,
                    totalAppsCount = stats.total,
                    resumeStatsEmpty = stats.resumeStats.isEmpty(),
                    onJobClick = { job ->
                        if (isSelectionModeActive) {
                            viewModel.toggleJobSelection(job.id)
                        } else {
                            onNavigateToDetail(job.id)
                        }
                    },
                    onJobLongClick = { job ->
                        if (isSelectionModeActive) {
                            viewModel.toggleJobSelection(job.id)
                        } else {
                            viewModel.enterSelectionMode(job.id)
                        }
                    },
                    onEditClick = onNavigateToAddEdit,
                    onDeleteClick = { job ->
                        jobToDelete = job
                        showDeleteConfirmDialog = true
                    },
                    onAddClick = { onNavigateToAddEdit(null) }
                )
            }
        }
    }

    // Confirmation Dialogs
    ApplicationsDialogs(
        jobToDelete = jobToDelete,
        onDismissDeleteSingle = {
            showDeleteConfirmDialog = false
            jobToDelete = null
        },
        onConfirmDeleteSingle = { job ->
            viewModel.requestDeleteApplication(job)
            showDeleteConfirmDialog = false
            jobToDelete = null
        },
        jobsToDeleteList = jobToDeleteList,
        onDismissDeleteList = {
            showDeleteListConfirmDialog = false
            jobToDeleteList = emptyList()
        },
        onConfirmDeleteList = { list ->
            viewModel.requestDeleteApplications(list)
            viewModel.exitSelectionMode()
            showDeleteListConfirmDialog = false
            jobToDeleteList = emptyList()
        }
    )

    // Sort Options Bottom Sheet
    if (showSortBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SortOptionsSheet(
                sortOption = sortOption,
                onOptionSelect = { option ->
                    val currentIndex = lazyListState.firstVisibleItemIndex
                    val currentOffset = lazyListState.firstVisibleItemScrollOffset
                    
                    viewModel.sortOption.value = option
                    showSortBottomSheet = false
                    
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(10)
                        lazyListState.scrollToItem(currentIndex, currentOffset)
                    }
                }
            )
        }
    }
}