package com.spendless.app.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.ui.components.CircularProgressRing
import com.spendless.app.ui.components.DotMatrixBackground
import com.spendless.app.ui.components.DotMatrixLoader
import com.spendless.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var showImportRangeDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        viewModel.onSmsPermissionResult(smsGranted)
        if (smsGranted) {
            scope.launch {
                pagerState.animateScrollToPage(2)
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Dot matrix background decoration
        DotMatrixBackground(
            modifier = Modifier.fillMaxSize().alpha(0.06f),
            dotColor = MaterialTheme.colorScheme.onBackground
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PermissionsPage(
                    onEnableAutoTrack = {
                        val perms = mutableListOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_CONTACTS
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        smsPermissionLauncher.launch(perms.toTypedArray())
                    },
                    onSelectManualMode = {
                        viewModel.setSmsAutoImportEnabled(false)
                        scope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    }
                )
                2 -> BudgetSetupPage(
                    budgetAmount = uiState.budgetAmount,
                    resetDay = uiState.resetDay,
                    onBudgetChange = viewModel::setBudgetAmount,
                    onResetDayChange = viewModel::setResetDay
                )
                3 -> ImportingPage(
                    progress = uiState.importProgress,
                    importedCount = uiState.importedCount,
                    totalCount = uiState.totalSmsCount
                )
            }
        }

        // Bottom navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val indicatorsCount = if (uiState.smsPermissionGranted) 4 else 3
                repeat(indicatorsCount) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            .animateContentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action button (visible on Welcome, Budget, Importing, or if SMS permission was granted)
            val showActionButton = pagerState.currentPage != 1 || uiState.smsPermissionGranted
            if (showActionButton) {
                Button(
                    onClick = {
                        scope.launch {
                            when (pagerState.currentPage) {
                                0 -> pagerState.animateScrollToPage(1)
                                1 -> {
                                    if (uiState.smsPermissionGranted) {
                                        pagerState.animateScrollToPage(2)
                                    }
                                }
                                2 -> {
                                    scope.launch {
                                        viewModel.saveBudget()
                                        if (uiState.smsPermissionGranted) {
                                            showImportRangeDialog = true
                                        } else {
                                            viewModel.completeOnboarding()
                                            onOnboardingComplete()
                                        }
                                    }
                                }
                                3 -> {
                                    if (uiState.importComplete) {
                                        viewModel.completeOnboarding()
                                        onOnboardingComplete()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    val label = when (pagerState.currentPage) {
                        0 -> "Get Started"
                        1 -> "Continue"
                        2 -> if (uiState.smsPermissionGranted) "Set Budget" else "Set Budget & Finish"
                        3 -> if (uiState.importComplete) "Start Tracking" else "Importing..."
                        else -> "Continue"
                    }
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (showImportRangeDialog) {
            ImportRangeDialog(
                onDismiss = { showImportRangeDialog = false },
                onConfirm = { startDateMs ->
                    showImportRangeDialog = false
                    scope.launch {
                        pagerState.animateScrollToPage(3)
                        viewModel.startHistoricalImport(startDateMs)
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 3 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // App logo - dot matrix circle
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressRing(
                        percent = 70f,
                        size = 100.dp,
                        strokeWidth = 6.dp
                    )
                    Text("₹", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "SpendLess",
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = SpaceMono),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your money.\nFinally, under control.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Feature highlights
                listOf(
                    "🔒" to "100% Private. Offline only.",
                    "📱" to "Reads your bank SMS automatically.",
                    "📊" to "Smart spending insights."
                ).forEach { (emoji, text) ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    onEnableAutoTrack: () -> Unit,
    onSelectManualMode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("01", style = DotMatrixLabel.copy(fontSize = 12.sp, letterSpacing = 4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tracking Setup",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select how you would like to track your expenses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Choice 1: Auto-Track via SMS
        Surface(
            onClick = onEnableAutoTrack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = SmallCardShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic tracking",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reads transaction SMS messages automatically. Keeps data 100% offline & secure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Choice 2: Manual Mode Only
        Surface(
            onClick = onSelectManualMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = SmallCardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📝", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manual mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter transactions manually. Requests no device permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetSetupPage(
    budgetAmount: String,
    resetDay: Int,
    onBudgetChange: (String) -> Unit,
    onResetDayChange: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Smoothly scroll to the selected resetDay when page loads or selected day changes
    LaunchedEffect(resetDay) {
        val targetIndex = (resetDay - 1 - 2).coerceAtLeast(0)
        listState.animateScrollToItem(targetIndex)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("02", style = DotMatrixLabel.copy(fontSize = 12.sp, letterSpacing = 4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set Your Budget",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Budget input
        OutlinedTextField(
            value = budgetAmount,
            onValueChange = onBudgetChange,
            label = { Text("Monthly Budget (₹)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            prefix = { Text("₹", style = MonoAmount, color = MaterialTheme.colorScheme.onBackground) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MonoAmountLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = SmallCardShape
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Budget resets on day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Reset day scrollable selector (options 1 to 31)
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(31) { idx ->
                val day = idx + 1
                val isSelected = day == resetDay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onResetDayChange(day) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = SpaceMono
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportingPage(
    progress: Int,
    importedCount: Int,
    totalCount: Int
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("03", style = DotMatrixLabel.copy(fontSize = 12.sp, letterSpacing = 4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Importing History",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressRing(
            percent = progress.toFloat(),
            size = 160.dp,
            strokeWidth = 10.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$progress%",
                    style = MonoAmountLarge.copy(fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (totalCount > 0)
                "Found $importedCount transactions from $totalCount messages"
            else
                "Scanning your messages...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        DotMatrixLoader()
    }
}

@Composable
fun ImportRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedOption by remember { mutableStateOf("ALL") }
    var customTimestamp by remember { mutableStateOf<Long?>(null) }
    var customDateLabel by remember { mutableStateOf("Select Custom Date") }
    var showDatePicker by remember { mutableStateOf(false) }

    val options = listOf(
        "ALL" to "All time",
        "LAST_MONTH" to "Last Month",
        "CURRENT_QUARTER" to "Current Quarter",
        "LAST_QUARTER" to "Last Quarter",
        "CUSTOM" to customDateLabel
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Import Date Range", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Choose how far back to scan your SMS history for bank transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                options.forEach { (optionKey, optionLabel) ->
                    val isSelected = selectedOption == optionKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (optionKey == "CUSTOM") {
                                    showDatePicker = true
                                } else {
                                    selectedOption = optionKey
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (optionKey == "CUSTOM" && customTimestamp != null) "Custom: $optionLabel" else optionLabel,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cal = java.util.Calendar.getInstance()
                    val startMs = when (selectedOption) {
                        "ALL" -> 0L
                        "LAST_MONTH" -> {
                            cal.add(java.util.Calendar.MONTH, -1)
                            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                        "CURRENT_QUARTER" -> {
                            val currentMonth = cal.get(java.util.Calendar.MONTH)
                            val quarterStartMonth = (currentMonth / 3) * 3
                            cal.set(java.util.Calendar.MONTH, quarterStartMonth)
                            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                        "LAST_QUARTER" -> {
                            val currentMonth = cal.get(java.util.Calendar.MONTH)
                            val thisQuarterStartMonth = (currentMonth / 3) * 3
                            cal.set(java.util.Calendar.MONTH, thisQuarterStartMonth)
                            cal.add(java.util.Calendar.MONTH, -3)
                            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }
                        "CUSTOM" -> customTimestamp ?: 0L
                        else -> 0L
                    }
                    onConfirm(startMs)
                }
            ) {
                Text("Import", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showDatePicker) {
        com.spendless.app.lend.ui.SimpleDatePickerDialog(
            initialDateMs = customTimestamp ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { ms ->
                customTimestamp = ms
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
                val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val month = cal.get(java.util.Calendar.MONTH)
                val year = cal.get(java.util.Calendar.YEAR)
                customDateLabel = "${dayOfMonth}/${month + 1}/${year}"
                selectedOption = "CUSTOM"
                showDatePicker = false
            }
        )
    }
}
