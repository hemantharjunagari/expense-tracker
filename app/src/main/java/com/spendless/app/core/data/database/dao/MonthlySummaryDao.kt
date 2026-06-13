package com.spendless.app.core.data.database.dao

import androidx.room.*
import com.spendless.app.core.data.database.entities.MonthlySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: MonthlySummary)

    @Update
    suspend fun update(summary: MonthlySummary)

    @Query("SELECT * FROM monthly_summaries WHERE cycleId = :cycleId")
    suspend fun getSummaryForCycle(cycleId: Long): MonthlySummary?

    @Query("SELECT * FROM monthly_summaries WHERE cycleId = :cycleId")
    fun getSummaryForCycleFlow(cycleId: Long): Flow<MonthlySummary?>

    @Query("""
        SELECT * FROM monthly_summaries 
        ORDER BY id DESC 
        LIMIT :count
    """)
    fun getRecentSummaries(count: Int = 12): Flow<List<MonthlySummary>>

    @Query("DELETE FROM monthly_summaries WHERE cycleId = :cycleId")
    suspend fun deleteByCycleId(cycleId: Long)
}
