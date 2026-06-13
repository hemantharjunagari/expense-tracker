package com.spendless.app.ui.screens.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.core.data.database.entities.Transaction
import com.spendless.app.core.data.database.entities.TransactionStatus
import com.spendless.app.core.data.database.entities.PaymentMethod
import com.spendless.app.core.data.database.entities.TransactionType
import com.spendless.app.core.data.database.entities.TransactionSplit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.spendless.app.ui.components.*
import com.spendless.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.spendless.app.lend.ui.SimpleDatePickerDialog
import com.spendless.app.lend.ui.SimpleTimePickerDialog
import java.util.Calendar
import com.spendless.app.core.data.database.dao.DbMonthSummary
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.ContactsContract
import androidx.compose.material3.OutlinedIconButton

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val quickStats by viewModel.quickStats.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val transactions: LazyPagingItems<TransactionUiModel> =
        viewModel.transactions.collectAsLazyPagingItems()
    val currentRange by viewModel.currentRange.collectAsStateWithLifecycle()

    var showCategoryPicker by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAddManualTransaction by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showBulkCategoryPicker by remember { mutableStateOf(false) }
    var collapsedMonths by rememberSaveable { mutableStateOf(emptyList<String>()) }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showPeriodSheet by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showCategoryFilterSheet by remember { mutableStateOf(false) }
    var showAmountFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectionMode.isNotEmpty()) {
                SelectionTopBar(
                    selectedCount = selectionMode.size,
                    onClearSelection = viewModel::clearSelection,
                    onApprove = viewModel::bulkApprove,
                    onSelfTransfer = viewModel::bulkSelfTransfer,
                    onChangeCategoryClick = { showBulkCategoryPicker = true },
                    onIgnore = viewModel::bulkIgnore,
                    onDelete = viewModel::bulkDelete
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Text(
                            "Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        SortSelector(
                            selectedSort = filterState.sortBy,
                            onSortSelected = viewModel::setSortBy
                        )

                        if (filterState.searchQuery.isNotBlank() || 
                            filterState.selectedCategory != null ||
                            filterState.minAmount != null ||
                            filterState.maxAmount != null ||
                            filterState.status != null ||
                            filterState.paymentMethod != null ||
                            filterState.isManual != null ||
                            filterState.isExcludedFromBudget != null ||
                            filterState.isExcludedFromAnalytics != null ||
                            filterState.contactName != null ||
                            filterState.selectedMerchant != null ||
                            filterState.period != TransactionPeriod.CURRENT_CYCLE
                        ) {
                            TextButton(onClick = {
                                viewModel.clearFilters()
                                viewModel.setPeriod(TransactionPeriod.CURRENT_CYCLE)
                            }) {
                                Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                // TabRow for Transaction types/modes
                ScrollableTabRow(
                    selectedTabIndex = filterState.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[filterState.selectedTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    TransactionTab.entries.forEach { tab ->
                        Tab(
                            selected = filterState.selectedTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = { 
                                val tabName = when (tab) {
                                    TransactionTab.ALL -> "All"
                                    TransactionTab.PENDING -> "Pending"
                                    TransactionTab.EXPENSES -> "Expenses"
                                    TransactionTab.INCOME -> "Income"
                                    TransactionTab.SELF_TRANSFER -> "Self Transfer"
                                    TransactionTab.UNCATEGORIZED -> "Uncategorized"
                                }
                                Text(tabName, style = MaterialTheme.typography.labelMedium) 
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Stats Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = SmallCardShape
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TXNS", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${quickStats.count}", style = MonoAmount.copy(fontSize = 14.sp))
                        }
                        Column {
                            Text("EXPENSES", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", quickStats.expenses)}", style = MonoAmount.copy(fontSize = 14.sp), color = ColorNegative)
                        }
                        Column {
                            Text("INCOME", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", quickStats.income)}", style = MonoAmount.copy(fontSize = 14.sp), color = ColorPositive)
                        }
                        Column {
                            Text("PENDING", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${quickStats.pendingCount}", style = MonoAmount.copy(fontSize = 14.sp), color = ColorWarning)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Consolidated Zomato/Amazon style search and filters pill row
                if (isSearchExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = filterState.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text("Search merchants, notes...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (filterState.searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { isSearchExpanded = false }) {
                                        Icon(Icons.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = ChipShape
                        )
                    }
                } else {
                    val hasActiveAdvancedFilters = filterState.minAmount != null || 
                        filterState.maxAmount != null || 
                        filterState.status != null || 
                        filterState.paymentMethod != null || 
                        filterState.isManual != null || 
                        filterState.isExcludedFromBudget != null || 
                        filterState.isExcludedFromAnalytics != null || 
                        filterState.contactName != null || 
                        filterState.selectedMerchant != null

                    val isSearchActive = filterState.searchQuery.isNotEmpty()
                    val searchLabel = if (isSearchActive) "Search: \"${filterState.searchQuery}\"" else "Search"

                    val isPeriodActive = filterState.period != TransactionPeriod.CURRENT_CYCLE
                    val periodLabel = filterState.period.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() }

                    val isCategoryActive = filterState.selectedCategory != null
                    val categoryLabel = filterState.selectedCategory?.let { "${it.emoji} ${it.displayName}" } ?: "Category: All"

                    val minAmount = filterState.minAmount
                    val maxAmount = filterState.maxAmount
                    val isAmountActive = minAmount != null || maxAmount != null
                    val amountLabel = when {
                        minAmount != null && maxAmount != null -> "Amount: ₹${minAmount.toInt()} - ₹${maxAmount.toInt()}"
                        minAmount != null -> "Amount: >₹${minAmount.toInt()}"
                        maxAmount != null -> "Amount: <₹${maxAmount.toInt()}"
                        else -> "Amount: Any"
                    }

                    val isBudgetActive = filterState.isExcludedFromBudget != null
                    val budgetLabel = when (filterState.isExcludedFromBudget) {
                        null -> "Budget: Either"
                        false -> "Budget: Included"
                        true -> "Budget: Excluded"
                    }

                    val isAnalyticsActive = filterState.isExcludedFromAnalytics != null
                    val analyticsLabel = when (filterState.isExcludedFromAnalytics) {
                        null -> "Analytics: Either"
                        false -> "Analytics: Included"
                        true -> "Analytics: Excluded"
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Advanced Filters Pill
                        item {
                            FilterPill(
                                selected = hasActiveAdvancedFilters,
                                onClick = { showFilterSheet = true },
                                label = "Filters",
                                icon = {
                                    Icon(
                                        Icons.Outlined.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // 2. Search Pill
                        item {
                            FilterPill(
                                selected = isSearchActive,
                                onClick = { isSearchExpanded = true },
                                label = searchLabel,
                                icon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // 3. Date Range Pill
                        item {
                            FilterPill(
                                selected = isPeriodActive,
                                onClick = { showPeriodSheet = true },
                                label = periodLabel,
                                icon = {
                                    Icon(
                                        Icons.Outlined.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // 4. Category Pill
                        item {
                            FilterPill(
                                selected = isCategoryActive,
                                onClick = { showCategoryFilterSheet = true },
                                label = categoryLabel,
                                icon = {
                                    Icon(
                                        Icons.Outlined.Label,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // 5. Amount Pill
                        item {
                            FilterPill(
                                selected = isAmountActive,
                                onClick = { showAmountFilterSheet = true },
                                label = amountLabel,
                                icon = {
                                    Icon(
                                        Icons.Outlined.Payments,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // 6. In Budget Pill
                        item {
                            FilterPill(
                                selected = isBudgetActive,
                                onClick = {
                                    viewModel.updateFilters { it.copy(
                                        isExcludedFromBudget = when (it.isExcludedFromBudget) {
                                            null -> false
                                            false -> true
                                            true -> null
                                        }
                                    ) }
                                },
                                label = budgetLabel
                            )
                        }

                        // 7. In Analytics Pill
                        item {
                            FilterPill(
                                selected = isAnalyticsActive,
                                onClick = {
                                    viewModel.updateFilters { it.copy(
                                        isExcludedFromAnalytics = when (it.isExcludedFromAnalytics) {
                                            null -> false
                                            false -> true
                                            true -> null
                                        }
                                    ) }
                                },
                                label = analyticsLabel
                            )
                        }
                    }
                }

                if (transactions.itemCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Collapse All",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val allMonths = (0 until transactions.itemCount).mapNotNull { index ->
                                        when (val item = transactions[index]) {
                                            is TransactionUiModel.Header -> item.summary.monthYear
                                            else -> null
                                        }
                                    }
                                    collapsedMonths = allMonths
                                }
                            )
                            Text(
                                text = "Expand All",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    collapsedMonths = emptyList()
                                }
                            )
                        }
                    }
                }
            }
        }
    },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddManualTransaction = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, "Add Transaction")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(transactions.itemCount) { index ->
                when (val item = transactions[index]) {
                    is TransactionUiModel.Item -> {
                        val transaction = item.transaction
                        val month = getMonthYear(transaction.timestamp)
                        if (collapsedMonths.contains(month)) {
                            Spacer(modifier = Modifier.height(0.dp))
                        } else {
                            val isSelected = selectionMode.contains(transaction.id)
                            
                            Box(modifier = Modifier.combinedClickable(
                                onClick = { 
                                    if (selectionMode.isNotEmpty()) viewModel.toggleSelection(transaction.id)
                                    else editingTransaction = transaction 
                                },
                                onLongClick = { viewModel.toggleSelection(transaction.id) }
                            )) {
                                TransactionItem(
                                    transaction = transaction,
                                    modifier = Modifier.background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(20.dp)
                                    )
                                }
                            }
                            TransactionDivider()
                        }
                    }
                    is TransactionUiModel.Header -> {
                        val month = item.summary.monthYear
                        val isCollapsed = collapsedMonths.contains(month)
                        MonthHeaderItem(
                            monthYear = month,
                            viewModel = viewModel,
                            isCollapsed = isCollapsed,
                            onToggleCollapse = {
                                collapsedMonths = if (isCollapsed) {
                                    collapsedMonths - month
                                } else {
                                    collapsedMonths + month
                                }
                            }
                        )
                    }
                    null -> {}
                }
            }

            if (transactions.itemCount == 0) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No transactions found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        // Transaction Details dialog
        editingTransaction?.let { transaction ->
            TransactionDetailsDialog(
                transaction = transaction,
                allCategories = allCategories,
                onDismiss = { editingTransaction = null },
                viewModel = viewModel
            )
        }

        if (showAddManualTransaction) {
            AddTransactionSheet(
                allCategories = allCategories,
                onDismiss = { showAddManualTransaction = false },
                onSave = { amount, type, category, merchant, timestamp, paymentMethod, note, excludedFromBudget, excludedFromAnalytics, tags, attachments, locationName, contactName, contactPhone ->
                    viewModel.addTransaction(
                        amount = amount,
                        type = type,
                        category = category,
                        merchant = merchant,
                        timestamp = timestamp,
                        paymentMethod = paymentMethod,
                        userNote = note,
                        isExcludedFromBudget = excludedFromBudget,
                        isExcludedFromAnalytics = excludedFromAnalytics,
                        tags = tags,
                        attachments = attachments,
                        locationName = locationName,
                        contactName = contactName,
                        contactPhone = contactPhone
                    )
                    showAddManualTransaction = false
                },
                viewModel = viewModel
            )
        }

        if (showFilterSheet) {
            AdvancedFilterSheet(
                filterState = filterState,
                allCategories = allCategories,
                onDismiss = { showFilterSheet = false },
                onApplyFilters = { newFilters ->
                    viewModel.updateFilters { newFilters }
                },
                onResetFilters = {
                    viewModel.clearFilters()
                }
            )
        }

        if (showPeriodSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPeriodSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = BottomSheetShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Select Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TransactionPeriod.entries.forEach { period ->
                            val label = period.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                            val isSelected = filterState.period == period
                            Surface(
                                onClick = {
                                    if (period == TransactionPeriod.CUSTOM) {
                                        showCustomDatePicker = true
                                    } else {
                                        viewModel.setPeriod(period)
                                    }
                                    showPeriodSheet = false
                                },
                                shape = ChipShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.weight(1f))
                                    if (isSelected) {
                                        Icon(Icons.Outlined.Check, null, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCustomDatePicker) {
            SimpleDateRangePickerDialog(
                onDismiss = { showCustomDatePicker = false },
                onDateRangeSelected = { range ->
                    viewModel.updateFilters { it.copy(
                        period = TransactionPeriod.CUSTOM,
                        customDateRange = range
                    ) }
                }
            )
        }

        if (showCategoryFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCategoryFilterSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = BottomSheetShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        "Select Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))

                    val categoriesFiltered = allCategories.filter { cat ->
                        when (filterState.selectedTab) {
                            TransactionTab.EXPENSES -> cat.name != "INCOME" && cat.name != "TRANSFER" && cat.name != "SELF_TRANSFER" && cat.name != "UNCATEGORIZED"
                            TransactionTab.INCOME -> cat.name == "INCOME"
                            TransactionTab.SELF_TRANSFER -> cat.name == "SELF_TRANSFER"
                            TransactionTab.UNCATEGORIZED -> cat.name == "UNCATEGORIZED"
                            else -> true
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        item {
                            CategoryGridItem(
                                category = Category(name = "ANY", displayName = "Any Category", emoji = "🏷️", color = "#000000"),
                                isSelected = filterState.selectedCategory == null,
                                onClick = {
                                    viewModel.setCategory(null)
                                    showCategoryFilterSheet = false
                                }
                            )
                        }
                        items(categoriesFiltered) { category ->
                            CategoryGridItem(
                                category = category,
                                isSelected = filterState.selectedCategory?.name == category.name,
                                onClick = {
                                    viewModel.setCategory(category)
                                    showCategoryFilterSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showAmountFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAmountFilterSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = BottomSheetShape
            ) {
                var minAmountText by remember { mutableStateOf(filterState.minAmount?.toString() ?: "") }
                var maxAmountText by remember { mutableStateOf(filterState.maxAmount?.toString() ?: "") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Amount Range",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minAmountText,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) minAmountText = it },
                            label = { Text("Min (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = ChipShape
                        )
                        OutlinedTextField(
                            value = maxAmountText,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) maxAmountText = it },
                            label = { Text("Max (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = ChipShape
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateFilters { it.copy(minAmount = null, maxAmount = null) }
                                showAmountFilterSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = ChipShape
                        ) {
                            Text("Clear")
                        }
                        Button(
                            onClick = {
                                viewModel.updateFilters { it.copy(
                                    minAmount = minAmountText.toDoubleOrNull(),
                                    maxAmount = maxAmountText.toDoubleOrNull()
                                ) }
                                showAmountFilterSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = ChipShape
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        if (showBulkCategoryPicker) {
            ModalBottomSheet(
                onDismissRequest = { showBulkCategoryPicker = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = BottomSheetShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        "Assign Category to Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))

                    val categories = allCategories.filter {
                        it.name != "INCOME" && it.name != "TRANSFER" && it.name != "UNCATEGORIZED"
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(categories) { category ->
                            CategoryGridItem(
                                category = category,
                                isSelected = false,
                                onClick = {
                                    viewModel.bulkUpdateCategory(category)
                                    showBulkCategoryPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TransactionPeriod,
    onPeriodSelected: (TransactionPeriod) -> Unit,
    resolvedRange: Pair<Long, Long>,
    customRange: Pair<Long, Long>? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Box {
        Column(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = selectedPeriod.name.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            val rangeLabel = if (resolvedRange.first == 0L && resolvedRange.second == Long.MAX_VALUE) {
                "All Time"
            } else {
                "${dateFormat.format(Date(resolvedRange.first))} → ${dateFormat.format(Date(resolvedRange.second))}"
            }
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            TransactionPeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            period.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                        ) 
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SortSelector(
    selectedSort: SortBy,
    onSortSelected: (SortBy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.Sort, null, tint = MaterialTheme.colorScheme.onBackground)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            SortBy.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            sort.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                        ) 
                    },
                    leadingIcon = {
                        if (selectedSort == sort) {
                            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        onSortSelected(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onApprove: () -> Unit,
    onSelfTransfer: () -> Unit,
    onChangeCategoryClick: () -> Unit,
    onIgnore: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Outlined.Close, null)
            }
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onApprove) {
                Icon(Icons.Outlined.CheckCircle, "Approve")
            }
            IconButton(onClick = onSelfTransfer) {
                Icon(Icons.Outlined.SwapHoriz, "Self Transfer")
            }
            IconButton(onClick = onChangeCategoryClick) {
                Icon(Icons.Outlined.Label, "Change Category")
            }
            IconButton(onClick = onIgnore) {
                Icon(Icons.Outlined.Block, "Ignore")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete")
            }
        }
    }
}

@Composable
private fun MonthHeaderItem(
    monthYear: String,
    viewModel: TransactionsViewModel,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit
) {
    val summaries by viewModel.monthlySummaries.collectAsStateWithLifecycle()
    val summary = remember(summaries, monthYear) {
        summaries.find { viewModel.formatMonthKey(it.monthKey) == monthYear }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCollapse() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCollapsed) Icons.Outlined.KeyboardArrowRight else Icons.Outlined.KeyboardArrowDown,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthYear,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (summary != null) {
                    val net = summary.income - summary.expenses
                    Text(
                        text = "Net: ₹${String.format("%.0f", net)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (net >= 0) ColorPositive else ColorNegative
                    )
                }
            }
            
            if (summary != null && summary.count > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${summary.count} Transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "In: ₹${String.format("%.0f", summary.income)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorPositive
                        )
                        Text(
                            "Out: ₹${String.format("%.0f", summary.expenses)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorNegative
                        )
                        if (summary.pendingCount > 0) {
                            Text(
                                "Pending: ${summary.pendingCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, Category, String, Long, PaymentMethod, String, Boolean, Boolean, List<String>, List<String>, String?, String?, String?) -> Unit,
    viewModel: TransactionsViewModel
) {
    var amount by remember { mutableStateOf("") }
    var transactionState by remember { mutableStateOf("Approved") } // "Pending Review", "Approved"
    var flowType by remember { mutableStateOf("General") } // "General", "Self Transfer", "Lending"
    var subTypeGeneral by remember { mutableStateOf("Expense") } // "Expense", "Income"
    var subTypeLending by remember { mutableStateOf("Lent") } // "Lent", "Borrowed"

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var tagsText by remember { mutableStateOf("") }
    var attachmentsText by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatEmoji by remember { mutableStateOf("🛍️") }
    var newCatColor by remember { mutableStateOf("#FF9800") }
    var newCatParent by remember { mutableStateOf<Category?>(null) }
    var newCatBudgetEnabled by remember { mutableStateOf(true) }
    var newCatAnalyticsEnabled by remember { mutableStateOf(true) }
    var showPaymentMethodPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = timestamp } }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val computedType = when (transactionState) {
        "Pending Review" -> TransactionType.DEBIT
        else -> when (flowType) {
            "Self Transfer" -> TransactionType.SELF_TRANSFER
            "Lending" -> if (subTypeLending == "Borrowed") TransactionType.BORROWED else TransactionType.LENT
            else -> if (subTypeGeneral == "Income") TransactionType.CREDIT else TransactionType.DEBIT
        }
    }

    var excludedFromBudget by remember { mutableStateOf(false) }
    var excludedFromAnalytics by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = screenHeight * 0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Header Card: Amount & Date Pickers
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                "AMOUNT",
                                style = DotMatrixLabel.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = amount,
                                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                                textStyle = MonoAmountLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "₹",
                                            style = MonoAmountLarge.copy(
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                        if (amount.isEmpty()) {
                                            Text(
                                                "0.00",
                                                style = MonoAmountLarge.copy(
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "DATE & TIME",
                                style = DotMatrixLabel.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .clickable { showDatePicker = true }
                                    .padding(vertical = 2.dp),
                                color = Color.Transparent
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.CalendarToday,
                                        null,
                                        Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        dateFormat.format(Date(timestamp)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .clickable { showTimePicker = true }
                                    .padding(vertical = 2.dp),
                                color = Color.Transparent
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        null,
                                        Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        timeFormat.format(Date(timestamp)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Merchant input field
                Column {
                    Text("MERCHANT / DESCRIPTION", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Hierarchical State/Type Selectors (Apple Camera style sliders)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("TRANSACTION STATE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val states = listOf("Pending Review", "Approved")
                    
                    TactileSegmentedControl(
                        options = states,
                        selectedOption = transactionState,
                        onOptionSelected = { stateStr ->
                            transactionState = stateStr
                        },
                        labelProvider = { it }
                    )
                    
                    if (transactionState == "Approved") {
                        val flows = listOf("General", "Self Transfer", "Lending")
                        
                        Text("FLOW TYPE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TactileSegmentedControl(
                            options = flows,
                            selectedOption = flowType,
                            onOptionSelected = { flowStr ->
                                flowType = flowStr
                            },
                            labelProvider = { it }
                        )
                        
                        if (flowType == "General") {
                            val subTypes = listOf("Expense", "Income")
                            
                            Text("SUB-TYPE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TactileSegmentedControl(
                                options = subTypes,
                                selectedOption = subTypeGeneral,
                                onOptionSelected = { subTypeStr ->
                                    subTypeGeneral = subTypeStr
                                },
                                labelProvider = { it }
                            )
                        } else if (flowType == "Lending") {
                            val subTypes = listOf("Lent", "Borrowed")
                            
                            Text("SUB-TYPE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TactileSegmentedControl(
                                options = subTypes,
                                selectedOption = subTypeLending,
                                onOptionSelected = { subTypeStr ->
                                    subTypeLending = subTypeStr
                                },
                                labelProvider = { it }
                            )
                        }
                    }
                }

                // Category selector row (only shown if type is DEBIT / Expense)
                if (computedType == TransactionType.DEBIT) {
                    Column {
                        Text("CATEGORY", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryPicker = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val cat = selectedCategory ?: allCategories.find { it.name == "UNCATEGORIZED" } ?: allCategories.first()
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cat.emoji, fontSize = 20.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            cat.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                
                                 Button(
                                    onClick = { showCategoryPicker = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Change", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Contact name & Phone (only shown for Lent / Borrowed)
                val isLendingFlow = transactionState == "Approved" && flowType == "Lending"
                if (isLendingFlow) {
                    val contactPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickContact(),
                        onResult = { uri ->
                            uri?.let {
                                val contentResolver = context.contentResolver
                                var name: String? = null
                                var phone: String? = null
                                
                                val cursor = contentResolver.query(it, null, null, null, null)
                                cursor?.use { c ->
                                    if (c.moveToFirst()) {
                                        val idCol = c.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                                        val nameCol = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                                        val hasPhoneCol = c.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                                        
                                        if (nameCol >= 0) {
                                            name = c.getString(nameCol)
                                        }
                                        if (idCol >= 0 && hasPhoneCol >= 0 && c.getInt(hasPhoneCol) > 0) {
                                            val contactId = c.getString(idCol)
                                            val phoneCursor = contentResolver.query(
                                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                null,
                                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                                arrayOf(contactId),
                                                null
                                            )
                                            phoneCursor?.use { pc ->
                                                if (pc.moveToFirst()) {
                                                    val numCol = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                                    if (numCol >= 0) {
                                                        phone = pc.getString(numCol)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (name != null) {
                                    contactName = name!!
                                }
                                if (phone != null) {
                                    contactPhone = phone!!.replace(Regex("[\\s\\-\\(\\)]"), "")
                                }
                            }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CONTACT INFO", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Contact Name") },
                            placeholder = { Text("e.g. Rahul") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Contact Phone") },
                                placeholder = { Text("e.g. 9876543210") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            
                            OutlinedIconButton(
                                onClick = { contactPickerLauncher.launch(null) },
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Contacts,
                                    contentDescription = "Select Contact",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Notes field
                Column {
                    Text("NOTES", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Add custom notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                // Advanced Details collapsing group
                var showAdvancedDetails by remember { mutableStateOf(false) }
                Column(modifier = Modifier.animateContentSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedDetails = !showAdvancedDetails }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ADVANCED DETAILS", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            if (showAdvancedDetails) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (showAdvancedDetails) {
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Payment Method
                            Column {
                                Text("Payment Method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { showPaymentMethodPicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ChipShape
                                ) {
                                    Text(paymentMethod.name)
                                }
                            }
                            
                            // Tags
                            OutlinedTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                label = { Text("Tags (comma separated)") },
                                placeholder = { Text("food, trip, gift") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Attachments
                            OutlinedTextField(
                                value = attachmentsText,
                                onValueChange = { attachmentsText = it },
                                label = { Text("Attachments (comma separated)") },
                                placeholder = { Text("receipt.jpg, bill.png") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Location
                            OutlinedTextField(
                                value = locationName,
                                onValueChange = { locationName = it },
                                label = { Text("Location Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // Exclusion Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Exclude from Budget", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = excludedFromBudget, onCheckedChange = { excludedFromBudget = it })
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Exclude from Analytics", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = excludedFromAnalytics, onCheckedChange = { excludedFromAnalytics = it })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Bottom Buttons: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = ChipShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amountVal = amount.toDoubleOrNull() ?: 0.0
                            val cat = when (computedType) {
                                TransactionType.SELF_TRANSFER -> Category.SELF_TRANSFER
                                TransactionType.LENT -> Category.LENT
                                TransactionType.BORROWED -> Category.BORROWED
                                TransactionType.CREDIT -> Category.INCOME
                                else -> selectedCategory ?: allCategories.find { it.name == "UNCATEGORIZED" } ?: allCategories.first()
                            }
                            val tagsList = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val attachmentsList = attachmentsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onSave(
                                amountVal, computedType, cat, merchant.trim(), timestamp, paymentMethod, note.trim(), excludedFromBudget, excludedFromAnalytics,
                                tagsList, attachmentsList, locationName.ifBlank { null }, contactName.ifBlank { null }, contactPhone.ifBlank { null }
                            )
                        },
                        enabled = amount.isNotBlank() && merchant.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = ChipShape
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                val categories = allCategories.filter {
                    it.name != "INCOME" && it.name != "TRANSFER" && it.name != "UNCATEGORIZED" && it.name != "LENT" && it.name != "BORROWED" && it.name != "SELF_TRANSFER"
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    items(categories.size + 1) { idx ->
                        if (idx < categories.size) {
                            val category = categories[idx]
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    showCategoryPicker = false
                                },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(category.emoji, fontSize = 20.sp)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            category.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        } else {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    showCategoryPicker = false
                                    newCatName = ""
                                    newCatEmoji = "🛍️"
                                    newCatColor = "#FF9800"
                                    newCatParent = null
                                    newCatBudgetEnabled = true
                                    newCatAnalyticsEnabled = true
                                    showCreateCategory = true
                                },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text("➕", fontSize = 20.sp)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "+ New",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateCategory) {
        ModalBottomSheet(
            onDismissRequest = { showCreateCategory = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Create New Category",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Name input
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Emoji Icon and Color Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newCatEmoji,
                        onValueChange = { newCatEmoji = it.take(2) },
                        label = { Text("Emoji") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = newCatColor,
                        onValueChange = { newCatColor = it },
                        label = { Text("Hex Color") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Preset Colors Swatch Selector
                val presetColors = listOf("#FF9800", "#4CAF50", "#03A9F4", "#FF5722", "#E91E63", "#9C27B0", "#795548", "#F44336", "#009688", "#3F51B5")
                Column {
                    Text("CHOOSE ACCENT COLOR", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { newCatColor = hex }
                                    .border(
                                        width = if (newCatColor == hex) 3.dp else 1.dp,
                                        color = if (newCatColor == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Optional Parent Category Selector Dropdown
                var showParentDropdown by remember { mutableStateOf(false) }
                Column {
                    Text("PARENT CATEGORY (OPTIONAL)", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { showParentDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                newCatParent?.displayName ?: "No Parent Category",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Parent Category") },
                                onClick = {
                                    newCatParent = null
                                    showParentDropdown = false
                                }
                            )
                            allCategories.filter { !it.isCustom && it.name != "UNCATEGORIZED" }.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text("${parent.emoji} ${parent.displayName}") },
                                    onClick = {
                                        newCatParent = parent
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Toggles: Budget & Analytics
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include in Budget calculations", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Consumes monthly budget amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = newCatBudgetEnabled,
                            onCheckedChange = { newCatBudgetEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include in Analytics Reports", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Show up in spending pie-charts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = newCatAnalyticsEnabled,
                            onCheckedChange = { newCatAnalyticsEnabled = it }
                        )
                    }
                }

                // Confirm Save
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.createCategory(
                                displayName = newCatName.trim(),
                                emoji = newCatEmoji,
                                color = newCatColor,
                                parentCategoryName = newCatParent?.name,
                                isBudgetTrackingEnabled = newCatBudgetEnabled,
                                includeInAnalytics = newCatAnalyticsEnabled
                            ) { createdCat ->
                                selectedCategory = createdCat
                            }
                            showCreateCategory = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChipShape
                ) {
                    Text("Save & Categorize")
                }
            }
        }
    }

    if (showPaymentMethodPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentMethodPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Select Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                ) {
                    PaymentMethod.entries.forEach { pm ->
                        item {
                            FilterChip(
                                selected = paymentMethod == pm,
                                onClick = {
                                    paymentMethod = pm
                                    showPaymentMethodPicker = false
                                },
                                label = { Text(pm.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        SimpleDatePickerDialog(
            initialDateMs = timestamp,
            onDismiss = { showDatePicker = false },
            onDateSelected = { ms ->
                timestamp = ms
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        SimpleTimePickerDialog(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                timestamp = calendar.timeInMillis
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailsDialog(
    transaction: Transaction,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    viewModel: TransactionsViewModel
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
    var draftTxn by remember { mutableStateOf(transaction) }

    var merchantInput by remember { mutableStateOf(transaction.merchant) }
    var notesInput by remember { mutableStateOf(transaction.userNote) }
    var contactNameInput by remember { mutableStateOf(transaction.contactName ?: "") }
    var contactPhoneInput by remember { mutableStateOf(transaction.contactPhone ?: "") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showSmsBody by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var showBulkReassignDialog by remember { mutableStateOf<String?>(null) } // Newly created category name

    // States for custom category creation
    var newCatName by remember { mutableStateOf("") }
    var newCatEmoji by remember { mutableStateOf("🏷️") }
    var newCatColor by remember { mutableStateOf("#FF9800") }
    var newCatParent by remember { mutableStateOf<Category?>(null) }
    var newCatBudgetEnabled by remember { mutableStateOf(true) }
    var newCatAnalyticsEnabled by remember { mutableStateOf(true) }

    fun updateDraftFlow(state: TransactionStatus, type: TransactionType) {
        val newStatus = when (type) {
            TransactionType.SELF_TRANSFER -> TransactionStatus.SELF_TRANSFER
            TransactionType.LENT -> TransactionStatus.LENT
            TransactionType.BORROWED -> TransactionStatus.BORROWED
            else -> if (state == TransactionStatus.PENDING_REVIEW) TransactionStatus.PENDING_REVIEW else TransactionStatus.APPROVED
        }
        val newCategory = when (type) {
            TransactionType.SELF_TRANSFER -> Category.SELF_TRANSFER
            TransactionType.LENT -> Category.LENT
            TransactionType.BORROWED -> Category.BORROWED
            TransactionType.CREDIT -> Category.INCOME
            TransactionType.DEBIT -> {
                if (draftTxn.category.name == "LENT" || draftTxn.category.name == "BORROWED" || draftTxn.category.name == "SELF_TRANSFER" || draftTxn.category.name == "INCOME") {
                    Category.UNCATEGORIZED
                } else {
                    draftTxn.category
                }
            }
        }
        val isExcludedBudget = !newCategory.isBudgetTrackingEnabled
        val isExcludedAnalytics = !newCategory.includeInAnalytics
        draftTxn = draftTxn.copy(
            status = newStatus,
            type = type,
            category = newCategory,
            isExcludedFromBudget = isExcludedBudget,
            isExcludedFromAnalytics = isExcludedAnalytics
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = screenHeight * 0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: Amount & Date
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "AMOUNT",
                                style = DotMatrixLabel.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "₹${draftTxn.amount}",
                                style = MonoAmountLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "DATE",
                                style = DotMatrixLabel.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                dateFormat.format(Date(draftTxn.timestamp)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Merchant input field
                Column {
                    Text("MERCHANT", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Hierarchical State/Type Selectors (Apple Camera style sliders)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("TRANSACTION STATE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val states = listOf("Pending Review", "Approved")
                    val currentState = if (draftTxn.status == TransactionStatus.PENDING_REVIEW) "Pending Review" else "Approved"
                    
                    TactileSegmentedControl(
                        options = states,
                        selectedOption = currentState,
                        onOptionSelected = { stateStr ->
                            val newStatus = if (stateStr == "Pending Review") TransactionStatus.PENDING_REVIEW else TransactionStatus.APPROVED
                            updateDraftFlow(newStatus, if (newStatus == TransactionStatus.PENDING_REVIEW) TransactionType.DEBIT else draftTxn.type)
                        },
                        labelProvider = { it }
                    )
                    
                    if (currentState == "Approved") {
                        val flows = listOf("General", "Self Transfer", "Lending")
                        val currentFlow = when (draftTxn.type) {
                            TransactionType.SELF_TRANSFER -> "Self Transfer"
                            TransactionType.LENT, TransactionType.BORROWED -> "Lending"
                            else -> "General"
                        }
                        
                        Text("FLOW TYPE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TactileSegmentedControl(
                            options = flows,
                            selectedOption = currentFlow,
                            onOptionSelected = { flowStr ->
                                val newType = when (flowStr) {
                                    "Self Transfer" -> TransactionType.SELF_TRANSFER
                                    "Lending" -> TransactionType.LENT
                                    else -> TransactionType.DEBIT
                                }
                                updateDraftFlow(TransactionStatus.APPROVED, newType)
                            },
                            labelProvider = { it }
                        )
                        
                        if (currentFlow == "General") {
                            val subTypes = listOf("Expense", "Income")
                            val currentSubType = if (draftTxn.type == TransactionType.CREDIT) "Income" else "Expense"
                            
                            Text("SUB-TYPE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TactileSegmentedControl(
                                options = subTypes,
                                selectedOption = currentSubType,
                                onOptionSelected = { subTypeStr ->
                                    val newType = if (subTypeStr == "Income") TransactionType.CREDIT else TransactionType.DEBIT
                                    updateDraftFlow(TransactionStatus.APPROVED, newType)
                                },
                                labelProvider = { it }
                            )
                        } else if (currentFlow == "Lending") {
                            val subTypes = listOf("Lent", "Borrowed")
                            val currentSubType = if (draftTxn.type == TransactionType.BORROWED) "Borrowed" else "Lent"
                            
                            Text("SUB-TYPE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TactileSegmentedControl(
                                options = subTypes,
                                selectedOption = currentSubType,
                                onOptionSelected = { subTypeStr ->
                                    val newType = if (subTypeStr == "Borrowed") TransactionType.BORROWED else TransactionType.LENT
                                    updateDraftFlow(TransactionStatus.APPROVED, newType)
                                },
                                labelProvider = { it }
                            )
                        }
                    }
                }

                // Category selector row (only if Expense / Debit) - placed at the bottom after flows are done
                if (draftTxn.type == TransactionType.DEBIT) {
                    Column {
                        Text("CATEGORY", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryPicker = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(draftTxn.category.emoji, fontSize = 20.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            draftTxn.category.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                
                                Button(
                                    onClick = { showCategoryPicker = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Change", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                }

                // Contact name & Phone (only shown for Lent / Borrowed)
                if (draftTxn.type == TransactionType.LENT || draftTxn.type == TransactionType.BORROWED) {
                    val context = LocalContext.current
                    val contactPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickContact(),
                        onResult = { uri ->
                            uri?.let {
                                val contentResolver = context.contentResolver
                                var name: String? = null
                                var phone: String? = null
                                
                                val cursor = contentResolver.query(it, null, null, null, null)
                                cursor?.use { c ->
                                    if (c.moveToFirst()) {
                                        val idCol = c.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                                        val nameCol = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                                        val hasPhoneCol = c.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                                        
                                        if (nameCol >= 0) {
                                            name = c.getString(nameCol)
                                        }
                                        if (idCol >= 0 && hasPhoneCol >= 0 && c.getInt(hasPhoneCol) > 0) {
                                            val contactId = c.getString(idCol)
                                            val phoneCursor = contentResolver.query(
                                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                null,
                                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                                arrayOf(contactId),
                                                null
                                            )
                                            phoneCursor?.use { pc ->
                                                if (pc.moveToFirst()) {
                                                    val numCol = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                                    if (numCol >= 0) {
                                                        phone = pc.getString(numCol)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (name != null) {
                                    contactNameInput = name!!
                                }
                                if (phone != null) {
                                    contactPhoneInput = phone!!.replace(Regex("[\\s\\-\\(\\)]"), "")
                                }
                            }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CONTACT INFO", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        OutlinedTextField(
                            value = contactNameInput,
                            onValueChange = { contactNameInput = it },
                            label = { Text("Contact Name") },
                            placeholder = { Text("e.g. Rahul") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = contactPhoneInput,
                                onValueChange = { contactPhoneInput = it },
                                label = { Text("Contact Phone") },
                                placeholder = { Text("e.g. 9876543210") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            
                            OutlinedIconButton(
                                onClick = { contactPickerLauncher.launch(null) },
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Contacts,
                                    contentDescription = "Select Contact",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Notes field
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NOTES", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (draftTxn.rawSmsBody.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    showSmsBody = !showSmsBody
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sms,
                                    contentDescription = "Toggle raw SMS",
                                    tint = if (showSmsBody) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("Add custom notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    // Raw SMS card shown when toggled on
                    if (showSmsBody && draftTxn.rawSmsBody.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "RAW SMS MESSAGE",
                                    style = DotMatrixLabel.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = draftTxn.rawSmsBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Splits display if present
                if (draftTxn.splits.isNotEmpty()) {
                    Column {
                        Text("TRANSACTION SPLITS", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, shape = SmallCardShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SmallCardShape)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            draftTxn.splits.forEach { split ->
                                val cat = allCategories.find { it.name == split.categoryName }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat?.emoji ?: "🏷️", fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            cat?.displayName ?: split.categoryName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (!split.note.isNullOrBlank()) {
                                            Text(
                                                " (${split.note})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        "₹${split.amount}",
                                        style = MonoAmount.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }

                // Advanced Details collapsing group
                var showAdvancedDetails by remember { mutableStateOf(false) }
                Column(modifier = Modifier.animateContentSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedDetails = !showAdvancedDetails }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ADVANCED DETAILS", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            if (showAdvancedDetails) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (showAdvancedDetails) {
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(draftTxn.paymentMethod.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            }
                            if (draftTxn.tags.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tags", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(draftTxn.tags.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            val locationName = draftTxn.locationName
                            if (locationName != null) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Location", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(locationName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            val contactName = draftTxn.contactName
                            if (contactName != null) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Contact", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${contactName} (${draftTxn.contactPhone ?: ""})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Source", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (draftTxn.isManualEntry) "Manual Entry" else "SMS Imported", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exclude from Budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (draftTxn.isExcludedFromBudget) "Yes" else "No", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exclude from Analytics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (draftTxn.isExcludedFromAnalytics) "Yes" else "No", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }

                // Split Transaction Button (only if DEBIT)
                var showSplitDialog by remember { mutableStateOf(false) }
                if (draftTxn.type == TransactionType.DEBIT) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { showSplitDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = ChipShape
                    ) {
                        Icon(Icons.Outlined.CallSplit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Split Transaction")
                    }
                }

                // Delete Transaction Button
                Button(
                    onClick = {
                        viewModel.deleteTransaction(draftTxn)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White
                    ),
                    shape = ChipShape
                ) {
                    Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Transaction")
                }

                Spacer(Modifier.height(8.dp))

                // Bottom Buttons: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = ChipShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val contactName = contactNameInput.ifBlank { null }
                            val contactPhone = contactPhoneInput.ifBlank { null }
                            val finalTxn = draftTxn.copy(
                                merchant = merchantInput.trim(),
                                merchantNormalized = com.spendless.app.core.sms.SmsParser.normalizeMerchant(merchantInput),
                                userNote = notesInput.trim(),
                                contactName = contactName,
                                contactPhone = contactPhone
                            )
                            viewModel.updateTransaction(finalTxn)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = ChipShape
                    ) {
                        Text("Save")
                    }
                }

                if (showSplitDialog) {
                    SplitTransactionDialog(
                        transaction = draftTxn,
                        allCategories = allCategories,
                        onDismiss = { showSplitDialog = false },
                        onSave = { updatedSplits ->
                            draftTxn = draftTxn.copy(splits = updatedSplits)
                            showSplitDialog = false
                        }
                    )
                }
            }
        }
    }

    // Changing Category Selector Sub-Sheet
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                val categories = allCategories.filter {
                    it.name != "INCOME" && it.name != "TRANSFER" && it.name != "UNCATEGORIZED"
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    items(categories.size + 1) { idx ->
                        if (idx < categories.size) {
                            val cat = categories[idx]
                            FilterChip(
                                selected = draftTxn.category.name == cat.name,
                                onClick = {
                                    val newType = when (cat.name) {
                                        "LENT" -> TransactionType.LENT
                                        "BORROWED" -> TransactionType.BORROWED
                                        else -> if (draftTxn.type == TransactionType.LENT || draftTxn.type == TransactionType.BORROWED) {
                                            TransactionType.DEBIT
                                        } else {
                                            draftTxn.type
                                        }
                                    }
                                    val newStatus = when (cat.name) {
                                        "LENT" -> TransactionStatus.LENT
                                        "BORROWED" -> TransactionStatus.BORROWED
                                        else -> if (draftTxn.status == TransactionStatus.LENT || draftTxn.status == TransactionStatus.BORROWED) {
                                            TransactionStatus.APPROVED
                                        } else {
                                            draftTxn.status
                                        }
                                    }
                                    draftTxn = draftTxn.copy(
                                        category = cat,
                                        type = newType,
                                        status = newStatus,
                                        isExcludedFromBudget = !cat.isBudgetTrackingEnabled || newType == TransactionType.LENT || newType == TransactionType.BORROWED,
                                        isExcludedFromAnalytics = !cat.includeInAnalytics || newType == TransactionType.LENT || newType == TransactionType.BORROWED
                                    )
                                    showCategoryPicker = false
                                },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(cat.emoji, fontSize = 20.sp)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            cat.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        } else {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    showCategoryPicker = false
                                    newCatName = ""
                                    newCatEmoji = "🛍️"
                                    newCatColor = "#FF9800"
                                    newCatParent = null
                                    newCatBudgetEnabled = true
                                    newCatAnalyticsEnabled = true
                                    showCreateCategory = true
                                },
                                label = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text("➕", fontSize = 20.sp)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "+ New",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Category Creation Bottom Sheet / Modal
    if (showCreateCategory) {
        ModalBottomSheet(
            onDismissRequest = { showCreateCategory = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Create New Category",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Name input
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Emoji Icon and Color Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newCatEmoji,
                        onValueChange = { newCatEmoji = it.take(2) },
                        label = { Text("Emoji") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = newCatColor,
                        onValueChange = { newCatColor = it },
                        label = { Text("Hex Color") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Preset Colors Swatch Selector
                val presetColors = listOf("#FF9800", "#4CAF50", "#03A9F4", "#FF5722", "#E91E63", "#9C27B0", "#795548", "#F44336", "#009688", "#3F51B5")
                Column {
                    Text("CHOOSE ACCENT COLOR", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { newCatColor = hex }
                                    .border(
                                        width = if (newCatColor == hex) 3.dp else 1.dp,
                                        color = if (newCatColor == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Optional Parent Category Selector Dropdown
                var showParentDropdown by remember { mutableStateOf(false) }
                Column {
                    Text("PARENT CATEGORY (OPTIONAL)", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { showParentDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                newCatParent?.displayName ?: "No Parent Category",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Parent Category") },
                                onClick = {
                                    newCatParent = null
                                    showParentDropdown = false
                                }
                            )
                            allCategories.filter { !it.isCustom && it.name != "UNCATEGORIZED" }.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text("${parent.emoji} ${parent.displayName}") },
                                    onClick = {
                                        newCatParent = parent
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Toggles: Budget & Analytics
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include in Budget calculations", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Consumes monthly budget amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = newCatBudgetEnabled,
                            onCheckedChange = { newCatBudgetEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include in Analytics Reports", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Show up in spending pie-charts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = newCatAnalyticsEnabled,
                            onCheckedChange = { newCatAnalyticsEnabled = it }
                        )
                    }
                }

                // Confirm Save
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            showBulkReassignDialog = newCatName.trim()
                            showCreateCategory = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChipShape
                ) {
                    Text("Save & Categorize")
                }
            }
        }
    }

    // Bulk Reassignment Dialog
    showBulkReassignDialog?.let { catName ->
        AlertDialog(
            onDismissRequest = {
                viewModel.createCategoryAndAssign(
                    transaction = draftTxn,
                    displayName = catName,
                    emoji = newCatEmoji,
                    color = newCatColor,
                    parentCategoryName = newCatParent?.name,
                    isBudgetTrackingEnabled = newCatBudgetEnabled,
                    includeInAnalytics = newCatAnalyticsEnabled,
                    bulkOption = "NONE"
                )
                showBulkReassignDialog = null
                onDismiss()
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Bulk Reassignment", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Apply this new category to similar transactions from ${draftTxn.merchant}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    val options = listOf(
                        "NONE" to "No, only this transaction",
                        "DAYS_30" to "All for Last 30 Days",
                        "MONTHS_6" to "All for Last 6 Months",
                        "ALL_TIME" to "All Entire History"
                    )

                    options.forEach { (key, label) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.createCategoryAndAssign(
                                    transaction = draftTxn,
                                    displayName = catName,
                                    emoji = newCatEmoji,
                                    color = newCatColor,
                                    parentCategoryName = newCatParent?.name,
                                    isBudgetTrackingEnabled = newCatBudgetEnabled,
                                    includeInAnalytics = newCatAnalyticsEnabled,
                                    bulkOption = key
                                )
                                showBulkReassignDialog = null
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(label, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            },
            confirmButton = {}
        )
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
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary 
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(category.emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                category.displayName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitTransactionDialog(
    transaction: Transaction,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (List<TransactionSplit>) -> Unit
) {
    var splits by remember {
        mutableStateOf(
            if (transaction.splits.isNotEmpty()) transaction.splits
            else listOf(TransactionSplit(categoryName = transaction.category.name, amount = transaction.amount))
        )
    }

    val totalAmount = transaction.amount
    val allocatedAmount = remember(splits) { splits.sumOf { it.amount } }
    val unallocatedAmount = remember(totalAmount, allocatedAmount) { totalAmount - allocatedAmount }

    val categories = remember(allCategories) {
        allCategories.filter {
            it.name != "INCOME" && it.name != "TRANSFER" && it.name != "SELF_TRANSFER" && it.name != "UNCATEGORIZED"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Split Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Total Amount: ₹${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = when {
                        Math.abs(unallocatedAmount) < 0.01 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        unallocatedAmount > 0 -> ColorWarning.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
                    shape = SmallCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                Math.abs(unallocatedAmount) < 0.01 -> "Perfectly Allocated"
                                unallocatedAmount > 0 -> "Unallocated Amount"
                                else -> "Over Allocated"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "₹${String.format("%.2f", unallocatedAmount)}",
                            style = MonoAmount,
                            color = when {
                                Math.abs(unallocatedAmount) < 0.01 -> ColorPositive
                                unallocatedAmount > 0 -> ColorWarning
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                splits.forEachIndexed { index, split ->
                    var showCatPickerForSplit by remember { mutableStateOf(false) }
                    val currentCategory = categories.find { it.name == split.categoryName } ?: categories.firstOrNull()

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = SmallCardShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(ChipShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { showCatPickerForSplit = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currentCategory?.emoji ?: "🏷️", fontSize = 16.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        currentCategory?.displayName ?: split.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                                }

                                if (splits.size > 1) {
                                    IconButton(
                                        onClick = {
                                            splits = splits.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Remove Split",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = if (split.amount == 0.0) "" else split.amount.toString(),
                                    onValueChange = { value ->
                                        val newAmount = value.toDoubleOrNull() ?: 0.0
                                        splits = splits.mapIndexed { i, s ->
                                            if (i == index) s.copy(amount = newAmount) else s
                                        }
                                    },
                                    label = { Text("Amount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = MonoAmount,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = split.note ?: "",
                                    onValueChange = { value ->
                                        splits = splits.mapIndexed { i, s ->
                                            if (i == index) s.copy(note = value) else s
                                        }
                                    },
                                    label = { Text("Note (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }

                    if (showCatPickerForSplit) {
                        ModalBottomSheet(
                            onDismissRequest = { showCatPickerForSplit = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            shape = BottomSheetShape
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .navigationBarsPadding()
                            ) {
                                Text(
                                    "Select Category",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(16.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                ) {
                                    items(categories) { category ->
                                        CategoryGridItem(
                                            category = category,
                                            isSelected = category.name == split.categoryName,
                                            onClick = {
                                                splits = splits.mapIndexed { i, s ->
                                                    if (i == index) s.copy(categoryName = category.name) else s
                                                }
                                                showCatPickerForSplit = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val amountToAllocate = if (unallocatedAmount > 0) unallocatedAmount else 0.0
                        splits = splits + TransactionSplit(
                            categoryName = categories.firstOrNull()?.name ?: "UNCATEGORIZED",
                            amount = amountToAllocate
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChipShape
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Category Split")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(splits) },
                enabled = Math.abs(unallocatedAmount) < 0.01,
                shape = ChipShape
            ) {
                Text("Save Splits")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun AdvancedFilterSheet(
    filterState: FilterState,
    allCategories: List<Category>,
    onDismiss: () -> Unit,
    onApplyFilters: (FilterState) -> Unit,
    onResetFilters: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(filterState.searchQuery) }
    var selectedCategory by remember { mutableStateOf(filterState.selectedCategory) }
    var minAmountText by remember { mutableStateOf(filterState.minAmount?.toString() ?: "") }
    var maxAmountText by remember { mutableStateOf(filterState.maxAmount?.toString() ?: "") }
    var status by remember { mutableStateOf(filterState.status) }
    var paymentMethod by remember { mutableStateOf(filterState.paymentMethod) }
    var isManual by remember { mutableStateOf(filterState.isManual) }
    var isExcludedFromBudget by remember { mutableStateOf(filterState.isExcludedFromBudget) }
    var isExcludedFromAnalytics by remember { mutableStateOf(filterState.isExcludedFromAnalytics) }
    var contactName by remember { mutableStateOf(filterState.contactName ?: "") }
    var selectedMerchant by remember { mutableStateOf(filterState.selectedMerchant ?: "") }
    var showCatPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = BottomSheetShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Advanced Filters",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = {
                    searchQuery = ""
                    selectedCategory = null
                    minAmountText = ""
                    maxAmountText = ""
                    status = null
                    paymentMethod = null
                    isManual = null
                    isExcludedFromBudget = null
                    isExcludedFromAnalytics = null
                    contactName = ""
                    selectedMerchant = ""
                    onResetFilters()
                }) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            }

            // Merchant Filter
            OutlinedTextField(
                value = selectedMerchant,
                onValueChange = { selectedMerchant = it },
                label = { Text("Filter by Merchant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category selector
            Column {
                Text("CATEGORY", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showCatPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ChipShape
                ) {
                    Text(selectedCategory?.let { "${it.emoji} ${it.displayName}" } ?: "Select Category")
                }
            }

            // Amount Range
            Column {
                Text("AMOUNT RANGE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) minAmountText = it },
                        label = { Text("Min (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) maxAmountText = it },
                        label = { Text("Max (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }



            // Payment Method
            Column {
                Text("PAYMENT METHOD", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = paymentMethod == null,
                            onClick = { paymentMethod = null },
                            label = { Text("Any") }
                        )
                    }
                    PaymentMethod.entries.forEach { pm ->
                        item {
                            FilterChip(
                                selected = paymentMethod == pm,
                                onClick = { paymentMethod = pm },
                                label = { Text(pm.name) }
                            )
                        }
                    }
                }
            }

            // Import source / Manual entry
            Column {
                Text("IMPORT SOURCE", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = isManual == null,
                        onClick = { isManual = null },
                        label = { Text("All") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isManual == true,
                        onClick = { isManual = true },
                        label = { Text("Manual Entry") },
                        modifier = Modifier.weight(1.5f)
                    )
                    FilterChip(
                        selected = isManual == false,
                        onClick = { isManual = false },
                        label = { Text("SMS Imported") },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }

            // Contact Name Filter
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Filter by Contact Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Exclude Flags
            Column {
                Text("BUDGET & ANALYTICS EXCLUSIONS", style = DotMatrixLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Budget Excluded", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        FilterChip(
                            selected = isExcludedFromBudget == null,
                            onClick = { isExcludedFromBudget = null },
                            label = { Text("Either") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isExcludedFromBudget == true,
                            onClick = { isExcludedFromBudget = true },
                            label = { Text("Yes") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isExcludedFromBudget == false,
                            onClick = { isExcludedFromBudget = false },
                            label = { Text("No") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Analytics Excluded", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        FilterChip(
                            selected = isExcludedFromAnalytics == null,
                            onClick = { isExcludedFromAnalytics = null },
                            label = { Text("Either") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isExcludedFromAnalytics == true,
                            onClick = { isExcludedFromAnalytics = true },
                            label = { Text("Yes") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isExcludedFromAnalytics == false,
                            onClick = { isExcludedFromAnalytics = false },
                            label = { Text("No") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onApplyFilters(
                        filterState.copy(
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            minAmount = minAmountText.toDoubleOrNull(),
                            maxAmount = maxAmountText.toDoubleOrNull(),
                            status = status,
                            paymentMethod = paymentMethod,
                            isManual = isManual,
                            isExcludedFromBudget = isExcludedFromBudget,
                            isExcludedFromAnalytics = isExcludedFromAnalytics,
                            contactName = contactName.ifBlank { null },
                            selectedMerchant = selectedMerchant.ifBlank { null }
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = ChipShape
            ) {
                Text("Apply Filters")
            }
        }
    }

    if (showCatPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCatPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = BottomSheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    item {
                        CategoryGridItem(
                            category = Category(name = "ANY", displayName = "Any Category", emoji = "🏷️", color = "#000000"),
                            isSelected = selectedCategory == null,
                            onClick = {
                                selectedCategory = null
                                showCatPicker = false
                            }
                        )
                    }
                    items(allCategories) { category ->
                        CategoryGridItem(
                            category = category,
                            isSelected = selectedCategory?.name == category.name,
                            onClick = {
                                selectedCategory = category
                                showCatPicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getMonthYear(timestamp: Long): String {
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return monthYearFormat.format(Date(timestamp))
}

@Composable
fun FilterPill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = ChipShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Pair<Long, Long>) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onDateRangeSelected(start to end)
                    }
                    onDismiss()
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.height(450.dp)) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
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
}
