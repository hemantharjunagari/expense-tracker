package com.spendless.app.core.data.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.Transaction as RoomTransaction
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.Transaction
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.database.entities.TransactionStatus
import com.spendless.app.core.data.database.entities.TransactionSplit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    // ── Cycle queries ──────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM transactions 
        WHERE cycleId = :cycleId AND status != 'IGNORED'
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByCycle(cycleId: Long): PagingSource<Int, Transaction>

    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND t.type = 'DEBIT' 
          AND t.status = 'APPROVED'
          AND c.isBudgetTrackingEnabled = 1
        ORDER BY t.timestamp DESC
    """)
    fun getExpensesByCycle(cycleId: Long): PagingSource<Int, Transaction>

    @Query("""
        SELECT SUM(t.amount) FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND t.type = 'DEBIT' 
          AND t.status = 'APPROVED'
          AND c.isBudgetTrackingEnabled = 1
    """)
    fun getTotalSpentInCycle(cycleId: Long): Flow<Double?>

    @Query("""
        SELECT SUM(t.amount) FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND t.type = 'DEBIT' 
          AND t.status = 'APPROVED'
          AND c.isBudgetTrackingEnabled = 1
    """)
    suspend fun getTotalSpentInCycleSync(cycleId: Long): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE cycleId = :cycleId 
          AND type = 'CREDIT' 
          AND status = 'APPROVED'
    """)
    fun getTotalIncomeInCycle(cycleId: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE cycleId = :cycleId 
          AND type = 'CREDIT' 
          AND status = 'APPROVED'
    """)
    suspend fun getTotalIncomeInCycleSync(cycleId: Long): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE cycleId = :cycleId AND category = :category AND type = 'DEBIT' AND status = 'APPROVED'
    """)
    suspend fun getSpentByCategory(cycleId: Long, category: Category): Double?

    // ── Pending Review aggregates ──────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING_REVIEW'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'PENDING_REVIEW'")
    fun getPendingSumFlow(): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING_REVIEW' ORDER BY timestamp DESC")
    fun getPendingTransactionsPaged(): PagingSource<Int, Transaction>

    @Query("""
        SELECT SUM(t.amount) FROM transactions t
        LEFT JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND (
            t.status = 'SELF_TRANSFER' OR 
            t.status = 'IGNORED' OR 
            (t.status = 'APPROVED' AND c.isBudgetTrackingEnabled = 0)
          )
    """)
    fun getExcludedTransactionsSum(cycleId: Long): Flow<Double?>

    // ── All transactions (paged) ────────────────────────────────────────────────

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'IGNORED'
        ORDER BY timestamp DESC
    """)
    fun getAllTransactionsPaged(): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startMs AND :endMs AND status != 'IGNORED'
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByDateRange(startMs: Long, endMs: Long): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE timestamp BETWEEN :startMs AND :endMs AND status != 'IGNORED'
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsByDateRangeList(startMs: Long, endMs: Long): List<Transaction>

    // ── Search ─────────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM transactions 
        WHERE (merchant LIKE '%' || :query || '%' OR rawSmsBody LIKE '%' || :query || '%') AND status != 'IGNORED'
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): PagingSource<Int, Transaction>

    // ── Analytics ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.timestamp BETWEEN :startMs AND :endMs 
          AND t.status = 'APPROVED'
          AND c.includeInAnalytics = 1
        ORDER BY t.timestamp DESC
    """)
    fun getTransactionsForAnalytics(startMs: Long, endMs: Long): Flow<List<Transaction>>

    @Query("""
        SELECT t.merchantNormalized, SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND t.type = 'DEBIT' 
          AND t.status = 'APPROVED'
          AND c.isBudgetTrackingEnabled = 1
        GROUP BY t.merchantNormalized
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopMerchants(cycleId: Long, limit: Int = 10): List<MerchantTotal>

    @Query("""
        SELECT t.category, SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND t.type = 'DEBIT' 
          AND t.status = 'APPROVED'
          AND c.includeInAnalytics = 1
        GROUP BY t.category
        ORDER BY total DESC
    """)
    suspend fun getCategoryBreakdown(cycleId: Long): List<CategoryTotal>

    // ── Deduplication ──────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM transactions WHERE merchantNormalized = :merchantNormalized")
    suspend fun getMerchantTransactionCount(merchantNormalized: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE smsHash = :hash")
    suspend fun countBySmsHash(hash: String): Int

    @Query("SELECT MAX(timestamp) FROM transactions")
    suspend fun getLatestTransactionTimestamp(): Long?

    // ── Cycle assignment ───────────────────────────────────────────────────────

    @Query("""
        UPDATE transactions SET cycleId = :cycleId
        WHERE timestamp BETWEEN :startMs AND :endMs AND cycleId IS NULL
    """)
    suspend fun assignCycleToTransactions(cycleId: Long, startMs: Long, endMs: Long)

    @Query("UPDATE transactions SET category = :targetCategoryName WHERE category = :sourceCategoryName")
    suspend fun mergeCategories(sourceCategoryName: String, targetCategoryName: String)

    // ── Counts ─────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM transactions WHERE status != 'IGNORED'")
    fun getTotalTransactionCount(): Flow<Int>

    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN categories c ON t.category = c.name
        WHERE t.cycleId = :cycleId 
          AND t.type = 'DEBIT' 
          AND t.status = 'APPROVED'
          AND c.isBudgetTrackingEnabled = 1
        ORDER BY t.amount DESC LIMIT 5
    """)
    suspend fun getTopExpensesInCycle(cycleId: Long): List<Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE (:searchQuery = '' OR merchant LIKE '%' || :searchQuery || '%' OR rawSmsBody LIKE '%' || :searchQuery || '%')
          AND (:selectedCategoryName IS NULL OR category = :selectedCategoryName)
          AND status != 'IGNORED'
          AND (
            :tab = 'ALL' OR
            (:tab = 'EXPENSES' AND type = 'DEBIT' AND status = 'APPROVED' AND category != 'UNCATEGORIZED' AND category != 'SELF_TRANSFER') OR
            (:tab = 'INCOME' AND type = 'CREDIT' AND status = 'APPROVED') OR
            (:tab = 'SELF_TRANSFER' AND (status = 'SELF_TRANSFER' OR category = 'SELF_TRANSFER')) OR
            (:tab = 'UNCATEGORIZED' AND (category = 'UNCATEGORIZED' OR status = 'PENDING_REVIEW'))
          )
        ORDER BY timestamp DESC
    """)
    fun getFilteredTransactionsPaged(
        searchQuery: String,
        selectedCategoryName: String?,
        tab: String
    ): PagingSource<Int, Transaction>

    @Query("""
        UPDATE transactions 
        SET category = :newCategoryName, isManuallyEdited = 1, status = 'APPROVED'
        WHERE merchantNormalized = :merchantNormalized
          AND (:startTime = 0 OR timestamp >= :startTime)
    """)
    suspend fun bulkUpdateCategory(merchantNormalized: String, newCategoryName: String, startTime: Long)

    @Query("""
        SELECT * FROM transactions
        WHERE (:searchQuery = '' 
               OR merchant LIKE '%' || :searchQuery || '%' 
               OR rawSmsBody LIKE '%' || :searchQuery || '%' 
               OR userNote LIKE '%' || :searchQuery || '%'
               OR contactName LIKE '%' || :searchQuery || '%'
               OR contactPhone LIKE '%' || :searchQuery || '%')
          AND (:selectedCategoryName IS NULL OR category = :selectedCategoryName)
          AND (:type IS NULL OR type = :type)
          AND (:status IS NULL OR status = :status)
          AND (:startTime = 0 OR timestamp >= :startTime)
          AND (:endTime = 0 OR timestamp <= :endTime)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:paymentMethod IS NULL OR paymentMethod = :paymentMethod)
          AND (:isManual IS NULL OR isManualEntry = :isManual)
          AND (:isExcludedFromBudget IS NULL OR isExcludedFromBudget = :isExcludedFromBudget)
          AND (:isExcludedFromAnalytics IS NULL OR isExcludedFromAnalytics = :isExcludedFromAnalytics)
          AND (:contactName IS NULL OR contactName = :contactName)
          AND (:merchantName IS NULL OR merchant = :merchantName)
        ORDER BY 
            CASE WHEN :sortBy = 'PENDING_REVIEW_FIRST' AND status = 'PENDING_REVIEW' THEN 0 ELSE 1 END ASC,
            CASE WHEN :sortBy = 'NEWEST' THEN timestamp END DESC,
            CASE WHEN :sortBy = 'OLDEST' THEN timestamp END ASC,
            CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
            CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC,
            CASE WHEN :sortBy = 'MERCHANT' THEN merchant END ASC,
            CASE WHEN :sortBy = 'CATEGORY' THEN category END ASC,
            CASE WHEN :sortBy = 'RECENTLY_EDITED' THEN createdAt END DESC,
            CASE WHEN :sortBy = 'DUE_DATE' THEN timestamp END DESC
    """)
    fun getFilteredTransactionsPagedAdvanced(
        searchQuery: String,
        selectedCategoryName: String?,
        type: String?,
        status: String?,
        startTime: Long,
        endTime: Long,
        minAmount: Double?,
        maxAmount: Double?,
        paymentMethod: String?,
        isManual: Boolean?,
        isExcludedFromBudget: Boolean?,
        isExcludedFromAnalytics: Boolean?,
        contactName: String?,
        merchantName: String?,
        sortBy: String = "NEWEST"
    ): PagingSource<Int, Transaction>

    @Query("""
        SELECT 
            COUNT(t.id) as count,
            SUM(CASE WHEN t.type = 'DEBIT' AND t.status = 'APPROVED' AND t.isExcludedFromBudget = 0 AND UPPER(t.category) != 'UNCATEGORIZED' AND (c.isBudgetTrackingEnabled = 1 OR c.isBudgetTrackingEnabled IS NULL) THEN t.amount ELSE 0 END) as totalSpent,
            SUM(CASE WHEN t.type = 'CREDIT' AND t.status = 'APPROVED' THEN t.amount ELSE 0 END) as totalIncome,
            SUM(CASE WHEN t.status = 'PENDING_REVIEW' THEN 1 ELSE 0 END) as pendingCount
        FROM transactions t
        LEFT JOIN categories c ON t.category = c.name
        WHERE t.timestamp BETWEEN :startTime AND :endTime
          AND (:searchQuery = '' 
               OR t.merchant LIKE '%' || :searchQuery || '%' 
               OR t.rawSmsBody LIKE '%' || :searchQuery || '%' 
               OR t.userNote LIKE '%' || :searchQuery || '%'
               OR t.contactName LIKE '%' || :searchQuery || '%'
               OR t.contactPhone LIKE '%' || :searchQuery || '%')
          AND (:categoryName IS NULL OR t.category = :categoryName)
          AND (:type IS NULL OR t.type = :type)
          AND (:status IS NULL OR t.status = :status)
          AND (:minAmount IS NULL OR t.amount >= :minAmount)
          AND (:maxAmount IS NULL OR t.amount <= :maxAmount)
          AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod)
          AND (:isManual IS NULL OR t.isManualEntry = :isManual)
          AND (:isExcludedFromBudget IS NULL OR t.isExcludedFromBudget = :isExcludedFromBudget)
          AND (:isExcludedFromAnalytics IS NULL OR t.isExcludedFromAnalytics = :isExcludedFromAnalytics)
          AND (:contactName IS NULL OR t.contactName = :contactName)
          AND (:merchantName IS NULL OR t.merchant = :merchantName)
    """)
    fun getQuickStats(
        startTime: Long,
        endTime: Long,
        searchQuery: String,
        categoryName: String?,
        type: String?,
        status: String?,
        minAmount: Double?,
        maxAmount: Double?,
        paymentMethod: String?,
        isManual: Boolean?,
        isExcludedFromBudget: Boolean?,
        isExcludedFromAnalytics: Boolean?,
        contactName: String?,
        merchantName: String?
    ): Flow<TransactionStats>

    @Query("SELECT * FROM transactions WHERE cycleId = :cycleId")
    fun getTransactionsForCycleFlow(cycleId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE cycleId = :cycleId")
    suspend fun getTransactionsForCycleSync(cycleId: Long): List<Transaction>

    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as monthKey,
            COUNT(*) as count,
            SUM(CASE WHEN type = 'DEBIT' AND status = 'APPROVED' AND isExcludedFromBudget = 0 AND UPPER(category) != 'UNCATEGORIZED' THEN amount ELSE 0 END) as expenses,
            SUM(CASE WHEN type = 'CREDIT' AND status = 'APPROVED' THEN amount ELSE 0 END) as income,
            SUM(CASE WHEN status = 'PENDING_REVIEW' THEN 1 ELSE 0 END) as pendingCount
        FROM transactions
        GROUP BY monthKey
    """)
    fun getMonthlySummariesFlow(): Flow<List<DbMonthSummary>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<Long>): List<Transaction>

    @Query("UPDATE transactions SET status = :status WHERE id IN (:ids)")
    suspend fun bulkUpdateStatus(ids: List<Long>, status: String)

    @Query("""
        UPDATE transactions 
        SET category = :categoryName, 
            isManuallyEdited = 1,
            isExcludedFromBudget = :isExcludedBudget,
            isExcludedFromAnalytics = :isExcludedAnalytics
        WHERE id IN (:ids)
    """)
    suspend fun bulkUpdateCategoryByIds(
        ids: List<Long>, 
        categoryName: String, 
        isExcludedBudget: Boolean, 
        isExcludedAnalytics: Boolean
    )

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<Transaction>

    @Delete
    suspend fun bulkDelete(transactions: List<Transaction>)
}

