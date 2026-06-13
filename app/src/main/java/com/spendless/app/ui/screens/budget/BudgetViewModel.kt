package com.spendless.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.entities.Budget
import com.spendless.app.core.data.database.entities.BudgetCycle
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.dao.CategoryDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.datastore.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BudgetUiState(
    val isLoading: Boolean = true,
    val currentBudget: Double = 0.0,
    val budgetInput: String = "",
    val resetDay: Int = 1,
    val cycleStartLabel: String = "",
    val cycleEndLabel: String = "",
    val categoryBudgets: Map<Category, Double> = emptyMap(),
    val isSaved: Boolean = false,
    val spentAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val excludedAmount: Double = 0.0
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetEngine: BudgetEngine,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    val allCategories = categoryDao.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    init {
        viewModelScope.launch {
            budgetDao.getActiveBudget().collectLatest { budget ->
                val resetDay = budget?.resetDay ?: 1
                val cycle = budgetEngine.getCurrentCycle(resetDay)
                val categoryBudgets = try {
                    Json.decodeFromString<Map<String, Double>>(budget?.categoryBudgetsJson ?: "{}")
                        .mapKeys { Category.fromName(it.key) }
                } catch (e: Exception) {
                    emptyMap()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentBudget = budget?.totalBudget ?: 0.0,
                        budgetInput = (budget?.totalBudget ?: 10000.0).toInt().toString(),
                        resetDay = resetDay,
                        cycleStartLabel = dateFormat.format(Date(cycle.startMs)),
                        cycleEndLabel = dateFormat.format(Date(cycle.endMs)),
                        categoryBudgets = categoryBudgets
                    )
                }
            }
        }

        viewModelScope.launch {
            budgetDao.getActiveCycle().collectLatest { cycle ->
                if (cycle != null) {
                    combine(
                        transactionDao.getTotalSpentInCycle(cycle.id),
                        transactionDao.getPendingSumFlow(),
                        transactionDao.getExcludedTransactionsSum(cycle.id)
                    ) { spent, pending, excluded ->
                        _uiState.update {
                            it.copy(
                                spentAmount = spent ?: 0.0,
                                pendingAmount = pending ?: 0.0,
                                excludedAmount = excluded ?: 0.0
                            )
                        }
                    }.collect()
                } else {
                    _uiState.update {
                        it.copy(
                            spentAmount = 0.0,
                            pendingAmount = 0.0,
                            excludedAmount = 0.0
                        )
                    }
                }
            }
        }
    }

    fun setBudgetInput(value: String) {
        _uiState.update { it.copy(budgetInput = value.filter { c -> c.isDigit() }, isSaved = false) }
    }

    fun setResetDay(day: Int) {
        val cycle = budgetEngine.getCurrentCycle(day)
        _uiState.update {
            it.copy(
                resetDay = day,
                cycleStartLabel = dateFormat.format(Date(cycle.startMs)),
                cycleEndLabel = dateFormat.format(Date(cycle.endMs)),
                isSaved = false
            )
        }
    }

    fun setCategoryBudget(category: Category, amount: Double) {
        _uiState.update {
            it.copy(
                categoryBudgets = it.categoryBudgets + (category to amount),
                isSaved = false
            )
        }
    }

    fun saveBudget() {
        viewModelScope.launch {
            val amount = _uiState.value.budgetInput.toDoubleOrNull() ?: return@launch
            val resetDay = _uiState.value.resetDay

            val catBudgetsJson = Json.encodeToString(
                _uiState.value.categoryBudgets.mapKeys { it.key.name }
            )

            budgetDao.deactivateAllBudgets()
            budgetDao.deactivateAllCycles()

            val budgetId = budgetDao.insertBudget(
                Budget(
                    totalBudget = amount,
                    resetDay = resetDay,
                    categoryBudgetsJson = catBudgetsJson,
                    isActive = true
                )
            )
            preferencesDataStore.setActiveBudgetId(budgetId)
            preferencesDataStore.setResetDay(resetDay)

            // Create new active cycle
            val cycle = budgetEngine.getCurrentCycle(resetDay)
            val activeCycleId = budgetDao.insertCycle(
                BudgetCycle(
                    budgetId = budgetId,
                    startDate = cycle.startMs,
                    endDate = cycle.endMs,
                    isActive = true
                )
            )

            // Re-assign all existing transactions to new cycles
            val allTxns = transactionDao.getAllTransactionsSync()
            val cycleCache = mutableMapOf<Long, Long>() // startDateMs -> cycleId
            cycleCache[cycle.startMs] = activeCycleId

            allTxns.forEach { txn ->
                val range = budgetEngine.getCycleForTimestamp(resetDay, txn.timestamp)
                val cycleId = cycleCache.getOrPut(range.startMs) {
                    var c = budgetDao.getCycleForTimestamp(budgetId, txn.timestamp)
                    if (c == null) {
                        val isCurrent = range.startMs == cycle.startMs
                        val newId = budgetDao.insertCycle(
                            BudgetCycle(
                                budgetId = budgetId,
                                startDate = range.startMs,
                                endDate = range.endMs,
                                isActive = isCurrent
                            )
                        )
                        newId
                    } else {
                        c.id
                    }
                }
                transactionDao.update(txn.copy(cycleId = cycleId))
            }

            // Recalculate totals for all newly created/associated cycles
            cycleCache.values.forEach { cid ->
                val spent = transactionDao.getTotalSpentInCycleSync(cid) ?: 0.0
                val income = transactionDao.getTotalIncomeInCycleSync(cid) ?: 0.0
                budgetDao.updateCycleTotals(cid, spent, income)
            }

            _uiState.update { it.copy(isSaved = true, currentBudget = amount) }
        }
    }
}
