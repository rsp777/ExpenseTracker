package com.tracker.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.model.ExpenseSummary
import com.tracker.expensetracker.domain.model.ThemeMode
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val preferencesRepository: UserPreferencesRepository = object : UserPreferencesRepository {
        private val _currency = MutableStateFlow(AppCurrency.USD)
        private val _theme = MutableStateFlow(CustomAppTheme.DYNAMIC)
        private val _mode = MutableStateFlow(ThemeMode.SYSTEM)

        override val selectedCurrency: StateFlow<AppCurrency> = _currency.asStateFlow()
        override val selectedTheme: StateFlow<CustomAppTheme> = _theme.asStateFlow()
        override val themeMode: StateFlow<ThemeMode> = _mode.asStateFlow()

        override suspend fun setCurrency(currency: AppCurrency) {
            _currency.value = currency
        }
        override suspend fun setTheme(theme: CustomAppTheme) {
            _theme.value = theme
        }
        override suspend fun setThemeMode(mode: ThemeMode) {
            _mode.value = mode
        }
    }
) : ViewModel() {

    private val _filter = MutableStateFlow(ExpenseFilter.ALL)
    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _sheetState = MutableStateFlow<Pair<Boolean, Expense?>>(Pair(false, null))
    private val _isCurrencyDialogVisible = MutableStateFlow(false)
    private val _isAddCategoryDialogVisible = MutableStateFlow(false)
    private val _isThemeDialogVisible = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val dataFlow = combine(
        repository.getAllExpenses(),
        repository.getAllCategories(),
        preferencesRepository.selectedCurrency
    ) { expenses, categories, currency ->
        Triple(expenses, categories, currency)
    }

    private val themeFlow = combine(
        preferencesRepository.selectedTheme,
        preferencesRepository.themeMode
    ) { theme, mode ->
        Pair(theme, mode)
    }

    private val filterFlow = combine(
        _filter,
        _selectedCategory
    ) { filter, category ->
        Pair(filter, category)
    }

    private val dialogsFlow = combine(
        _sheetState,
        _isCurrencyDialogVisible,
        _isAddCategoryDialogVisible,
        _isThemeDialogVisible
    ) { sheetState, currencyVisible, addCatVisible, themeVisible ->
        DialogsState(
            isAddEditSheetOpen = sheetState.first,
            editingExpense = sheetState.second,
            isCurrencyDialogVisible = currencyVisible,
            isAddCategoryDialogVisible = addCatVisible,
            isThemeDialogVisible = themeVisible
        )
    }

    val uiState: StateFlow<ExpenseUiState> = combine(
        dataFlow,
        themeFlow,
        filterFlow,
        dialogsFlow,
        _userMessage
    ) { data, themePair, filters, dialogs, message ->
        val (allExpenses, categories, currency) = data
        val (theme, mode) = themePair
        val (filter, category) = filters
        val filteredExpenses = applyFilter(allExpenses, filter, category)
        val summary = calculateSummary(filteredExpenses)
        ExpenseUiState(
            expenses = filteredExpenses,
            summary = summary,
            filter = filter,
            selectedCategory = category,
            categories = categories,
            currency = currency,
            theme = theme,
            themeMode = mode,
            isAddEditSheetOpen = dialogs.isAddEditSheetOpen,
            isCurrencyDialogVisible = dialogs.isCurrencyDialogVisible,
            isAddCategoryDialogVisible = dialogs.isAddCategoryDialogVisible,
            isThemeDialogVisible = dialogs.isThemeDialogVisible,
            editingExpense = dialogs.editingExpense,
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

    fun onOpenCurrencyDialog() {
        _isCurrencyDialogVisible.value = true
    }

    fun onDismissCurrencyDialog() {
        _isCurrencyDialogVisible.value = false
    }

    fun onSelectCurrency(currency: AppCurrency) {
        viewModelScope.launch {
            preferencesRepository.setCurrency(currency)
            _isCurrencyDialogVisible.value = false
            _userMessage.value = "Currency set to ${currency.code} (${currency.symbol})"
        }
    }

    fun onOpenThemeDialog() {
        _isThemeDialogVisible.value = true
    }

    fun onDismissThemeDialog() {
        _isThemeDialogVisible.value = false
    }

    fun onSelectTheme(theme: CustomAppTheme) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
            _userMessage.value = "Theme updated to ${theme.displayName}"
        }
    }

    fun onSelectThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            _userMessage.value = "Appearance mode: ${mode.displayName}"
        }
    }

    fun onOpenAddCategory() {
        _isAddCategoryDialogVisible.value = true
    }

    fun onDismissAddCategory() {
        _isAddCategoryDialogVisible.value = false
    }

    fun onAddCategory(name: String, colorHex: Long, iconName: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _userMessage.value = "Category name cannot be empty"
            return
        }
        viewModelScope.launch {
            val slug = trimmedName.uppercase().replace(" ", "_")
            val newCategory = ExpenseCategory(
                id = slug,
                displayName = trimmedName,
                colorHex = colorHex,
                iconName = iconName,
                isDefault = false
            )
            repository.insertCategory(newCategory)
            _isAddCategoryDialogVisible.value = false
            _userMessage.value = "Category '$trimmedName' created successfully"
        }
    }

    fun onDeleteCategory(category: ExpenseCategory) {
        if (category.isDefault) {
            _userMessage.value = "Default categories cannot be deleted"
            return
        }
        viewModelScope.launch {
            repository.deleteCategory(category)
            _userMessage.value = "Category '${category.displayName}' deleted"
        }
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

    private data class DialogsState(
        val isAddEditSheetOpen: Boolean = false,
        val editingExpense: Expense? = null,
        val isCurrencyDialogVisible: Boolean = false,
        val isAddCategoryDialogVisible: Boolean = false,
        val isThemeDialogVisible: Boolean = false
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication)
                ExpenseViewModel(
                    repository = application.container.expenseRepository,
                    preferencesRepository = application.container.userPreferencesRepository
                )
            }
        }
    }
}
