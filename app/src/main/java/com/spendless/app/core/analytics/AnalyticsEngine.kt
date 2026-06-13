package com.spendless.app.core.analytics

import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.Transaction
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.database.entities.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsEngine @Inject constructor(
    private val transactionDao: TransactionDao
) {
    /**
     * Get daily spending aggregated for a date range.
     * Returns list of (date epoch day, total amount).
     */
    suspend fun getDailySpending(startMs: Long, endMs: Long): List<DailySpending> {
        val transactions = transactionDao.getTransactionsByDateRangeList(startMs, endMs)
        return transactions
            .filter { it.type == TransactionType.DEBIT && it.status == TransactionStatus.APPROVED && it.category.includeInAnalytics && it.category.name != "UNCATEGORIZED" && it.category.name != "SELF_TRANSFER" }
            .groupBy { toDateKey(it.timestamp) }
            .map { (day, txns) -> DailySpending(day, txns.sumOf { it.amount }) }
            .sortedBy { it.dateMs }
    }

    /**
     * Get income vs expense by day for a range.
     */
    suspend fun getDailyIncomeVsExpense(startMs: Long, endMs: Long): List<DailyIncomeExpense> {
        val transactions = transactionDao.getTransactionsByDateRangeList(startMs, endMs)
        val grouped = transactions.groupBy { toDateKey(it.timestamp) }

        val startCal = Calendar.getInstance().apply {
            timeInMillis = startMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = mutableListOf<DailyIncomeExpense>()
        val currentCal = startCal.clone() as Calendar
        while (!currentCal.after(endCal)) {
            val dayMs = currentCal.timeInMillis
            val txns = grouped[dayMs]
            if (txns != null) {
                result.add(
                    DailyIncomeExpense(
                        dateMs = dayMs,
                        income = txns.filter { it.type == TransactionType.CREDIT && it.status == TransactionStatus.APPROVED && it.category.name != "UNCATEGORIZED" && it.category.name != "SELF_TRANSFER" }.sumOf { it.amount },
                        expense = txns.filter { it.type == TransactionType.DEBIT && it.status == TransactionStatus.APPROVED && it.category.includeInAnalytics && it.category.name != "UNCATEGORIZED" && it.category.name != "SELF_TRANSFER" }.sumOf { it.amount }
                    )
                )
            } else {
                result.add(
                    DailyIncomeExpense(
                        dateMs = dayMs,
                        income = 0.0,
                        expense = 0.0
                    )
                )
            }
            currentCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    /**
     * Get category breakdown for a cycle.
     */
    suspend fun getCategoryBreakdown(cycleId: Long): Map<Category, Double> {
        return transactionDao.getCategoryBreakdown(cycleId)
            .associate { it.category to it.total }
    }

    /**
     * Get spending flow for the analytics screen (reactive).
     */
    fun getSpendingFlow(startMs: Long, endMs: Long): Flow<AnalyticsSummary> =
        transactionDao.getTransactionsForAnalytics(startMs, endMs).map { transactions ->
            val debits = transactions.filter { 
                it.type == TransactionType.DEBIT && it.status == TransactionStatus.APPROVED && it.category.includeInAnalytics && 
                it.category.name != "UNCATEGORIZED" && it.category.name != "SELF_TRANSFER"
            }
            val credits = transactions.filter { 
                it.type == TransactionType.CREDIT && it.status == TransactionStatus.APPROVED &&
                it.category.name != "UNCATEGORIZED" && it.category.name != "SELF_TRANSFER"
            }

            val categoryBreakdown = debits
                .groupBy { it.category }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }

            val dailySpending = debits
                .groupBy { toDateKey(it.timestamp) }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }

            val highestDay = dailySpending.maxByOrNull { it.value }
            val highestCategory = categoryBreakdown.maxByOrNull { it.value }

            AnalyticsSummary(
                totalExpense = debits.sumOf { it.amount },
                totalIncome = credits.sumOf { it.amount },
                transactionCount = transactions.size,
                categoryBreakdown = categoryBreakdown,
                dailySpending = dailySpending,
                highestSpendingDay = highestDay?.key,
                highestSpendingDayAmount = highestDay?.value ?: 0.0,
                highestCategory = highestCategory?.key,
                avgDailyExpense = if (dailySpending.isNotEmpty())
                    dailySpending.values.average() else 0.0
            )
        }

    /**
     * Get spending for a specific period type.
     */
    fun getPeriodRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endMs = cal.timeInMillis

        val startMs = when (period) {
            AnalyticsPeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            AnalyticsPeriod.WEEKLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            AnalyticsPeriod.MONTHLY -> {
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            AnalyticsPeriod.YEARLY -> {
                cal.add(Calendar.YEAR, -1)
                cal.timeInMillis
            }
        }

        return startMs to endMs
    }

    private fun toDateKey(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // ── Data Models ────────────────────────────────────────────────────────────

    data class DailySpending(val dateMs: Long, val amount: Double)

    data class DailyIncomeExpense(
        val dateMs: Long,
        val income: Double,
        val expense: Double
    )

    data class AnalyticsSummary(
        val totalExpense: Double,
        val totalIncome: Double,
        val transactionCount: Int,
        val categoryBreakdown: Map<Category, Double>,
        val dailySpending: Map<Long, Double>,
        val highestSpendingDay: Long?,
        val highestSpendingDayAmount: Double,
        val highestCategory: Category?,
        val avgDailyExpense: Double
    )

    enum class AnalyticsPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }
}
