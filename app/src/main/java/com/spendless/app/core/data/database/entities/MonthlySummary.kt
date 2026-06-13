package com.spendless.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Precomputed monthly analytics summary per budget cycle.
 * Cached to enable instant dashboard loading without re-aggregating transactions.
 */
@Entity(
    tableName = "monthly_summaries",
    foreignKeys = [
        ForeignKey(
            entity = BudgetCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cycleId", unique = true)]
)
data class MonthlySummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cycleId: Long,

    /** JSON map of Category.name → amount spent */
    val categoryBreakdownJson: String = "{}",

    /** JSON list of DailySpending{date, amount} */
    val dailySpendingJson: String = "[]",

    /** Timestamp of highest spending day */
    val highestSpendingDay: Long = 0L,

    /** Amount spent on highest day */
    val highestDayAmount: Double = 0.0,

    /** Category with most spending (Category.name) */
    val highestCategory: String = "",

    /** Total transactions count */
    val transactionCount: Int = 0,

    /** Average daily spending */
    val avgDailySpending: Double = 0.0,

    /** Last updated timestamp */
    val updatedAt: Long = System.currentTimeMillis()
)
