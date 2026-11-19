package com.tracker.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.model.ExpenseSummary
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun ExpenseCategory.getIcon(): ImageVector {
    return when (this) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingCart
        ExpenseCategory.BILLS -> Icons.Default.Receipt
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
        ExpenseCategory.HEALTH -> Icons.Default.FitnessCenter
        ExpenseCategory.EDUCATION -> Icons.Default.School
        ExpenseCategory.OTHER -> Icons.Default.Category
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

@Composable
fun SummaryCard(
    summary: ExpenseSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Total Expenses",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(summary.totalSpent),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.totalCount} transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                summary.highestCategory?.let { topCat ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(topCat.colorHex).copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "Top: ${topCat.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipRow(
    currentFilter: ExpenseFilter,
    selectedCategory: ExpenseCategory?,
    onFilterSelected: (ExpenseFilter, ExpenseCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExpenseFilter.values().filter { it != ExpenseFilter.BY_CATEGORY }.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter && selectedCategory == null,
                onClick = { onFilterSelected(filter, null) },
                label = { Text(filter.label) }
            )
        }

        // Category filter chips
        ExpenseCategory.values().forEach { cat ->
            FilterChip(
                selected = currentFilter == ExpenseFilter.BY_CATEGORY && selectedCategory == cat,
                onClick = { onFilterSelected(ExpenseFilter.BY_CATEGORY, cat) },
                label = { Text(cat.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = cat.getIcon(),
                        contentDescription = cat.displayName,
                        tint = Color(cat.colorHex),
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(expense.category.colorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = expense.category.getIcon(),
                    contentDescription = expense.category.displayName,
                    tint = Color(expense.category.colorHex),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${expense.category.displayName} • ${expense.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
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
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete '${expense.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: ExpenseCategory, date: LocalDate, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountText by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(expenseToEdit?.category ?: ExpenseCategory.FOOD) }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (expenseToEdit == null) "Add Expense" else "Edit Expense",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Amount ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Title / Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ExpenseCategory.values()) { cat ->
                    val isSelected = selectedCategory == cat
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        leadingIcon = {
                            Icon(
                                imageVector = cat.getIcon(),
                                contentDescription = null,
                                tint = Color(cat.colorHex),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "Please enter a title"
                        return@Button
                    }
                    onSave(
                        title,
                        amount,
                        selectedCategory,
                        expenseToEdit?.date ?: LocalDate.now(),
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (expenseToEdit == null) "Add Expense" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
