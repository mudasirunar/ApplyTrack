package com.example.ui.jobdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.JobApplication
import com.example.ui.theme.LinkBlue

@Composable
fun PlatformDetailsCard(
    job: JobApplication,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Platform Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            InfoRow(label = "Platform", value = job.platform ?: "Unrecorded (Direct)")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "URL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                val hasUrl = !job.url.isNullOrBlank()
                Text(
                    text = if (hasUrl) job.url!! else "Not specified",
                    style = if (hasUrl) {
                        MaterialTheme.typography.bodyMedium.copy(
                            color = LinkBlue,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 16.dp)
                        .then(
                            if (hasUrl) {
                                Modifier.clickable {
                                    try {
                                        val rawUrl = job.url!!
                                        val intentUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) && 
                                                           !rawUrl.startsWith("https://", ignoreCase = true)) {
                                            "https://$rawUrl"
                                        } else {
                                            rawUrl
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // no-op
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                )
            }

            val hasEmail = !job.email.isNullOrBlank()
            if (hasEmail) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contact Email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = job.email!!,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LinkBlue,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 16.dp)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${job.email!!}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // no-op
                                }
                            }
                    )
                }
            }
        }
    }
}
