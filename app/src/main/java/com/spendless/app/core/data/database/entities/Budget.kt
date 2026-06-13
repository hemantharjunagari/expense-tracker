package com.spendless.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User's budget configuration.
 * Stores the main budget amount and per-category budgets.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Total monthly budget in rupees */
    val totalBudget: Double,

    /**
     * The day of month on which the budget cycle resets.
     * Valid range: 1..31
     * For months shorter than resetDay, last day of month is used.
     */
    val resetDay: Int,

    /**
     * JSON-serialized Map<String, Double> of category budgets.
     * Key = Category.name, Value = budget amount
     * TypeConverter handles serialization.
     */
    val categoryBudgetsJson: String = "{}",

    /** Whether this is the active budget */
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
