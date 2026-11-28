package com.tracker.expensetracker.data.backup

import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.ThemeMode
import java.util.Locale

internal data class BackupPreferences(
    val currency: AppCurrency,
    val theme: CustomAppTheme,
    val themeMode: ThemeMode,
)

internal data class BackupSnapshot(
    val expenses: List<ExpenseEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val preferences: BackupPreferences,
)

internal data class RestorePlan(
    val expenses: List<ExpenseEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
)

internal fun planRestore(local: BackupSnapshot, backup: BackupSnapshot): RestorePlan {
    val categoryIds = local.categories.mapTo(hashSetOf()) { normalizedId(it.id) }
    val newCategories = backup.categories.filter { categoryIds.add(normalizedId(it.id)) }
    val budgetIds = local.budgets.mapTo(hashSetOf()) { normalizedId(it.scopeId) }
    val newBudgets = backup.budgets.filter { budgetIds.add(normalizedId(it.scopeId)) }

    val remainingMatches = local.expenses.groupingBy { it.restoreKey() }.eachCount().toMutableMap()
    val newExpenses = buildList {
        for (expense in backup.expenses) {
            val key = expense.restoreKey()
            val remaining = remainingMatches[key] ?: 0
            if (remaining > 0) {
                remainingMatches[key] = remaining - 1
            } else {
                // Numeric IDs are local to each installation. Room assigns a new one.
                add(expense.copy(id = 0))
            }
        }
    }
    return RestorePlan(newExpenses, newCategories, newBudgets)
}

internal fun normalizedId(id: String): String = id.uppercase(Locale.ROOT)

private fun ExpenseEntity.restoreKey(): ExpenseEntity = copy(
    id = 0,
    category = category.copy(
        id = normalizedId(category.id),
        displayName = "",
        colorHex = 0,
        iconName = "",
        isDefault = false,
        isArchived = false,
    ),
)
