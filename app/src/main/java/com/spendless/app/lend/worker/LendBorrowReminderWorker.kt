package com.spendless.app.lend.worker

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
import com.spendless.app.core.data.database.dao.LendBorrowDao
import com.spendless.app.core.data.database.entities.LendBorrowStatus
import com.spendless.app.core.data.database.entities.LendBorrowType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that fires daily at 09:00 to send lending/borrowing reminders.
 *
 * Reminder schedule:
 *   7 days before due → "Due in 7 days"
 *   3 days before due → "Due in 3 days"
 *   1 day before due  → "Due tomorrow"
 *   0 days            → "Due today"
 *   Overdue           → "Overdue by N days"
 */
@HiltWorker
class LendBorrowReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val lendBorrowDao: LendBorrowDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "lend_borrow_reminders"
        private const val CHANNEL_ID = "lend_borrow_reminders"
        private const val CHANNEL_NAME = "Lent & Borrowed Reminders"
        private const val TAG = "LendBorrowReminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LendBorrowReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setInitialDelay(
                    calculateDelayUntil9AM(),
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateDelayUntil9AM(): Long {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running lend/borrow reminder check")
        createNotificationChannel()

        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        // Get records due in the next 7 days
        val upcoming = lendBorrowDao.getRecordsDueInRange(
            startMs = now - dayMs * 365, // also include overdue
            endMs = now + dayMs * 8
        )

        upcoming.forEach { record ->
            if (record.status == LendBorrowStatus.COMPLETED) return@forEach

            val daysUntilDue = record.daysUntilDue ?: return@forEach
            val typeLabel = if (record.type == LendBorrowType.LENT) "lent to" else "borrowed from"

            val (notifId, title, message) = when {
                daysUntilDue < 0 -> Triple(
                    (record.id * 10 + 0).toInt(),
                    "Overdue ${record.status.emoji}",
                    "₹${record.outstanding.toLong()} ${typeLabel} ${record.contactName} is overdue by ${-daysUntilDue} day(s)."
                )
                daysUntilDue == 0L -> Triple(
                    (record.id * 10 + 1).toInt(),
                    "Due Today 🔴",
                    "₹${record.outstanding.toLong()} ${typeLabel} ${record.contactName} is due today."
                )
                daysUntilDue == 1L -> Triple(
                    (record.id * 10 + 2).toInt(),
                    "Due Tomorrow 🟠",
                    "₹${record.outstanding.toLong()} ${typeLabel} ${record.contactName} is due tomorrow."
                )
                daysUntilDue <= 3L -> Triple(
                    (record.id * 10 + 3).toInt(),
                    "Due in ${daysUntilDue} days 🟡",
                    "₹${record.outstanding.toLong()} ${typeLabel} ${record.contactName} is due in ${daysUntilDue} days."
                )
                daysUntilDue <= 7L -> Triple(
                    (record.id * 10 + 4).toInt(),
                    "Upcoming Payment",
                    "₹${record.outstanding.toLong()} ${typeLabel} ${record.contactName} is due in ${daysUntilDue} days."
                )
                else -> return@forEach
            }

            sendNotification(notifId, title, message, record.contactPhone, record.id)
        }

        return Result.success()
    }

    private fun sendNotification(
        notifId: Int,
        title: String,
        message: String,
        phone: String,
        recordId: Long
    ) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Main tap → open app
        val mainIntent = Intent(applicationContext, MainActivity::class.java)
        val mainPi = PendingIntent.getActivity(
            applicationContext, notifId,
            mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // WhatsApp action
        val waIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://wa.me/91${phone.removePrefix("+91").removePrefix("0")}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val waPi = PendingIntent.getActivity(
            applicationContext, notifId + 1000,
            waIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Call action
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$phone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callPi = PendingIntent.getActivity(
            applicationContext, notifId + 2000,
            callIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainPi)
            .setAutoCancel(true)
            .addAction(0, "📞 Call", callPi)
            .addAction(0, "💬 WhatsApp", waPi)
            .build()

        nm.notify(notifId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for money lent and borrowed"
                enableVibration(true)
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
