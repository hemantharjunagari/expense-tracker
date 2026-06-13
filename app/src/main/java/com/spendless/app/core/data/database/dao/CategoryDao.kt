package com.spendless.app.core.data.database.dao

import androidx.room.*
import com.spendless.app.core.data.database.entities.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY displayOrder ASC, isSystem DESC, displayName ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY displayOrder ASC, isSystem DESC, displayName ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, isSystem DESC, displayName ASC")
    fun getAllCategoriesWithArchivedFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
