package com.spendless.app.lend.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.spendless.app.R

import com.spendless.app.widget.WidgetUpdateUtil

/**
 * Lent & Borrowed home screen widget (4x2).
 * Shows outstanding receivables and overdue amounts.
 */
class LendBorrowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateUtil.updateAllWidgets(context)
    }
}
