package com.spendless.app.ui.screens.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spendless.app.core.data.database.dao.CategoryDao
import com.spendless.app.core.data.database.dao.TransactionDao
import com.spendless.app.core.data.database.entities.Category
import com.spendless.app.ui.components.SpendLessCard
import com.spendless.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    val categories = categoryDao.getAllCategoriesWithArchivedFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addCategory(
        displayName: String,
        emoji: String,
        color: String,
        parentCategoryName: String?,
        isBudgetTrackingEnabled: Boolean,
        includeInAnalytics: Boolean
    ) {
        val normalizedName = "CUSTOM_" + displayName.uppercase().replace(Regex("[^A-Z0-9]"), "_") + "_" + System.currentTimeMillis()
        val newCategory = Category(
            name = normalizedName,
            displayName = displayName,
            emoji = emoji,
            isCustom = true,
            color = color,
            parentCategoryName = parentCategoryName,
            isBudgetTrackingEnabled = isBudgetTrackingEnabled,
            includeInAnalytics = includeInAnalytics
        )
        viewModelScope.launch {
            categoryDao.insert(newCategory)
            Category.customCache[normalizedName] = newCategory
        }
    }

    fun updateCategory(
        category: Category,
        newDisplayName: String,
        newEmoji: String,
        newColor: String,
        newParentCategoryName: String?,
        isBudgetTrackingEnabled: Boolean,
        includeInAnalytics: Boolean,
        isArchived: Boolean
    ) {
        val updated = category.copy(
            displayName = newDisplayName,
            emoji = newEmoji,
            color = newColor,
            parentCategoryName = newParentCategoryName,
            isBudgetTrackingEnabled = isBudgetTrackingEnabled,
            includeInAnalytics = includeInAnalytics,
            isArchived = isArchived
        )
        viewModelScope.launch {
            categoryDao.update(updated)
            Category.customCache[category.name] = updated
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.delete(category)
            Category.customCache.remove(category.name)
        }
    }

    fun mergeCategories(source: Category, target: Category) {
        viewModelScope.launch {
            // Update all transactions of source to target category name
            transactionDao.mergeCategories(source.name, target.name)
            // Delete source category
            categoryDao.delete(source)
            Category.customCache.remove(source.name)
        }
    }

    fun reorderCategories(orderedList: List<Category>) {
        viewModelScope.launch {
            orderedList.forEachIndexed { index, category ->
                val updated = category.copy(displayOrder = index)
                categoryDao.update(updated)
                Category.customCache[category.name] = updated
            }
        }
    }
}

