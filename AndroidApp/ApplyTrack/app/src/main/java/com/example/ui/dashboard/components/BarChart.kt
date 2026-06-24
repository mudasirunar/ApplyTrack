package com.example.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.MonthActivity

@Composable
fun BarChart(
    data: List<MonthActivity>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    animationDuration: Int = 800,
    onBarClick: (Int) -> Unit = {}
) {
    val maxCount = data.maxOfOrNull { it.count } ?: 1
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less Active",
                style = labelStyle,
                color = labelColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE53935),
                                Color(0xFFFFB300),
                                Color(0xFF4CAF50)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "More Active",
                style = labelStyle,
                color = labelColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .width(600.dp)
                    .padding(vertical = 8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .pointerInput(data, animatedProgress.value) {
                            detectTapGestures { offset ->
                                val barCount = data.size
                                if (barCount > 0) {
                                    val cellWidth = size.width.toFloat() / barCount
                                    val barWidthPx = 20.dp.toPx()
                                    val maxBarHeightPx = size.height.toFloat() - 24.dp.toPx()
                                    val bottomY = size.height.toFloat() - 20.dp.toPx()
                                    
                                    data.forEachIndexed { index, item ->
                                        if (item.count > 0) {
                                            val barHeight = (item.count.toFloat() / maxCount) * maxBarHeightPx * animatedProgress.value
                                            val x = index * cellWidth + (cellWidth - barWidthPx) / 2
                                            val y = bottomY - barHeight
                                            
                                            if (offset.x >= x && offset.x <= x + barWidthPx &&
                                                offset.y >= y && offset.y <= bottomY) {
                                                onBarClick(index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val barCount = data.size
                    if (barCount == 0) return@Canvas

                    val cellWidth = size.width / barCount
                    val barWidth = 20.dp.toPx()
                    val maxBarHeight = size.height - 24.dp.toPx()

                    data.forEachIndexed { index, item ->
                        val barHeight = if (maxCount > 0) {
                            (item.count.toFloat() / maxCount) * maxBarHeight * animatedProgress.value
                        } else 0f
                        val x = index * cellWidth + (cellWidth - barWidth) / 2
                        val y = size.height - 20.dp.toPx() - barHeight

                        if (barHeight > 0f) {
                            val ratio = if (maxCount > 0) item.count.toFloat() / maxCount else 0f
                            val barColorForRatio = if (ratio < 0.5f) {
                                val fraction = ratio / 0.5f
                                lerp(Color(0xFFE53935), Color(0xFFFFB300), fraction)
                            } else {
                                val fraction = (ratio - 0.5f) / 0.5f
                                lerp(Color(0xFFFFB300), Color(0xFF4CAF50), fraction)
                            }

                            val gradientBrush = Brush.verticalGradient(
                                colors = listOf(
                                    barColorForRatio,
                                    barColorForRatio.copy(alpha = 0.5f)
                                ),
                                startY = y,
                                endY = y + barHeight
                            )

                            drawRect(
                                brush = gradientBrush,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight)
                            )

                            drawRect(
                                color = outlineColor.copy(alpha = 0.3f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEach { item ->
                        val ratio = if (maxCount > 0) item.count.toFloat() / maxCount else 0f
                        val barColorForRatio = if (ratio < 0.5f) {
                            val fraction = ratio / 0.5f
                            lerp(Color(0xFFE53935), Color(0xFFFFB300), fraction)
                        } else {
                            val fraction = (ratio - 0.5f) / 0.5f
                            lerp(Color(0xFFFFB300), Color(0xFF4CAF50), fraction)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.count.toString(),
                                style = labelStyle,
                                fontWeight = FontWeight.Bold,
                                color = if (item.count > 0) barColorForRatio else labelColor.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.label,
                                style = labelStyle,
                                color = labelColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
