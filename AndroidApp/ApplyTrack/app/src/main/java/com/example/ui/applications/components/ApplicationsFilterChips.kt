package com.example.ui.applications

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

val filterStatuses = listOf("All", "Applied", "Interview", "Offer", "Rejected", "Saved", "Response", "Resume", "Platform", "Date")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsFilterChips(
    selectedStatus: String,
    onStatusClick: (String) -> Unit,
    scrollState: ScrollState,
    chipPositions: MutableMap<Int, Int>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        filterStatuses.forEachIndexed { index, status ->
            val isSelected = selectedStatus == status
            FilterChip(
                selected = isSelected,
                onClick = { onStatusClick(status) },
                label = { Text(status) },
                modifier = Modifier
                    .testTag("filter_chip_$status")
                    .onGloballyPositioned { coordinates ->
                        chipPositions[index] = coordinates.positionInParent().x.toInt()
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
