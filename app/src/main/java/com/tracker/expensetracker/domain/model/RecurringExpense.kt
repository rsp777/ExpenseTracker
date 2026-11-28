package com.tracker.expensetracker.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class RecurrenceCadence {
    DAILY, WEEKLY, MONTHLY, YEARLY;

    fun firstOnOrAfter(anchor: LocalDate, date: LocalDate): LocalDate {
        if (!date.isAfter(anchor)) return anchor
        val periods = when (this) {
            DAILY -> ChronoUnit.DAYS.between(anchor, date)
            WEEKLY -> ChronoUnit.WEEKS.between(anchor, date)
            MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(date))
            YEARLY -> date.year.toLong() - anchor.year
        }
        val candidate = occurrence(anchor, periods)
        return if (candidate.isBefore(date)) occurrence(anchor, periods + 1) else candidate
    }

    private fun occurrence(anchor: LocalDate, periods: Long): LocalDate = when (this) {
        DAILY -> anchor.plusDays(periods)
        WEEKLY -> anchor.plusWeeks(periods)
        MONTHLY -> anchor.plusMonths(periods)
        YEARLY -> anchor.plusYears(periods)
    }
}

data class RecurringExpenseDetails(
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val startDate: LocalDate,
    val notes: String,
    val cadence: RecurrenceCadence,
)

data class RecurringExpense(
    val id: Long,
    val details: RecurringExpenseDetails,
    val nextDate: LocalDate,
    val paused: Boolean,
)
