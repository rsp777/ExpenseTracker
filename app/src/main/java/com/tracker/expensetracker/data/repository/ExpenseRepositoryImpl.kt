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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> {
        // Stored categories can use different formats and have different display metadata.
        return expenseDao.getAllExpenses()
            .map { entities -> entities.filter { it.category == category }.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getExpenseById(id: Long): Expense? = withContext(ioDispatcher) {
        expenseDao.getExpenseById(id)?.toDomainModel()
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

    override suspend fun insertCategory(category: ExpenseCategory) = withContext(ioDispatcher) {
        categoryDao.insertCategory(CategoryEntity.fromDomainModel(category))
    }

    override suspend fun deleteCategory(category: ExpenseCategory) = withContext(ioDispatcher) {
        categoryDao.deleteCategory(CategoryEntity.fromDomainModel(category))
    }
}
