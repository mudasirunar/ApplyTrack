package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LinkBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.SavedGray

@Composable
fun StatusPill(status: String) {
    val chipBgColor = when (status.lowercase()) {
        "applied" -> WarningAmber.copy(alpha = 0.1f)
        "saved" -> SavedGray.copy(alpha = 0.1f)
        "interview", "interviewing" -> AccentGreen.copy(alpha = 0.1f)
        "rejected" -> ErrorRed.copy(alpha = 0.1f)
        "offer" -> LinkBlue.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }

    val chipContentColor = when (status.lowercase()) {
        "applied" -> WarningAmber
        "saved" -> SavedGray
        "interview", "interviewing" -> AccentGreen
        "rejected" -> ErrorRed
        "offer" -> LinkBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = chipBgColor,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = chipContentColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TimelineNode(
    stage: String,
    date: String,
    isCurrent: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val lineColor = MaterialTheme.colorScheme.surfaceVariant
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
                .drawBehind {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val strokeWidth = 2.dp.toPx()
                    
                    if (!isFirst) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, centerY),
                            strokeWidth = strokeWidth
                        )
                    }
                    if (!isLast) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, centerY),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 16.dp else 12.dp)
                    .background(
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = stage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
