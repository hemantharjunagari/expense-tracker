package com.spendless.app.ui.screens.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.spendless.app.core.categorization.CategorizationEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.dao.CategoryDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.dao.DbMonthSummary
import com.spendless.app.core.data.database.dao.getTotalSpentInCycleSyncFlow
import com.spendless.app.core.data.database.dao.getTotalIncomeInCycleSyncFlow
import com.spendless.app.core.data.database.entities.*
import com.spendless.app.lend.repository.LendBorrowRepository
import com.spendless.app.widget.WidgetUpdateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.paging.insertSeparators
import androidx.paging.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class TransactionTab { ALL, PENDING, EXPENSES, INCOME, SELF_TRANSFER, UNCATEGORIZED }

enum class TransactionPeriod {
    CURRENT_CYCLE, PREVIOUS_CYCLE, CURRENT_MONTH, PREVIOUS_MONTH,
    CURRENT_QUARTER, CURRENT_YEAR, FINANCIAL_YEAR, CUSTOM, ALL
}

enum class SortBy {
    NEWEST, OLDEST, AMOUNT_DESC, AMOUNT_ASC, MERCHANT, CATEGORY, RECENTLY_EDITED, PENDING_REVIEW_FIRST, DUE_DATE
}

data class FilterState(
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val selectedTab: TransactionTab = TransactionTab.ALL,
    val period: TransactionPeriod = TransactionPeriod.CURRENT_CYCLE,
    val sortBy: SortBy = SortBy.NEWEST,
    val customDateRange: Pair<Long, Long>? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val status: TransactionStatus? = null,
    val paymentMethod: PaymentMethod? = null,
    val isManual: Boolean? = null,
    val isExcludedFromBudget: Boolean? = null,
    val isExcludedFromAnalytics: Boolean? = null,
    val contactName: String? = null,
    val selectedMerchant: String? = null
)

data class QuickStats(
    val count: Int = 0,
    val expenses: Double = 0.0,
    val income: Double = 0.0,
    val pendingCount: Int = 0
)

data class MonthSummary(
    val monthYear: String,
    val count: Int = 0,
    val expenses: Double = 0.0,
    val income: Double = 0.0,
    val pendingCount: Int = 0
)

sealed class TransactionUiModel {
    data class Item(val transaction: Transaction) : TransactionUiModel()
    data class Header(val summary: MonthSummary) : TransactionUiModel()
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val categorizationEngine: CategorizationEngine,
    private val budgetEngine: com.spendless.app.core.analytics.BudgetEngine,
    private val lendBorrowRepository: LendBorrowRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    init {
        savedStateHandle.get<String>("tab")?.let { tabStr ->
            try {
                val tab = TransactionTab.valueOf(tabStr)
                _filterState.update { it.copy(selectedTab = tab) }
            } catch (e: java.lang.Exception) {
                // Ignore invalid tab name
            }
        }
    }

    private val _selectionMode = MutableStateFlow<Set<Long>>(emptySet())
    val selectionMode: StateFlow<Set<Long>> = _selectionMode.asStateFlow()

    val allCategories = categoryDao.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeBudget = budgetDao.getActiveBudget()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentRange: StateFlow<Pair<Long, Long>> = combine(_filterState, activeBudget) { filter, budget ->
        calculateDateRange(filter, budget)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L to Long.MAX_VALUE)

