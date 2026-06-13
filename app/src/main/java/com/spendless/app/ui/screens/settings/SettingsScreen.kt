package com.spendless.app.ui.screens.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.ui.components.SpendLessCard
import com.spendless.app.ui.theme.*
import com.spendless.app.ui.screens.onboarding.ImportRangeDialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudget: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showRescanRangeDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.READ_SMS] ?: false
        viewModel.setSmsAutoImportEnabled(smsGranted)
        if (smsGranted) {
            showRescanRangeDialog = true
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Theme ──────────────────────────────────────────────────────────
            SettingsSectionHeader("APPEARANCE")
            SpendLessCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Theme Style", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(8.dp))
                        TactileSegmentedControl(
                            options = listOf("standard", "glass"),
                            selectedOption = uiState.themeStyle,
                            onOptionSelected = { viewModel.setThemeStyle(it) },
                            labelProvider = { if (it == "standard") "Standard" else "Glass" }
                        )
                    }

                    Column {
                        Text("Theme Mode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(8.dp))
                        TactileSegmentedControl(
                            options = listOf("light", "dark", "system"),
                            selectedOption = uiState.themeMode,
                            onOptionSelected = { viewModel.setThemeMode(it) },
                            labelProvider = {
                                when (it) {
                                    "light" -> "Light"
                                    "dark" -> "Dark"
                                    else -> "System"
                                }
                            }
                        )
                    }
                }
            }

            // ── App Identity ───────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionHeader("APP IDENTITY")
            SettingsRow(
                icon = Icons.Outlined.Edit,
                title = "App Name",
                subtitle = uiState.appName,
                onClick = { showRenameDialog = true }
            )
            SettingsRow(
                icon = Icons.Outlined.AppShortcut,
                title = "App Icon",
                subtitle = "Choose launcher icon",
                onClick = { showIconPicker = true }
            )
            SettingsRow(
                icon = Icons.Outlined.Category,
                title = "Categories",
                subtitle = "Manage custom categories",
                onClick = onNavigateToCategories
            )
            SettingsRow(
                icon = Icons.Outlined.Wallet,
                title = "Budget",
                subtitle = "₹${uiState.currentBudget.toInt()} · Resets on day ${uiState.resetDay}",
                onClick = onNavigateToBudget
            )

            // ── Security ───────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionHeader("SECURITY")
            SpendLessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Fingerprint, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Lock", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Lock app on background", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = viewModel::setBiometricEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // ── Notifications ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionHeader("NOTIFICATIONS")
            SpendLessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Budget Alerts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("50%, 75%, 90%, 100% alerts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // ── Data ───────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionHeader("DATA")
            SpendLessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Sms, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMS Auto-Import", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Scan bank SMS for transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = uiState.smsAutoImportEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                smsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            } else {
                                viewModel.setSmsAutoImportEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            SettingsRow(
                icon = Icons.Outlined.Refresh,
                title = "Re-scan SMS",
                subtitle = if (uiState.isRescanning)
                    "Scanning... ${uiState.rescanProgress}%"
                else
                    "Import new transactions from SMS",
                onClick = {
                    if (!uiState.isRescanning) {
                        val hasSms = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasSms) {
                            showRescanRangeDialog = true
                        } else {
                            smsPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            )
                        }
                    }
                }
            )
            SettingsRow(
                icon = Icons.Outlined.Backup,
                title = "Backup & Restore",
                subtitle = "Export / import your data",
                onClick = { /* TODO: Phase 2 */ }
            )

            // ── About ──────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionHeader("ABOUT")
            SpendLessCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("SpendLess", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("v1.0.0 · Privacy-first · Offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(uiState.appName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Rename App", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setAppName(nameInput)
                    showRenameDialog = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Icon picker dialog
    if (showIconPicker) {
        val icons = listOf(
            "default" to "Default",
            "minimal" to "Minimal",
            "dotmatrix" to "Dot Matrix"
        )
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Choose App Icon", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    icons.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActiveIcon(key)
                                    applyIconAlias(context, key)
                                    showIconPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.activeIcon == key,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPicker = false }) {
                    Text("Done", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showRescanRangeDialog) {
        ImportRangeDialog(
            onDismiss = { showRescanRangeDialog = false },
            onConfirm = { startDateMs ->
                showRescanRangeDialog = false
                viewModel.startRescan(startDateMs)
            }
        )
    }
}

private fun applyIconAlias(context: android.content.Context, iconKey: String) {
    val pm = context.packageManager
    val packageName = context.packageName

    val aliases = mapOf(
        "default" to "$packageName.MainActivityDefault",
        "minimal" to "$packageName.MainActivityMinimal",
        "dotmatrix" to "$packageName.MainActivityDotMatrix"
    )

    // Disable all aliases first
    aliases.values.forEach { alias ->
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(packageName, alias),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    // Enable selected alias
    val selectedAlias = aliases[iconKey] ?: return
    runCatching {
        pm.setComponentEnabledSetting(
            ComponentName(packageName, selectedAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 3.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SpendLessCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun <T> TactileSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(option),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
