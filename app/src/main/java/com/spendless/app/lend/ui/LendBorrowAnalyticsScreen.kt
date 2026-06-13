package com.spendless.app.lend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.ui.components.BudgetProgressRing
import com.spendless.app.ui.components.SpendLessCard
import com.spendless.app.ui.theme.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.data.database.dao.ContactAggregate
import com.spendless.app.lend.repository.LendBorrowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class LendBorrowAnalyticsState(
    val isLoading: Boolean = true,
    val totalLent: Double = 0.0,
    val totalRecovered: Double = 0.0,
    val pendingReceivable: Double = 0.0,
    val overdueReceivable: Double = 0.0,
    val recoveryRate: Float = 0f,
    val totalBorrowed: Double = 0.0,
    val outstandingPayable: Double = 0.0,
    val overduePayable: Double = 0.0,
    val topBorrowers: List<ContactAggregate> = emptyList(),
    val activeRecords: Int = 0
)

@HiltViewModel
class LendBorrowAnalyticsViewModel @Inject constructor(
    private val repository: LendBorrowRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LendBorrowAnalyticsState())
    val state: StateFlow<LendBorrowAnalyticsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getSummary(),
                repository.getTotalRecovered(),
                repository.getTopContacts(10),
                repository.getActiveRecordCount()
            ) { summary, recovered, contacts, active ->
                val recoveryRate = if (summary.totalLent > 0)
                    ((recovered / summary.totalLent) * 100f).toFloat().coerceIn(0f, 100f)
                else 0f

                LendBorrowAnalyticsState(
                    isLoading = false,
                    totalLent = summary.totalLent,
                    totalRecovered = recovered,
                    pendingReceivable = summary.totalOutstandingReceivable,
                    overdueReceivable = summary.totalOverdueReceivable,
                    recoveryRate = recoveryRate,
                    totalBorrowed = summary.totalBorrowed,
                    outstandingPayable = summary.totalOutstandingPayable,
                    overduePayable = summary.totalOverduePayable,
                    topBorrowers = contacts,
                    activeRecords = active
                )
            }.collect { _state.value = it }
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun LendBorrowAnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LendBorrowAnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("Lend Analytics", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp)
        ) {
            // Recovery rate hero ring
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BudgetProgressRing(
                        spent = state.totalRecovered,
                        budget = state.totalLent,
                        size = 200.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Recovery Rate", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Lending stats grid
            item {
                AnalyticsSectionHeader("LENDING")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCell("Total Lent", state.totalLent, Modifier.weight(1f))
                    AnalyticsCell("Recovered", state.totalRecovered, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCell("Pending", state.pendingReceivable, Modifier.weight(1f))
                    AnalyticsCell("Overdue ⛔", state.overdueReceivable, Modifier.weight(1f))
                }
            }

            // Borrowing stats grid
            item {
                Spacer(Modifier.height(16.dp))
                AnalyticsSectionHeader("BORROWING")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCell("Total Borrowed", state.totalBorrowed, Modifier.weight(1f))
                    AnalyticsCell("Outstanding", state.outstandingPayable, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalyticsCell("Overdue ⛔", state.overduePayable, Modifier.weight(1f))
                    AnalyticsCell("Active Loans", state.activeRecords.toDouble(), Modifier.weight(1f), isCount = true)
                }
            }

            // Top contacts
            if (state.topBorrowers.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    AnalyticsSectionHeader("TOP CONTACTS BY OUTSTANDING")
                }
                items(state.topBorrowers) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(contact.name, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                contact.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "${contact.recordCount} loan(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val outstandingLong = contact.outstanding.toLong()
                        val (text, color) = when {
                            outstandingLong > 0 -> "+₹$outstandingLong" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            outstandingLong < 0 -> "-₹${kotlin.math.abs(outstandingLong)}" to androidx.compose.ui.graphics.Color(0xFFE53935)
                            else -> "₹0" to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = text,
                            style = MonoAmount.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsSectionHeader(title: String) {
    Text(
        title,
        style = DotMatrixLabel.copy(letterSpacing = 2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun AnalyticsCell(
    label: String,
    value: Double,
    modifier: Modifier,
    isCount: Boolean = false
) {
    SpendLessCard(modifier = modifier) {
        Text(label, style = DotMatrixLabel.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(
            if (isCount) value.toInt().toString() else "₹${value.toLong()}",
            style = MonoAmount.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
