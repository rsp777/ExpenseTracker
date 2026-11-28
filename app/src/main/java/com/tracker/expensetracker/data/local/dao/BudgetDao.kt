package com.tracker.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tracker.expensetracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY scopeId")
    fun observeLimits(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE scopeId = :scopeId")
    suspend fun remove(scopeId: String)
}
