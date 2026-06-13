package com.spendless.app.core.data.database.dao

import androidx.room.*
import com.spendless.app.core.data.database.entities.CategoryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRule): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<CategoryRule>)

    @Update
    suspend fun update(rule: CategoryRule)

    @Delete
    suspend fun delete(rule: CategoryRule)

    @Query("SELECT * FROM category_rules WHERE isEnabled = 1 ORDER BY priority DESC, isUserDefined DESC")
    suspend fun getAllRulesSorted(): List<CategoryRule>

    @Query("SELECT * FROM category_rules WHERE isEnabled = 1 ORDER BY priority DESC, isUserDefined DESC")
    fun getAllRulesFlow(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules WHERE isUserDefined = 1 ORDER BY createdAt DESC")
    fun getUserDefinedRules(): Flow<List<CategoryRule>>

    @Query("SELECT COUNT(*) FROM category_rules WHERE isUserDefined = 0")
    suspend fun getSystemRuleCount(): Int

    @Query("DELETE FROM category_rules WHERE isUserDefined = 0")
    suspend fun clearSystemRules()
}
