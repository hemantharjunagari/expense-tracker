package com.spendless.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.spendless.app.R
import com.spendless.app.core.data.database.SpendLessDatabase
import com.spendless.app.core.data.database.entities.LendBorrowStatus
import com.spendless.app.core.data.database.entities.LendBorrowType
import com.spendless.app.lend.widget.LendBorrowWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Utility for updating all home screen widgets.
 * Called after every new transaction to keep widgets fresh.
 * Battery-efficient: only called when data actually changes.
 */
object WidgetUpdateUtil {

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SpendLessDatabase.getInstance(context)
                val budgetDao = db.budgetDao()
                val transactionDao = db.transactionDao()
                val lendBorrowDao = db.lendBorrowDao()

                val activeBudget = budgetDao.getActiveBudgetSync()
                val activeCycle = budgetDao.getActiveCycleSync()

                val totalBudget = activeBudget?.totalBudget ?: 0.0
                val totalSpent = activeCycle?.totalSpent ?: 0.0
                val remaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
                val percent = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

                val appWidgetManager = AppWidgetManager.getInstance(context)

                // 1. Update Budget Widget
                val budgetWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, BudgetWidgetProvider::class.java)
                )
                if (budgetWidgetIds.isNotEmpty()) {
                    val budgetViews = RemoteViews(context.packageName, R.layout.widget_budget)
                    budgetViews.setTextViewText(R.id.widget_spent, "₹${formatAmount(totalSpent)}")
                    budgetViews.setTextViewText(R.id.widget_budget_label, "of ₹${formatAmount(totalBudget)} budget")
                    budgetViews.setProgressBar(R.id.widget_progress, 100, percent, false)
                    budgetViews.setTextViewText(R.id.widget_percent, "$percent% used")
                    if (totalSpent > totalBudget) {
                        val overspent = totalSpent - totalBudget
                        budgetViews.setTextViewText(R.id.widget_remaining, "₹${formatAmount(overspent)} over budget")
                        budgetViews.setTextColor(R.id.widget_remaining, android.graphics.Color.parseColor("#FF3E3E"))
                    } else {
                        budgetViews.setTextViewText(R.id.widget_remaining, "₹${formatAmount(remaining)} left")
                        budgetViews.setTextColor(R.id.widget_remaining, android.graphics.Color.parseColor("#AAAAAA"))
                    }
                    appWidgetManager.updateAppWidget(budgetWidgetIds, budgetViews)
                }

                // 2. Update Compact Widget
                val compactWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, CompactWidgetProvider::class.java)
                )
                if (compactWidgetIds.isNotEmpty()) {
                    val compactViews = RemoteViews(context.packageName, R.layout.widget_compact)
                    if (totalSpent > totalBudget) {
                        val overspent = totalSpent - totalBudget
                        compactViews.setTextViewText(R.id.compact_remaining, "₹${formatAmount(overspent)}")
                        compactViews.setTextViewText(R.id.compact_label, "OVER BUDGET")
                        compactViews.setTextColor(R.id.compact_remaining, android.graphics.Color.parseColor("#FF3E3E"))
                    } else {
                        compactViews.setTextViewText(R.id.compact_remaining, "₹${formatAmount(remaining)}")
                        compactViews.setTextViewText(R.id.compact_label, "REMAINING")
                        compactViews.setTextColor(R.id.compact_remaining, android.graphics.Color.parseColor("#FFFFFF"))
                    }
                    compactViews.setProgressBar(R.id.compact_progress, 100, percent, false)
                    appWidgetManager.updateAppWidget(compactWidgetIds, compactViews)
                }

                // 3. Update Medium Widget (top categories)
                val mediumWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, MediumWidgetProvider::class.java)
                )
                if (mediumWidgetIds.isNotEmpty()) {
                    val mediumViews = RemoteViews(context.packageName, R.layout.widget_medium)
                    mediumViews.setTextViewText(R.id.medium_spent, "₹${formatAmount(totalSpent)}")
                    mediumViews.setTextViewText(R.id.medium_budget, "of ₹${formatAmount(totalBudget)}")
                    mediumViews.setTextViewText(R.id.medium_percent, "$percent%")
                    mediumViews.setProgressBar(R.id.medium_progress, 100, percent, false)

                    // Get top 3 categories by spending for current cycle
                    if (activeCycle != null) {
                        val breakdown = transactionDao.getCategoryBreakdown(activeCycle.id)
                        val top3 = breakdown.take(3)
                        val cat1Text = if (top3.size > 0) "${top3[0].category.emoji} ${top3[0].category.displayName}  ₹${formatAmount(top3[0].total)}" else ""
                        val cat2Text = if (top3.size > 1) "${top3[1].category.emoji} ${top3[1].category.displayName}  ₹${formatAmount(top3[1].total)}" else ""
                        val cat3Text = if (top3.size > 2) "${top3[2].category.emoji} ${top3[2].category.displayName}  ₹${formatAmount(top3[2].total)}" else ""

                        mediumViews.setTextViewText(R.id.medium_cat1, cat1Text)
                        mediumViews.setTextViewText(R.id.medium_cat2, cat2Text)
                        mediumViews.setTextViewText(R.id.medium_cat3, cat3Text)
                    } else {
                        mediumViews.setTextViewText(R.id.medium_cat1, "")
                        mediumViews.setTextViewText(R.id.medium_cat2, "")
                        mediumViews.setTextViewText(R.id.medium_cat3, "")
                    }
                    appWidgetManager.updateAppWidget(mediumWidgetIds, mediumViews)
                }

                // 4. Update Lend & Borrow Widget
                val lendWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, LendBorrowWidgetProvider::class.java)
                )
                if (lendWidgetIds.isNotEmpty()) {
                    val lendViews = RemoteViews(context.packageName, R.layout.widget_lend)
                    val now = System.currentTimeMillis()
                    val sevenDays = now + 7L * 24 * 60 * 60 * 1000
                    val summary = lendBorrowDao.getSummary(now, sevenDays).first()

                    lendViews.setTextViewText(R.id.lend_widget_receivable, "₹${formatAmount(summary.totalOutstandingReceivable)}")

                    val overdueVal = summary.totalOverdueReceivable
                    lendViews.setTextViewText(R.id.lend_widget_overdue, "₹${formatAmount(overdueVal)}")
                    if (overdueVal > 0) {
                        lendViews.setTextColor(R.id.lend_widget_overdue, android.graphics.Color.parseColor("#FF3E3E"))
                    } else {
                        lendViews.setTextColor(R.id.lend_widget_overdue, android.graphics.Color.parseColor("#909090"))
                    }

                    // Find upcoming due record
                    val upcoming = lendBorrowDao.getUpcomingAndOverdueRecords().first()
                    val nextRecord = upcoming.firstOrNull { it.status != LendBorrowStatus.COMPLETED && it.dueDate != null }
                    val nextDueText = if (nextRecord != null) {
                        val days = nextRecord.daysUntilDue ?: 0L
                        val prefix = if (nextRecord.type == LendBorrowType.LENT) "Receive" else "Pay"
                        val dueDayStr = when {
                            days < 0 -> "Overdue by ${-days}d"
                            days == 0L -> "Due Today"
                            days == 1L -> "Due Tomorrow"
                            else -> "Due in ${days}d"
                        }
                        "$prefix ₹${formatAmount(nextRecord.outstanding)} from ${nextRecord.contactName} ($dueDayStr)"
                    } else {
                        "No upcoming dues"
                    }
                    lendViews.setTextViewText(R.id.lend_widget_next_due, nextDueText)

                    appWidgetManager.updateAppWidget(lendWidgetIds, lendViews)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return when {
            amount >= 100_000 -> String.format(Locale.US, "%.1fL", amount / 100_000)
            amount >= 1_000 -> String.format(Locale.US, "%.1fK", amount / 1_000)
            else -> amount.toInt().toString()
        }
    }
}
