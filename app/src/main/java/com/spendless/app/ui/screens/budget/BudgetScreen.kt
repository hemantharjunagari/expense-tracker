package com.spendless.app.ui.screens.budget

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val resetDayOptions = listOf(1, 5, 10, 15, 20, 21, 25, 28, 30, 31)

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
                Text("Budget", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
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
                if (uiState.isSaved) {
                    Text(
                        "✓ Budget saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = viewModel::saveBudget,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save Budget", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Budget amount input
            Column {
                Text("MONTHLY BUDGET", style = DotMatrixLabel.copy(letterSpacing = 2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.budgetInput,
                    onValueChange = viewModel::setBudgetInput,
                    prefix = { Text("₹", style = MonoAmountLarge, color = MaterialTheme.colorScheme.onBackground) },
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
                    shape = SmallCardShape,
                    placeholder = { Text("10,000", style = MonoAmountLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            // Financial Status Summary
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CYCLE SUMMARY", style = DotMatrixLabel.copy(letterSpacing = 2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "BUDGET USED",
                        amount = uiState.spentAmount,
                        modifier = Modifier.weight(1f)
                    )
                    val remaining = maxOf(0.0, (uiState.currentBudget) - uiState.spentAmount)
                    MetricCard(
                        label = "REMAINING",
                        amount = remaining,
                        isHighlighted = remaining > 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "PENDING REVIEW",
                        amount = uiState.pendingAmount,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "EXCLUDED",
                        amount = uiState.excludedAmount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Cycle preview
            SpendLessCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT CYCLE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${uiState.cycleStartLabel} → ${uiState.cycleEndLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Icon(Icons.Outlined.Loop, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Reset day selector
            Column {
                Text("RESET DATE", style = DotMatrixLabel.copy(letterSpacing = 2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Budget resets on this day each month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(100.dp)
                ) {
                    items(resetDayOptions.size) { idx ->
                        val day = resetDayOptions[idx]
                        FilterChip(
                            selected = day == uiState.resetDay,
                            onClick = { viewModel.setResetDay(day) },
                            label = {
                                Text(
                                    day.toString(),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Category budgets
            val categories = allCategories.filter { it.name != "INCOME" && it.name != "TRANSFER" && it.name != "SELF_TRANSFER" && it.name != "UNCATEGORIZED" }
            if (categories.isNotEmpty()) {
                Column {
                    Text("CATEGORY BUDGETS (OPTIONAL)", style = DotMatrixLabel.copy(letterSpacing = 2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    categories.forEach { category ->
                        CategoryBudgetRow(
                            category = category,
                            amount = uiState.categoryBudgets[category] ?: 0.0,
                            totalBudget = uiState.budgetInput.toDoubleOrNull() ?: 0.0,
                            onAmountChange = { viewModel.setCategoryBudget(category, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetRow(
    category: Category,
    amount: Double,
    totalBudget: Double,
    onAmountChange: (Double) -> Unit
) {
    var inputText by remember { mutableStateOf(if (amount > 0) amount.toInt().toString() else "") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            category.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it.filter { c -> c.isDigit() }
                onAmountChange(inputText.toDoubleOrNull() ?: 0.0)
            },
            prefix = { Text("₹", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.width(120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = SmallCardShape
        )
    }
}
