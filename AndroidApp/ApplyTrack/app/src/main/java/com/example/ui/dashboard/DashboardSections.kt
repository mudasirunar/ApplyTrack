package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DashboardAnalytics
import com.example.ui.PlatformStat
import com.example.ui.ResumeStat
import com.example.ui.StatusSlice
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons

// ======================== Section A: Header & Message ========================

@Composable
fun DashboardHeader(totalCount: Int) {
    var message by remember(totalCount) {
        mutableStateOf(DashboardMessageProvider.getDashboardMessage(totalCount))
    }

    LaunchedEffect(totalCount) {
        while (true) {
            delay(10000L)
            message = DashboardMessageProvider.getDashboardMessage(totalCount)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Text(
            text = "Your Personal Job Tracker",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Crossfade(
            targetState = message,
            animationSpec = androidx.compose.animation.core.tween(500),
            label = "messageAnimation"
        ) { currentMessage ->
            Text(
                text = currentMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ======================== Section B: Overview Stats Row ========================

@Composable
fun OverviewStatsRow(
    analytics: DashboardAnalytics,
    onNavigateToAdd: () -> Unit,
    onTotalApplicationsClick: () -> Unit
) {
    Card(
        onClick = onTotalApplicationsClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Applications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = analytics.total.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Activity pulse badges
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActivityBadge(label = "This Week", count = analytics.applicationsThisWeek)
                    ActivityBadge(label = "This Month", count = analytics.applicationsThisMonth)
                }
            }

            if (analytics.total == 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onNavigateToAdd,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Add Your First Application",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityBadge(label: String, count: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ======================== Section C: Status Cards Grid ========================

@Composable
fun StatusCardsGrid(
    analytics: DashboardAnalytics,
    onStatusClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Applied, Saved, Interview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusStatCard(
                title = "Applied",
                count = analytics.applied.toString(),
                textColor = WarningAmber,
                tint = WarningAmber.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Applied") }
            )
            StatusStatCard(
                title = "Saved",
                count = analytics.saved.toString(),
                textColor = SavedGray,
                tint = SavedGray.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Saved") }
            )
            StatusStatCard(
                title = "Interview",
                count = analytics.interviews.toString(),
                textColor = AccentGreen,
                tint = AccentGreen.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Interview") }
            )
        }
        // Row 2: Offer, Rejected, Response Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusStatCard(
                title = "Offers",
                count = analytics.offers.toString(),
                textColor = LinkBlue,
                tint = LinkBlue.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Offer") }
            )
            StatusStatCard(
                title = "Rejected",
                count = analytics.rejected.toString(),
                textColor = ErrorRed,
                tint = ErrorRed.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Rejected") }
            )
            StatusStatCard(
                title = "Response",
                count = analytics.responses.toString(),
                textColor = MaterialTheme.colorScheme.primary,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                modifier = Modifier.weight(1f),
                onClick = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusStatCard(
    title: String,
    count: String,
    textColor: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.height(90.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            StatusStatCardContent(title, count, textColor, tint)
        }
    } else {
        Card(
            modifier = modifier.height(90.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            StatusStatCardContent(title, count, textColor, tint)
        }
    }
}

@Composable
private fun StatusStatCardContent(
    title: String,
    count: String,
    textColor: Color,
    tint: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tint.copy(alpha = 0.05f))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = count,
            style = MaterialTheme.typography.headlineMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

// ======================== Section D: Conversion Funnel ========================

@Composable
fun ConversionFunnelRow(analytics: DashboardAnalytics) {
    SectionCard(title = "Conversion Rates") {
        if (analytics.total == 0) {
            EmptyStateText("No conversion rates available. Add applications with different statuses to calculate your success and interview rates.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RateCard(
                    label = "Success",
                    percentage = analytics.successRate,
                    color = LinkBlue
                )
                RateCard(
                    label = "Interview",
                    percentage = analytics.interviewRate,
                    color = AccentGreen
                )
                RateCard(
                    label = "Rejection",
                    percentage = analytics.rejectionRate,
                    color = ErrorRed
                )
                RateCard(
                    label = "Response",
                    percentage = analytics.responseRate,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RateCard(
    label: String,
    percentage: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressRing(
            percentage = percentage,
            color = color,
            size = 60.dp,
            strokeWidth = 5.dp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ======================== Section E: Monthly Activity ========================

@Composable
fun MonthlyActivitySection(
    analytics: DashboardAnalytics,
    year: String,
    onYearChange: (String) -> Unit,
    onMonthClick: (Int) -> Unit = {}
) {
    val isEmpty = analytics.monthlyActivity.all { it.count == 0 }

    SectionCard(
        title = "Monthly Activity",
        action = {
            YearFilterInput(
                year = year,
                onYearChange = onYearChange
            )
        }
    ) {
        if (isEmpty) {
            EmptyStateText("No application activity recorded in $year. Try adding or editing applications to see activity.")
        } else {
            BarChart(
                data = analytics.monthlyActivity,
                modifier = Modifier.fillMaxWidth(),
                onBarClick = { index -> onMonthClick(index + 1) }
            )
        }
    }
}

@Composable
fun YearFilterInput(
    year: String,
    onYearChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Previous Year Button
        IconButton(
            onClick = {
                val currentYear = year.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val newYear = (currentYear - 1).toString()
                onYearChange(newYear)
                focusManager.clearFocus()
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Year",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        BasicTextField(
            value = year,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= 4) {
                    onYearChange(filtered)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.width(36.dp)
        )

        // Next Year Button
        IconButton(
            onClick = {
                val currentYear = year.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val newYear = (currentYear + 1).toString()
                onYearChange(newYear)
                focusManager.clearFocus()
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Year",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ======================== Section F: Platform Breakdown ========================

@Composable
fun PlatformBreakdownSection(
    platforms: List<PlatformStat>,
    onPlatformClick: (String) -> Unit = {}
) {
    SectionCard(title = "Platforms") {
        if (platforms.isEmpty()) {
            EmptyStateText("No platform data recorded yet. Add platforms to your applications to see breakdown.")
        } else {
            val maxCount = platforms.maxOf { it.count }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                platforms.forEach { platform ->
                    HorizontalBarRow(
                        label = platform.platform,
                        count = platform.count,
                        maxCount = maxCount,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onPlatformClick(platform.platform)
                        }
                    )
                }
            }
        }
    }
}

// ======================== Section G: Resume Effectiveness ========================

@Composable
fun ResumeEffectivenessSection(
    resumeStats: List<ResumeStat>,
    onResumeClick: (String) -> Unit = {}
) {
    SectionCard(title = "Resume Effectiveness") {
        if (resumeStats.isEmpty()) {
            EmptyStateText("No resumes attached yet. Attach a CV/resume to applications to track which works best.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Resume",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Used",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = "Int.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = "Offer",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LinkBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = "Rej.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                resumeStats.forEach { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayName = stat.resumeName.let { name ->
                            when {
                                name.endsWith(".pdf", ignoreCase = true) -> name.dropLast(4)
                                name.endsWith(".docx", ignoreCase = true) -> name.dropLast(5)
                                name.endsWith(".doc", ignoreCase = true) -> name.dropLast(4)
                                else -> name
                            }
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onResumeClick(stat.resumeName)
                                }
                        )
                        Text(
                            text = stat.totalUsed.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            text = stat.interviewCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            text = stat.offerCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = LinkBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            text = stat.rejectedCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
        }
    }
}

// ======================== Section H: Status Distribution ========================

@Composable
fun StatusDistributionSection(slices: List<StatusSlice>) {
    SectionCard(title = "Status Distribution") {
        if (slices.isEmpty()) {
            EmptyStateText("Add applications to see your status distribution chart.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DonutChart(
                    slices = slices,
                    modifier = Modifier,
                    size = 160.dp,
                    strokeWidth = 22.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Legend
                FlowLegend(slices = slices)
            }
        }
    }
}

@Composable
private fun FlowLegend(slices: List<StatusSlice>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            slices.forEach { slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(slice.color), CircleShape)
                    )
                    Text(
                        text = "${slice.status} (${slice.count})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ======================== Shared Components ========================

@Composable
fun SectionCard(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (action != null) {
                    action()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun EmptyStateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

// ======================== Shimmer Loading ========================

@Composable
fun DashboardShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            ShimmerPlaceholder(
                height = when (it) {
                    0 -> 50.dp
                    1 -> 100.dp
                    else -> 120.dp
                }
            )
        }
    }
}

@Composable
private fun ShimmerPlaceholder(
    height: androidx.compose.ui.unit.Dp
) {
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(shimmerColor, RoundedCornerShape(16.dp))
    )
}
