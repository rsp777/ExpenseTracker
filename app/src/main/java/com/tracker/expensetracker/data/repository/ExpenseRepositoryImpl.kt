package com.tracker.expensetracker.data.repository

import com.tracker.expensetracker.data.local.dao.CategoryDao
import com.tracker.expensetracker.data.local.dao.ExpenseDao
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> = observeExpenses(expenseDao.getAllExpenses())

    override fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>> =
        observeExpenses(expenseDao.getExpensesByDateRange(startDate, endDate))

    override fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> =
        getAllExpenses().map { expenses -> expenses.filter { it.category.hasSameId(category) } }
            .flowOn(ioDispatcher)

    private fun observeExpenses(expenses: Flow<List<ExpenseEntity>>): Flow<List<Expense>> =
        expenses.combine(getAllCategories()) { entities, categories ->
            val categoriesById = categories.associateBy { it.id.uppercase() }
            entities.map { entity ->
                val expense = entity.toDomainModel()
                val category = categoriesById[expense.category.id.uppercase()]
                if (category != null) expense.copy(category = category) else expense
            }
        }.flowOn(ioDispatcher)

    override suspend fun getExpenseById(id: Long): Expense? = withContext(ioDispatcher) {
        val expense = expenseDao.getExpenseById(id)?.toDomainModel() ?: return@withContext null
        val category = categoryDao.getCategoryById(expense.category.id)?.toDomainModel()
        if (category != null) expense.copy(category = category) else expense
    }

    override suspend fun insertExpense(expense: Expense): Long = withContext(ioDispatcher) {
        expenseDao.insertExpense(ExpenseEntity.fromDomainModel(expense))
    }

    override suspend fun updateExpense(expense: Expense) = withContext(ioDispatcher) {
        expenseDao.updateExpense(ExpenseEntity.fromDomainModel(expense))
    }

    override suspend fun deleteExpense(expense: Expense) = withContext(ioDispatcher) {
        expenseDao.deleteExpense(ExpenseEntity.fromDomainModel(expense))
    }

    override suspend fun deleteExpenseById(id: Long) = withContext(ioDispatcher) {
        expenseDao.deleteExpenseById(id)
    }

    override fun getAllCategories(): Flow<List<ExpenseCategory>> {
        return categoryDao.getAllCategories()
            .map { entities ->
                if (entities.isEmpty()) {
                    ExpenseCategory.DEFAULT_CATEGORIES
                } else {
                    entities.map { it.toDomainModel() }
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun insertCategory(category: ExpenseCategory): Boolean = withContext(ioDispatcher) {
        !category.isBuiltIn && categoryDao.insertCategory(CategoryEntity.fromDomainModel(category)) != -1L
    }

    override suspend fun updateCategory(category: ExpenseCategory): Boolean = withContext(ioDispatcher) {
        !category.isBuiltIn &&
            categoryDao.updateCategory(category.id, category.displayName, category.colorHex, category.iconName) > 0
    }

    override suspend fun setCategoryArchived(category: ExpenseCategory, archived: Boolean): Boolean =
        withContext(ioDispatcher) {
            !category.isBuiltIn && categoryDao.setCategoryArchived(category.id, archived) > 0
        }

    override suspend fun deleteCategory(category: ExpenseCategory) = withContext(ioDispatcher) {
        categoryDao.deleteCategory(CategoryEntity.fromDomainModel(category))
    }
}
