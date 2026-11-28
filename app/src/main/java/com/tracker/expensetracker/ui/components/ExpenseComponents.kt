package com.tracker.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracker.expensetracker.BuildConfig
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.model.ExpenseSummary
import com.tracker.expensetracker.domain.model.ThemeMode
import com.tracker.expensetracker.ui.CategoryEditorState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun ExpenseCategory.getIcon(): ImageVector {
    return when (iconName.lowercase()) {
        "restaurant", "food" -> Icons.Default.Restaurant
        "directionscar", "directions_car", "transport" -> Icons.Default.DirectionsCar
        "shoppingcart", "shopping_cart", "shopping" -> Icons.Default.ShoppingCart
        "receipt", "bills" -> Icons.Default.Receipt
        "movie", "entertainment" -> Icons.Default.Movie
        "fitnesscenter", "fitness_center", "health", "fitness" -> Icons.Default.FitnessCenter
        "school", "education" -> Icons.Default.School
        "work", "business" -> Icons.Default.Work
        "flight", "travel" -> Icons.Default.Flight
        "home", "housing" -> Icons.Default.Home
        "pets", "pet" -> Icons.Default.Pets
        "savings", "investment" -> Icons.Default.Savings
        "star", "favorite" -> Icons.Default.Star
        "localcafe", "local_cafe", "cafe", "coffee" -> Icons.Default.LocalCafe
        "medicalservices", "medical_services", "medical" -> Icons.Default.MedicalServices
        else -> Icons.Default.Category
    }
}

val AVAILABLE_CATEGORY_ICONS: List<Pair<String, ImageVector>> = listOf(
    "Category" to Icons.Default.Category,
    "Restaurant" to Icons.Default.Restaurant,
    "DirectionsCar" to Icons.Default.DirectionsCar,
    "ShoppingCart" to Icons.Default.ShoppingCart,
    "Receipt" to Icons.Default.Receipt,
    "Movie" to Icons.Default.Movie,
    "FitnessCenter" to Icons.Default.FitnessCenter,
    "School" to Icons.Default.School,
    "Work" to Icons.Default.Work,
    "Flight" to Icons.Default.Flight,
    "Home" to Icons.Default.Home,
    "Pets" to Icons.Default.Pets,
    "Savings" to Icons.Default.Savings,
    "Star" to Icons.Default.Star,
    "LocalCafe" to Icons.Default.LocalCafe,
    "MedicalServices" to Icons.Default.MedicalServices
)

val CATEGORY_PALETTE_COLORS: List<Long> = listOf(
    0xFFFF7043, // Coral
    0xFF42A5F5, // Blue
    0xFFAB47BC, // Purple
    0xFFEF5350, // Red
    0xFFFFCA28, // Amber
    0xFF26A69A, // Teal
    0xFF7E57C2, // Deep Purple
    0xFF4CAF50, // Green
    0xFFE91E63, // Pink
    0xFF3F51B5, // Indigo
    0xFFFF9800, // Orange
    0xFF78909C  // Blue Grey
)

