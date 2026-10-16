package com.mudasir.applytrack.ui.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DashboardHeader(totalCount: Int, isSyncing: Boolean = false) {
    var message by remember(totalCount) {
        mutableStateOf(DashboardMessageProvider.getDashboardMessage(totalCount))
    }

    LaunchedEffect(totalCount, isSyncing) {
        if (!isSyncing) {
            while (true) {
                delay(10000L)
                message = DashboardMessageProvider.getDashboardMessage(totalCount)
            }
        }
    }

    val currentDisplayMessage = if (isSyncing) "Syncing your applications..." else message

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
            targetState = currentDisplayMessage,
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
