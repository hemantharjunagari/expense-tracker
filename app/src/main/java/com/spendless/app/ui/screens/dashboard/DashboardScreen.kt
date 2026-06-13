package com.spendless.app.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToTransactions: (String?) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLendBorrow: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            SpendLessBottomBar(
                currentRoute = "dashboard",
                onDashboard = {},
                onTransactions = { onNavigateToTransactions(null) },
                onAnalytics = onNavigateToAnalytics,
                onLendBorrow = onNavigateToLendBorrow
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DotMatrixLoader()
            }
            return@Scaffold
        }

        var headerHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = headerHeight,
                    bottom = paddingValues.calculateBottomPadding() + 24.dp
                )
            ) {
                // Budget progress ring
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BudgetProgressRing(
                            spent = uiState.totalSpent,
                            budget = uiState.totalBudget,
                            size = 220.dp
                        )
                    }
                }

                // Metric cards row
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            MetricCard(
                                label = "BUDGET",
                                amount = uiState.totalBudget,
                                modifier = Modifier.width(140.dp),
                                onClick = { onNavigateToTransactions("EXPENSES") }
                            )
                        }
                        item {
                            MetricCard(
                                label = "SPENT",
                                amount = uiState.totalSpent,
                                modifier = Modifier.width(140.dp),
                                isHighlighted = true,
                                onClick = { onNavigateToTransactions("EXPENSES") }
                            )
                        }
                        item {
                            MetricCard(
                                label = "REMAINING",
                                amount = uiState.remaining,
                                modifier = Modifier.width(140.dp),
                                onClick = { onNavigateToTransactions("EXPENSES") }
                            )
                        }
                        item {
                            MetricCard(
                                label = "INCOME",
                                amount = uiState.totalIncome,
                                modifier = Modifier.width(140.dp),
                                onClick = { onNavigateToTransactions("INCOME") }
                            )
                        }
                    }
                }

                // Category breakdown donut chart
                if (uiState.categoryBreakdown.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "Spending by Category", onClick = onNavigateToAnalytics)
                        Spacer(modifier = Modifier.height(16.dp))

                        val totalSpent = uiState.categoryBreakdown.sumOf { it.second }
                        val slices = uiState.categoryBreakdown.map { (cat, amount) ->
                            DonutSlice(
                                category = cat,
                                amount = amount,
                                percentage = if (totalSpent > 0) ((amount / totalSpent) * 100).toFloat() else 0f
                            )
                        }

                        SpendLessCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                            DonutChart(
                                slices = slices,
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                            )
                        }
                    }
                }

                // Recent transactions
                if (uiState.recentTransactions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "Recent Transactions", onClick = { onNavigateToTransactions(null) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(uiState.recentTransactions) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onNavigateToTransactions(null) }
                        )
                        TransactionDivider()
                    }

                    item {
                        TextButton(
                            onClick = { onNavigateToTransactions(null) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = "View All Transactions",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // Lent & Borrowed quick access banner
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        onClick = onNavigateToLendBorrow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = com.spendless.app.ui.theme.CardShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤝", fontSize = 28.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "LENT & BORROWED",
                                    style = DotMatrixLabel.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Track money lent & borrowed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Icon(
                                Icons.Outlined.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Empty state
                if (uiState.totalSpent == 0.0 && uiState.totalIncome == 0.0) {
                    item {
                        EmptyDashboard()
                    }
                }
            }

            // Fixed DashboardHeader overlay
            DashboardHeader(
                appName = uiState.appName,
                cycleStart = uiState.cycleStartLabel,
                cycleEnd = uiState.cycleEndLabel,
                pendingReviewCount = uiState.pendingReviewCount,
                onReviewClick = onNavigateToReview,
                onSettingsClick = onNavigateToSettings,
                onTrackingClick = { viewModel.setTrackingPopupVisible(true) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { coordinates ->
                        headerHeight = with(density) { coordinates.size.height.toDp() }
                    }
            )

            if (uiState.showTrackingPopup) {
                TrackingDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.setTrackingPopupVisible(false) },
                    onPeriodSelected = { viewModel.setTrackingPeriod(it) }
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    appName: String,
    cycleStart: String,
    cycleEnd: String,
    pendingReviewCount: Int,
    onReviewClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTrackingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName.uppercase(),
                    style = DotMatrixLabel.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$cycleStart – $cycleEnd",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onTrackingClick) {
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = "Tracking",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onReviewClick) {
                    Box {
                        Icon(
                            imageVector = if (pendingReviewCount > 0) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = if (pendingReviewCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pendingReviewCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text("See All", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyDashboard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Bank SMS will appear here automatically",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SpendLessBottomBar(
    currentRoute: String,
    onDashboard: () -> Unit,
    onTransactions: () -> Unit,
    onAnalytics: () -> Unit,
    onLendBorrow: () -> Unit
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentRoute == "dashboard",
                onClick = onDashboard,
                icon = { Icon(Icons.Outlined.Home, null) },
                label = { Text("Home") },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
            NavigationBarItem(
                selected = currentRoute == "transactions",
                onClick = onTransactions,
                icon = { Icon(Icons.Outlined.Receipt, null) },
                label = { Text("Transactions") },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
            NavigationBarItem(
                selected = currentRoute == "analytics",
                onClick = onAnalytics,
                icon = { Icon(Icons.Outlined.BarChart, null) },
                label = { Text("Analytics") },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
            NavigationBarItem(
                selected = currentRoute == "lend/dashboard",
                onClick = onLendBorrow,
                icon = { Icon(Icons.Outlined.Handshake, null) },
                label = { Text("Lending") },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            )
        }
    }
}