@Composable
fun SummaryCard(
    summary: ExpenseSummary,
    currency: AppCurrency,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Total expenses",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = currency.format(summary.totalSpent),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            summary.highestCategory?.let { category ->
                Text(
                    text = "Top category: ${category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun FilterChipRow(
    currentFilter: ExpenseFilter,
    selectedCategory: ExpenseCategory?,
    categories: List<ExpenseCategory>,
    onDateFilterSelected: (ExpenseFilter) -> Unit,
    onCategoryFilterSelected: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(100.dp)
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpenseFilter.entries.forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onDateFilterSelected(filter) },
                    label = { Text(filter.label) },
                    shape = shape,
                    colors = chipColors
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory?.hasSameId(category) == true,
                    onClick = { onCategoryFilterSelected(category) },
                    label = { Text(if (category.isArchived) "${category.displayName} (archived)" else category.displayName) },
                    shape = shape,
                    leadingIcon = {
                        Icon(
                            imageVector = category.getIcon(),
                            contentDescription = null,
                            tint = Color(category.colorHex),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = chipColors
                )
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    currency: AppCurrency,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(expense.category.colorHex).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = expense.category.getIcon(),
                    contentDescription = expense.category.displayName,
                    tint = Color(expense.category.colorHex),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.category.displayName} • ${expense.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currency.format(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete Expense",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Are you sure you want to delete '${expense.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseSheet(
    expenseToEdit: Expense?,
    categories: List<ExpenseCategory>,
    currency: AppCurrency,
    onDismiss: () -> Unit,
    onOpenAddCategory: () -> Unit,
    onSave: (title: String, amount: Double, category: ExpenseCategory, date: LocalDate, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountText by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var selectedCategory by remember {
        mutableStateOf(expenseToEdit?.category ?: categories.firstOrNull { !it.isArchived } ?: ExpenseCategory.FOOD)
    }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }
    var date by rememberSaveable(expenseToEdit?.id) { mutableStateOf(expenseToEdit?.date ?: LocalDate.now()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAmountError by remember { mutableStateOf(false) }
    var showTitleError by remember { mutableStateOf(false) }
    val amountRequester = remember { BringIntoViewRequester() }
    val titleRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = if (expenseToEdit == null) "Add Expense" else "Edit Expense",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    showAmountError = false
                },
                label = { Text("Amount (${currency.symbol})") },
                isError = showAmountError,
                supportingText = if (showAmountError) {
                    { Text("Enter an amount greater than 0") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(amountRequester)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    showTitleError = false
                },
                label = { Text("Title / Description") },
                isError = showTitleError,
                supportingText = if (showTitleError) {
                    { Text("Enter a title") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(titleRequester)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onOpenAddCategory) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Category", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectableCategories = categories.filter { category ->
                    !category.isArchived || expenseToEdit?.category?.hasSameId(category) == true
                }
                items(selectableCategories, key = { it.id }) { cat ->
                    val isSelected = selectedCategory.hasSameId(cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(if (cat.isArchived) "${cat.displayName} (archived)" else cat.displayName) },
                        shape = RoundedCornerShape(100.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = cat.getIcon(),
                                contentDescription = null,
                                tint = Color(cat.colorHex),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Date: ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || !amount.isFinite() || amount <= 0.0) {
                        showAmountError = true
                        scope.launch { amountRequester.bringIntoView() }
                        return@Button
                    }
                    if (title.isBlank()) {
                        showTitleError = true
                        scope.launch { titleRequester.bringIntoView() }
                        return@Button
                    }
                    onSave(
                        title,
                        amount,
                        selectedCategory,
                        date,
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (expenseToEdit == null) "Add Expense" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun CurrencySelectionDialog(
    currentCurrency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Currency",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(AppCurrency.SUPPORTED_CURRENCIES) { currency ->
                    val isSelected = currency.code == currentCurrency.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onCurrencySelected(currency) }
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(44.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currency.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CategoryManagerDialog(
    categories: List<ExpenseCategory>,
    onAdd: () -> Unit,
    onEdit: (ExpenseCategory) -> Unit,
    onSetArchived: (ExpenseCategory, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage categories") },
        text = {
            if (categories.isEmpty()) {
                Text("No custom categories yet.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(categories, key = { it.id }) { category ->
                        ListItem(
                            headlineContent = { Text(category.displayName) },
                            supportingContent = {
                                if (category.isArchived) Text("Archived")
                            },
                            leadingContent = {
                                Icon(category.getIcon(), contentDescription = null, tint = Color(category.colorHex))
                            },
                            trailingContent = {
                                Column {
                                    TextButton(onClick = { onEdit(category) }) { Text("Edit") }
                                    TextButton(onClick = { onSetArchived(category, !category.isArchived) }) {
                                        Text(if (category.isArchived) "Restore" else "Archive")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAdd) { Text("New category") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun CategoryEditorDialog(
    state: CategoryEditorState,
    onNameChanged: () -> Unit,
    onSave: (name: String, colorHex: Long, iconName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val category = state.category
    var name by rememberSaveable(category?.id) { mutableStateOf(category?.displayName.orEmpty()) }
    var selectedColor by rememberSaveable(category?.id) {
        mutableStateOf(category?.colorHex ?: CATEGORY_PALETTE_COLORS.first())
    }
    var selectedIconName by rememberSaveable(category?.id) {
        mutableStateOf(category?.iconName ?: AVAILABLE_CATEGORY_ICONS.first().first)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (category == null) "New Expense Category" else "Edit Expense Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onNameChanged()
                    },
                    label = { Text("Category Name") },
                    isError = state.error != null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                state.error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CATEGORY_PALETTE_COLORS) { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColor = colorHex }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AVAILABLE_CATEGORY_ICONS) { (iconName, iconVector) ->
                        val isSelected = selectedIconName == iconName
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedIconName = iconName },
                            color = if (isSelected) Color(selectedColor).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(selectedColor)) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = iconName,
                                    tint = if (isSelected) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val previewCat = ExpenseCategory(
                    id = "PREVIEW",
                    displayName = name.ifBlank { "Category Name" },
                    colorHex = selectedColor,
                    iconName = selectedIconName
                )
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color(selectedColor).copy(alpha = 0.2f),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = previewCat.getIcon(),
                            contentDescription = null,
                            tint = Color(selectedColor),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = previewCat.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(selectedColor)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, selectedColor, selectedIconName) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(if (category == null) "Create Category" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: CustomAppTheme,
    currentMode: ThemeMode,
    onThemeSelected: (CustomAppTheme) -> Unit,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Mode", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentMode == mode,
                                onClick = { onModeSelected(mode) },
                                label = { Text(mode.displayName) }
                            )
                        }
                    }
                }

                item {
                    Text("Color palette", style = MaterialTheme.typography.titleSmall)
                }

                items(CustomAppTheme.entries) { theme ->
                    val isSelected = currentTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onThemeSelected(theme) }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(theme.previewColorHex), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(theme.displayName, style = MaterialTheme.typography.bodyMedium)
                            if (theme == CustomAppTheme.DYNAMIC) {
                                Text(
                                    text = "Uses Android 12+ wallpaper colors",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        RadioButton(selected = isSelected, onClick = null)
                    }
                }

                item {
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
