package com.spendless.app.core.analytics

import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.entities.Budget
import com.spendless.app.core.data.database.entities.BudgetCycle
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Budget engine responsible for custom budget cycle management.
 *
 * Key responsibility: Given a reset day (1-31), compute the correct
 * cycle start/end dates for any given point in time.
 *
 * Example:
 *   resetDay = 21
 *   currentDate = June 15 → cycle = May 21 → June 20
 *   currentDate = June 25 → cycle = June 21 → July 20
 */
@Singleton
class BudgetEngine @Inject constructor(
    private val budgetDao: BudgetDao
) {
    /**
     * Get the budget cycle that contains the given timestamp.
     * Creates cycles as needed.
     */
    fun getCycleForTimestamp(resetDay: Int, timestamp: Long): CycleRange {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // Determine if we're before or after the reset day this month
        // For short months (like February or 30-day months), cap the resetDay
        val maxDayThisMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cappedResetDay = minOf(resetDay, maxDayThisMonth)

        return if (currentDay >= cappedResetDay) {
            // Cycle started this month on resetDay
            val start = cycleStartMillis(currentYear, currentMonth, resetDay)
            val end = cycleEndMillis(currentYear, currentMonth + 1, resetDay)
            CycleRange(start, end)
        } else {
            // Cycle started last month on resetDay
            val start = cycleStartMillis(currentYear, currentMonth - 1, resetDay)
            val end = cycleEndMillis(currentYear, currentMonth, resetDay)
            CycleRange(start, end)
        }
    }

    /**
     * Get the current active cycle range.
     */
    fun getCurrentCycle(resetDay: Int): CycleRange =
        getCycleForTimestamp(resetDay, System.currentTimeMillis())

    /**
     * Get the previous N cycles in descending order.
     */
    fun getPreviousCycles(resetDay: Int, count: Int): List<CycleRange> {
        val cycles = mutableListOf<CycleRange>()
        val current = getCurrentCycle(resetDay)
        var cycleStart = current.startMs

        repeat(count) {
            val prevEnd = cycleStart - 1
            val prevStart = getPreviousCycleStart(resetDay, cycleStart)
            cycles.add(CycleRange(prevStart, prevEnd))
            cycleStart = prevStart
        }
        return cycles
    }

    private fun getPreviousCycleStart(resetDay: Int, currentStart: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentStart }
        cal.add(Calendar.MONTH, -1)
        return cycleStartMillis(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), resetDay)
    }

    /**
     * Start of cycle = resetDay at 00:00:00.000 of given month
     * Handles month-end edge cases (e.g., resetDay=31 in Feb → Feb 28)
     */
    private fun cycleStartMillis(year: Int, month: Int, resetDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1) // Set to 1st first to avoid overflow
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(resetDay, maxDay))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * End of cycle = day before resetDay in next month, at 23:59:59.999
     */
    private fun cycleEndMillis(year: Int, month: Int, resetDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(resetDay, maxDay) - 1)
            if (get(Calendar.DAY_OF_MONTH) == 0) {
                // Rolled back to previous month
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Calculate budget utilization percentage.
     */
    fun getUtilizationPercent(spent: Double, budget: Double): Float {
        if (budget <= 0) return 0f
        return ((spent / budget) * 100).coerceIn(0.0, 100.0).toFloat()
    }

    /**
     * Calculate remaining budget.
     */
    fun getRemaining(spent: Double, budget: Double): Double =
        (budget - spent).coerceAtLeast(0.0)

    /**
     * Calculate savings percentage (how much of income was saved).
     */
    fun getSavingsPercent(income: Double, spent: Double): Float {
        if (income <= 0) return 0f
        val saved = (income - spent).coerceAtLeast(0.0)
        return ((saved / income) * 100).coerceIn(0.0, 100.0).toFloat()
    }

    /**
     * Check which notification threshold has been crossed.
     * Returns the threshold (50, 75, 90, 100) or null if none.
     */
    fun checkBudgetThreshold(spent: Double, budget: Double, alreadyNotified: Set<Int>): Int? {
        val percent = getUtilizationPercent(spent, budget).toInt()
        return when {
            percent >= 100 && 100 !in alreadyNotified -> 100
            percent >= 90 && 90 !in alreadyNotified -> 90
            percent >= 75 && 75 !in alreadyNotified -> 75
            percent >= 50 && 50 !in alreadyNotified -> 50
            else -> null
        }
    }

    data class CycleRange(
        val startMs: Long,
        val endMs: Long
    ) {
        val durationDays: Int
            get() = ((endMs - startMs) / (1000 * 60 * 60 * 24)).toInt() + 1
    }
}
