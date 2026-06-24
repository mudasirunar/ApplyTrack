package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                // Section A: Header & Motivational Message
                DashboardHeader(totalCount = analytics.total)

                // Section B: Overview Stats Row
                OverviewStatsRow(
                    analytics = analytics,
                    onNavigateToAdd = onNavigateToAdd,
                    onTotalApplicationsClick = {
                        viewModel.statusFilter.value = "All"
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                // Section C: Status Cards Grid (3×2)
                StatusCardsGrid(
                    analytics = analytics,
                    onStatusClick = { status ->
                        viewModel.statusFilter.value = status
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                // Section D: Conversion Funnel / Rate Cards
                ConversionFunnelRow(analytics = analytics)

                // Section H: Status Distribution Donut Chart
                StatusDistributionSection(slices = analytics.statusDistribution)

                // Section E: Monthly Activity Bar Chart
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

                // Section F: Platform Breakdown
                PlatformBreakdownSection(
                    platforms = analytics.platforms,
                    onPlatformClick = { platform ->
                        viewModel.statusFilter.value = "Platform"
                        viewModel.selectedPlatform.value = platform
                        viewModel.shouldScrollToFilter.value = true
                        onNavigateToApplications()
                    }
                )

                // Section G: Resume/CV Effectiveness
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