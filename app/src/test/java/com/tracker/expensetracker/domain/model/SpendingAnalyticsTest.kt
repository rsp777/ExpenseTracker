package com.tracker.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SpendingAnalyticsTest {
    private val today = LocalDate.of(2026, 11, 28)

    @Test
    fun rangeStartsMatchLabelsAndJanuaryYearToDate() {
        assertEquals(LocalDate.of(2026, 11, 22), AnalyticsRange.LAST_WEEK.startDate(today))
        assertEquals(LocalDate.of(2026, 10, 30), AnalyticsRange.LAST_30_DAYS.startDate(today))
        assertEquals(LocalDate.of(2026, 9, 1), AnalyticsRange.THREE_MONTHS.startDate(today))
        assertEquals(LocalDate.of(2026, 6, 1), AnalyticsRange.SIX_MONTHS.startDate(today))
        assertEquals(LocalDate.of(2026, 1, 1), AnalyticsRange.YEAR_TO_DATE.startDate(today))
    }

    @Test
    fun rangeExcludesPastBoundaryAndFutureAndAveragesAllDays() {
        val result = calculateSpendingAnalytics(
            listOf(
                expense(70.0, today.minusDays(6)),
                expense(700.0, today.minusDays(7)),
                expense(700.0, today.plusDays(1)),
                expense(Double.POSITIVE_INFINITY, today)
            ),
            AnalyticsRange.LAST_WEEK,
            today
        )

        assertEquals(7, result.days.size)
        assertEquals(0, result.total.compareTo(BigDecimal("70")))
        assertEquals(0, result.dailyAverage.compareTo(BigDecimal.TEN))
        assertEquals(6, result.days.count { it.amount.signum() == 0 })
    }

    @Test
    fun monthlyBarsIncludeEmptyMonthsAndCategoryUsesStableIdentity() {
        val renamed = ExpenseCategory.FOOD.copy(id = "food", displayName = "Dining", isArchived = true)
        val result = calculateSpendingAnalytics(
            listOf(
                expense(0.10, LocalDate.of(2026, 9, 5)),
                expense(0.20, today, renamed),
                expense(18.0, today, ExpenseCategory.TRANSPORT)
            ),
            AnalyticsRange.THREE_MONTHS,
            today
        )
        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11)), result.months.map { it.period })
        assertEquals(0, result.months[1].amount.signum())
        assertEquals(listOf("Transportation", "Dining"), result.categories.map { it.category.displayName })
        assertEquals(0, result.categories.last().amount.compareTo(BigDecimal("0.30")))
    }

    @Test
    fun noSpendingKeepsAnInclusiveLeapDayRange() {
        val result = calculateSpendingAnalytics(emptyList(), AnalyticsRange.LAST_30_DAYS, LocalDate.of(2024, 3, 1))
        assertEquals(LocalDate.of(2024, 2, 1), result.days.first().period)
        assertTrue(result.days.any { it.period == LocalDate.of(2024, 2, 29) })
        assertEquals(0, result.dailyAverage.signum())
        assertTrue(result.categories.isEmpty())
    }

    private fun expense(amount: Double, date: LocalDate, category: ExpenseCategory = ExpenseCategory.FOOD) =
        Expense(title = "Test", amount = amount, date = date, category = category)
}
