package com.tracker.expensetracker.domain.repository

import com.tracker.expensetracker.domain.model.RecurringExpense
import com.tracker.expensetracker.domain.model.RecurringExpenseDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecurringExpenseRepository {
    fun observeSchedules(): Flow<List<RecurringExpense>>
    suspend fun save(id: Long?, details: RecurringExpenseDetails, today: LocalDate): Boolean
    suspend fun setPaused(id: Long, paused: Boolean, today: LocalDate): Boolean
    suspend fun cancel(id: Long): Boolean
    suspend fun postDueExpenses(today: LocalDate): Int
}
