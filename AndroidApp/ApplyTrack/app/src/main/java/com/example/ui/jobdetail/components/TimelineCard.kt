package com.example.ui.jobdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.JobApplication
import com.example.model.StatusHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineCard(
    job: JobApplication,
    modifier: Modifier = Modifier
) {
    val sdfApplied = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Column {
                val historyEntries = remember(job.statusHistory, job.createdAt, job.updatedAt, job.status) {
                    val parsed = job.statusHistory
                    if (!parsed.isNullOrEmpty()) {
                        parsed
                    } else {
                        if (job.updatedAt > job.createdAt + 1000L) {
                            listOf(
                                StatusHistoryEntry("Saved", job.createdAt),
                                StatusHistoryEntry(job.status, job.updatedAt)
                            )
                        } else {
                            listOf(
                                StatusHistoryEntry(job.status, job.createdAt)
                            )
                        }
                    }
                }

                historyEntries.asReversed().forEachIndexed { index, entry ->
                    val isCurrent = index == 0
                    val isFirst = index == 0
                    val isLast = index == historyEntries.size - 1
                    
                    val stageLabel = entry.status.replaceFirstChar { it.uppercase() }
                    
                    TimelineNode(
                        stage = stageLabel,
                        date = sdfApplied.format(Date(entry.timestamp)),
                        isCurrent = isCurrent,
                        isFirst = isFirst,
                        isLast = isLast
                    )
                }
            }
        }
    }
}
