package com.tracker.expensetracker.data.repository

import com.tracker.expensetracker.data.local.dao.ExpenseDao
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExpenseRepositoryImplTest {

    private val expenseDao: ExpenseDao = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        repository = ExpenseRepositoryImpl(expenseDao, testDispatcher)
    }

    @Test
    fun getAllExpenses_mapsEntitiesToDomainModels() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val entities = listOf(
            ExpenseEntity(id = 1L, title = "Lunch", amount = 15.5, category = ExpenseCategory.FOOD, date = today),
            ExpenseEntity(id = 2L, title = "Bus", amount = 2.75, category = ExpenseCategory.TRANSPORT, date = today)
        )
        every { expenseDao.getAllExpenses() } returns flowOf(entities)

        val result = repository.getAllExpenses().first()

        assertEquals(2, result.size)
        assertEquals("Lunch", result[0].title)
        assertEquals(15.5, result[0].amount, 0.001)
        assertEquals(ExpenseCategory.FOOD, result[0].category)
        assertEquals("Bus", result[1].title)
    }

    @Test
    fun insertExpense_delegatesToDao() = runTest(testDispatcher) {
        val expense = Expense(title = "Coffee", amount = 4.5, category = ExpenseCategory.FOOD, date = LocalDate.now())
        coEvery { expenseDao.insertExpense(any()) } returns 42L

        val id = repository.insertExpense(expense)

        assertEquals(42L, id)
        coVerify { expenseDao.insertExpense(match { it.title == "Coffee" && it.amount == 4.5 }) }
    }

    @Test
    fun deleteExpense_delegatesToDao() = runTest(testDispatcher) {
        val expense = Expense(id = 99L, title = "Groceries", amount = 60.0, category = ExpenseCategory.SHOPPING, date = LocalDate.now())

        repository.deleteExpense(expense)

        coVerify { expenseDao.deleteExpense(match { it.id == 99L }) }
    }
}
