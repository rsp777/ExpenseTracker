package com.tracker.expensetracker.data.repository

import com.tracker.expensetracker.data.local.converter.Converters
import com.tracker.expensetracker.data.local.dao.CategoryDao
import com.tracker.expensetracker.data.local.dao.ExpenseDao
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExpenseRepositoryImplTest {

    private val expenseDao: ExpenseDao = mockk(relaxed = true)
    private val categoryDao: CategoryDao = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        every { categoryDao.getAllCategories() } returns flowOf(emptyList())
        repository = ExpenseRepositoryImpl(expenseDao, categoryDao, testDispatcher)
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
    fun historicalExpensesUseCurrentCategoryDuringRenameAndArchive() = runTest(testDispatcher) {
        val original = ExpenseCategory("WORK:::TRAVEL", "Work:::Travel", 0xFF2E7D32, "Flight")
        val renamed = original.copy(displayName = "Business travel", colorHex = 0xFF42A5F5, iconName = "Work")
        val legacySnapshot = "${original.id}:::${original.displayName}:::${original.colorHex}:::Flight:::false"
        val storedCategory = checkNotNull(Converters().toCategory(legacySnapshot))
        val categories = MutableStateFlow(listOf(CategoryEntity.fromDomainModel(original)))
        val expense = ExpenseEntity(
            id = 11L, title = "Train", amount = 18.50, category = storedCategory, date = LocalDate.of(2026, 11, 1)
        )
        every { categoryDao.getAllCategories() } returns categories
        every { expenseDao.getAllExpenses() } returns flowOf(listOf(expense))

        assertEquals(original, repository.getExpensesByCategory(original).first().single().category)
        categories.value = listOf(CategoryEntity.fromDomainModel(renamed.copy(isArchived = true)))

        val result = repository.getExpensesByCategory(renamed).first().single()
        assertEquals(11L, result.id)
        assertEquals(renamed.copy(isArchived = true), result.category)
    }

    @Test
    fun builtInCategoriesCannotBeUpdatedOrArchived() = runTest(testDispatcher) {
        val legacyCategory = ExpenseCategory.FOOD.copy(isDefault = false)

        assertFalse(repository.updateCategory(legacyCategory))
        assertFalse(repository.setCategoryArchived(legacyCategory, true))

        coVerify(exactly = 0) { categoryDao.updateCategory(any(), any(), any(), any()) }
        coVerify(exactly = 0) { categoryDao.setCategoryArchived(any(), any()) }
    }

    @Test
    fun changesArchiveStateWithoutDeletingCategory() = runTest(testDispatcher) {
        val category = ExpenseCategory("PETS", "Pets", 0xFFFF9800, "Pets")
        coEvery { categoryDao.setCategoryArchived(category.id, any()) } returns 1

        assertTrue(repository.setCategoryArchived(category, true))
        assertTrue(repository.setCategoryArchived(category, false))

        coVerify { categoryDao.setCategoryArchived(category.id, true) }
        coVerify { categoryDao.setCategoryArchived(category.id, false) }
        coVerify(exactly = 0) { categoryDao.deleteCategory(any()) }
    }

    @Test
    fun getExpensesByCategory_matchesCategoryIdAcrossStoredSnapshots() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 11, 22)
        val savedCategory = ExpenseCategory("WORK:::TRAVEL", "Work:::Travel", 0xFF2E7D32, "Flight")
        val currentCategory = savedCategory.copy(displayName = "Business travel", iconName = "DirectionsCar")
        every { expenseDao.getAllExpenses() } returns flowOf(
            listOf(
                ExpenseEntity(id = 3L, title = "Lunch", amount = 15.0, category = ExpenseCategory.FOOD, date = date),
                ExpenseEntity(id = 2L, title = "Flight", amount = 45.0, category = savedCategory, date = date),
                ExpenseEntity(id = 1L, title = "Taxi", amount = 20.0, category = currentCategory, date = date)
            )
        )

        val result = repository.getExpensesByCategory(currentCategory).first()

        assertEquals(listOf(2L, 1L), result.map { it.id })
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

    @Test
    fun getAllCategories_returnsDefaultWhenDaoEmpty() = runTest(testDispatcher) {
        every { categoryDao.getAllCategories() } returns flowOf(emptyList())

        val result = repository.getAllCategories().first()

        assertEquals(ExpenseCategory.DEFAULT_CATEGORIES.size, result.size)
    }

    @Test
    fun getAllCategories_mapsCustomCategories() = runTest(testDispatcher) {
        val customEntities = listOf(
            CategoryEntity(id = "FITNESS", displayName = "Fitness", colorHex = 0xFF4CAF50, iconName = "FitnessCenter")
        )
        every { categoryDao.getAllCategories() } returns flowOf(customEntities)

        val result = repository.getAllCategories().first()

        assertEquals(1, result.size)
        assertEquals("Fitness", result[0].displayName)
        assertEquals(0xFF4CAF50, result[0].colorHex)
    }

    @Test
    fun insertCategory_delegatesToDao() = runTest(testDispatcher) {
        val category = ExpenseCategory(id = "PETS", displayName = "Pets", colorHex = 0xFFFF9800, iconName = "Pets")
        coEvery { categoryDao.insertCategory(any()) } returns 1L

        assertTrue(repository.insertCategory(category))

        coVerify { categoryDao.insertCategory(match { it.id == "PETS" && it.displayName == "Pets" }) }
    }

    @Test
    fun insertCategory_reportsIgnoredConflicts() = runTest(testDispatcher) {
        coEvery { categoryDao.insertCategory(any()) } returns -1L

        assertFalse(repository.insertCategory(ExpenseCategory.FOOD))
    }
}