@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var mergeSourceCategory by remember { mutableStateOf<Category?>(null) }

    // Preset color list for selector
    val presetColors = listOf("#FF9800", "#4CAF50", "#03A9F4", "#FF5722", "#E91E63", "#9C27B0", "#795548", "#F44336", "#009688", "#3F51B5", "#9E9E9E")

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
                Text("Manage Categories", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Outlined.Add, "Add Custom Category", tint = MaterialTheme.colorScheme.onBackground)
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CUSTOM CATEGORIES",
                    style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            val customCats = categories.filter { it.isCustom && !it.isArchived }
            if (customCats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom categories added yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(customCats.size) { idx ->
                    val category = customCats[idx]
                    SpendLessCard(onClick = { editingCategory = category }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category color swatch
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(category.color)))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(category.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                                val parentLabel = category.parentCategoryName?.let { "Parent: ${it.split("_").lastOrNull()?.lowercase()?.capitalize()}" } ?: "Custom category"
                                Text(parentLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Reordering arrows
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val reordered = customCats.toMutableList()
                                            val temp = reordered[idx]
                                            reordered[idx] = reordered[idx - 1]
                                            reordered[idx - 1] = temp
                                            viewModel.reorderCategories(reordered)
                                        }
                                    },
                                    enabled = idx > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, "Move Up", tint = if (idx > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (idx < customCats.lastIndex) {
                                            val reordered = customCats.toMutableList()
                                            val temp = reordered[idx]
                                            reordered[idx] = reordered[idx + 1]
                                            reordered[idx + 1] = temp
                                            viewModel.reorderCategories(reordered)
                                        }
                                    },
                                    enabled = idx < customCats.lastIndex,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, "Move Down", tint = if (idx < customCats.lastIndex) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Archived Categories Section
            val archivedCats = categories.filter { it.isArchived }
            if (archivedCats.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ARCHIVED CATEGORIES",
                        style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 3.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                items(archivedCats) { category ->
                    SpendLessCard(onClick = { editingCategory = category }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji, fontSize = 24.sp, modifier = Modifier.alpha(0.6f))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            Icon(Icons.Outlined.Unarchive, "Restore", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SYSTEM CATEGORIES",
                    style = DotMatrixLabel.copy(fontSize = 10.sp, letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            val systemCats = categories.filter { !it.isCustom }
            items(systemCats) { category ->
                SpendLessCard(onClick = { editingCategory = category }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(category.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("System built-in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Add category dialog
    if (showAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        var emojiInput by remember { mutableStateOf("🏷️") }
        var colorInput by remember { mutableStateOf("#FF9800") }
        var parentInput by remember { mutableStateOf<Category?>(null) }
        var budgetToggle by remember { mutableStateOf(true) }
        var analyticsToggle by remember { mutableStateOf(true) }

        var showParentDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Add Category", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emojiInput,
                            onValueChange = { emojiInput = it.take(2) },
                            label = { Text("Emoji") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        OutlinedTextField(
                            value = colorInput,
                            onValueChange = { colorInput = it },
                            label = { Text("Hex Color") },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { colorInput = hex }
                                    .border(
                                        width = if (colorInput == hex) 2.dp else 0.dp,
                                        color = if (colorInput == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Parent
                    Box {
                        OutlinedButton(
                            onClick = { showParentDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipShape
                        ) {
                            Text(parentInput?.displayName ?: "No Parent Category", color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false }
                        ) {
                            DropdownMenuItem(text = { Text("No Parent Category") }, onClick = { parentInput = null; showParentDropdown = false })
                            categories.filter { !it.isCustom && it.name != "UNCATEGORIZED" }.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text("${parent.emoji} ${parent.displayName}") },
                                    onClick = { parentInput = parent; showParentDropdown = false }
                                )
                            }
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include in Budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = budgetToggle, onCheckedChange = { budgetToggle = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include in Analytics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = analyticsToggle, onCheckedChange = { analyticsToggle = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        viewModel.addCategory(
                            displayName = nameInput.trim(),
                            emoji = emojiInput.ifBlank { "🏷️" },
                            color = colorInput,
                            parentCategoryName = parentInput?.name,
                            isBudgetTrackingEnabled = budgetToggle,
                            includeInAnalytics = analyticsToggle
                        )
                        showAddDialog = false
                    }
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Edit category dialog
    editingCategory?.let { category ->
        var nameInput by remember { mutableStateOf(category.displayName) }
        var emojiInput by remember { mutableStateOf(category.emoji) }
        var colorInput by remember { mutableStateOf(category.color) }
        var parentInput by remember { mutableStateOf(categories.firstOrNull { it.name == category.parentCategoryName }) }
        var budgetToggle by remember { mutableStateOf(category.isBudgetTrackingEnabled) }
        var analyticsToggle by remember { mutableStateOf(category.includeInAnalytics) }
        var archiveToggle by remember { mutableStateOf(category.isArchived) }

        var showParentDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Edit Category", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        enabled = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emojiInput,
                            onValueChange = { emojiInput = it.take(2) },
                            label = { Text("Emoji") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        OutlinedTextField(
                            value = colorInput,
                            onValueChange = { colorInput = it },
                            label = { Text("Hex Color") },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                            enabled = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { colorInput = hex }
                                    .border(
                                        width = if (colorInput == hex) 2.dp else 0.dp,
                                        color = if (colorInput == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Parent
                    Box {
                        OutlinedButton(
                            onClick = { showParentDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipShape
                        ) {
                            Text(parentInput?.displayName ?: "No Parent Category", color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false }
                        ) {
                            DropdownMenuItem(text = { Text("No Parent Category") }, onClick = { parentInput = null; showParentDropdown = false })
                            categories.filter { !it.isCustom && it.name != "UNCATEGORIZED" && it.name != category.name }.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text("${parent.emoji} ${parent.displayName}") },
                                    onClick = { parentInput = parent; showParentDropdown = false }
                                )
                            }
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include in Budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = budgetToggle, onCheckedChange = { budgetToggle = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include in Analytics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = analyticsToggle, onCheckedChange = { analyticsToggle = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Archive Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                        Switch(checked = archiveToggle, onCheckedChange = { archiveToggle = it })
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (category.isCustom) {
                        TextButton(
                            onClick = {
                                mergeSourceCategory = category
                                editingCategory = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Merge")
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteCategory(category)
                                editingCategory = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { editingCategory = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.updateCategory(
                                category = category,
                                newDisplayName = nameInput.trim(),
                                newEmoji = emojiInput.ifBlank { "🏷️" },
                                newColor = colorInput,
                                newParentCategoryName = parentInput?.name,
                                isBudgetTrackingEnabled = budgetToggle,
                                includeInAnalytics = analyticsToggle,
                                isArchived = archiveToggle
                            )
                            editingCategory = null
                        }
                    }) {
                        Text("Save", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )
    }

    // Merge categories confirmation dialog
    mergeSourceCategory?.let { source ->
        var showTargetDropdown by remember { mutableStateOf(false) }
        var targetCategory by remember { mutableStateOf<Category?>(null) }

        AlertDialog(
            onDismissRequest = { mergeSourceCategory = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Merge Categories", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Merge all transactions from ${source.displayName} into another category. This will delete ${source.displayName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box {
                        OutlinedButton(
                            onClick = { showTargetDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipShape
                        ) {
                            Text(targetCategory?.displayName ?: "Choose Target Category", color = MaterialTheme.colorScheme.onBackground)
                        }

                        DropdownMenu(
                            expanded = showTargetDropdown,
                            onDismissRequest = { showTargetDropdown = false }
                        ) {
                            categories.filter { it.name != source.name && it.name != "UNCATEGORIZED" && !it.isArchived }.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text("${target.emoji} ${target.displayName}") },
                                    onClick = {
                                        targetCategory = target
                                        showTargetDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetCategory?.let { target ->
                            viewModel.mergeCategories(source, target)
                            mergeSourceCategory = null
                        }
                    },
                    enabled = targetCategory != null
                ) {
                    Text("Confirm Merge", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { mergeSourceCategory = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
