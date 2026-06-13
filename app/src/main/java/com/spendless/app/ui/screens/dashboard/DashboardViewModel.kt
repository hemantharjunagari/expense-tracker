package com.spendless.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.analytics.AnalyticsEngine
import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.categorization.CategorizationEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.dao.CategoryDao
import com.spendless.app.core.data.database.dao.MonthlySummaryDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.entities.Budget
import com.spendless.app.core.data.database.entities.BudgetCycle
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.Transaction
import com.spendless.app.core.data.datastore.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val budget: Budget? = null,
    val activeCycle: BudgetCycle? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val remaining: Double = 0.0,
    val utilizationPercent: Float = 0f,
    val savingsPercent: Float = 0f,
    val categoryBreakdown: List<Pair<Category, Double>> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val cycleStartLabel: String = "",
    val cycleEndLabel: String = "",
    val pendingReviewCount: Int = 0,
    val pendingReviewSum: Double = 0.0,
    val appName: String = "SpendLess",
    val error: String? = null,
    val showTrackingPopup: Boolean = false,
    val trackingPeriod: String = "Current Cycle",
    val trackingData: List<AnalyticsEngine.DailyIncomeExpense> = emptyList(),
    val trackingIsLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val monthlySummaryDao: MonthlySummaryDao,
    private val budgetEngine: BudgetEngine,
    private val analyticsEngine: AnalyticsEngine,
    private val preferencesDataStore: PreferencesDataStore,
    private val categorizationEngine: CategorizationEngine,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    data class DashboardData(
        val spent: Double,
        val income: Double,
        val pendingCount: Int,
        val pendingSum: Double
    )

    init {
        viewModelScope.launch {
            categorizationEngine.seedSystemCategoriesIfNeeded(categoryDao)
            loadDashboard()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                budgetDao.getActiveBudget(),
                budgetDao.getActiveCycle(),
                preferencesDataStore.appName
            ) { budget, cycle, appName ->
                Triple(budget, cycle, appName)
            }.collectLatest { (budget, cycle, appName) ->
                if (budget == null) {
                    _uiState.update { it.copy(isLoading = false, appName = appName) }
                    return@collectLatest
                }

                // Ensure we have an active cycle
                val activeCycle = cycle ?: run {
                    val cycleRange = budgetEngine.getCurrentCycle(budget.resetDay)
                    val newCycleId = budgetDao.insertCycle(
                        BudgetCycle(
                            budgetId = budget.id,
                            startDate = cycleRange.startMs,
                            endDate = cycleRange.endMs,
                            isActive = true
                        )
                    )
                    budgetDao.activateNewCycle(newCycleId)
                    budgetDao.getActiveCycleSync()
                } ?: return@collectLatest

                // Load spending data
                combine(
                    transactionDao.getTotalSpentInCycle(activeCycle.id),
                    transactionDao.getTotalIncomeInCycle(activeCycle.id),
                    transactionDao.getPendingCountFlow(),
                    transactionDao.getPendingSumFlow()
                ) { spent, income, pendingCount, pendingSum ->
                    DashboardData(
                        spent = spent ?: 0.0,
                        income = income ?: 0.0,
                        pendingCount = pendingCount,
                        pendingSum = pendingSum ?: 0.0
                    )
                }.collectLatest { data ->
                    val remaining = budgetEngine.getRemaining(data.spent, budget.totalBudget)
                    val utilPercent = budgetEngine.getUtilizationPercent(data.spent, budget.totalBudget)
                    val savingsPercent = budgetEngine.getSavingsPercent(data.income, data.spent)

                    // Category breakdown
                    val breakdown = transactionDao.getCategoryBreakdown(activeCycle.id)
                        .sortedByDescending { it.total }
                        .map { it.category to it.total }

                    // Recent transactions (last 5)
                    val recent = transactionDao.getTopExpensesInCycle(activeCycle.id)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            budget = budget,
                            activeCycle = activeCycle,
                            totalBudget = budget.totalBudget,
                            totalSpent = data.spent,
                            totalIncome = data.income,
                            remaining = remaining,
                            utilizationPercent = utilPercent,
                            savingsPercent = savingsPercent,
                            categoryBreakdown = breakdown,
                            recentTransactions = recent,
                            cycleStartLabel = dateFormat.format(Date(activeCycle.startDate)),
                            cycleEndLabel = dateFormat.format(Date(activeCycle.endDate)),
                            pendingReviewCount = data.pendingCount,
                            pendingReviewSum = data.pendingSum,
                            appName = appName
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDashboard()
    }

    fun setTrackingPopupVisible(visible: Boolean) {
        _uiState.update { it.copy(showTrackingPopup = visible) }
        if (visible) {
            loadTrackingData()
        }
    }

    fun setTrackingPeriod(period: String) {
        _uiState.update { it.copy(trackingPeriod = period) }
        loadTrackingData()
    }

    private fun loadTrackingData() {
        viewModelScope.launch {
            _uiState.update { it.copy(trackingIsLoading = true) }
            val period = _uiState.value.trackingPeriod
            val cycle = _uiState.value.activeCycle
            
            val (startMs, endMs) = when (period) {
                "Current Cycle" -> {
                    if (cycle != null) {
                        cycle.startDate to cycle.endDate
                    } else {
                        getDefaultMonthRange()
                    }
                }
                "Current Month" -> getDefaultMonthRange()
                "Last 30 Days" -> {
                    val now = System.currentTimeMillis()
                    (now - 30L * 24 * 60 * 60 * 1000) to now
                }
                else -> getDefaultMonthRange()
            }
            
            val data = analyticsEngine.getDailyIncomeVsExpense(startMs, endMs)
            _uiState.update { 
                it.copy(
                    trackingData = data,
                    trackingIsLoading = false
                )
            }
        }
    }

    private fun getDefaultMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return start to calendar.timeInMillis
    }
}

// Extension to activate a specific cycle
suspend fun BudgetDao.activateNewCycle(cycleId: Long) {
    deactivateAllCycles()
    val cycle = getCycleById(cycleId)
    if (cycle != null) updateCycle(cycle.copy(isActive = true))
}
