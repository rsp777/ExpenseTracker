package com.tracker.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.BudgetProgress
import com.tracker.expensetracker.domain.model.BudgetStatus
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onSave: suspend (String?, Double) -> Boolean,
    onRemove: suspend (String?) -> Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val overall = state.progress.firstOrNull { it.limit.categoryId == null }
    val categoryBudgets = state.progress.filter { it.limit.categoryId != null }
    val availableCategories = state.categories.filter { category ->
        !category.isArchived && categoryBudgets.none { it.limit.categoryId.equals(category.id, ignoreCase = true) }
    }

    var choosingCategory by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun categoryName(id: String?) =
        if (id == null) "Monthly budget"
        else state.categories.firstOrNull { it.id.equals(id, ignoreCase = true) }?.displayName ?: id

    fun openEditor(id: String?, amount: Double? = null) {
        categoryId = id
        amountText = amount?.toBigDecimal()?.stripTrailingZeros()?.toPlainString().orEmpty()
        error = null
        editing = true
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (overall == null) {
                        Text("Monthly budget", style = MaterialTheme.typography.titleMedium)
                    } else {
                        BudgetProgressBar("Monthly budget", overall, state.currency)
                    }
                    TextButton(onClick = { openEditor(null, overall?.limit?.amount) }) {
                        Text(if (overall == null) "Set monthly limit" else "Edit monthly limit")
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category budgets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (availableCategories.isNotEmpty()) {
                    TextButton(onClick = { choosingCategory = true }) { Text("Add budget") }
                }
            }
        }
        items(categoryBudgets, key = { it.limit.categoryId.orEmpty() }) { budget ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val name = categoryName(budget.limit.categoryId)
                    BudgetProgressBar(name, budget, state.currency)
                    TextButton(onClick = { openEditor(budget.limit.categoryId, budget.limit.amount) }) {
                        Text("Edit $name")
                    }
                }
            }
        }
        item { BudgetAlertSettings() }
    }

    if (choosingCategory) {
        AlertDialog(
            onDismissRequest = { choosingCategory = false },
            title = { Text("Choose a category") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(availableCategories, key = { it.id }) { category ->
                        Text(
                            category.displayName,
                            modifier = Modifier.fillMaxWidth().clickable {
                                choosingCategory = false
                                openEditor(category.id)
                            }.padding(14.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosingCategory = false }) { Text("Cancel") } }
        )
    }

    if (editing) {
        val existing = state.progress.firstOrNull { it.limit.categoryId.equals(categoryId, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { if (!saving) editing = false },
            title = { Text(categoryName(categoryId)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            error = null
                         },
                        label = { Text("Monthly limit (${state.currency.symbol})") },
                        singleLine = true,
                        enabled = !saving,
                        isError = error != null,
                        supportingText = error?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (existing != null) {
                        TextButton(enabled = !saving, onClick = {
                            saving = true
                            scope.launch {
                                try {
                                    if (onRemove(categoryId)) editing = false
                                    else error = "Couldn’t remove this limit. Try again."
                                } finally {
                                    saving = false
                                }
                            }
                        }) { Text("Remove limit") }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    val amount = amountText.trim().toDoubleOrNull()
                    if (amount == null || !amount.isFinite() || amount <= 0) {
                        error = "Enter a limit greater than 0"
                    } else {
                        saving = true
                        scope.launch {
                            try {
                                if (onSave(categoryId, amount)) editing = false
                                else error = "Couldn’t save this limit. Try again."
                            } finally {
                                saving = false
                            }
                        }
                    }
                }) { Text("Save limit") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { editing = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BudgetProgressBar(
    title: String,
    progress: BudgetProgress,
    currency: AppCurrency,
    modifier: Modifier = Modifier
) {
    val status = when (progress.status) {
        BudgetStatus.ON_TRACK -> "On track"
        BudgetStatus.NEAR_LIMIT -> "Near limit"
        BudgetStatus.LIMIT_REACHED -> "Limit reached"
        BudgetStatus.OVER_BUDGET -> "Over budget"
    }
    val color = when (progress.status) {
        BudgetStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        BudgetStatus.NEAR_LIMIT -> MaterialTheme.colorScheme.tertiary
        BudgetStatus.LIMIT_REACHED, BudgetStatus.OVER_BUDGET -> MaterialTheme.colorScheme.error
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            "${currency.format(progress.spent)} of ${currency.format(progress.limit.amount)}",
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
        Text(status, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
