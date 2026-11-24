package com.tracker.expensetracker.data.repository

import com.tracker.expensetracker.data.local.dao.BudgetDao
import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.domain.model.BudgetLimit
import com.tracker.expensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.map
import java.util.Locale

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val onLimitsChanged: () -> Unit
) : BudgetRepository {
    override fun observeLimits() = budgetDao.observeLimits().map { rows ->
        rows.map { BudgetLimit(it.scopeId.ifEmpty { null }, it.amount) }
    }

    override suspend fun setLimit(limit: BudgetLimit) {
        require(limit.amount.isFinite() && limit.amount > 0)
        budgetDao.upsert(BudgetEntity(limit.categoryId.orEmpty().uppercase(Locale.ROOT), limit.amount))
        onLimitsChanged()
    }

    override suspend fun removeLimit(categoryId: String?) {
        budgetDao.remove(categoryId.orEmpty().uppercase(Locale.ROOT))
        onLimitsChanged()
    }
}
