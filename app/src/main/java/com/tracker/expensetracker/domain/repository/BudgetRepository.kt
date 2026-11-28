package com.tracker.expensetracker.domain.repository

import com.tracker.expensetracker.domain.model.BudgetLimit
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeLimits(): Flow<List<BudgetLimit>>
    suspend fun setLimit(limit: BudgetLimit)
    suspend fun removeLimit(categoryId: String?)
}
