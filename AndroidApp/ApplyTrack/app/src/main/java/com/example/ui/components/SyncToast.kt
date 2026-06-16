package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.SyncState

@Composable
fun SyncToast(
    visible: Boolean,
    toastState: SyncState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (toastState) {
                    SyncState.SYNCING -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    SyncState.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(0xFF4CAF50), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    SyncState.ERROR -> {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(0xFFF44336), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    else -> {}
                }

                val textMessage = when (toastState) {
                    SyncState.SYNCING -> "Syncing..."
                    SyncState.SUCCESS -> "Sync successful"
                    SyncState.ERROR -> "Sync failed"
                    else -> ""
                }

                Text(
                    text = textMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
