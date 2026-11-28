package com.tracker.expensetracker.data.repository

import androidx.room.withTransaction
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.local.entity.RecurringExpenseEntity
import com.tracker.expensetracker.domain.model.RecurrenceCadence
import com.tracker.expensetracker.domain.model.RecurringExpenseDetails
import com.tracker.expensetracker.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RecurringExpenseRepositoryImpl(private val database: ExpenseDatabase) : RecurringExpenseRepository {
    private val schedules = database.recurringExpenseDao()

    override fun observeSchedules() = schedules.observeSchedules().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(id: Long?, details: RecurringExpenseDetails, today: LocalDate): Boolean =
        database.withTransaction {
            require(details.title.isNotBlank() && details.amount.isFinite() && details.amount > 0)
            val previous = if (id != null) schedules.find(id) ?: return@withTransaction false else null
            val hasSameDates = previous != null && previous.startDate == details.startDate &&
                previous.cadence == details.cadence.name
            val next = if (previous != null && hasSameDates) {
                previous.nextDate
            } else {
                details.cadence.firstOnOrAfter(details.startDate, afterLatestPost(today, previous?.lastPostedDate))
            }
            val row = RecurringExpenseEntity(
                id = previous?.id ?: 0,
                title = details.title.trim(),
                amount = details.amount,
                category = details.category,
                startDate = details.startDate,
                notes = details.notes.trim(),
                cadence = details.cadence.name,
                nextDate = next,
                lastPostedDate = previous?.lastPostedDate,
                paused = previous?.paused ?: false,
            )
            if (previous == null) schedules.insert(row) else schedules.update(row)
            true
        }

    override suspend fun setPaused(id: Long, paused: Boolean, today: LocalDate): Boolean =
        database.withTransaction {
            val row = schedules.find(id) ?: return@withTransaction false
            if (row.paused != paused) {
                val next = if (paused) row.nextDate else RecurrenceCadence.valueOf(row.cadence)
                    .firstOnOrAfter(row.startDate, afterLatestPost(today, row.lastPostedDate))
                schedules.update(row.copy(paused = paused, nextDate = next))
            }
            true
        }

    override suspend fun cancel(id: Long): Boolean = schedules.delete(id) > 0

    override suspend fun postDueExpenses(today: LocalDate): Int {
        var total = 0
        do {
            val posted = database.withTransaction {
                var count = 0
                while (count < POST_BATCH_SIZE) {
                    val due = schedules.findDue(today) ?: break
                    val following = RecurrenceCadence.valueOf(due.cadence)
                        .firstOnOrAfter(due.startDate, due.nextDate.plusDays(1))
                    // Advancing the schedule and inserting its transaction commit together.
                    database.expenseDao().insertExpense(due.toExpense())
                    schedules.update(due.copy(nextDate = following, lastPostedDate = due.nextDate))
                    count++
                }
                count
            }
            total += posted
        } while (posted == POST_BATCH_SIZE)
        return total
    }

    private fun afterLatestPost(today: LocalDate, lastPostedDate: LocalDate?): LocalDate =
        if (lastPostedDate != null && !lastPostedDate.isBefore(today)) lastPostedDate.plusDays(1) else today

    private companion object {
        const val POST_BATCH_SIZE = 50
    }
}
