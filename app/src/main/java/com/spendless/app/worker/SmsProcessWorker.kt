package com.spendless.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.categorization.CategorizationEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.TransactionStatus
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.datastore.PreferencesDataStore
import com.spendless.app.core.sms.SmsParser
import com.spendless.app.widget.WidgetUpdateUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that processes a single incoming SMS message.
 *
 * Runs quickly (typically < 100ms) and then stops.
 * Battery-safe: no continuous operation.
 */
@HiltWorker
class SmsProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categorizationEngine: CategorizationEngine,
    private val budgetEngine: BudgetEngine,
    private val preferencesDataStore: PreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIMESTAMP = "timestamp"
        private const val TAG = "SmsProcessWorker"
    }

    override suspend fun doWork(): Result {
        val isAutoImportEnabled = preferencesDataStore.smsAutoImportEnabled.first()
        if (!isAutoImportEnabled) {
            Log.d(TAG, "SMS Auto-Import is disabled. Skipping processing.")
            return Result.success()
        }

        val sender = inputData.getString(KEY_SENDER) ?: return Result.success()
        val body = inputData.getString(KEY_BODY) ?: return Result.success()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

        Log.d(TAG, "Processing SMS from $sender")

        return try {
            // Parse the SMS
            val parseResult = SmsParser.parse(sender, body, timestamp)
                ?: return Result.success() // Not a financial SMS

            // Check for duplicate
            val isDuplicate = transactionDao.countBySmsHash(parseResult.smsHash) > 0
            if (isDuplicate) {
                Log.d(TAG, "Duplicate SMS detected, skipping")
                return Result.success()
            }

            // Categorize the transaction
            val category = categorizationEngine.categorize(
                merchantNormalized = SmsParser.normalizeMerchant(parseResult.merchant),
                rawSmsBody = body,
                transactionType = parseResult.type
            )

            // Create transaction entity
            val transaction = SmsParser.toTransaction(parseResult, category)

            // Find the appropriate budget cycle
            val activeBudget = budgetDao.getActiveBudgetSync()
            val cycleId = if (activeBudget != null) {
                val cycleRange = budgetEngine.getCycleForTimestamp(activeBudget.resetDay, timestamp)
                budgetDao.getCycleForTimestamp(activeBudget.id, timestamp)?.id
                    ?: budgetDao.insertCycle(
                        com.spendless.app.core.data.database.entities.BudgetCycle(
                            budgetId = activeBudget.id,
                            startDate = cycleRange.startMs,
                            endDate = cycleRange.endMs,
                            isActive = cycleRange.endMs > System.currentTimeMillis()
                        )
                    )
            } else null

            // Insert transaction with cycle assignment
            val inserted = transactionDao.insert(transaction.copy(cycleId = cycleId))
            Log.d(TAG, "Inserted transaction id=$inserted amount=${parseResult.amount} merchant=${parseResult.merchant}")

            // Update cycle totals cache
            if (cycleId != null) {
                updateCycleTotals(cycleId)
            }

            // Update widgets
            WidgetUpdateUtil.updateAllWidgets(applicationContext)

            // Check budget alerts
            if (activeBudget != null && cycleId != null && parseResult.type == TransactionType.DEBIT) {
                val alertWork = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
                    .setInputData(workDataOf(
                        BudgetAlertWorker.KEY_CYCLE_ID to cycleId,
                        BudgetAlertWorker.KEY_BUDGET to activeBudget.totalBudget
                    ))
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(alertWork)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS: ${e.message}", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private suspend fun updateCycleTotals(cycleId: Long) {
        val spent = transactionDao.getTotalSpentInCycle(cycleId).first() ?: 0.0
        val income = transactionDao.getTotalIncomeInCycle(cycleId).first() ?: 0.0
        budgetDao.updateCycleTotals(cycleId, spent, income)
    }
}
