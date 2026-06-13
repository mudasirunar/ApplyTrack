package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ZoomableImage(
    file: File,
    isZoomed: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(file) { mutableStateOf(1f) }
    var offset by remember(file) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isZoomed) {
        if (!isZoomed) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RectangleShape)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { constraints.maxWidth.toFloat() }
        val heightPx = with(density) { constraints.maxHeight.toFloat() }

        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scale) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            val numPointers = event.changes.size

                            if (scale > 1f) {
                                val pan = event.calculatePan()
                                val zoom = event.calculateZoom()

                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                val maxX = (widthPx * (newScale - 1f)) / 2f
                                val maxY = (heightPx * (newScale - 1f)) / 2f

                                val newOffset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )

                                scale = newScale
                                offset = newOffset
                                onZoomChanged(newScale > 1.05f)

                                event.changes.forEach { it.consume() }
                            } else {
                                if (numPointers > 1) {
                                    val pan = event.calculatePan()
                                    val zoom = event.calculateZoom()

                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    val maxX = (widthPx * (newScale - 1f)) / 2f
                                    val maxY = (heightPx * (newScale - 1f)) / 2f

                                    val newOffset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )

                                    scale = newScale
                                    offset = newOffset
                                    onZoomChanged(newScale > 1.05f)

                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                                onZoomChanged(false)
                            } else {
                                val targetScale = 2f
                                val center = Offset(widthPx / 2f, heightPx / 2f)
                                val newOffset = (center - tapOffset) * (targetScale - 1f)

                                val maxX = (widthPx * (targetScale - 1f)) / 2f
                                val maxY = (heightPx * (targetScale - 1f)) / 2f

                                scale = targetScale
                                offset = Offset(
                                    x = newOffset.x.coerceIn(-maxX, maxX),
                                    y = newOffset.y.coerceIn(-maxY, maxY)
                                )
                                onZoomChanged(true)
                            }
                        }
                    )
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
