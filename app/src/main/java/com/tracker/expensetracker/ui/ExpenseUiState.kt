package com.tracker.expensetracker.ui

import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.model.ExpenseSummary

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val summary: ExpenseSummary = ExpenseSummary(),
    val filter: ExpenseFilter = ExpenseFilter.ALL,
    val selectedCategory: ExpenseCategory? = null,
    val categories: List<ExpenseCategory> = ExpenseCategory.DEFAULT_CATEGORIES,
    val currency: AppCurrency = AppCurrency.USD,
    val isAddEditSheetOpen: Boolean = false,
    val isCurrencyDialogVisible: Boolean = false,
    val isAddCategoryDialogVisible: Boolean = false,
    val editingExpense: Expense? = null,
    val userMessage: String? = null
)
