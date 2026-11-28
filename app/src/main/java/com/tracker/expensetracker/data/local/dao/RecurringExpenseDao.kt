package com.tracker.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tracker.expensetracker.data.local.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY paused, nextDate, id")
    fun observeSchedules(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun find(id: Long): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses WHERE paused = 0 AND nextDate <= :today ORDER BY nextDate, id LIMIT 1")
    suspend fun findDue(today: LocalDate): RecurringExpenseEntity?

    @Insert
    suspend fun insert(schedule: RecurringExpenseEntity): Long

    @Update
    suspend fun update(schedule: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun delete(id: Long): Int
}
