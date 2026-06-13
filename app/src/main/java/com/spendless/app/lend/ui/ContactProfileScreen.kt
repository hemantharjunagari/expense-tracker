package com.spendless.app.lend.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun ContactProfileScreenContent(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ContactProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val contact = state.contact
    val contactName = contact?.name ?: state.records.firstOrNull()?.contactName ?: "Unknown"
    val contactPhone = contact?.phone ?: state.records.firstOrNull()?.contactPhone ?: ""

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
                Text("Contact Profile", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Profile header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContactAvatar(contactName, 80.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(contactName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    if (contactPhone.isNotBlank()) {
                        Text(contactPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (contactPhone.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { openDialer(context, contactPhone) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = ChipShape
                            ) {
                                Icon(Icons.Outlined.Call, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call")
                            }
                            OutlinedButton(
                                onClick = {
                                    state.records.firstOrNull()?.let {
                                        openWhatsApp(context, contactPhone, it)
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = ChipShape
                            ) {
                                Icon(Icons.Outlined.Message, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("WhatsApp")
                            }
                        }
                    }
                }
            }

            // Financial summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    contact?.let { c ->
                        SummaryTile("LENT", c.totalLent, Modifier.weight(1f))
                        SummaryTile("BORROWED", c.totalBorrowed, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    contact?.let { c ->
                        SummaryTile("RECEIVABLE", c.outstandingReceivable, Modifier.weight(1f))
                        SummaryTile("PAYABLE", c.outstandingPayable, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    contact?.let { c ->
                        val net = c.outstandingReceivable - c.outstandingPayable
                        NetSummaryTile("NET OUTSTANDING", net, Modifier.fillMaxWidth())
                    }
                }
            }

            // Transaction history
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "TRANSACTION HISTORY",
                    style = DotMatrixLabel.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.records.isEmpty()) {
                item {
                    Text(
                        "No records yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            } else {
                items(state.records) { record ->
                    LendRecordCard(
                        record = record,
                        onTap = { onNavigateToDetail(record.id) },
                        onCall = { openDialer(context, record.contactPhone) },
                        onWhatsApp = { openWhatsApp(context, record.contactPhone, record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, amount: Double, modifier: Modifier) {
    SpendLessCard(modifier = modifier) {
        Text(label, style = DotMatrixLabel.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text("₹${amount.toLong()}", style = MonoAmount.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun NetSummaryTile(label: String, amount: Double, modifier: Modifier) {
    val amountLong = amount.toLong()
    val (text, color) = when {
        amountLong > 0 -> "+₹$amountLong" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        amountLong < 0 -> "-₹${kotlin.math.abs(amountLong)}" to androidx.compose.ui.graphics.Color(0xFFE53935)
        else -> "₹0" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SpendLessCard(modifier = modifier) {
        Text(label, style = DotMatrixLabel.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MonoAmount.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = color)
    }
}
