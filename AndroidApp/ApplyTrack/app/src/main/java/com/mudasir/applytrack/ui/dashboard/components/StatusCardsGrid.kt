package com.mudasir.applytrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudasir.applytrack.ui.DashboardAnalytics
import com.mudasir.applytrack.ui.theme.AccentGreen
import com.mudasir.applytrack.ui.theme.ErrorRed
import com.mudasir.applytrack.ui.theme.LinkBlue
import com.mudasir.applytrack.ui.theme.WarningAmber
import com.mudasir.applytrack.ui.theme.SavedGray

@Composable
fun StatusCardsGrid(
    analytics: DashboardAnalytics,
    onStatusClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusStatCard(
                title = "Applied",
                count = if (isSyncing) "—" else analytics.applied.toString(),
                textColor = WarningAmber,
                tint = WarningAmber.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Applied") }
            )
            StatusStatCard(
                title = "Saved",
                count = if (isSyncing) "—" else analytics.saved.toString(),
                textColor = SavedGray,
                tint = SavedGray.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Saved") }
            )
            StatusStatCard(
                title = "Interview",
                count = if (isSyncing) "—" else analytics.interviews.toString(),
                textColor = AccentGreen,
                tint = AccentGreen.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Interview") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusStatCard(
                title = "Offers",
                count = if (isSyncing) "—" else analytics.offers.toString(),
                textColor = LinkBlue,
                tint = LinkBlue.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Offer") }
            )
            StatusStatCard(
                title = "Rejected",
                count = if (isSyncing) "—" else analytics.rejected.toString(),
                textColor = ErrorRed,
                tint = ErrorRed.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Rejected") }
            )
            StatusStatCard(
                title = "Response",
                count = if (isSyncing) "—" else analytics.responses.toString(),
                textColor = MaterialTheme.colorScheme.primary,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                modifier = Modifier.weight(1f),
                onClick = { onStatusClick("Response") }
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
