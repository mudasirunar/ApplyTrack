package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JobApplication
import com.example.ui.DashboardStats
import com.example.ui.JobViewModel
import com.example.ui.SyncState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.LinkBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: JobViewModel,
    onNavigateToAddEdit: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val apps by viewModel.filteredApplications.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val sortByLatest by viewModel.sortByLatest.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var jobToDelete by remember { mutableStateOf<JobApplication?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var previousIndex by rememberSaveable { mutableStateOf(lazyListState.firstVisibleItemIndex) }
    var previousScrollOffset by rememberSaveable { mutableStateOf(lazyListState.firstVisibleItemScrollOffset) }
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    val isSearchFocused by viewModel.isSearchFocused.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchFocused) {
        focusManager.clearFocus()
    }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        val currentIndex = lazyListState.firstVisibleItemIndex
        val currentOffset = lazyListState.firstVisibleItemScrollOffset
        
        if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset)) {
            // Scrolling down
            isFabVisible = false
        } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousScrollOffset)) {
            // Scrolling up
            isFabVisible = true
        }
        
        previousIndex = currentIndex
        previousScrollOffset = currentOffset
    }



    Scaffold(

        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible && !isSearchFocused,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
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
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(bottom = 80.dp)
                        .testTag("add_job_fab"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Job Application")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Grid Section
            AnimatedVisibility(
                visible = !isSearchFocused,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                StatsGrid(stats = stats)
            }

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

                // Smoothly animate the collapse/expansion of chips and filters
                AnimatedVisibility(
                    visible = !isSearchFocused,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
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
                                            viewModel.sortByLatest.value = true
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

                        // Sorting Info Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.sortByLatest.value = !sortByLatest }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Sort Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (sortByLatest) 0f else 180f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortByLatest) "Sorted: Latest applied" else "Sorted: Oldest applied",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Job Applications List with Loader and Empty State Handler
            if (isInitialLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            }
                        )
                    }
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
                        viewModel.deleteSelectedApplication(jobToDelete!!.id) {}
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
}

@Composable
fun StatsGrid(stats: DashboardStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Total Applications - Full Width Row with Motivational Message
        TotalApplicationsCard(totalCount = stats.total)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Remaining 4 cards in 2x2 layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Applied (Saved)",
                count = stats.saved.toString(),
                textColor = WarningAmber,
                modifier = Modifier.weight(1f),
                tint = WarningAmber.copy(alpha = 0.1f)
            )
            StatCard(
                title = "Interviews",
                count = stats.interviews.toString(),
                textColor = AccentGreen,
                modifier = Modifier.weight(1f),
                tint = AccentGreen.copy(alpha = 0.1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Offers",
                count = stats.offers.toString(),
                textColor = LinkBlue,
                modifier = Modifier.weight(1f),
                tint = LinkBlue.copy(alpha = 0.1f)
            )
            StatCard(
                title = "Rejected",
                count = stats.rejected.toString(),
                textColor = ErrorRed,
                modifier = Modifier.weight(1f),
                tint = ErrorRed.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    tint: Color
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tint.copy(alpha = 0.05f))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = count,
                style = MaterialTheme.typography.headlineLarge,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TotalApplicationsCard(totalCount: Int) {
    var message by remember(totalCount) {
        mutableStateOf(DashboardMessageProvider.getDashboardMessage(totalCount))
    }
    
    LaunchedEffect(totalCount) {
        while (true) {
            kotlinx.coroutines.delay(10000L) // Switch message every 10 seconds for a dynamic feel
            message = DashboardMessageProvider.getDashboardMessage(totalCount)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Total Applications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Subtle, premium vertical divider line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun JobCard(
    job: JobApplication,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val leftBarColor = when (job.status.lowercase()) {
        "applied", "saved" -> WarningAmber
        "interview", "interviewing" -> AccentGreen
        "rejected" -> ErrorRed
        "offer" -> LinkBlue
        else -> MaterialTheme.colorScheme.outline
    }

    val chipBgColor = when (job.status.lowercase()) {
        "applied", "saved" -> WarningAmber.copy(alpha = 0.1f)
        "interview", "interviewing" -> AccentGreen.copy(alpha = 0.1f)
        "rejected" -> ErrorRed.copy(alpha = 0.1f)
        "offer" -> LinkBlue.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }

    val chipContentColor = when (job.status.lowercase()) {
        "applied", "saved" -> WarningAmber
        "interview", "interviewing" -> AccentGreen
        "rejected" -> ErrorRed
        "offer" -> LinkBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val formattedDate = remember(job.createdAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(job.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("job_card_${job.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Precise vertical timeline indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(leftBarColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = job.role ?: "Position unassigned",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Status chip
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = chipBgColor,
                            modifier = Modifier.testTag("card_status_chip")
                        ) {
                            Text(
                                text = job.status,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = chipContentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = job.companyName ?: "Unknown Company",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Applied date flag",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(32.dp).testTag("edit_job_button_${job.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Job",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(32.dp).testTag("delete_job_button_${job.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Job",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
