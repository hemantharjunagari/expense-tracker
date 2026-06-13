package com.spendless.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.analytics.AnalyticsEngine
import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.entities.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AnalyticsEngine.AnalyticsPeriod = AnalyticsEngine.AnalyticsPeriod.MONTHLY,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val transactionCount: Int = 0,
    val avgDailyExpense: Double = 0.0,
    val highestSpendingDayLabel: String = "",
    val highestSpendingDayAmount: Double = 0.0,
    val highestCategory: Category? = null,
    val categoryBreakdown: Map<Category, Double> = emptyMap(),
    val dailyExpenses: List<Pair<String, Double>> = emptyList(),
    val topMerchants: List<Pair<String, Double>> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsEngine: AnalyticsEngine,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    init {
        loadAnalytics(AnalyticsEngine.AnalyticsPeriod.MONTHLY)
    }

    fun selectPeriod(period: AnalyticsEngine.AnalyticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
        loadAnalytics(period)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAnalytics(period: AnalyticsEngine.AnalyticsPeriod) {
        viewModelScope.launch {
            val (startMs, endMs) = analyticsEngine.getPeriodRange(period)

            analyticsEngine.getSpendingFlow(startMs, endMs)
                .collectLatest { summary ->
                    // Get top merchants
                    val cycle = budgetDao.getActiveCycleSync()
                    val topMerchants = if (cycle != null) {
                        transactionDao.getTopMerchants(cycle.id, 5)
                            .map { it.merchantNormalized.replaceFirstChar { c -> c.uppercase() } to it.total }
                    } else emptyList()

                    // Format daily expenses for chart
                    val dailyExpenses = summary.dailySpending.entries
                        .sortedBy { it.key }
                        .map { (dateMs, amount) ->
                            dayFormat.format(Date(dateMs)) to amount
                        }

                    val highestDayLabel = summary.highestSpendingDay?.let {
                        dayFormat.format(Date(it))
                    } ?: ""

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalExpense = summary.totalExpense,
                            totalIncome = summary.totalIncome,
                            transactionCount = summary.transactionCount,
                            avgDailyExpense = summary.avgDailyExpense,
                            highestSpendingDayLabel = highestDayLabel,
                            highestSpendingDayAmount = summary.highestSpendingDayAmount,
                            highestCategory = summary.highestCategory,
                            categoryBreakdown = summary.categoryBreakdown,
                            dailyExpenses = dailyExpenses,
                            topMerchants = topMerchants
                        )
                    }
                }
        }
    }
}
