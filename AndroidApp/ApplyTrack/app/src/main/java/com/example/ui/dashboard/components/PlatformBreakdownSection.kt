package com.example.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.PlatformStat

@Composable
fun PlatformBreakdownSection(
    platforms: List<PlatformStat>,
    onPlatformClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Platforms",
        modifier = modifier
    ) {
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
