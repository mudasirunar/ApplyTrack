package com.example.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.DashboardAnalytics
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LinkBlue

@Composable
fun ConversionFunnelRow(
    analytics: DashboardAnalytics,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Conversion Rates",
        modifier = modifier
    ) {
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