data class DbMonthSummary(
    val monthKey: String, // format YYYY-MM
    val count: Int,
    val expenses: Double,
    val income: Double,
    val pendingCount: Int
)

data class TransactionStats(
    val count: Int,
    val totalSpent: Double?,
    val totalIncome: Double?,
    val pendingCount: Int
)

data class MerchantTotal(
    val merchantNormalized: String,
    val total: Double
)

data class CategoryTotal(
    val category: Category,
    val total: Double
)

fun TransactionDao.getTotalSpentInCycleFlow(cycleId: Long): Flow<Double> {
    return this.getTransactionsForCycleFlow(cycleId).map { transactions ->
        transactions.sumOf { txn ->
            if (txn.status == TransactionStatus.APPROVED && !txn.isExcludedFromBudget) {
                if (txn.splits.isNotEmpty()) {
                    txn.splits.sumOf { split ->
                        val cat = Category.fromName(split.categoryName)
                        if (cat.isBudgetTrackingEnabled && cat.name.uppercase() != "UNCATEGORIZED" && txn.type == TransactionType.DEBIT) split.amount else 0.0
                    }
                } else {
                    if (txn.category.isBudgetTrackingEnabled && txn.category.name.uppercase() != "UNCATEGORIZED" && txn.type == TransactionType.DEBIT) txn.amount else 0.0
                }
            } else {
                0.0
            }
        }
    }
}

