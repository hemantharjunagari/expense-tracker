package com.spendless.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.spendless.app.MainActivity
import com.spendless.app.R
import com.spendless.app.core.analytics.BudgetEngine
import com.spendless.app.core.data.database.dao.BudgetDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.datastore.PreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val budgetEngine: BudgetEngine,
    private val preferencesDataStore: PreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_CYCLE_ID = "cycle_id"
        const val KEY_BUDGET = "budget"
        const val CHANNEL_ID = "budget_alerts"
        const val CHANNEL_NAME = "Budget Alerts"
        private const val TAG = "BudgetAlertWorker"
    }

    override suspend fun doWork(): Result {
        val notificationsEnabled = preferencesDataStore.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        val cycleId = inputData.getLong(KEY_CYCLE_ID, -1L)
        val budget = inputData.getDouble(KEY_BUDGET, 0.0)

        if (cycleId < 0 || budget <= 0) return Result.success()

        val spent = transactionDao.getTotalSpentInCycle(cycleId).first() ?: return Result.success()
        val notifState = preferencesDataStore.getNotificationState()

        // Only check notifications if this is the same cycle we last notified for
        if (notifState.lastCycleId != -1L && notifState.lastCycleId != cycleId) {
            preferencesDataStore.resetNotificationFlags(cycleId)
        }

        val alreadyNotified = buildSet {
            if (notifState.notified50) add(50)
            if (notifState.notified75) add(75)
            if (notifState.notified90) add(90)
            if (notifState.notified100) add(100)
        }

        val threshold = budgetEngine.checkBudgetThreshold(spent, budget, alreadyNotified)
            ?: return Result.success()

        val remaining = budgetEngine.getRemaining(spent, budget)
        sendNotification(threshold, spent, budget, remaining)
        preferencesDataStore.markNotificationSent(threshold)

        return Result.success()
    }

    private fun sendNotification(
        threshold: Int,
        spent: Double,
        budget: Double,
        remaining: Double
    ) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create notification channel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Budget utilization alerts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val (title, message) = when (threshold) {
            50 -> "Budget Half Used" to
                "You've used 50% of your monthly budget. ₹${remaining.toInt()} remaining."
            75 -> "Budget Alert ⚡" to
                "75% of your budget is spent. Only ₹${remaining.toInt()} left."
            90 -> "Almost Over Budget ⚠️" to
                "90% used! Only ₹${remaining.toInt()} remains in this cycle."
            100 -> "Budget Exceeded 🚨" to
                "You've exceeded your budget of ₹${budget.toInt()}. You've spent ₹${spent.toInt()}."
            else -> return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, threshold, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(threshold, notification)
        Log.d(TAG, "Budget alert sent: $threshold%")
    }
}
