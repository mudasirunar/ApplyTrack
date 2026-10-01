package com.example.ui.applications

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val filterStatuses = listOf("All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Response", "Resume", "Platform", "Date")

data class ChipLayoutInfo(val x: Int, val width: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsFilterChips(
    selectedStatus: String,
    onStatusClick: (String) -> Unit,
    scrollState: ScrollState,
    shouldScrollToFilter: Boolean = false,
    onScrollToFilterConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration, density) {
        with(density) { configuration.screenWidthDp.dp.roundToPx() }
    }
    var containerWidth by remember { mutableIntStateOf(screenWidthPx) }
    val chipBounds = remember { mutableStateMapOf<Int, ChipLayoutInfo>() }
    val coroutineScope = rememberCoroutineScope()

    fun scrollToCenter(index: Int) {
        val bounds = chipBounds[index] ?: return
        val effectiveWidth = if (containerWidth > 0) containerWidth else screenWidthPx
        val chipCenter = bounds.x + bounds.width / 2
        val target = (chipCenter - effectiveWidth / 2).coerceIn(0, scrollState.maxValue)
        coroutineScope.launch {
            scrollState.animateScrollTo(
                value = target,
                animationSpec = tween(durationMillis = 350)
            )
        }
    }

    // Auto-scroll when selectedStatus changes or shouldScrollToFilter is triggered from Dashboard
    LaunchedEffect(selectedStatus, shouldScrollToFilter) {
        val index = filterStatuses.indexOf(selectedStatus)
        if (index >= 0) {
            // If bounds are already measured, scroll immediately; otherwise wait briefly for layout
            var attempts = 0
            while (attempts < 10) {
                if (chipBounds.containsKey(index)) {
                    scrollToCenter(index)
                    break
                }
                delay(30)
                attempts++
            }
        }
        if (shouldScrollToFilter) {
            onScrollToFilterConsumed()
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                if (coordinates.size.width > 0) {
                    containerWidth = coordinates.size.width
                }
            }
            .horizontalScroll(scrollState)
    ) {
        filterStatuses.forEachIndexed { index, status ->
            val isSelected = selectedStatus == status
            FilterChip(
                selected = isSelected,
                onClick = {
                    onStatusClick(status)
                    scrollToCenter(index)
                },
                label = { Text(status) },
                modifier = Modifier
                    .testTag("filter_chip_$status")
                    .onGloballyPositioned { coordinates ->
                        val pos = coordinates.positionInParent()
                        val x = pos.x.toInt()
                        val width = coordinates.size.width
                        val current = chipBounds[index]
                        if (current == null || current.x != x || current.width != width) {
                            chipBounds[index] = ChipLayoutInfo(x = x, width = width)
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
