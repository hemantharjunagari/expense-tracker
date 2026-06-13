package com.spendless.app.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spendless.app.core.analytics.AnalyticsEngine
import com.spendless.app.ui.components.DotMatrixLoader
import com.spendless.app.ui.theme.DotMatrixLabel
import com.spendless.app.ui.theme.MonoAmount
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackingDialog(
    uiState: DashboardUiState,
    onDismiss: () -> Unit,
    onPeriodSelected: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf("Trend") } // "Trend", "Daily", "Calendar"
    val tabs = listOf("Trend", "Daily", "Calendar")
    var showPeriodDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVITY TRACKING",
                            style = DotMatrixLabel.copy(fontSize = 11.sp, letterSpacing = 2.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showPeriodDropdown = true }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = uiState.trackingPeriod,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "Select Period",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showPeriodDropdown,
                                onDismissRequest = { showPeriodDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                listOf("Current Cycle", "Current Month", "Last 30 Days").forEach { period ->
                                    DropdownMenuItem(
                                        text = { Text(period, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            onPeriodSelected(period)
                                            showPeriodDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Dialog",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Smooth Sliding Segmented Control Toggle
                SlidingTactileSegmentedControl(
                    options = tabs,
                    selectedOption = currentTab,
                    onOptionSelected = { currentTab = it },
                    labelProvider = { it }
                )

                // Content View with Smooth Horizontal Sliding Transitions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.trackingIsLoading) {
                        DotMatrixLoader()
                    } else if (uiState.trackingData.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📊", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No transactions found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val targetIndex = tabs.indexOf(targetState)
                                val initialIndex = tabs.indexOf(initialState)
                                val slideDirection = if (targetIndex > initialIndex) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                                slideIntoContainer(slideDirection, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) togetherWith
                                        slideOutOfContainer(slideDirection, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                            },
                            label = "TrackingContentTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { tab ->
                            when (tab) {
                                "Trend" -> TrendView(uiState.trackingData)
                                "Daily" -> DailyBarView(uiState.trackingData)
                                "Calendar" -> CalendarView(uiState.trackingData)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SlidingTactileSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
    ) {
        val width = maxWidth
        val tabWidth = width / options.size
        
        val animatedOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "SegmentOffset"
        )
        
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(tabWidth)
                .height(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOptionSelected(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(option),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendView(data: List<AnalyticsEngine.DailyIncomeExpense>) {
    val sortedData = data.sortedBy { it.dateMs }
    var cumulativeExpense = 0.0
    var cumulativeIncome = 0.0
    val points = sortedData.map { dayData ->
        cumulativeExpense += dayData.expense
        cumulativeIncome += dayData.income
        dayData.dateMs to (cumulativeExpense to cumulativeIncome)
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Legend indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp, 3.dp).background(Color(0xFFFF3B30), RoundedCornerShape(1.dp)))
                Spacer(Modifier.width(6.dp))
                Text("Expenses", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp, 3.dp).background(Color(0xFF34C759), RoundedCornerShape(1.dp)))
                Spacer(Modifier.width(6.dp))
                Text("Income", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val paddingLeft = 100f
                val paddingRight = 30f
                val paddingTop = 20f
                val paddingBottom = 60f

                val graphWidth = w - paddingLeft - paddingRight
                val graphHeight = h - paddingTop - paddingBottom
                
                val gridStroke = 1.dp.toPx()
                
                // Y-Axis Labels Paint
                val textPaint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 8.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    typeface = android.graphics.Typeface.DEFAULT
                }
                
                // X-Axis Labels Paint
                val xLabelPaint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 8.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT
                }

                val trendDateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                val textHeightOffset = -(textPaint.ascent() + textPaint.descent()) / 2

                if (points.isNotEmpty()) {
                    val maxAmount = points.maxOf { maxOf(it.second.first, it.second.second) }.coerceAtLeast(1.0)
                    
                    // Draw horizontal gridlines and Y-axis scale labels
                    for (i in 0..4) {
                        val y = paddingTop + i * graphHeight / 4
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(w - paddingRight, y),
                            strokeWidth = gridStroke
                        )
                        
                        val value = maxAmount * (4 - i) / 4
                        val label = "₹${formatCompact(value)}"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            paddingLeft - 15f,
                            y + textHeightOffset,
                            textPaint
                        )
                    }

                    val xStep = graphWidth / (points.size - 1).coerceAtLeast(1)
                    val expensePath = Path()
                    val incomePath = Path()
                    val expenseFillPath = Path()
                    val incomeFillPath = Path()
                    
                    points.forEachIndexed { index, (_, values) ->
                        val (exp, inc) = values
                        val x = paddingLeft + index * xStep
                        val yExp = paddingTop + graphHeight - (exp / maxAmount).toFloat() * graphHeight
                        val yInc = paddingTop + graphHeight - (inc / maxAmount).toFloat() * graphHeight
                        
                        if (index == 0) {
                            expensePath.moveTo(x, yExp)
                            incomePath.moveTo(x, yInc)
                            
                            expenseFillPath.moveTo(x, paddingTop + graphHeight)
                            expenseFillPath.lineTo(x, yExp)
                            
                            incomeFillPath.moveTo(x, paddingTop + graphHeight)
                            incomeFillPath.lineTo(x, yInc)
                        } else {
                            expensePath.lineTo(x, yExp)
                            incomePath.lineTo(x, yInc)
                            
                            expenseFillPath.lineTo(x, yExp)
                            incomeFillPath.lineTo(x, yInc)
                        }
                        
                        if (index == points.lastIndex) {
                            expenseFillPath.lineTo(x, paddingTop + graphHeight)
                            expenseFillPath.close()
                            
                            incomeFillPath.lineTo(x, paddingTop + graphHeight)
                            incomeFillPath.close()
                        }
                    }
                    
                    // Draw filled area with gradient
                    drawPath(
                        path = expenseFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF3B30).copy(alpha = 0.12f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + graphHeight
                        )
                    )
                    drawPath(
                        path = incomeFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF34C759).copy(alpha = 0.12f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + graphHeight
                        )
                    )
                    
                    // Draw lines
                    drawPath(path = expensePath, color = Color(0xFFFF3B30), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(path = incomePath, color = Color(0xFF34C759), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // Draw X-axis labels at Start, Mid, and End
                    val labelIndices = if (points.size >= 3) {
                        listOf(0, points.size / 2, points.size - 1)
                    } else {
                        points.indices.toList()
                    }
                    
                    labelIndices.forEach { index ->
                        val (dateMs, _) = points[index]
                        val x = paddingLeft + index * xStep
                        val label = trendDateFormat.format(Date(dateMs))
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            paddingTop + graphHeight + 35f,
                            xLabelPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBarView(data: List<AnalyticsEngine.DailyIncomeExpense>) {
    val sortedData = data.sortedBy { it.dateMs }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        val w = size.width
        val h = size.height
        val paddingLeft = 100f
        val paddingRight = 30f
        val paddingTop = 20f
        val paddingBottom = 60f

        val graphWidth = w - paddingLeft - paddingRight
        val graphHeight = h - paddingTop - paddingBottom
        
        val gridStroke = 1.dp.toPx()
        
        // Y-Axis Labels Paint
        val textPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.DEFAULT
        }
        
        // X-Axis Labels Paint
        val xLabelPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }

        val textHeightOffset = -(textPaint.ascent() + textPaint.descent()) / 2

        if (sortedData.isNotEmpty()) {
            val maxExpense = sortedData.maxOf { it.expense }.coerceAtLeast(1.0)
            
            // Draw horizontal gridlines and Y-axis scale labels
            for (i in 0..4) {
                val y = paddingTop + i * graphHeight / 4
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(w - paddingRight, y),
                    strokeWidth = gridStroke
                )
                
                val value = maxExpense * (4 - i) / 4
                val label = "₹${formatCompact(value)}"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    paddingLeft - 15f,
                    y + textHeightOffset,
                    textPaint
                )
            }

            // Draw baseline (X-axis line)
            drawLine(
                color = labelColor.copy(alpha = 0.25f),
                start = Offset(paddingLeft, paddingTop + graphHeight),
                end = Offset(w - paddingRight, paddingTop + graphHeight),
                strokeWidth = 1.dp.toPx()
            )

            val barCount = sortedData.size
            val xStep = graphWidth / barCount
            val barWidth = (xStep * 0.6f).coerceAtLeast(4f)
            
            val dailyDateFormat = SimpleDateFormat("d", Locale.getDefault())
            
            // Determine label step to avoid overlapping on small screen sizes/large counts
            val labelInterval = when {
                xStep >= 20.dp.toPx() -> 1
                xStep >= 10.dp.toPx() -> 2
                else -> 5
            }
            
            val labelTextSize = if (barCount > 15) 7.5.sp else 8.5.sp
            xLabelPaint.textSize = labelTextSize.toPx()

            sortedData.forEachIndexed { index, dayData ->
                val exp = dayData.expense
                val xCenter = paddingLeft + index * xStep + xStep / 2
                
                // Draw a small tick on baseline
                drawLine(
                    color = labelColor.copy(alpha = 0.2f),
                    start = Offset(xCenter, paddingTop + graphHeight),
                    end = Offset(xCenter, paddingTop + graphHeight + 6f),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw bar if expense > 0
                val barHeight = (exp / maxExpense).toFloat() * graphHeight
                val y = paddingTop + graphHeight - barHeight
                val xBarLeft = xCenter - barWidth / 2
                
                if (exp > 0) {
                    drawRoundRect(
                        color = Color(0xFFFF3B30),
                        topLeft = Offset(xBarLeft, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
                
                // Draw X-axis label (day of month) for chosen intervals
                if (index % labelInterval == 0) {
                    val dayStr = dailyDateFormat.format(Date(dayData.dateMs))
                    drawContext.canvas.nativeCanvas.drawText(
                        dayStr,
                        xCenter,
                        paddingTop + graphHeight + 30f,
                        xLabelPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarView(data: List<AnalyticsEngine.DailyIncomeExpense>) {
    val tempCal = Calendar.getInstance()
    // Group day data by (Year, Month)
    val monthsData = data.groupBy { dayData ->
        tempCal.timeInMillis = dayData.dateMs
        tempCal.get(Calendar.YEAR) to tempCal.get(Calendar.MONTH)
    }.toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

    val dataByDay = data.associateBy { dayData ->
        val cal = Calendar.getInstance().apply { timeInMillis = dayData.dateMs }
        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp) // Spacing between different months
    ) {
        if (monthsData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data to display in calendar", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            monthsData.forEach { (yearMonth, _) ->
                val (year, month) = yearMonth
                
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
                val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon, ...
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                // Construct grid cells
                val totalCells = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7
                val daysList = mutableListOf<Int?>()
                for (i in 1 until firstDayOfWeek) {
                    daysList.add(null)
                }
                for (day in 1..daysInMonth) {
                    daysList.add(day)
                }
                while (daysList.size < totalCells) {
                    daysList.add(null)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month name header label
                    Text(
                        text = "$monthName $year".uppercase(),
                        style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // Weekday Row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Calendar grid cells
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        daysList.chunked(7).forEach { weekDays ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                weekDays.forEach { day ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .border(
                                                width = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
                                            )
                                            .padding(1.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        if (day != null) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxHeight()
                                            ) {
                                                Text(
                                                    text = day.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                
                                                val dayKey = "$year-$month-$day"
                                                val dayData = dataByDay[dayKey]
                                                
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    if (dayData != null && dayData.expense > 0) {
                                                        Text(
                                                            text = "-${formatCompact(dayData.expense)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                                                            color = Color(0xFFFF3B30),
                                                            maxLines = 1
                                                        )
                                                    }
                                                    if (dayData != null && dayData.income > 0) {
                                                        Text(
                                                            text = "+${formatCompact(dayData.income)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                                                            color = Color(0xFF34C759),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompact(amount: Double): String {
    return when {
        amount >= 100_000 -> String.format(Locale.US, "%.0fL", amount / 100_000)
        amount >= 1_000 -> String.format(Locale.US, "%.1fK", amount / 1_000).replace(".0", "")
        else -> amount.toInt().toString()
    }
}
