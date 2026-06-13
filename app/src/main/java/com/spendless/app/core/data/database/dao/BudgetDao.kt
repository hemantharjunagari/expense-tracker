package com.spendless.app.core.data.database.dao

import androidx.room.*
import com.spendless.app.core.data.database.entities.Budget
import com.spendless.app.core.data.database.entities.BudgetCycle
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // ── Budget ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE isActive = 1 LIMIT 1")
    fun getActiveBudget(): Flow<Budget?>

    @Query("SELECT * FROM budgets WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBudgetSync(): Budget?

    @Query("UPDATE budgets SET isActive = 0")
    suspend fun deactivateAllBudgets()

    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    // ── BudgetCycle ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: BudgetCycle): Long

    @Update
    suspend fun updateCycle(cycle: BudgetCycle)

    @Query("SELECT * FROM budget_cycles WHERE isActive = 1 LIMIT 1")
    fun getActiveCycle(): Flow<BudgetCycle?>

    @Query("SELECT * FROM budget_cycles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCycleSync(): BudgetCycle?

    @Query("""
        SELECT * FROM budget_cycles 
        WHERE budgetId = :budgetId AND startDate <= :timestamp AND endDate >= :timestamp
        LIMIT 1
    """)
    suspend fun getCycleForTimestamp(budgetId: Long, timestamp: Long): BudgetCycle?

    @Query("""
        SELECT * FROM budget_cycles 
        WHERE budgetId = :budgetId
        ORDER BY startDate DESC
    """)
    fun getCyclesForBudget(budgetId: Long): Flow<List<BudgetCycle>>

    @Query("""
        SELECT * FROM budget_cycles 
        WHERE budgetId = :budgetId
        ORDER BY startDate DESC
    """)
    suspend fun getCyclesForBudgetSync(budgetId: Long): List<BudgetCycle>

    @Query("UPDATE budget_cycles SET isActive = 0")
    suspend fun deactivateAllCycles()

    @Query("""
        UPDATE budget_cycles 
        SET totalSpent = :totalSpent, totalIncome = :totalIncome 
        WHERE id = :cycleId
    """)
    suspend fun updateCycleTotals(cycleId: Long, totalSpent: Double, totalIncome: Double)

    @Query("SELECT * FROM budget_cycles WHERE id = :cycleId")
    suspend fun getCycleById(cycleId: Long): BudgetCycle?

    @Query("""
        SELECT * FROM budget_cycles
        ORDER BY startDate DESC
        LIMIT :limit
    """)
    fun getRecentCycles(limit: Int = 12): Flow<List<BudgetCycle>>
}
