package com.spendless.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.spendless.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

/**
 * Budget Widget (4×2) — Shows circular ring, spent/budget, remaining balance.
 */
@AndroidEntryPoint
class BudgetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateUtil.updateAllWidgets(context)
    }
}

/**
 * Compact Widget (2×2) — Minimal ring + remaining amount.
 */
@AndroidEntryPoint
class CompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateUtil.updateAllWidgets(context)
    }
}

/**
 * Medium Widget (4×3) — Top categories + remaining budget.
 */
@AndroidEntryPoint
class MediumWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateUtil.updateAllWidgets(context)
    }
}