    val monthlySummaries: StateFlow<List<DbMonthSummary>> = transactionDao.getMonthlySummariesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val transactions = combine(_filterState, activeBudget) { filter, budget ->
        filter to budget
    }.debounce(300L)
    .flatMapLatest { (filter, budget) ->
        val range = calculateDateRange(filter, budget)
        val tabType = if (filter.selectedTab == TransactionTab.PENDING) null else mapTabToType(filter.selectedTab)
        val tabStatus = when (filter.selectedTab) {
            TransactionTab.PENDING -> "PENDING_REVIEW"
            TransactionTab.EXPENSES -> "APPROVED"
            TransactionTab.INCOME -> "APPROVED"
            TransactionTab.SELF_TRANSFER -> "SELF_TRANSFER"
            else -> filter.status?.name
        }
        Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false)
        ) {
            transactionDao.getFilteredTransactionsPagedAdvanced(
                searchQuery = filter.searchQuery,
                selectedCategoryName = if (filter.selectedTab == TransactionTab.UNCATEGORIZED) "UNCATEGORIZED" else filter.selectedCategory?.name,
                type = if (filter.selectedTab == TransactionTab.ALL) null else tabType,
                status = tabStatus,
                startTime = range.first,
                endTime = range.second,
                minAmount = filter.minAmount,
                maxAmount = filter.maxAmount,
                paymentMethod = filter.paymentMethod?.name,
                isManual = filter.isManual,
                isExcludedFromBudget = filter.isExcludedFromBudget,
                isExcludedFromAnalytics = filter.isExcludedFromAnalytics,
                contactName = filter.contactName,
                merchantName = filter.selectedMerchant,
                sortBy = filter.sortBy.name
            )
        }.flow.map { pagingData ->
            pagingData.map { TransactionUiModel.Item(it) }
                .insertSeparators { before, after ->
                    val beforeMonth = before?.let { getMonthYear((it as TransactionUiModel.Item).transaction.timestamp) }
                    val afterMonth = after?.let { getMonthYear((it as TransactionUiModel.Item).transaction.timestamp) }
                    
                    if (afterMonth != null && beforeMonth != afterMonth) {
                        TransactionUiModel.Header(MonthSummary(monthYear = afterMonth))
                    } else if (before == null && afterMonth != null) {
                        TransactionUiModel.Header(MonthSummary(monthYear = afterMonth))
                    } else {
                        null
                    }
                }
        }
    }
    .cachedIn(viewModelScope)

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    private fun getMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    val quickStats = combine(_filterState, activeBudget) { filter, budget ->
        filter to budget
    }.debounce(300L)
    .flatMapLatest { (filter, budget) ->
        val range = calculateDateRange(filter, budget)
        val tabType = if (filter.selectedTab == TransactionTab.PENDING) null else mapTabToType(filter.selectedTab)
        val tabStatus = when (filter.selectedTab) {
            TransactionTab.PENDING -> "PENDING_REVIEW"
            TransactionTab.EXPENSES -> "APPROVED"
            TransactionTab.INCOME -> "APPROVED"
            TransactionTab.SELF_TRANSFER -> "SELF_TRANSFER"
            else -> filter.status?.name
        }
        transactionDao.getQuickStats(
            startTime = range.first,
            endTime = range.second,
            searchQuery = filter.searchQuery,
            categoryName = if (filter.selectedTab == TransactionTab.UNCATEGORIZED) "UNCATEGORIZED" else filter.selectedCategory?.name,
            type = if (filter.selectedTab == TransactionTab.ALL) null else tabType,
            status = tabStatus,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            paymentMethod = filter.paymentMethod?.name,
            isManual = filter.isManual,
            isExcludedFromBudget = filter.isExcludedFromBudget,
            isExcludedFromAnalytics = filter.isExcludedFromAnalytics,
            contactName = filter.contactName,
            merchantName = filter.selectedMerchant
        ).map { stats ->
            QuickStats(
                count = stats.count,
                expenses = stats.totalSpent ?: 0.0,
                income = stats.totalIncome ?: 0.0,
                pendingCount = stats.pendingCount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickStats())

    private fun mapTabToType(tab: TransactionTab): String? {
        return when (tab) {
            TransactionTab.EXPENSES -> "DEBIT"
            TransactionTab.INCOME -> "CREDIT"
            TransactionTab.SELF_TRANSFER -> "SELF_TRANSFER"
            else -> null
        }
    }

    fun formatMonthKey(monthKey: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = parser.parse(monthKey)
            if (date != null) formatter.format(date) else monthKey
        } catch (e: Exception) {
            monthKey
        }
    }

    fun formatRange(range: Pair<Long, Long>): String {
        if (range.first == 0L && range.second == Long.MAX_VALUE) {
            return "All Time"
        }
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return "${sdf.format(Date(range.first))} → ${sdf.format(Date(range.second))}"
    }

    private fun calculateDateRange(filter: FilterState, budget: Budget?): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when (filter.period) {
            TransactionPeriod.CURRENT_CYCLE -> {
                val range = budgetEngine.getCurrentCycle(budget?.resetDay ?: 1)
                range.startMs to range.endMs
            }
            TransactionPeriod.PREVIOUS_CYCLE -> {
                val prevCycles = budgetEngine.getPreviousCycles(budget?.resetDay ?: 1, 1)
                prevCycles.firstOrNull()?.let { it.startMs to it.endMs } ?: (0L to now)
            }
            TransactionPeriod.CURRENT_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                start to calendar.timeInMillis
            }
            TransactionPeriod.PREVIOUS_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                start to calendar.timeInMillis
            }
            TransactionPeriod.CURRENT_QUARTER -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3
                calendar.set(Calendar.MONTH, quarterStartMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 3)
                calendar.add(Calendar.MILLISECOND, -1)
                start to calendar.timeInMillis
            }
            TransactionPeriod.CURRENT_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                start to calendar.timeInMillis
            }
            TransactionPeriod.FINANCIAL_YEAR -> {
                val currentMonth = calendar.get(Calendar.MONTH)
                if (currentMonth < Calendar.APRIL) {
                    calendar.add(Calendar.YEAR, -1)
                }
                calendar.set(Calendar.MONTH, Calendar.APRIL)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                start to calendar.timeInMillis
            }
            TransactionPeriod.CUSTOM -> {
                filter.customDateRange ?: (0L to now)
            }
            TransactionPeriod.ALL -> 0L to Long.MAX_VALUE
        }
    }

    fun setPeriod(period: TransactionPeriod) {
        _filterState.update { it.copy(period = period) }
    }

    fun setSortBy(sortBy: SortBy) {
        _filterState.update { it.copy(sortBy = sortBy) }
    }

    fun setTab(tab: TransactionTab) {
        _filterState.update { it.copy(selectedTab = tab, selectedCategory = null) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun setCategory(category: Category?) {
        _filterState.update { it.copy(selectedCategory = category) }
    }

    fun updateFilters(updater: (FilterState) -> FilterState) {
        _filterState.update(updater)
    }

    fun clearFilters() {
        _filterState.update { 
            FilterState(
                sortBy = it.sortBy,
                period = it.period,
                selectedTab = it.selectedTab
            ) 
        }
    }

    fun updateTransactionNotes(transaction: Transaction, notes: String) {
        viewModelScope.launch {
            transactionDao.update(transaction.copy(userNote = notes, isManuallyEdited = true))
        }
    }

    fun updateTransactionStatus(transaction: Transaction, status: TransactionStatus) {
        viewModelScope.launch {
            var updated = transaction.copy(status = status, isManuallyEdited = true)
            if (status == TransactionStatus.LENT) {
                updated = updated.copy(
                    category = Category.LENT,
                    type = TransactionType.LENT,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else if (status == TransactionStatus.BORROWED) {
                updated = updated.copy(
                    category = Category.BORROWED,
                    type = TransactionType.BORROWED,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else if (status == TransactionStatus.SELF_TRANSFER) {
                updated = updated.copy(
                    category = Category.SELF_TRANSFER,
                    type = TransactionType.SELF_TRANSFER,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            }
            transactionDao.update(updated)
            checkAndSyncLendBorrow(updated)
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun updateTransactionCategory(transaction: Transaction, category: Category) {
        viewModelScope.launch {
            val isExcludedBudget = !category.isBudgetTrackingEnabled
            val isExcludedAnalytics = !category.includeInAnalytics
            var updated = transaction.copy(
                category = category, 
                isManuallyEdited = true,
                isExcludedFromBudget = isExcludedBudget || transaction.isExcludedFromBudget,
                isExcludedFromAnalytics = isExcludedAnalytics || transaction.isExcludedFromAnalytics
            )
            if (category.name == "LENT") {
                updated = updated.copy(
                    status = TransactionStatus.LENT,
                    type = TransactionType.LENT,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else if (category.name == "BORROWED") {
                updated = updated.copy(
                    status = TransactionStatus.BORROWED,
                    type = TransactionType.BORROWED,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else if (category.name == "SELF_TRANSFER") {
                updated = updated.copy(
                    status = TransactionStatus.SELF_TRANSFER,
                    type = TransactionType.SELF_TRANSFER,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else {
                updated = updated.copy(status = TransactionStatus.APPROVED)
            }
            transactionDao.update(updated)
            if (category.name == "LENT" || category.name == "BORROWED") {
                checkAndSyncLendBorrow(updated)
            }
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    private fun checkAndSyncLendBorrow(transaction: Transaction) {
        viewModelScope.launch {
            val phone = transaction.contactPhone
            val name = transaction.contactName
            if (phone.isNullOrBlank() || name.isNullOrBlank()) return@launch

            if (transaction.status == TransactionStatus.LENT || transaction.status == TransactionStatus.BORROWED) {
                val existing = lendBorrowRepository.findMatchingRecord(phone, transaction.amount, transaction.timestamp)
                if (existing == null) {
                    val record = LendBorrowRecord(
                        contactName = name,
                        contactPhone = phone,
                        type = if (transaction.status == TransactionStatus.LENT) LendBorrowType.LENT else LendBorrowType.BORROWED,
                        amount = transaction.amount,
                        givenDate = transaction.timestamp,
                        dueDate = null,
                        totalPaid = 0.0,
                        outstanding = transaction.amount,
                        status = LendBorrowStatus.ACTIVE
                    )
                    lendBorrowRepository.createRecord(record)
                }
            }
        }
    }

    fun createCategoryAndAssign(
        transaction: Transaction,
        displayName: String,
        emoji: String,
        color: String,
        parentCategoryName: String?,
        isBudgetTrackingEnabled: Boolean,
        includeInAnalytics: Boolean,
        bulkOption: String
    ) {
        viewModelScope.launch {
            val newCategory = Category(
                name = displayName.uppercase().replace(" ", "_"),
                displayName = displayName,
                emoji = emoji,
                color = color,
                isCustom = true,
                parentCategoryName = parentCategoryName,
                isBudgetTrackingEnabled = isBudgetTrackingEnabled,
                includeInAnalytics = includeInAnalytics
            )
            categoryDao.insert(newCategory)

            val startTime = when (bulkOption) {
                "DAYS_30" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                "MONTHS_6" -> System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000
                "ALL_TIME" -> 0L
                else -> -1L // Only this one
            }

            if (startTime == -1L) {
                transactionDao.update(transaction.copy(category = newCategory, status = TransactionStatus.APPROVED, isManuallyEdited = true))
            } else {
                transactionDao.bulkUpdateCategory(transaction.merchantNormalized, newCategory.name, startTime)
            }
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun createCategory(
        displayName: String,
        emoji: String,
        color: String,
        parentCategoryName: String?,
        isBudgetTrackingEnabled: Boolean,
        includeInAnalytics: Boolean,
        onCreated: (Category) -> Unit
    ) {
        viewModelScope.launch {
            val newCategory = Category(
                name = displayName.uppercase().replace(" ", "_"),
                displayName = displayName,
                emoji = emoji,
                color = color,
                isCustom = true,
                parentCategoryName = parentCategoryName,
                isBudgetTrackingEnabled = isBudgetTrackingEnabled,
                includeInAnalytics = includeInAnalytics
            )
            categoryDao.insert(newCategory)
            onCreated(newCategory)
        }
    }

    fun updateTransactionMerchant(transaction: Transaction, merchant: String) {
        viewModelScope.launch {
            transactionDao.update(transaction.copy(
                merchant = merchant, 
                merchantNormalized = com.spendless.app.core.sms.SmsParser.normalizeMerchant(merchant),
                isManuallyEdited = true
            ))
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun updateTransactionType(transaction: Transaction, type: TransactionType) {
        viewModelScope.launch {
            var updated = transaction.copy(type = type, isManuallyEdited = true)
            if (type == TransactionType.LENT) {
                updated = updated.copy(
                    category = Category.LENT,
                    status = TransactionStatus.LENT,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else if (type == TransactionType.BORROWED) {
                updated = updated.copy(
                    category = Category.BORROWED,
                    status = TransactionStatus.BORROWED,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            } else if (type == TransactionType.SELF_TRANSFER) {
                updated = updated.copy(
                    category = Category.SELF_TRANSFER,
                    status = TransactionStatus.SELF_TRANSFER,
                    isExcludedFromBudget = true,
                    isExcludedFromAnalytics = true
                )
            }
            transactionDao.update(updated)
            if (type == TransactionType.LENT || type == TransactionType.BORROWED) {
                checkAndSyncLendBorrow(updated)
            }
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun toggleSelection(id: Long) {
        _selectionMode.update { 
            if (it.contains(id)) it - id else it + id
        }
    }

    fun clearSelection() {
        _selectionMode.value = emptySet()
    }

    fun bulkApprove() {
        viewModelScope.launch {
            val ids = _selectionMode.value.toList()
            transactionDao.bulkUpdateStatus(ids, TransactionStatus.APPROVED.name)
            refreshAllCycleTotals()
            clearSelection()
        }
    }

    fun bulkDelete() {
        viewModelScope.launch {
            val ids = _selectionMode.value.toList()
            val txns = transactionDao.getTransactionsByIds(ids)
            transactionDao.bulkDelete(txns)
            refreshAllCycleTotals()
            clearSelection()
        }
    }

    fun bulkUpdateCategory(category: Category) {
        viewModelScope.launch {
            val ids = _selectionMode.value.toList()
            val isExcludedBudget = !category.isBudgetTrackingEnabled
            val isExcludedAnalytics = !category.includeInAnalytics
            transactionDao.bulkUpdateCategoryByIds(ids, category.name, isExcludedBudget, isExcludedAnalytics)
            refreshAllCycleTotals()
            clearSelection()
        }
    }

    fun bulkIgnore() {
        viewModelScope.launch {
            val ids = _selectionMode.value.toList()
            transactionDao.bulkUpdateStatus(ids, TransactionStatus.IGNORED.name)
            refreshAllCycleTotals()
            clearSelection()
        }
    }

    fun bulkSelfTransfer() {
        viewModelScope.launch {
            val ids = _selectionMode.value.toList()
            transactionDao.bulkUpdateStatus(ids, TransactionStatus.SELF_TRANSFER.name)
            refreshAllCycleTotals()
            clearSelection()
        }
    }


    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            var updated = transaction
            if (updated.status == TransactionStatus.LENT && updated.category.name != "LENT") {
                updated = updated.copy(category = Category.LENT, type = TransactionType.LENT, isExcludedFromBudget = true, isExcludedFromAnalytics = true)
            } else if (updated.status == TransactionStatus.BORROWED && updated.category.name != "BORROWED") {
                updated = updated.copy(category = Category.BORROWED, type = TransactionType.BORROWED, isExcludedFromBudget = true, isExcludedFromAnalytics = true)
            } else if (updated.status == TransactionStatus.SELF_TRANSFER && updated.category.name != "SELF_TRANSFER") {
                updated = updated.copy(category = Category.SELF_TRANSFER, type = TransactionType.SELF_TRANSFER, isExcludedFromBudget = true, isExcludedFromAnalytics = true)
            } else if (updated.category.name == "LENT" && (updated.type != TransactionType.LENT || updated.status != TransactionStatus.LENT)) {
                updated = updated.copy(type = TransactionType.LENT, status = TransactionStatus.LENT, isExcludedFromBudget = true, isExcludedFromAnalytics = true)
            } else if (updated.category.name == "BORROWED" && (updated.type != TransactionType.BORROWED || updated.status != TransactionStatus.BORROWED)) {
                updated = updated.copy(type = TransactionType.BORROWED, status = TransactionStatus.BORROWED, isExcludedFromBudget = true, isExcludedFromAnalytics = true)
            } else if (updated.category.name == "SELF_TRANSFER" && (updated.type != TransactionType.SELF_TRANSFER || updated.status != TransactionStatus.SELF_TRANSFER)) {
                updated = updated.copy(type = TransactionType.SELF_TRANSFER, status = TransactionStatus.SELF_TRANSFER, isExcludedFromBudget = true, isExcludedFromAnalytics = true)
            }
            transactionDao.update(updated.copy(isManuallyEdited = true))
            checkAndSyncLendBorrow(updated)
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun splitTransaction(transaction: Transaction, splits: List<TransactionSplit>) {
        viewModelScope.launch {
            transactionDao.update(transaction.copy(splits = splits, isManuallyEdited = true))
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.delete(transaction)
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: Category,
        merchant: String,
        timestamp: Long,
        paymentMethod: PaymentMethod,
        userNote: String = "",
        isExcludedFromBudget: Boolean = false,
        isExcludedFromAnalytics: Boolean = false,
        tags: List<String> = emptyList(),
        attachments: List<String> = emptyList(),
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        contactName: String? = null,
        contactPhone: String? = null
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                type = type,
                merchant = merchant,
                merchantNormalized = merchant.lowercase().trim(),
                category = category,
                timestamp = timestamp,
                paymentMethod = paymentMethod,
                userNote = userNote,
                isManualEntry = true,
                status = TransactionStatus.APPROVED,
                isExcludedFromBudget = isExcludedFromBudget,
                isExcludedFromAnalytics = isExcludedFromAnalytics,
                tags = tags,
                attachments = attachments,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                contactName = contactName,
                contactPhone = contactPhone
            )
            
            val id = transactionDao.insert(transaction)
            resolveAndAssignCycle(id, timestamp)
            refreshAllCycleTotals()
            WidgetUpdateUtil.updateAllWidgets(context)
        }
    }

    private suspend fun resolveAndAssignCycle(transactionId: Long, timestamp: Long) {
        val budget = budgetDao.getActiveBudgetSync() ?: return
        val cycleRange = budgetEngine.getCycleForTimestamp(budget.resetDay, timestamp)
        
        var cycle = budgetDao.getCycleForTimestamp(budget.id, timestamp)
        if (cycle == null) {
            cycle = BudgetCycle(
                budgetId = budget.id,
                startDate = cycleRange.startMs,
                endDate = cycleRange.endMs,
                isActive = System.currentTimeMillis() in cycleRange.startMs..cycleRange.endMs
            )
            val cycleId = budgetDao.insertCycle(cycle)
            cycle = cycle.copy(id = cycleId)
        }
        
        transactionDao.getById(transactionId)?.let { txn ->
            transactionDao.update(txn.copy(cycleId = cycle.id))
        }
    }

    private suspend fun refreshAllCycleTotals() {
        val budget = budgetDao.getActiveBudgetSync() ?: return
        val cycles = budgetDao.getCyclesForBudgetSync(budget.id)
        
        cycles.forEach { cycle ->
            val txns = transactionDao.getTransactionsForCycleSync(cycle.id)
            val spent = transactionDao.getTotalSpentInCycleSyncFlow(txns)
            val income = transactionDao.getTotalIncomeInCycleSyncFlow(txns)
            budgetDao.updateCycleTotals(cycle.id, spent, income)
        }
    }

    suspend fun getMerchantTransactionCount(merchantNormalized: String): Int {
        return transactionDao.getMerchantTransactionCount(merchantNormalized)
    }
}
