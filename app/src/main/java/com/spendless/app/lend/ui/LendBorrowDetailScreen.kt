package com.spendless.app.lend.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.core.data.database.entities.*
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LendBorrowDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContact: (String) -> Unit,
    viewModel: LendBorrowDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Navigate back when deleted
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    val record = state.record ?: return

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
                Text("Record Detail", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.deleteRecord() }) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Contact header
            item {
                ContactHeader(
                    record = record,
                    dateFormat = dateFormat,
                    onCall = { openDialer(context, record.contactPhone) },
                    onWhatsApp = { openWhatsApp(context, record.contactPhone, record) },
                    onViewContact = { onNavigateToContact(record.contactPhone) }
                )
            }

            // Outstanding amount card
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SpendLessCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("OUTSTANDING", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "₹${record.outstanding.toLong()}",
                                    style = MonoAmountLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ORIGINAL", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text("₹${record.amount.toLong()}", style = MonoAmount, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Paid ₹${record.totalPaid.toLong()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Progress bar
                        if (record.amount > 0) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (record.totalPaid / record.amount).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }

                        // Status chip
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = ChipShape
                        ) {
                            Text(
                                record.status.emoji + " " + record.status.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // Due date info
            record.dueDate?.let { due ->
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Event, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Due: ${dateFormat.format(Date(due))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        record.daysUntilDue?.let { days ->
                            Text(
                                " (${if (days < 0) "Overdue ${-days}d" else "${days}d remaining"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick action buttons
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (record.status != LendBorrowStatus.COMPLETED) {
                        ActionButton("+ Payment", Icons.Outlined.Add, Modifier.weight(1f)) {
                            viewModel.showPaymentSheet()
                        }
                        ActionButton("Mark Paid", Icons.Outlined.CheckCircle, Modifier.weight(1f)) {
                            viewModel.markAsPaid()
                        }
                        ActionButton("Extend", Icons.Outlined.Schedule, Modifier.weight(1f)) {
                            viewModel.showExtendSheet()
                        }
                    }
                }
            }

            // Payment history
            if (state.payments.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "PAYMENT HISTORY",
                        style = DotMatrixLabel.copy(letterSpacing = 2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(state.payments) { payment ->
                    PaymentHistoryRow(payment, dateFormat)
                }
            }

            // Notes
            if (!record.notes.isNullOrBlank()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("NOTES", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = SmallCardShape
                    ) {
                        Text(record.notes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Payment sheet
    if (state.showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hidePaymentSheet,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Record Payment", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Outstanding: ₹${record.outstanding.toLong()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = state.paymentInput,
                    onValueChange = viewModel::setPaymentAmount,
                    prefix = { Text("₹", style = MonoAmount, color = MaterialTheme.colorScheme.onBackground) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MonoAmount.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxWidth(),
                    colors = lendTextFieldColors(),
                    shape = SmallCardShape,
                    placeholder = { Text("Amount received", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                OutlinedTextField(
                    value = state.paymentNotes,
                    onValueChange = viewModel::setPaymentNotes,
                    placeholder = { Text("Optional notes", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = lendTextFieldColors(),
                    shape = SmallCardShape
                )

                Button(
                    onClick = viewModel::addPartialPayment,
                    enabled = state.paymentInput.isNotBlank() && !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Save Payment")
                }
            }
        }
    }

    // Extend due date sheet
    if (state.showExtendSheet) {
        var showDatePicker by remember { mutableStateOf(true) }
        if (showDatePicker) {
            SimpleDatePickerDialog(
                initialDateMs = record.dueDate ?: System.currentTimeMillis(),
                onDismiss = viewModel::hideExtendSheet,
                onDateSelected = { ms ->
                    viewModel.setExtendDate(ms)
                    viewModel.extendDueDate()
                    showDatePicker = false
                }
            )
        }
    }
}

@Composable
private fun ContactHeader(
    record: LendBorrowRecord,
    dateFormat: SimpleDateFormat,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onViewContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContactAvatar(record.contactName, 72.dp)
        Spacer(Modifier.height(12.dp))
        Text(record.contactName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (record.contactPhone.isNotBlank()) {
            Text(record.contactPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "Given on ${dateFormat.format(Date(record.givenDate))}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (record.contactPhone.isNotBlank()) {
                IconActionChip("Call", Icons.Outlined.Call, onCall)
                IconActionChip("WhatsApp", Icons.Outlined.Message, onWhatsApp)
            }
            IconActionChip("Profile", Icons.Outlined.Person, onViewContact)
        }
    }
}

@Composable
private fun IconActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = ChipShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = SmallCardShape
    ) {
        Icon(icon, null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PaymentHistoryRow(payment: LendBorrowPayment, dateFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Payment received", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            Text(dateFormat.format(Date(payment.paymentDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("₹${payment.amount.toLong()}", style = MonoAmount.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 20.dp))
}
