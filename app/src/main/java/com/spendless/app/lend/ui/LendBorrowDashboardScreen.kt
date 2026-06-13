package com.spendless.app.lend.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.spendless.app.core.data.database.entities.*
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*

@Composable
fun LendBorrowDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdd: (LendBorrowType) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToContact: (String) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: LendBorrowDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts(context)
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { onNavigateToAdd(LendBorrowType.BORROWED) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Outlined.ArrowDownward, "Add Borrowed", Modifier.size(18.dp))
                }
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToAdd(LendBorrowType.LENT) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Outlined.ArrowUpward, null) },
                    text = { Text("Lend Money", style = MaterialTheme.typography.labelLarge) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                LendBorrowHeader(
                    onNavigateBack = onNavigateBack,
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    onSyncClick = {
                        contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                )
            }

            // Summary row — 3 key metrics spaced evenly, no horizontal scrolling
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val net = state.summary.totalOutstandingReceivable - state.summary.totalOutstandingPayable
                    val netLong = net.toLong()
                    val (labelText, amountText, color) = when {
                        netLong > 0 -> Triple("NET (PROFIT)", "+₹$netLong", androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        netLong < 0 -> Triple("NET (DEBT)", "-₹${kotlin.math.abs(netLong)}", androidx.compose.ui.graphics.Color(0xFFE53935))
                        else -> Triple("NET", "₹0", MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    LendMetricCard("LENT", state.summary.totalLent, "↑", MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    LendMetricCard("BORROWED", state.summary.totalBorrowed, "↓", MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    LendMetricCard(
                        label = labelText,
                        amount = net,
                        icon = if (netLong >= 0) "📥" else "📤",
                        textColor = color,
                        displayAmountText = amountText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recovery ring
            item {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    SpendLessCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressRing(
                                percent = state.recoveryRate,
                                size = 100.dp,
                                strokeWidth = 8.dp
                            ) {
                                Text(
                                    "${state.recoveryRate.toInt()}%",
                                    style = MonoAmount.copy(fontSize = 16.sp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text(
                                    "Recovery Rate",
                                    style = DotMatrixLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "₹${state.totalRecovered.toLong()} recovered",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "${state.activeCount} active loan(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (state.summary.totalOverdueReceivable > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "⛔ ₹${state.summary.totalOverdueReceivable.toLong()} overdue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Overdue section
            if (state.overdueRecords.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("OVERDUE & DUE TODAY", state.overdueRecords.size)
                }
                items(state.overdueRecords) { record ->
                    LendRecordCard(
                        record = record,
                        onTap = { onNavigateToDetail(record.id) },
                        onCall = { openDialer(context, record.contactPhone) },
                        onWhatsApp = { openWhatsApp(context, record.contactPhone, record) }
                    )
                }
            }

            // Upcoming
            if (state.upcomingRecords.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("UPCOMING", state.upcomingRecords.size)
                }
                items(state.upcomingRecords) { record ->
                    LendRecordCard(
                        record = record,
                        onTap = { onNavigateToDetail(record.id) },
                        onCall = { openDialer(context, record.contactPhone) },
                        onWhatsApp = { openWhatsApp(context, record.contactPhone, record) }
                    )
                }
            }

            // Top contacts
            if (state.topContacts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("TOP CONTACTS", null)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.topContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToContact(contact.phone) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(name = contact.name, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                "${contact.recordCount} loan(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val outstandingLong = contact.outstanding.toLong()
                        val (text, color) = when {
                            outstandingLong > 0 -> "+₹$outstandingLong" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            outstandingLong < 0 -> "-₹${kotlin.math.abs(outstandingLong)}" to androidx.compose.ui.graphics.Color(0xFFE53935)
                            else -> "₹0" to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = text,
                            style = MonoAmount.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp))
                }
            }

            // Empty state
            if (!state.isLoading && state.activeCount == 0) {
                item {
                    EmptyLendState()
                }
            }
        }
    }
}

@Composable
private fun LendBorrowHeader(
    onNavigateBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "LENT & BORROWED",
                style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Debt Manager",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onSyncClick) {
            Icon(Icons.Outlined.Sync, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onNavigateToAnalytics) {
            Icon(Icons.Outlined.BarChart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LendMetricCard(
    label: String,
    amount: Double,
    icon: String,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    displayAmountText: String? = null
) {
    SpendLessCard(modifier = modifier) {
        Text(label, style = DotMatrixLabel.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(icon, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                displayAmountText ?: "₹${amount.toLong()}",
                style = MonoAmount.copy(fontSize = 16.sp),
                color = textColor
            )
        }
    }
}

@Composable
fun LendRecordCard(
    record: LendBorrowRecord,
    onTap: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type indicator
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (record.type == LendBorrowType.LENT) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (record.type == LendBorrowType.LENT) "↑" else "↓",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                record.contactName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.status.emoji + " " + record.status.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.daysUntilDue?.let { days ->
                    Text(
                        " · ${if (days < 0) "Overdue ${-days}d" else "Due in ${days}d"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "₹${record.outstanding.toLong()}",
                style = MonoAmount.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row {
                IconButton(onClick = onCall, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Call, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onWhatsApp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Message, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
fun ContactAvatar(name: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionLabel(title: String, count: Int?) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        count?.let {
            Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EmptyLendState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤝", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("No active loans", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tap + to record money lent or borrowed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Intent helpers ──────────────────────────────────────────────────────────────

fun openDialer(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

fun openWhatsApp(context: Context, phone: String, record: LendBorrowRecord) {
    val normalizedPhone = phone.removePrefix("+").removePrefix("91").removePrefix("0")
    val typeLabel = if (record.type == LendBorrowType.LENT) "lent" else "borrowed"
    val duePart = record.dueDate?.let {
        val fmt = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        " on ${fmt.format(java.util.Date(it))}"
    } ?: ""
    val message = "Hi ${record.contactName}, just a friendly reminder that ₹${record.outstanding.toLong()} " +
        "($typeLabel) is due$duePart. Please let me know when you can transfer. Thanks! 🙏"

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/91$normalizedPhone?text=${Uri.encode(message)}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp not installed — fallback to share
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(share, "Send reminder via"))
    }
}
