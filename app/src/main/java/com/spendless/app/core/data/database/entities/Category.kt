package com.spendless.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String,
    val displayName: String,
    val emoji: String,
    val isSystem: Boolean = false,
    val isCustom: Boolean = false,
    val color: String = "#FFFFFF",
    val parentCategoryName: String? = null,
    val isBudgetTrackingEnabled: Boolean = true,
    val includeInAnalytics: Boolean = true,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
) {
    companion object {
        val FOOD_DINING = Category("FOOD_DINING", "Food & Dining", "🍽️", isSystem = true, color = "#FF9800")
        val GROCERIES = Category("GROCERIES", "Groceries", "🛒", isSystem = true, color = "#4CAF50")
        val TRANSPORTATION = Category("TRANSPORTATION", "Transportation", "🚗", isSystem = true, color = "#03A9F4")
        val FUEL = Category("FUEL", "Fuel", "⛽", isSystem = true, color = "#FF5722")
        val SHOPPING = Category("SHOPPING", "Shopping", "🛍️", isSystem = true, color = "#E91E63")
        val ENTERTAINMENT = Category("ENTERTAINMENT", "Entertainment", "🎬", isSystem = true, color = "#9C27B0")
        val SUBSCRIPTIONS = Category("SUBSCRIPTIONS", "Subscriptions", "📱", isSystem = true, color = "#673AB7")
        val UTILITIES = Category("UTILITIES", "Utilities", "💡", isSystem = true, color = "#FFEB3B")
        val RENT = Category("RENT", "Rent", "🏠", isSystem = true, color = "#795548")
        val HEALTHCARE = Category("HEALTHCARE", "Healthcare", "🏥", isSystem = true, color = "#F44336")
        val EDUCATION = Category("EDUCATION", "Education", "📚", isSystem = true, color = "#00BCD4")
        val TRAVEL = Category("TRAVEL", "Travel", "✈️", isSystem = true, color = "#009688")
        val INVESTMENTS = Category("INVESTMENTS", "Investments", "📈", isSystem = true, color = "#4E342E", isBudgetTrackingEnabled = false)
        val EMI_LOANS = Category("EMI_LOANS", "EMI / Loans", "💳", isSystem = true, color = "#3F51B5")
        val INCOME = Category("INCOME", "Income", "💰", isSystem = true, color = "#81C784", isBudgetTrackingEnabled = false, includeInAnalytics = false)
        val TRANSFER = Category("TRANSFER", "Transfer", "↔️", isSystem = true, color = "#90A4AE", isBudgetTrackingEnabled = false, includeInAnalytics = false)
        val MISCELLANEOUS = Category("MISCELLANEOUS", "Miscellaneous", "📦", isSystem = true, color = "#9E9E9E", isBudgetTrackingEnabled = false)
        
        // New system categories
        val UNCATEGORIZED = Category("UNCATEGORIZED", "Uncategorized", "❓", isSystem = true, color = "#B0BEC5", isBudgetTrackingEnabled = false, includeInAnalytics = false)
        val SELF_TRANSFER = Category("SELF_TRANSFER", "Self Transfer", "🔄", isSystem = true, color = "#78909C", isBudgetTrackingEnabled = false, includeInAnalytics = false)
        val LENT = Category("LENT", "Lent", "🤝", isSystem = true, color = "#03A9F4", isBudgetTrackingEnabled = false, includeInAnalytics = false)
        val BORROWED = Category("BORROWED", "Borrowed", "🤝", isSystem = true, color = "#FF5722", isBudgetTrackingEnabled = false, includeInAnalytics = false)

        val systemList = listOf(
            FOOD_DINING, GROCERIES, TRANSPORTATION, FUEL, SHOPPING, ENTERTAINMENT,
            SUBSCRIPTIONS, UTILITIES, RENT, HEALTHCARE, EDUCATION, TRAVEL,
            INVESTMENTS, EMI_LOANS, INCOME, TRANSFER, MISCELLANEOUS, UNCATEGORIZED, SELF_TRANSFER, LENT, BORROWED
        )

        // Memory cache to resolve database custom categories inside TypeConverters
        val customCache = ConcurrentHashMap<String, Category>()

        fun cleanCustomName(name: String): String {
            if (!name.startsWith("CUSTOM_")) {
                return name.replace('_', ' ')
                    .split(' ')
                    .filter { it.isNotEmpty() }
                    .joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                        }
                    }
            }
            val withoutPrefix = name.substringAfter("CUSTOM_")
            val lastUnderscore = withoutPrefix.lastIndexOf('_')
            val base = if (lastUnderscore != -1 && withoutPrefix.substring(lastUnderscore + 1).all { it.isDigit() }) {
                withoutPrefix.substring(0, lastUnderscore)
            } else {
                withoutPrefix
            }
            return base.replace('_', ' ')
                .split(' ')
                .filter { it.isNotEmpty() }
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                }
        }

        fun fromName(name: String): Category {
            return customCache[name]
                ?: systemList.firstOrNull { it.name == name }
                ?: Category(name, cleanCustomName(name), "📦", isCustom = true)
        }
    }
}
