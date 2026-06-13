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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendless.app.ui.theme.*

/**
 * Nothing OS inspired circular progress ring.
 * Animates from 0 to the target percentage on first composition.
 */
@Composable
fun CircularProgressRing(
    percent: Float,       // 0f..100f
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    // Animate from 0 to target
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "progress_ring"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(
                this.size.width - strokePx,
                this.size.height - strokePx
            )
            val topLeft = Offset(inset, inset)

            // Track (background ring)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweepAngle = (animatedPercent / 100f) * 360f
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Dot at the progress end point (Nothing UI detail)
            if (animatedPercent > 0f && animatedPercent < 100f) {
                val angleRadians = Math.toRadians((-90f + sweepAngle).toDouble())
                val radius = (this.size.width - strokePx) / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val dotCenter = Offset(
                    (center.x + radius * kotlin.math.cos(angleRadians)).toFloat(),
                    (center.y + radius * kotlin.math.sin(angleRadians)).toFloat()
                )
                drawCircle(
                    color = progressColor,
                    radius = strokePx / 2f,
                    center = dotCenter
                )
            }
        }

        // Center content (amount, percent text, etc.)
        centerContent()
    }
}

/**
 * Full budget progress ring with amount display inside.
 */
@Composable
fun BudgetProgressRing(
    spent: Double,
    budget: Double,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val percent = if (budget > 0) ((spent / budget) * 100).coerceIn(0.0, 100.0).toFloat() else 0f
    val remaining = (budget - spent).coerceAtLeast(0.0)
    val isOverBudget = spent > budget

    CircularProgressRing(
        percent = percent,
        modifier = modifier,
        size = size,
        strokeWidth = 14.dp,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        progressColor = Color.Red
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${percent.toInt()}%",
                style = MonoAmountLarge.copy(fontSize = 32.sp),
                color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "used",
                style = DotMatrixLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isOverBudget) {
                Text(
                    text = "₹${formatAmount(spent - budget)} is over utilized from budget",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "₹${formatAmount(remaining)} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100_000 -> String.format("%.1fL", amount / 100_000)
        amount >= 1_000 -> String.format("%.1fK", amount / 1_000)
        else -> amount.toInt().toString()
    }
}
