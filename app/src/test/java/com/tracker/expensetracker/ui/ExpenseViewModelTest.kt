package com.tracker.expensetracker.ui

import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.model.ThemeMode
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val repository: ExpenseRepository = mockk(relaxed = true)
    private lateinit var preferencesRepository: FakeUserPreferencesRepository
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ExpenseViewModel

    class FakeUserPreferencesRepository : UserPreferencesRepository {
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakeUserPreferencesRepository()
        every { repository.getAllCategories() } returns flowOf(ExpenseCategory.DEFAULT_CATEGORIES)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_computesSummaryCorrectly() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val expenses = listOf(
            Expense(id = 1, title = "Groceries", amount = 50.0, category = ExpenseCategory.FOOD, date = today),
            Expense(id = 2, title = "Dinner", amount = 30.0, category = ExpenseCategory.FOOD, date = today),
            Expense(id = 3, title = "Subway", amount = 20.0, category = ExpenseCategory.TRANSPORT, date = today)
        )
        every { repository.getAllExpenses() } returns flowOf(expenses)

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.expenses.size)
        assertEquals(100.0, state.summary.totalSpent, 0.001)
        assertEquals(3, state.summary.totalCount)
        assertEquals(ExpenseCategory.FOOD, state.summary.highestCategory)
        assertEquals(80.0, state.summary.categoryTotals[ExpenseCategory.FOOD] ?: 0.0, 0.001)
        assertEquals(20.0, state.summary.categoryTotals[ExpenseCategory.TRANSPORT] ?: 0.0, 0.001)
    }

    @Test
    fun uiState_filtersByCategoryCorrectly() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val expenses = listOf(
            Expense(id = 1, title = "Food", amount = 50.0, category = ExpenseCategory.FOOD, date = today),
            Expense(id = 2, title = "Metro", amount = 10.0, category = ExpenseCategory.TRANSPORT, date = today)
        )
        every { repository.getAllExpenses() } returns flowOf(expenses)

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        viewModel.onCategoryFilterSelected(ExpenseCategory.TRANSPORT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.expenses.size)
        assertEquals("Metro", state.expenses[0].title)
        assertEquals(10.0, state.summary.totalSpent, 0.001)
    }

    @Test
    fun transactionFilters_combineTitleOrNotesWithDateAndCategory() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val expenses = listOf(
            Expense(id = 1, title = "Coffee", amount = 5.0, category = ExpenseCategory.FOOD, date = today),
            Expense(id = 2, title = "Breakfast", amount = 7.0, category = ExpenseCategory.FOOD, date = today, notes = "Includes COFFEE"),
            Expense(id = 3, title = "Coffee", amount = 4.0, category = ExpenseCategory.FOOD, date = today.minusDays(1)),
            Expense(id = 4, title = "Coffee", amount = 8.0, category = ExpenseCategory.SHOPPING, date = today),
            Expense(id = 5, title = "Lunch", amount = 9.0, category = ExpenseCategory.FOOD, date = today)
        )
        every { repository.getAllExpenses() } returns flowOf(expenses)
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSearchQueryChanged("  cOfFeE  ")
        viewModel.onDateFilterSelected(ExpenseFilter.TODAY)
        viewModel.onCategoryFilterSelected(ExpenseCategory.FOOD)
        advanceUntilIdle()

        val filtered = viewModel.uiState.value
        assertEquals(listOf(1L, 2L), filtered.expenses.map { it.id })
        assertEquals(2, filtered.summary.totalCount)
        assertEquals(12.0, filtered.summary.totalSpent, 0.001)

        viewModel.onCategoryFilterSelected(ExpenseCategory.FOOD)
        advanceUntilIdle()

        val withoutCategory = viewModel.uiState.value
        assertEquals(listOf(1L, 2L, 4L), withoutCategory.expenses.map { it.id })
        assertEquals(ExpenseFilter.TODAY, withoutCategory.filter)
        assertEquals("  cOfFeE  ", withoutCategory.searchQuery)
    }

    @Test
    fun onClearFilters_restoresAllExpensesAndTheirSummary() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val expenses = listOf(
            Expense(id = 1, title = "Coffee", amount = 5.0, category = ExpenseCategory.FOOD, date = today),
            Expense(id = 2, title = "Taxi", amount = 8.0, category = ExpenseCategory.TRANSPORT, date = today.minusDays(1))
        )
        every { repository.getAllExpenses() } returns flowOf(expenses)
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSearchQueryChanged("No match")
        viewModel.onDateFilterSelected(ExpenseFilter.TODAY)
        viewModel.onCategoryFilterSelected(ExpenseCategory.FOOD)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasActiveFilters)
        assertTrue(viewModel.uiState.value.expenses.isEmpty())
        assertEquals(0, viewModel.uiState.value.summary.totalCount)

        viewModel.onClearFilters()
        advanceUntilIdle()

        val cleared = viewModel.uiState.value
        assertFalse(cleared.hasActiveFilters)
        assertEquals("", cleared.searchQuery)
        assertEquals(ExpenseFilter.ALL, cleared.filter)
        assertEquals(null, cleared.selectedCategory)
        assertEquals(listOf(1L, 2L), cleared.expenses.map { it.id })
        assertEquals(2, cleared.summary.totalCount)
        assertEquals(13.0, cleared.summary.totalSpent, 0.001)
    }

    @Test
    fun onSaveExpense_insertsNewExpense() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { repository.insertExpense(any()) } returns 1L

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val purchaseDate = LocalDate.now().minusDays(2)
        viewModel.onOpenAddExpense()
        viewModel.onSaveExpense(
            title = "Coffee",
            amount = 4.5,
            category = ExpenseCategory.FOOD,
            date = purchaseDate,
            notes = "Morning latte"
        )
        advanceUntilIdle()

        coVerify {
            repository.insertExpense(match {
                it.title == "Coffee" && it.amount == 4.5 &&
                    it.category == ExpenseCategory.FOOD && it.date == purchaseDate
            })
        }
        assertEquals("Expense added successfully", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSaveExpense_rejectsNonFiniteAndNonPositiveAmounts() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        viewModel.onOpenAddExpense()

        for (amount in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0)) {
            viewModel.onSaveExpense("Coffee", amount, ExpenseCategory.FOOD, LocalDate.now(), "")
        }
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertExpense(any()) }
    }

    @Test
    fun onSaveExpense_ignoresRepeatedSubmissionsForTheSameSheet() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { repository.insertExpense(any()) } returns 1L
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        viewModel.onOpenAddExpense()
        val date = LocalDate.now()

        viewModel.onSaveExpense("Coffee", 4.5, ExpenseCategory.FOOD, date, "")
        viewModel.onSaveExpense("Coffee", 4.5, ExpenseCategory.FOOD, date, "")
        advanceUntilIdle()
        viewModel.onSaveExpense("Coffee", 4.5, ExpenseCategory.FOOD, date, "")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.insertExpense(any()) }
    }

    @Test
    fun onSaveExpense_updatesExistingExpenseAndDateFilters() = runTest(testDispatcher) {
        val existing = Expense(id = 5, title = "Lunch", amount = 12.0, category = ExpenseCategory.FOOD)
        val savedExpenses = MutableStateFlow(listOf(existing))
        every { repository.getAllExpenses() } returns savedExpenses
        coEvery { repository.updateExpense(any()) } coAnswers {
            savedExpenses.value = listOf(firstArg<Expense>())
        }

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSearchQueryChanged("Lunch")
        viewModel.onCategoryFilterSelected(ExpenseCategory.FOOD)
        viewModel.onDateFilterSelected(ExpenseFilter.TODAY)
        advanceUntilIdle()
        assertEquals(listOf(existing), viewModel.uiState.value.expenses)

        val correctedDate = existing.date.minusDays(1)
        viewModel.onOpenEditExpense(existing)
        viewModel.onSaveExpense(
            title = "Business Lunch",
            amount = 25.0,
            category = ExpenseCategory.FOOD,
            date = correctedDate,
            notes = "With client"
        )
        advanceUntilIdle()

        coVerify {
            repository.updateExpense(match {
                it.id == 5L && it.title == "Business Lunch" && it.amount == 25.0 && it.date == correctedDate
            })
        }
        assertEquals("Expense updated successfully", viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.expenses.isEmpty())
        assertEquals(0, viewModel.uiState.value.summary.totalCount)

        viewModel.onDateFilterSelected(ExpenseFilter.ALL)
        advanceUntilIdle()

        val savedExpense = viewModel.uiState.value.expenses.single()
        assertEquals(existing.id, savedExpense.id)
        assertEquals(correctedDate, savedExpense.date)
    }

    @Test
    fun onDeleteExpense_deletesExpenseAndShowsMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        val expense = Expense(id = 10, title = "Gym", amount = 45.0, category = ExpenseCategory.HEALTH)

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onDeleteExpense(expense)
        advanceUntilIdle()

        coVerify { repository.deleteExpense(expense) }
        assertEquals("Expense deleted", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSelectCurrency_updatesCurrencyStateAndMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertEquals(AppCurrency.USD, viewModel.uiState.value.currency)

        viewModel.onSelectCurrency(AppCurrency.EUR)
        advanceUntilIdle()

        assertEquals(AppCurrency.EUR, viewModel.uiState.value.currency)
        assertEquals("Currency set to EUR (€)", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSaveCategory_insertsCategoryAndShowsMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { repository.insertCategory(any()) } returns true

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onOpenAddCategory()
        viewModel.onSaveCategory("Streaming", 0xFFFFCA28, "Movie")
        advanceUntilIdle()

        coVerify {
            repository.insertCategory(match {
                it.displayName == "Streaming" && it.colorHex == 0xFFFFCA28 && it.iconName == "Movie"
            })
        }
        assertEquals("Category 'Streaming' created successfully", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSaveCategory_editsCategoryWithoutChangingIdOrArchiveState() = runTest(testDispatcher) {
        val original = ExpenseCategory("TRAVEL", "Travel", 0xFF2E7D32, "Flight", isArchived = true)
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        every { repository.getAllCategories() } returns flowOf(listOf(original))
        coEvery { repository.updateCategory(any()) } returns true
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onOpenEditCategory(original)
        viewModel.onSaveCategory("Work travel", 0xFF42A5F5, "Work")
        advanceUntilIdle()

        coVerify { repository.updateCategory(original.copy(displayName = "Work travel", colorHex = 0xFF42A5F5, iconName = "Work")) }
        coVerify(exactly = 0) { repository.insertCategory(any()) }
        assertEquals(null, viewModel.uiState.value.categoryEditor)
    }

    @Test
    fun categoryFilter_keepsArchivedExpensesWithTheSameId() = runTest(testDispatcher) {
        val original = ExpenseCategory("TRAVEL", "Travel", 0xFF2E7D32, "Flight")
        val archived = original.copy(displayName = "Work travel", isArchived = true)
        val expenses = listOf(
            Expense(id = 1, title = "Train", amount = 20.0, category = original),
            Expense(id = 2, title = "Taxi", amount = 15.0, category = archived),
            Expense(id = 3, title = "Lunch", amount = 7.0, category = ExpenseCategory.FOOD)
        )
        every { repository.getAllExpenses() } returns flowOf(expenses)
        every { repository.getAllCategories() } returns flowOf(listOf(archived))
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onCategoryFilterSelected(archived)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.expenses.map { it.id })
        assertEquals(1, viewModel.uiState.value.summary.categoryTotals.size)
        assertEquals(35.0, viewModel.uiState.value.summary.totalSpent, 0.001)
    }

    @Test
    fun onSaveExpense_rejectsArchivedCategoryForNewExpense() = runTest(testDispatcher) {
        val archived = ExpenseCategory("TRAVEL", "Travel", 0xFF2E7D32, "Flight", isArchived = true)
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        viewModel = ExpenseViewModel(repository, preferencesRepository)

        viewModel.onOpenAddExpense()
        viewModel.onSaveExpense("Train", 20.0, archived, LocalDate.now(), "")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertExpense(any()) }
    }

    @Test
    fun onSaveCategory_showsInsertConflictAndKeepsEditorOpen() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { repository.insertCategory(any()) } returns false
        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onOpenAddCategory()
        viewModel.onSaveCategory(" food ", 0xFFFFCA28, "Restaurant")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.insertCategory(match { it.id == ExpenseCategory.FOOD.id }) }
        val editor = checkNotNull(viewModel.uiState.value.categoryEditor)
        assertEquals(false, editor.isSaving)
        assertEquals("A category with this name already exists", editor.error)

        viewModel.onCategoryNameChanged()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.categoryEditor?.error)
    }

    @Test
    fun onDeleteCategory_preventsDefaultCategoryDeletion() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onDeleteCategory(ExpenseCategory.FOOD)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.deleteCategory(any()) }
        assertEquals("Default categories cannot be deleted", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSelectTheme_updatesThemeStateAndMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertEquals(CustomAppTheme.DYNAMIC, viewModel.uiState.value.theme)

        viewModel.onSelectTheme(CustomAppTheme.AMETHYST_ROYAL)
        advanceUntilIdle()

        assertEquals(CustomAppTheme.AMETHYST_ROYAL, viewModel.uiState.value.theme)
        assertEquals("Theme updated to Amethyst Violet", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSelectThemeMode_updatesThemeModeAndMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        viewModel.onSelectThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals("Appearance mode: Dark Mode", viewModel.uiState.value.userMessage)
    }
}
