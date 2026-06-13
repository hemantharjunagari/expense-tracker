package com.spendless.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single budget period/cycle.
 * E.g., if reset day is 21: cycle = May 21 → June 20
 *
 * Precomputed totals are cached here to avoid expensive re-queries.
 */
@Entity(
    tableName = "budget_cycles",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("budgetId"), Index("startDate"), Index("endDate")]
)
data class BudgetCycle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val budgetId: Long,

    /** Cycle start timestamp (inclusive) in millis */
    val startDate: Long,

    /** Cycle end timestamp (inclusive, end of day) in millis */
    val endDate: Long,

    /** Cached total debit amount for this cycle */
    val totalSpent: Double = 0.0,

    /** Cached total credit amount for this cycle */
    val totalIncome: Double = 0.0,

    /** Whether this is the current active cycle */
    val isActive: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)
