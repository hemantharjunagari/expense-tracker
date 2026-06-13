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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.core.data.database.entities.LendBorrowType
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddLendBorrowScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddLendBorrowViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showGivenDatePicker by remember { mutableStateOf(false) }

    // Navigate back on success
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    val typeColor = if (state.type == LendBorrowType.LENT) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
    val typeLabel = if (state.type == LendBorrowType.LENT) "Money Lent" else "Money Borrowed"
    val typeIcon = if (state.type == LendBorrowType.LENT) "↑" else "↓"

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
                    Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "$typeIcon  $typeLabel",
                    style = MaterialTheme.typography.titleLarge,
                    color = typeColor
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Record", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Duplicate warning
            state.duplicateRecord?.let { dup ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = SmallCardShape
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${dup.contactName} already has an active record of ₹${dup.outstanding.toLong()}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Contact name field with suggestions
            item {
                Column {
                    FormLabel("CONTACT NAME")
                    OutlinedTextField(
                        value = state.contactName,
                        onValueChange = viewModel::setContactName,
                        placeholder = { Text("Start typing a name...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = lendTextFieldColors(),
                        shape = SmallCardShape
                    )
                    // Contact suggestions dropdown
                    if (state.contactSuggestions.isNotEmpty()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = SmallCardShape) {
                            Column {
                                state.contactSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectContact(suggestion) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ContactAvatar(suggestion.name, 32.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(suggestion.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                                            Text(suggestion.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Phone number
            item {
                FormLabel("PHONE NUMBER (OPTIONAL)")
                OutlinedTextField(
                    value = state.contactPhone,
                    onValueChange = viewModel::setContactPhone,
                    placeholder = { Text("+91 98765 43210", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = lendTextFieldColors(),
                    shape = SmallCardShape
                )
            }

            // Amount
            item {
                FormLabel("AMOUNT")
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MonoAmountLarge) },
                    prefix = { Text("₹", style = MonoAmountLarge, color = MaterialTheme.colorScheme.onBackground) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MonoAmountLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxWidth(),
                    colors = lendTextFieldColors(),
                    shape = SmallCardShape
                )
            }

            // Dates row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        FormLabel("DATE GIVEN")
                        OutlinedButton(
                            onClick = { showGivenDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = SmallCardShape
                        ) {
                            Icon(Icons.Outlined.CalendarToday, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(dateFormat.format(Date(state.givenDate)), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        FormLabel("DUE DATE")
                        OutlinedButton(
                            onClick = { showDueDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (state.dueDate != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = SmallCardShape
                        ) {
                            Icon(Icons.Outlined.Event, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                state.dueDate?.let { dateFormat.format(Date(it)) } ?: "No due date",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Optional fields
            item {
                FormLabel("NOTES (OPTIONAL)")
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    placeholder = { Text("Add a note...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = lendTextFieldColors(),
                    shape = SmallCardShape
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        FormLabel("UPI ID (OPTIONAL)")
                        OutlinedTextField(
                            value = state.upiId,
                            onValueChange = viewModel::setUpiId,
                            placeholder = { Text("user@upi", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = lendTextFieldColors(),
                            shape = SmallCardShape
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FormLabel("INTEREST %/mo")
                        OutlinedTextField(
                            value = state.interestRate,
                            onValueChange = viewModel::setInterestRate,
                            placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = lendTextFieldColors(),
                            shape = SmallCardShape
                        )
                    }
                }
            }
        }
    }

    // Date pickers using Material 3 DatePickerDialog
    if (showGivenDatePicker) {
        SimpleDatePickerDialog(
            initialDateMs = state.givenDate,
            onDismiss = { showGivenDatePicker = false },
            onDateSelected = { ms ->
                viewModel.setGivenDate(ms)
                showGivenDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        SimpleDatePickerDialog(
            initialDateMs = state.dueDate ?: System.currentTimeMillis(),
            onDismiss = { showDueDatePicker = false },
            onDateSelected = { ms ->
                viewModel.setDueDate(ms)
                showDueDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDateMs: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMs)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
            }) { Text("OK", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                navigationContentColor = MaterialTheme.colorScheme.onSurface,
                yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                currentYearContentColor = MaterialTheme.colorScheme.onSurface,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(state.hour, state.minute)
                }
            ) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        style = DotMatrixLabel.copy(fontSize = 9.sp, letterSpacing = 2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun lendTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)
