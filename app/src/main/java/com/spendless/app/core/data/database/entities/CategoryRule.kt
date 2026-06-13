package com.spendless.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User-defined or system-defined categorization rules.
 * The engine checks these to assign categories to transactions.
 */
@Entity(
    tableName = "category_rules",
    indices = [Index("keyword"), Index("isUserDefined"), Index("priority")]
)
data class CategoryRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Keyword/pattern to match against merchant name or SMS body */
    val keyword: String,

    /** Merchant name pattern (regex-compatible) */
    val merchantPattern: String = "",

    /** Category to assign when rule matches */
    val category: String, // Category.name

    /**
     * Priority: higher number = checked first.
     * User-defined rules get priority 1000+
     * System rules get priority 1–999
     */
    val priority: Int = 100,

    /** Whether this rule was created by the user */
    val isUserDefined: Boolean = false,

    /** Whether this rule is active */
    val isEnabled: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)
