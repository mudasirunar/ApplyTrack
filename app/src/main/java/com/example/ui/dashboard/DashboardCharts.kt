package com.example.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.MonthActivity
import com.example.ui.StatusSlice
import com.example.ui.theme.SavedGray
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.LinkBlue
import com.example.ui.theme.ErrorRed

@Composable
fun CircularProgressRing(
    percentage: Float,
    color: Color,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    animationDuration: Int = 1000
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(percentage) {
        animatedProgress.animateTo(
            targetValue = percentage.coerceIn(0f, 100f),
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Foreground arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animatedProgress.value / 100f * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "${animatedProgress.value.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

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
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // Non-scrollable Intensity Heatmap legend above bars
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
                                Color(0xFFE53935), // Red
                                Color(0xFFFFB300), // Yellow
                                Color(0xFF4CAF50)  // Green
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

        // Horizontally scrollable bars area
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
                                    val barWidthPx = 14.dp.toPx()
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
                    val barWidth = 14.dp.toPx()
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

                // Month labels
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

@Composable
fun DonutChart(
    slices: List<StatusSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 24.dp,
    animationDuration: Int = 1000
) {
    val total = slices.sumOf { it.count }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (total > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val arcSize = this.size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)

                var startAngle = -90f
                slices.forEach { slice ->
                    val sweepAngle = (slice.count.toFloat() / total) * 360f * animatedProgress.value
                    drawArc(
                        color = Color(slice.color),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val arcSize = this.size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "No data",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HorizontalBarRow(
    label: String,
    count: Int,
    maxCount: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(fraction) {
        animatedFraction.animateTo(
            targetValue = fraction,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            // Track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Fill
            if (animatedFraction.value > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset.Zero,
                    size = Size(size.width * animatedFraction.value, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
