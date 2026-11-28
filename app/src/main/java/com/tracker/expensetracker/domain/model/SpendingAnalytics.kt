package com.tracker.expensetracker.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class AnalyticsRange(val label: String) {
    LAST_WEEK("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    THREE_MONTHS("3 months"),
    SIX_MONTHS("6 months"),
    YEAR_TO_DATE("Year to date");

    fun startDate(today: LocalDate): LocalDate = when (this) {
        LAST_WEEK -> today.minusDays(6)
        LAST_30_DAYS -> today.minusDays(29)
        THREE_MONTHS -> today.withDayOfMonth(1).minusMonths(2)
        SIX_MONTHS -> today.withDayOfMonth(1).minusMonths(5)
        YEAR_TO_DATE -> today.withDayOfYear(1)
    }
}

data class SpendingPoint<T>(val period: T, val amount: BigDecimal)

data class CategorySpending(val category: ExpenseCategory, val amount: BigDecimal)

data class SpendingAnalytics(
    val total: BigDecimal,
    val dailyAverage: BigDecimal,
    val days: List<SpendingPoint<LocalDate>>,
    val months: List<SpendingPoint<YearMonth>>,
    val categories: List<CategorySpending>
)

fun calculateSpendingAnalytics(
    expenses: List<Expense>,
    range: AnalyticsRange,
    today: LocalDate
): SpendingAnalytics {
    val start = range.startDate(today)
    val dayCount = ChronoUnit.DAYS.between(start, today).toInt() + 1
    val included = expenses.filter { it.date in start..today && it.amount.isFinite() && it.amount > 0 }

    val amountsByDay = included.groupingBy { it.date }.fold(BigDecimal.ZERO) { total, expense ->
        total + BigDecimal.valueOf(expense.amount)
    }
    val days = List(dayCount) { index ->
        val day = start.plusDays(index.toLong())
        SpendingPoint(day, amountsByDay[day] ?: BigDecimal.ZERO)
    }
    val months = days.groupingBy { YearMonth.from(it.period) }
        .fold(BigDecimal.ZERO) { total, day -> total + day.amount }
        .map { (month, amount) -> SpendingPoint(month, amount) }
    val categories = included.groupBy { it.category.id.uppercase(Locale.ROOT) }
        .map { (_, entries) ->
            val latest = entries.maxWith(compareBy<Expense> { it.date }.thenBy { it.id })
            val amount = entries.fold(BigDecimal.ZERO) { total, expense -> total + BigDecimal.valueOf(expense.amount) }
            CategorySpending(latest.category, amount)
        }.sortedWith(compareByDescending<CategorySpending> { it.amount }.thenBy { it.category.displayName })
    val total = amountsByDay.values.fold(BigDecimal.ZERO, BigDecimal::add)
    return SpendingAnalytics(
        total = total,
        dailyAverage = total.divide(BigDecimal.valueOf(dayCount.toLong()), MathContext.DECIMAL128),
        days = days,
        months = months,
        categories = categories
    )
}
