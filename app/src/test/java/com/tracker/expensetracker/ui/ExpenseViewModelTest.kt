package com.tracker.expensetracker.ui

import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
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
        override val selectedCurrency: StateFlow<AppCurrency> = _currency.asStateFlow()
        override suspend fun setCurrency(currency: AppCurrency) {
            _currency.value = currency
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
        viewModel.onFilterSelected(ExpenseFilter.BY_CATEGORY, ExpenseCategory.TRANSPORT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.expenses.size)
        assertEquals("Metro", state.expenses[0].title)
        assertEquals(10.0, state.summary.totalSpent, 0.001)
    }

    @Test
    fun onSaveExpense_insertsNewExpense() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { repository.insertExpense(any()) } returns 1L

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSaveExpense(
            title = "Coffee",
            amount = 4.5,
            category = ExpenseCategory.FOOD,
            date = LocalDate.now(),
            notes = "Morning latte"
        )
        advanceUntilIdle()

        coVerify {
            repository.insertExpense(match {
                it.title == "Coffee" && it.amount == 4.5 && it.category == ExpenseCategory.FOOD
            })
        }
        assertEquals("Expense added successfully", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSaveExpense_updatesExistingExpense() = runTest(testDispatcher) {
        val existing = Expense(id = 5, title = "Lunch", amount = 12.0, category = ExpenseCategory.FOOD)
        every { repository.getAllExpenses() } returns flowOf(listOf(existing))
        coEvery { repository.updateExpense(any()) } returns Unit

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onOpenEditExpense(existing)
        viewModel.onSaveExpense(
            title = "Business Lunch",
            amount = 25.0,
            category = ExpenseCategory.FOOD,
            date = existing.date,
            notes = "With client"
        )
        advanceUntilIdle()

        coVerify {
            repository.updateExpense(match {
                it.id == 5L && it.title == "Business Lunch" && it.amount == 25.0
            })
        }
        assertEquals("Expense updated successfully", viewModel.uiState.value.userMessage)
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
    fun onAddCategory_insertsCategoryAndShowsMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        coEvery { repository.insertCategory(any()) } returns Unit

        viewModel = ExpenseViewModel(repository, preferencesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onAddCategory("Streaming", 0xFFFFCA28, "Movie")
        advanceUntilIdle()

        coVerify {
            repository.insertCategory(match {
                it.displayName == "Streaming" && it.colorHex == 0xFFFFCA28 && it.iconName == "Movie"
            })
        }
        assertEquals("Category 'Streaming' created successfully", viewModel.uiState.value.userMessage)
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
}
