package com.example.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.ResumeStat
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LinkBlue

@Composable
fun ResumeEffectivenessSection(
    resumeStats: List<ResumeStat>,
    onResumeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Resume Effectiveness",
        modifier = modifier
    ) {
        if (resumeStats.isEmpty()) {
            EmptyStateText("No resumes attached yet. Attach a CV/resume to applications to track which works best.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
