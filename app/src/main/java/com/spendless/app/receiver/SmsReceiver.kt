package com.spendless.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.*
import com.spendless.app.worker.SmsProcessWorker

/**
 * BroadcastReceiver for incoming SMS messages.
 *
 * Battery-safe design:
 * - Does NOT run any processing here
 * - Immediately enqueues a one-time WorkManager job
 * - Returns to idle state within milliseconds
 * - WorkManager handles the actual processing with proper lifecycle
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Collect all message parts (long SMS can be split into multiple PDUs)
        val messagesByAddress = messages.groupBy { it.originatingAddress }

        messagesByAddress.forEach { (sender, parts) ->
            val fullBody = parts.joinToString("") { it.messageBody }
            val timestamp = parts.first().timestampMillis

            if (sender == null || fullBody.isBlank()) return@forEach

            Log.d("SmsReceiver", "Received SMS from $sender, enqueueing worker")

            // Enqueue a fast, one-time WorkManager job
            val inputData = workDataOf(
                SmsProcessWorker.KEY_SENDER to sender,
                SmsProcessWorker.KEY_BODY to fullBody,
                SmsProcessWorker.KEY_TIMESTAMP to timestamp
            )

            val request = OneTimeWorkRequestBuilder<SmsProcessWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