fun TransactionDao.getTotalIncomeInCycleFlow(cycleId: Long): Flow<Double> {
    return this.getTransactionsForCycleFlow(cycleId).map { transactions ->
        transactions.sumOf { txn ->
            if (txn.status == TransactionStatus.APPROVED) {
                if (txn.splits.isNotEmpty()) {
                    txn.splits.sumOf { split ->
                        if (txn.type == TransactionType.CREDIT) split.amount else 0.0
                    }
                } else {
                    if (txn.type == TransactionType.CREDIT) txn.amount else 0.0
                }
            } else {
                0.0
            }
        }
    }
}

fun TransactionDao.getTotalSpentInCycleSyncFlow(transactions: List<Transaction>): Double {
    return transactions.sumOf { txn ->
        if (txn.status == TransactionStatus.APPROVED && !txn.isExcludedFromBudget) {
            if (txn.splits.isNotEmpty()) {
                txn.splits.sumOf { split ->
                    val cat = Category.fromName(split.categoryName)
                    if (cat.isBudgetTrackingEnabled && cat.name.uppercase() != "UNCATEGORIZED" && txn.type == TransactionType.DEBIT) split.amount else 0.0
                }
            } else {
                if (txn.category.isBudgetTrackingEnabled && txn.category.name.uppercase() != "UNCATEGORIZED" && txn.type == TransactionType.DEBIT) txn.amount else 0.0
            }
        } else {
            0.0
        }
    }
}

fun TransactionDao.getTotalIncomeInCycleSyncFlow(transactions: List<Transaction>): Double {
    return transactions.sumOf { txn ->
        if (txn.status == TransactionStatus.APPROVED) {
            if (txn.splits.isNotEmpty()) {
                txn.splits.sumOf { split ->
                    if (txn.type == TransactionType.CREDIT) split.amount else 0.0
                }
            } else {
                if (txn.type == TransactionType.CREDIT) txn.amount else 0.0
            }
        } else {
            0.0
        }
    }
}
