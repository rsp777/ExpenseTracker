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
import kotlinx.coroutines.flow.update
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

    private val _transactionFilters = MutableStateFlow(TransactionFilters())
    private val _sheetState = MutableStateFlow<Pair<Boolean, Expense?>>(Pair(false, null))
    private var isSavingExpense = false
    private val _isCurrencyDialogVisible = MutableStateFlow(false)
    private val _categoryEditor = MutableStateFlow<CategoryEditorState?>(null)
    private val _isCategoryManagerVisible = MutableStateFlow(false)
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

    private val dialogsFlow = combine(
        _sheetState,
        _isCurrencyDialogVisible,
        _categoryEditor,
        _isCategoryManagerVisible,
        _isThemeDialogVisible
    ) { sheetState, currencyVisible, categoryEditor, categoryManagerVisible, themeVisible ->
        DialogsState(
            isAddEditSheetOpen = sheetState.first,
            editingExpense = sheetState.second,
            isCurrencyDialogVisible = currencyVisible,
            categoryEditor = categoryEditor,
            isCategoryManagerVisible = categoryManagerVisible,
            isThemeDialogVisible = themeVisible
        )
    }

    val uiState: StateFlow<ExpenseUiState> = combine(
        dataFlow,
        themeFlow,
        _transactionFilters,
        dialogsFlow,
        _userMessage
    ) { data, themePair, filters, dialogs, message ->
        val (allExpenses, categories, currency) = data
        val (theme, mode) = themePair
        val filteredExpenses = applyFilters(allExpenses, filters)
        val summary = calculateSummary(filteredExpenses)
        ExpenseUiState(
            expenses = filteredExpenses,
            summary = summary,
            filter = filters.dateFilter,
            selectedCategory = filters.category,
            searchQuery = filters.query,
            categories = categories,
            currency = currency,
            theme = theme,
            themeMode = mode,
            isAddEditSheetOpen = dialogs.isAddEditSheetOpen,
            isCurrencyDialogVisible = dialogs.isCurrencyDialogVisible,
            categoryEditor = dialogs.categoryEditor,
            isCategoryManagerVisible = dialogs.isCategoryManagerVisible,
            isThemeDialogVisible = dialogs.isThemeDialogVisible,
            editingExpense = dialogs.editingExpense,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _transactionFilters.update { it.copy(query = query) }
    }

    fun onDateFilterSelected(filter: ExpenseFilter) {
        _transactionFilters.update { it.copy(dateFilter = filter) }
    }

    fun onCategoryFilterSelected(category: ExpenseCategory) {
        _transactionFilters.update { current ->
            val alreadySelected = current.category?.hasSameId(category) == true
            current.copy(category = if (alreadySelected) null else category)
        }
    }

    fun onClearFilters() {
        _transactionFilters.value = TransactionFilters()
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

    fun onOpenCategoryManager() {
        _isCategoryManagerVisible.value = true
    }

    fun onDismissCategoryManager() {
        _isCategoryManagerVisible.value = false
    }

    fun onOpenAddCategory() {
        _categoryEditor.value = CategoryEditorState()
    }

    fun onOpenEditCategory(category: ExpenseCategory) {
        if (!category.isBuiltIn) _categoryEditor.value = CategoryEditorState(category)
    }

    fun onDismissCategoryEditor() {
        if (_categoryEditor.value?.isSaving != true) _categoryEditor.value = null
    }

    fun onCategoryNameChanged() {
        _categoryEditor.update { it?.copy(error = null) }
    }

    fun onSaveCategory(name: String, colorHex: Long, iconName: String) {
        val editor = _categoryEditor.value ?: return
        if (editor.isSaving) return
        val trimmedName = name.trim()
        val existing = editor.category
        val duplicate = uiState.value.categories.any {
            it.displayName.equals(trimmedName, ignoreCase = true) && existing?.hasSameId(it) != true
        }
        val error = when {
            trimmedName.isBlank() -> "Please enter a category name"
            duplicate -> "A category with this name already exists"
            else -> null
        }
        if (error != null) {
            _categoryEditor.value = editor.copy(error = error)
            return
        }

        val category = ExpenseCategory(
            id = existing?.id ?: ExpenseCategory.customIdForName(trimmedName),
            displayName = trimmedName,
            colorHex = colorHex,
            iconName = iconName,
            isArchived = existing?.isArchived ?: false,
        )
        _categoryEditor.value = editor.copy(error = null, isSaving = true)
        viewModelScope.launch {
            val saved = if (existing == null) repository.insertCategory(category) else repository.updateCategory(category)
            if (saved) {
                _categoryEditor.value = null
                _userMessage.value = if (existing == null) "Category '$trimmedName' created successfully"
                    else "Category '$trimmedName' updated"
            } else {
                val message = if (existing == null) "A category with this name already exists"
                    else "Couldn't update this category"
                _categoryEditor.value = editor.copy(error = message, isSaving = false)
            }
        }
    }

    fun onSetCategoryArchived(category: ExpenseCategory, archived: Boolean) {
        if (category.isBuiltIn) return
        viewModelScope.launch {
            if (repository.setCategoryArchived(category, archived)) {
                _userMessage.value = if (archived) "Category '${category.displayName}' archived"
                    else "Category '${category.displayName}' restored"
            }
        }
    }

    fun onDeleteCategory(category: ExpenseCategory) {
        if (category.isBuiltIn) {
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
        val sheet = _sheetState.value
        val (isOpen, editingExpense) = sheet
        if (!isOpen || isSavingExpense || title.isBlank() || !amount.isFinite() || amount <= 0.0) {
            return
        }

        if (category.isArchived && editingExpense?.category?.hasSameId(category) != true) {
            _userMessage.value = "Choose an active category"
            return
        }

        isSavingExpense = true
        viewModelScope.launch {
            try {
                val expense = Expense(
                    id = editingExpense?.id ?: 0,
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    date = date,
                    notes = notes.trim()
                )
                if (editingExpense == null) {
                    repository.insertExpense(expense)
                    _userMessage.value = "Expense added successfully"
                } else {
                    repository.updateExpense(expense)
                    _userMessage.value = "Expense updated successfully"
                }
                if (_sheetState.value === sheet) onDismissSheet()
            } finally {
                isSavingExpense = false
            }
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

    private fun applyFilters(expenses: List<Expense>, filters: TransactionFilters): List<Expense> {
        val today = LocalDate.now()
        val dateRange = when (filters.dateFilter) {
            ExpenseFilter.ALL -> null
            ExpenseFilter.TODAY -> today..today
            ExpenseFilter.THIS_WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                monday..monday.plusDays(6)
            }
            ExpenseFilter.THIS_MONTH -> today.withDayOfMonth(1)..today.with(TemporalAdjusters.lastDayOfMonth())
        }
        val category = filters.category
        val query = filters.query.trim()
        if (dateRange == null && category == null && query.isEmpty()) return expenses

        return expenses.filter { expense ->
            (dateRange == null || expense.date in dateRange) &&
                (category == null || expense.category.hasSameId(category)) &&
                (query.isEmpty() || expense.title.contains(query, ignoreCase = true) ||
                    expense.notes.contains(query, ignoreCase = true))
        }
    }

    private fun calculateSummary(expenses: List<Expense>): ExpenseSummary {
        if (expenses.isEmpty()) return ExpenseSummary()
        val total = expenses.sumOf { it.amount }
        val categoryTotals = expenses.groupBy { it.category.id.uppercase() }
            .values.associate { entries -> entries.first().category to entries.sumOf { it.amount } }
        val topCategory = categoryTotals.maxByOrNull { it.value }?.key

        return ExpenseSummary(
            totalSpent = total,
            totalCount = expenses.size,
            categoryTotals = categoryTotals,
            highestCategory = topCategory
        )
    }

    private data class TransactionFilters(
        val dateFilter: ExpenseFilter = ExpenseFilter.ALL,
        val category: ExpenseCategory? = null,
        val query: String = "",
    )

    private data class DialogsState(
        val isAddEditSheetOpen: Boolean = false,
        val editingExpense: Expense? = null,
        val isCurrencyDialogVisible: Boolean = false,
        val categoryEditor: CategoryEditorState? = null,
        val isCategoryManagerVisible: Boolean = false,
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
