package com.tracker.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.model.ExpenseSummary
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ExpenseFilter.ALL)
    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _sheetState = MutableStateFlow<Pair<Boolean, Expense?>>(Pair(false, null))
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.getAllExpenses(),
        _filter,
        _selectedCategory,
        _sheetState,
        _userMessage
    ) { allExpenses, filter, category, sheetState, message ->
        val filteredExpenses = applyFilter(allExpenses, filter, category)
        val summary = calculateSummary(filteredExpenses)
        ExpenseUiState(
            expenses = filteredExpenses,
            summary = summary,
            filter = filter,
            selectedCategory = category,
            isAddEditSheetOpen = sheetState.first,
            editingExpense = sheetState.second,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState()
    )

    fun onFilterSelected(filter: ExpenseFilter, category: ExpenseCategory? = null) {
        _filter.value = filter
        _selectedCategory.value = category
    }

    fun onOpenAddExpense() {
        _sheetState.value = Pair(true, null)
    }

    fun onOpenEditExpense(expense: Expense) {
        _sheetState.value = Pair(true, expense)
    }

    fun onDismissSheet() {
        _sheetState.value = Pair(false, null)
    }

    fun onSaveExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        date: LocalDate,
        notes: String
    ) {
        viewModelScope.launch {
            val currentEditing = _sheetState.value.second
            if (currentEditing == null) {
                val newExpense = Expense(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    date = date,
                    notes = notes.trim()
                )
                repository.insertExpense(newExpense)
                _userMessage.value = "Expense added successfully"
            } else {
                val updated = currentEditing.copy(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    date = date,
                    notes = notes.trim()
                )
                repository.updateExpense(updated)
                _userMessage.value = "Expense updated successfully"
            }
            onDismissSheet()
        }
    }

    fun onDeleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _userMessage.value = "Expense deleted"
        }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    private fun applyFilter(
        expenses: List<Expense>,
        filter: ExpenseFilter,
        category: ExpenseCategory?
    ): List<Expense> {
        val today = LocalDate.now()
        return when (filter) {
            ExpenseFilter.ALL -> expenses
            ExpenseFilter.TODAY -> expenses.filter { it.date == today }
            ExpenseFilter.THIS_WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                expenses.filter { !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek) }
            }
            ExpenseFilter.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                expenses.filter { !it.date.isBefore(startOfMonth) && !it.date.isAfter(endOfMonth) }
            }
            ExpenseFilter.BY_CATEGORY -> {
                if (category == null) expenses else expenses.filter { it.category == category }
            }
        }
    }

    private fun calculateSummary(expenses: List<Expense>): ExpenseSummary {
        if (expenses.isEmpty()) return ExpenseSummary()
        val total = expenses.sumOf { it.amount }
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val topCategory = categoryTotals.maxByOrNull { it.value }?.key

        return ExpenseSummary(
            totalSpent = total,
            totalCount = expenses.size,
            categoryTotals = categoryTotals,
            highestCategory = topCategory
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication)
                ExpenseViewModel(application.container.expenseRepository)
            }
        }
    }
}
