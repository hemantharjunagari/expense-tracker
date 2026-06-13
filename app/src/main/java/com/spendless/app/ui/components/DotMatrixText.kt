package com.spendless.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nothing OS dot-matrix grid background pattern.
 * Used as a decorative element on cards and hero sections.
 */
@Composable
fun DotMatrixBackground(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.outline,
    dotRadius: Dp = 1.dp,
    spacing: Dp = 12.dp
) {
    Canvas(modifier = modifier) {
        val spacingPx = spacing.toPx()
        val dotRadiusPx = dotRadius.toPx()

        val cols = (size.width / spacingPx).toInt() + 1
        val rows = (size.height / spacingPx).toInt() + 1

        for (row in 0..rows) {
            for (col in 0..cols) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(col * spacingPx, row * spacingPx)
                )
            }
        }
    }
}

/**
 * Animated dot-matrix loading indicator.
 * Three dots that pulse in sequence — Nothing OS style.
 */
@Composable
fun DotMatrixLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    dotSize: Dp = 8.dp,
    spacing: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")

    val delays = listOf(0, 160, 320)
    val scales = delays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale_$delay"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        scales.forEach { scale ->
            val scaleValue = scale.value
            Canvas(modifier = Modifier.size(dotSize)) {
                drawCircle(
                    color = color.copy(alpha = scaleValue),
                    radius = (dotSize.toPx() / 2) * scaleValue
                )
            }
        }
    }
}
