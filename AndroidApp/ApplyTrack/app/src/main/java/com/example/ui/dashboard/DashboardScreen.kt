package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JobViewModel
import com.example.ui.applications.DateFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: JobViewModel,
    onNavigateToAdd: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {}
) {
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val analytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val dashboardYear by viewModel.dashboardYear.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    val scrollToTopEvent by viewModel.dashboardScrollToTop.collectAsStateWithLifecycle()
    var lastScrollTrigger by remember { mutableStateOf(scrollToTopEvent) }

    LaunchedEffect(scrollToTopEvent) {
        if (scrollToTopEvent > lastScrollTrigger) {
            scrollState.animateScrollTo(0)
        }
        lastScrollTrigger = scrollToTopEvent
    }

    if (isInitialLoading) {
        DashboardShimmerScreen()
    } else {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardHeader(totalCount = analytics.total)

                OverviewStatsRow(
                    analytics = analytics,
                    onNavigateToAdd = onNavigateToAdd,
                    onTotalApplicationsClick = {
                        viewModel.statusFilter.value = "All"
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                StatusCardsGrid(
                    analytics = analytics,
                    onStatusClick = { status ->
                        viewModel.statusFilter.value = status
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                ConversionFunnelRow(analytics = analytics)

                StatusDistributionSection(slices = analytics.statusDistribution)

                MonthlyActivitySection(
                    analytics = analytics,
                    year = dashboardYear,
                    onYearChange = { viewModel.setDashboardYear(it) },
                    onMonthClick = { month ->
                        viewModel.statusFilter.value = "Date"
                        viewModel.updateDateFilter {
                            copy(
                                mode = DateFilterMode.MONTH,
                                month = month,
                                year = dashboardYear
                            )
                        }
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                PlatformBreakdownSection(
                    platforms = analytics.platforms,
                    onPlatformClick = { platform ->
                        viewModel.statusFilter.value = "Platform"
                        viewModel.selectedPlatform.value = platform
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                ResumeEffectivenessSection(
                    resumeStats = analytics.resumeStats,
                    onResumeClick = { resumeName ->
                        viewModel.statusFilter.value = "Resume"
                        viewModel.selectedResume.value = resumeName
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )
            }
        }
    }
}