package com.spendless.app.analytics

import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Unit tests for the custom budget cycle engine.
 * Tests edge cases in reset day calculation including month-end scenarios.
 */
class BudgetEngineTest {

    private lateinit var budgetEngine: BudgetEngine

    @Before
    fun setup() {
        val budgetDao = mockk<BudgetDao>(relaxed = true)
        budgetEngine = BudgetEngine(budgetDao)
    }

    // ── Cycle Calculation ──────────────────────────────────────────────────────

    @Test
    fun `cycle starts on reset day when current day is at or after reset day`() {
        // June 25 with resetDay = 21 → cycle should be June 21 → July 20
        val june25 = dateToMs(2024, Calendar.JUNE, 25)
        val cycle = budgetEngine.getCycleForTimestamp(resetDay = 21, timestamp = june25)

        val startCal = Calendar.getInstance().apply { timeInMillis = cycle.startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = cycle.endMs }

        assertEquals("Cycle should start on June 21", 21, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Cycle should start in June", Calendar.JUNE, startCal.get(Calendar.MONTH))
        assertEquals("Cycle should end on July 20", 20, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Cycle should end in July", Calendar.JULY, endCal.get(Calendar.MONTH))
    }

    @Test
    fun `cycle is from previous month when current day is before reset day`() {
        // June 15 with resetDay = 21 → cycle should be May 21 → June 20
        val june15 = dateToMs(2024, Calendar.JUNE, 15)
        val cycle = budgetEngine.getCycleForTimestamp(resetDay = 21, timestamp = june15)

        val startCal = Calendar.getInstance().apply { timeInMillis = cycle.startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = cycle.endMs }

        assertEquals("Cycle should start on May 21", 21, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Cycle should start in May", Calendar.MAY, startCal.get(Calendar.MONTH))
        assertEquals("Cycle should end on June 20", 20, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Cycle should end in June", Calendar.JUNE, endCal.get(Calendar.MONTH))
    }

    @Test
    fun `reset day 1 produces calendar month cycle`() {
        val june15 = dateToMs(2024, Calendar.JUNE, 15)
        val cycle = budgetEngine.getCycleForTimestamp(resetDay = 1, timestamp = june15)

        val startCal = Calendar.getInstance().apply { timeInMillis = cycle.startMs }
        assertEquals("Should start on June 1", 1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Should start in June", Calendar.JUNE, startCal.get(Calendar.MONTH))
    }

    @Test
    fun `reset day 31 in February uses last day of month`() {
        // February 15 with resetDay = 31 → start should be Jan 31 → Feb 27/28
        val feb15 = dateToMs(2024, Calendar.FEBRUARY, 15)
        val cycle = budgetEngine.getCycleForTimestamp(resetDay = 31, timestamp = feb15)

        val startCal = Calendar.getInstance().apply { timeInMillis = cycle.startMs }
        // Jan 31 should be valid
        assertEquals("Should start on Jan 31", 31, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Should start in January", Calendar.JANUARY, startCal.get(Calendar.MONTH))
    }

    @Test
    fun `cycle duration is approximately 30 days`() {
        val timestamp = System.currentTimeMillis()
        val cycle = budgetEngine.getCycleForTimestamp(resetDay = 15, timestamp = timestamp)
        assertTrue("Cycle should be at least 27 days", cycle.durationDays >= 27)
        assertTrue("Cycle should be at most 32 days", cycle.durationDays <= 32)
    }

    // ── Utilization ─────────────────────────────────────────────────────────────

    @Test
    fun `utilization percent is 50 when half budget spent`() {
        val percent = budgetEngine.getUtilizationPercent(spent = 5000.0, budget = 10000.0)
        assertEquals(50f, percent, 0.01f)
    }

    @Test
    fun `utilization percent is capped at 100`() {
        val percent = budgetEngine.getUtilizationPercent(spent = 15000.0, budget = 10000.0)
        assertEquals(100f, percent, 0.01f)
    }

    @Test
    fun `utilization percent is 0 when nothing spent`() {
        val percent = budgetEngine.getUtilizationPercent(spent = 0.0, budget = 10000.0)
        assertEquals(0f, percent, 0.01f)
    }

    @Test
    fun `remaining is 0 when over budget`() {
        val remaining = budgetEngine.getRemaining(spent = 12000.0, budget = 10000.0)
        assertEquals(0.0, remaining, 0.01)
    }

    // ── Threshold Detection ────────────────────────────────────────────────────

    @Test
    fun `detect 50 percent threshold`() {
        val threshold = budgetEngine.checkBudgetThreshold(
            spent = 5100.0, budget = 10000.0, alreadyNotified = emptySet()
        )
        assertEquals(50, threshold)
    }

    @Test
    fun `detect 75 percent threshold when 50 already notified`() {
        val threshold = budgetEngine.checkBudgetThreshold(
            spent = 7600.0, budget = 10000.0, alreadyNotified = setOf(50)
        )
        assertEquals(75, threshold)
    }

    @Test
    fun `return null when all thresholds already notified`() {
        val threshold = budgetEngine.checkBudgetThreshold(
            spent = 10000.0, budget = 10000.0, alreadyNotified = setOf(50, 75, 90, 100)
        )
        assertNull(threshold)
    }

    @Test
    fun `detect 100 percent when budget exceeded`() {
        val threshold = budgetEngine.checkBudgetThreshold(
            spent = 10001.0, budget = 10000.0, alreadyNotified = setOf(50, 75, 90)
        )
        assertEquals(100, threshold)
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private fun dateToMs(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
    }
}
