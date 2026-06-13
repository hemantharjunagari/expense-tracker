package com.spendless.app.ui.screens.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.core.analytics.AnalyticsEngine
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*

@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Period selector tabs
                val periods = AnalyticsEngine.AnalyticsPeriod.entries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEach { period ->
                        FilterChip(
                            selected = uiState.selectedPeriod == period,
                            onClick = { viewModel.selectPeriod(period) },
                            label = {
                                Text(period.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { DotMatrixLoader() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Summary metrics
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "TOTAL SPENT",
                        amount = uiState.totalExpense,
                        modifier = Modifier.weight(1f),
                        isHighlighted = true
                    )
                    MetricCard(
                        label = "TOTAL INCOME",
                        amount = uiState.totalIncome,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "TRANSACTIONS",
                        amount = uiState.transactionCount.toDouble(),
                        modifier = Modifier.weight(1f),
                        prefix = ""
                    )
                    MetricCard(
                        label = "AVG DAILY",
                        amount = uiState.avgDailyExpense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Insights row
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "INSIGHTS",
                    style = DotMatrixLabel.copy(letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Highest spending day
                    SpendLessCard(modifier = Modifier.weight(1f)) {
                        Text("📅", fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Highest Day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            uiState.highestSpendingDayLabel.ifBlank { "—" },
                            style = MonoAmount,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "₹${formatCurrency(uiState.highestSpendingDayAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Top category
                    SpendLessCard(modifier = Modifier.weight(1f)) {
                        Text(uiState.highestCategory?.emoji ?: "📦", fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Top Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            uiState.highestCategory?.displayName ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Category breakdown
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "BY CATEGORY",
                        style = DotMatrixLabel.copy(letterSpacing = 3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SpendLessCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                        val total = uiState.categoryBreakdown.values.sum()
                        uiState.categoryBreakdown.entries
                            .sortedByDescending { it.value }
                            .forEach { (cat, amount) ->
                                CategoryProgressRow(
                                    category = cat,
                                    amount = amount,
                                    total = total
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                    }
                }
            }

            // Top merchants
            if (uiState.topMerchants.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "TOP MERCHANTS",
                        style = DotMatrixLabel.copy(letterSpacing = 3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                items(uiState.topMerchants) { (merchant, amount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                merchant.first().uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            merchant,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "₹${formatCurrency(amount)}",
                            style = MonoAmount.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(
    category: com.spendless.app.core.data.database.entities.Category,
    amount: Double,
    total: Double
) {
    val percent = if (total > 0) (amount / total).toFloat() else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.emoji, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "₹${formatCurrency(amount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
