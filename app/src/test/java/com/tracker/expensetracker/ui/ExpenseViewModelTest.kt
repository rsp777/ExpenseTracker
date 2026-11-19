package com.tracker.expensetracker.ui

import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ExpenseFilter
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private val repository: ExpenseRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ExpenseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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

        viewModel = ExpenseViewModel(repository)
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

        viewModel = ExpenseViewModel(repository)
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

        viewModel = ExpenseViewModel(repository)
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
    fun onDeleteExpense_deletesExpenseAndShowsMessage() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        val expense = Expense(id = 10, title = "Gym", amount = 45.0, category = ExpenseCategory.HEALTH)

        viewModel = ExpenseViewModel(repository)
        viewModel.onDeleteExpense(expense)
        advanceUntilIdle()

        coVerify { repository.deleteExpense(expense) }
        assertEquals("Expense deleted", viewModel.uiState.value.userMessage)
    }
}
