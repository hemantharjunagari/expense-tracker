package com.spendless.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendless.app.ui.theme.*

/**
 * Nothing OS glass-morphism card component.
 * Features: subtle white border, slightly elevated dark background.
 */
@Composable
fun SpendLessCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isGlass: Boolean = LocalThemeStyle.current == "glass",
    elevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardBackground = if (isGlass) {
        if (LocalThemeStyle.current == "glass") {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(GlassBackground, Color.Transparent)
            )
        }
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface
            )
        )
    }

    val clickModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier

    Column(
        modifier = clickModifier
            .clip(CardShape)
            .background(brush = cardBackground)
            .border(
                width = 1.dp,
                color = if (isGlass) {
                    if (LocalThemeStyle.current == "glass") {
                        MaterialTheme.colorScheme.outline
                    } else {
                        GlassBorder
                    }
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CardShape
            )
            .padding(20.dp),
        content = content
    )
}

/**
 * Summary metric card (spent / remaining / income)
 */
@Composable
fun MetricCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "₹",
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    SpendLessCard(modifier = modifier, onClick = onClick) {
        Text(
            text = label,
            style = DotMatrixLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$prefix${formatCurrency(amount)}",
            style = MonoAmount,
            color = if (isHighlighted)
                MaterialTheme.colorScheme.onBackground
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatCurrency(amount: Double): String {
    return when {
        amount >= 10_00_000 -> String.format("%.2fL", amount / 10_00_000.0)
        amount >= 1_000 -> String.format("%.0f", amount)
        else -> String.format("%.0f", amount)
    }
}

fun formatCurrencyFull(amount: Double): String {
    return when {
        amount >= 10_00_000 -> String.format("%.2f L", amount / 10_00_000.0)
        amount >= 1_000 -> String.format("%.0f", amount)
        else -> String.format("%.2f", amount)
    }
}
