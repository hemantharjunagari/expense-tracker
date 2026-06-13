package com.spendless.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.ui.theme.ChartColors

data class DonutSlice(
    val category: Category,
    val amount: Double,
    val percentage: Float
)

/**
 * Animated donut chart for category spending breakdown.
 * Nothing OS inspired: monochrome shades on pure black.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    strokeWidth: Dp = 14.dp,
    selectedSlice: Category? = null,
    onSliceClick: (Category) -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "donut_progress"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val emptyColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                drawDonut(slices, strokePx, animatedProgress, selectedSlice, emptyColor)
            }

            // Center: total count or selected category
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = slices.size.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend
        Column(modifier = Modifier.weight(1f)) {
            slices.take(6).forEachIndexed { index, slice ->
                DonutLegendItem(
                    color = parseCategoryColor(slice.category.color),
                    label = slice.category.displayName,
                    percentage = slice.percentage,
                    isSelected = slice.category == selectedSlice
                )
            }
        }
    }
}

private fun parseCategoryColor(colorStr: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        Color.White
    }
}

private fun DrawScope.drawDonut(
    slices: List<DonutSlice>,
    strokeWidth: Float,
    animationProgress: Float,
    selectedSlice: Category?,
    emptyColor: Color
) {
    if (slices.isEmpty()) {
        drawArc(
            color = emptyColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(this.size.width - strokeWidth, this.size.height - strokeWidth),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        return
    }

    var startAngle = -90f
    val totalAnimated = 360f * animationProgress

    slices.forEachIndexed { index, slice ->
        val sweepAngle = (slice.percentage / 100f) * totalAnimated
        val color = parseCategoryColor(slice.category.color)
        val isSelected = slice.category == selectedSlice
        val adjustedStroke = if (isSelected) strokeWidth * 1.2f else strokeWidth
        val inset = adjustedStroke / 2f

        drawArc(
            color = if (isSelected) color else color.copy(alpha = 0.8f),
            startAngle = startAngle + 1f, // 1f gap between slices
            sweepAngle = (sweepAngle - 1f).coerceAtLeast(0f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - adjustedStroke, this.size.height - adjustedStroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = adjustedStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Butt
            )
        )
        startAngle += sweepAngle
    }
}

@Composable
private fun DonutLegendItem(
    color: Color,
    label: String,
    percentage: Float,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp, 8.dp)
                .let {
                    if (isSelected) it else it
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = color)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
