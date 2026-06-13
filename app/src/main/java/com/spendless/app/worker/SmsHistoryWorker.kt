package com.spendless.app.worker

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.categorization.CategorizationEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.dao.CategoryDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.entities.BudgetCycle
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.TransactionStatus
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.datastore.PreferencesDataStore
import com.spendless.app.core.sms.SmsParser
import com.spendless.app.widget.WidgetUpdateUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for importing historical SMS messages on first setup.
 *
 * Processes SMS in batches of 100 to avoid OOM.
 * Reports progress via WorkInfo.progress for UI display.
 * Idempotent: skips already-imported messages by SMS hash.
 */
@HiltWorker
class SmsHistoryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categorizationEngine: CategorizationEngine,
    private val budgetEngine: BudgetEngine,
    private val preferencesDataStore: PreferencesDataStore,
    private val categoryDao: CategoryDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "sms_history_import"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_IMPORTED = "imported"
        const val KEY_START_DATE = "start_date"
        private const val BATCH_SIZE = 100
        private const val TAG = "SmsHistoryWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting historical SMS import")
        setProgress(workDataOf(KEY_TOTAL to 0, KEY_PROGRESS to 0))

        try {
            // Seed categorization rules and categories
            categorizationEngine.seedSystemRulesIfNeeded()
            categorizationEngine.seedSystemCategoriesIfNeeded(categoryDao)

            val activeBudget = budgetDao.getActiveBudgetSync()
            val startDate = inputData.getLong(KEY_START_DATE, 0L)
            val selection = if (startDate > 0) "date >= ?" else null
            val selectionArgs = if (startDate > 0) arrayOf(startDate.toString()) else null
            
            // Count total SMS first for progress reporting
            val total = countTotalSms(selection, selectionArgs)
            Log.d(TAG, "Found $total SMS messages to process")
            setProgress(workDataOf(KEY_TOTAL to total, KEY_PROGRESS to 0))

            if (total == 0) {
                return@withContext Result.success(workDataOf(KEY_PROGRESS to 100))
            }

            var importedCount = 0
            var processedCount = 0
            val cycleCache = mutableMapOf<Long, Long>() // startDateMs -> cycleId

            // Process in batches using a cursor to save memory
            querySms(selection, selectionArgs) { cursor ->
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")

                val currentBatch = mutableListOf<Triple<String, String, Long>>()

                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val date = cursor.getLong(dateIdx)
                    currentBatch.add(Triple(address, body, date))

                    if (currentBatch.size >= BATCH_SIZE) {
                        importedCount += processBatch(
                            currentBatch, 
                            activeBudget, 
                            cycleCache, 
                            categorizationEngine, 
                            budgetEngine, 
                            transactionDao, 
                            budgetDao
                        )
                        processedCount += currentBatch.size
                        currentBatch.clear()

                        // Report progress
                        val progressPercent = ((processedCount.toFloat() / total) * 100).toInt()
                        setProgress(workDataOf(
                            KEY_PROGRESS to progressPercent,
                            KEY_TOTAL to total,
                            KEY_IMPORTED to importedCount
                        ))
                    }
                }

                // Process remaining
                if (currentBatch.isNotEmpty()) {
                    importedCount += processBatch(
                        currentBatch, 
                        activeBudget, 
                        cycleCache, 
                        categorizationEngine, 
                        budgetEngine, 
                        transactionDao, 
                        budgetDao
                    )
                    processedCount += currentBatch.size
                }
            }

            // Update cycle totals for all cycles
            updateAllCycleTotals(activeBudget?.id)

            preferencesDataStore.setHistoricalScanDone(true)
            WidgetUpdateUtil.updateAllWidgets(applicationContext)

            Log.d(TAG, "Historical import complete: $importedCount transactions imported from $total SMS")

            Result.success(workDataOf(
                KEY_IMPORTED to importedCount,
                KEY_TOTAL to total,
                KEY_PROGRESS to 100
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Historical import failed: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun processBatch(
        batch: List<Triple<String, String, Long>>,
        activeBudget: com.spendless.app.core.data.database.entities.Budget?,
        cycleCache: MutableMap<Long, Long>,
        categorizationEngine: CategorizationEngine,
        budgetEngine: BudgetEngine,
        transactionDao: TransactionDao,
        budgetDao: BudgetDao
    ): Int {
        val batchTransactions = mutableListOf<com.spendless.app.core.data.database.entities.Transaction>()

        batch.forEach { (sender, body, timestamp) ->
            val parseResult = SmsParser.parse(sender, body, timestamp)
            if (parseResult != null) {
                val isDuplicate = transactionDao.countBySmsHash(parseResult.smsHash) > 0
                if (!isDuplicate) {
                    val category = categorizationEngine.categorize(
                        merchantNormalized = SmsParser.normalizeMerchant(parseResult.merchant),
                        rawSmsBody = body,
                        transactionType = parseResult.type
                    )

                    // Find or create cycle
                    val cycleId = if (activeBudget != null) {
                        val cycleRange = budgetEngine.getCycleForTimestamp(
                            activeBudget.resetDay, timestamp
                        )
                        
                        cycleCache.getOrPut(cycleRange.startMs) {
                            val existingCycle = budgetDao.getCycleForTimestamp(activeBudget.id, timestamp)
                            existingCycle?.id ?: budgetDao.insertCycle(
                                BudgetCycle(
                                    budgetId = activeBudget.id,
                                    startDate = cycleRange.startMs,
                                    endDate = cycleRange.endMs,
                                    isActive = cycleRange.endMs > System.currentTimeMillis()
                                )
                            )
                        }
                    } else null

                    batchTransactions.add(
                        SmsParser.toTransaction(parseResult, category).copy(
                            cycleId = cycleId
                        )
                    )
                }
            }
        }

        return if (batchTransactions.isNotEmpty()) {
            transactionDao.insertAll(batchTransactions).count { it > 0 }
        } else 0
    }

    private fun countTotalSms(selection: String?, selectionArgs: Array<String>?): Int {
        val uri = Uri.parse("content://sms/inbox")
        val cursor = applicationContext.contentResolver.query(uri, arrayOf("COUNT(*)"), selection, selectionArgs, null)
        return cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        } ?: 0
    }

    private inline fun querySms(selection: String?, selectionArgs: Array<String>?, block: (Cursor) -> Unit) {
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        val sortOrder = "date DESC"

        val cursor: Cursor? = try {
            applicationContext.contentResolver.query(
                uri, projection, selection, selectionArgs, sortOrder
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SMS permission not granted: ${e.message}")
            null
        }

        cursor?.use { block(it) }
    }

    private suspend fun updateAllCycleTotals(budgetId: Long?) {
        if (budgetId == null) return
        try {
            val cycles = budgetDao.getCyclesForBudgetSync(budgetId)
            cycles.forEach { cycle ->
                val spent = transactionDao.getTotalSpentInCycleSync(cycle.id) ?: 0.0
                val income = transactionDao.getTotalIncomeInCycleSync(cycle.id) ?: 0.0
                budgetDao.updateCycleTotals(cycle.id, spent, income)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update cycle totals: ${e.message}")
        }
    }
}
