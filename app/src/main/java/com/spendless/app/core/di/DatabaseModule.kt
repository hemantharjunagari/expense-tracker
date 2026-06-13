package com.spendless.app.core.di

import android.content.Context
import com.spendless.app.core.data.database.SpendLessDatabase
import com.spendless.app.core.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.spendless.app.core.data.database.entities.Category

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpendLessDatabase =
        SpendLessDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideTransactionDao(db: SpendLessDatabase): TransactionDao =
        db.transactionDao()

    @Provides
    @Singleton
    fun provideBudgetDao(db: SpendLessDatabase): BudgetDao =
        db.budgetDao()

    @Provides
    @Singleton
    fun provideCategoryRuleDao(db: SpendLessDatabase): CategoryRuleDao =
        db.categoryRuleDao()

    @Provides
    @Singleton
    fun provideMonthlySummaryDao(db: SpendLessDatabase): MonthlySummaryDao =
        db.monthlySummaryDao()

    @Provides
    @Singleton
    fun provideLendBorrowDao(db: SpendLessDatabase): LendBorrowDao =
        db.lendBorrowDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: SpendLessDatabase): CategoryDao {
        val dao = db.categoryDao()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // Populate initial database categories immediately
                val initialList = dao.getAllCategories()
                Category.customCache.putAll(initialList.associateBy { it.name })

                // Observe future category updates and keep the cache in sync
                dao.getAllCategoriesWithArchivedFlow().collect { categories ->
                    Category.customCache.clear()
                    Category.customCache.putAll(categories.associateBy { it.name })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return dao
    }
}
