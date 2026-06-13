package com.spendless.app.core.categorization

import com.spendless.app.core.data.database.dao.CategoryDao
import com.spendless.app.core.data.database.dao.CategoryRuleDao
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.CategoryRule
import com.spendless.app.core.data.database.entities.TransactionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based categorization engine.
 *
 * Priority order:
 * 1. User-defined rules (priority 1000+)
 * 2. Exact merchant name database
 * 3. System keyword rules
 * 4. Fallback: MISCELLANEOUS (DEBIT) or INCOME (CREDIT)
 *
 * Rules are cached in memory and refreshed when the DB changes.
 */
@Singleton
class CategorizationEngine @Inject constructor(
    private val categoryRuleDao: CategoryRuleDao
) {
    private var cachedRules: List<CategoryRule>? = null
    private val cacheMutex = Mutex()
    private var cacheVersion = 0L

    /**
     * Seed the database with system rules if it's empty.
     */
    suspend fun seedSystemRulesIfNeeded() {
        val existingCount = categoryRuleDao.getSystemRuleCount()
        if (existingCount > 0) return

        val systemRules = buildSystemRules()
        categoryRuleDao.insertAll(systemRules)
        invalidateCache()
    }

    /**
     * Seed the database with system categories and initialize cache.
     */
    suspend fun seedSystemCategoriesIfNeeded(categoryDao: CategoryDao) {
        categoryDao.insertAllIgnore(Category.systemList)
        val allCats = categoryDao.getAllCategories()
        Category.customCache.clear()
        Category.customCache.putAll(allCats.associateBy { it.name })
    }

    /**
     * Categorize a transaction based on merchant name and SMS body.
     *
     * @param merchantNormalized Normalized merchant name (lowercase, cleaned)
     * @param rawSmsBody Original SMS for additional context
     * @param transactionType DEBIT or CREDIT
     * @return Best matching Category
     */
    suspend fun categorize(
        merchantNormalized: String,
        rawSmsBody: String,
        transactionType: TransactionType
    ): Category {
        // Credit transactions default to INCOME unless we find a specific match
        val defaultCategory = if (transactionType == TransactionType.CREDIT)
            Category.INCOME else Category.UNCATEGORIZED

        // 1. Check user-defined DB rules first (highest priority)
        val rules = getOrLoadRules()
        val userRules = rules.filter { it.isUserDefined }
        for (rule in userRules) {
            if (matchesRule(rule, merchantNormalized, rawSmsBody)) {
                return Category.fromName(rule.category)
            }
        }

        // 2. Exact merchant match from built-in database
        val exactMatch = MerchantDatabase.EXACT_MERCHANT_MAP[merchantNormalized]
        if (exactMatch != null) return exactMatch

        // 3. Partial merchant name match
        for ((keyword, category) in MerchantDatabase.EXACT_MERCHANT_MAP) {
            if (merchantNormalized.contains(keyword) || keyword.contains(merchantNormalized)) {
                return category
            }
        }

        // 4. Keyword rules from DB
        val systemRules = rules.filter { !it.isUserDefined }
        for (rule in systemRules) {
            if (matchesRule(rule, merchantNormalized, rawSmsBody)) {
                return Category.fromName(rule.category)
            }
        }

        // 5. Keyword match from built-in list
        val lowerMerchant = merchantNormalized.lowercase()
        val lowerSms = rawSmsBody.lowercase()
        for ((keyword, category) in MerchantDatabase.KEYWORD_CATEGORY_MAP) {
            if (lowerMerchant.contains(keyword) || lowerSms.contains(keyword)) {
                return category
            }
        }

        return defaultCategory
    }

    private fun matchesRule(rule: CategoryRule, merchantNormalized: String, rawSms: String): Boolean {
        val lowerMerchant = merchantNormalized.lowercase()
        val lowerSms = rawSms.lowercase()
        val lowerKeyword = rule.keyword.lowercase()

        return lowerMerchant.contains(lowerKeyword) || lowerSms.contains(lowerKeyword) ||
            (rule.merchantPattern.isNotEmpty() && runCatching {
                Regex(rule.merchantPattern, RegexOption.IGNORE_CASE).containsMatchIn(merchantNormalized)
            }.getOrDefault(false))
    }

    /**
     * Learn from user correction: create a user-defined rule.
     */
    suspend fun learnFromCorrection(merchantNormalized: String, correctedCategory: Category) {
        val rule = CategoryRule(
            keyword = merchantNormalized,
            category = correctedCategory.name,
            priority = 1000,
            isUserDefined = true
        )
        categoryRuleDao.insert(rule)
        invalidateCache()
    }

    private suspend fun getOrLoadRules(): List<CategoryRule> {
        cacheMutex.withLock {
            if (cachedRules == null) {
                cachedRules = categoryRuleDao.getAllRulesSorted()
            }
            return cachedRules!!
        }
    }

    fun invalidateCache() {
        cacheVersion++
        cachedRules = null
    }

    private fun buildSystemRules(): List<CategoryRule> {
        val rules = mutableListOf<CategoryRule>()
        var priority = 1

        for ((keyword, category) in MerchantDatabase.KEYWORD_CATEGORY_MAP) {
            rules.add(
                CategoryRule(
                    keyword = keyword,
                    category = category.name,
                    priority = priority++,
                    isUserDefined = false
                )
            )
        }

        return rules
    }
}
