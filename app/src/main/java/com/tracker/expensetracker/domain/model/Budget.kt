package com.tracker.expensetracker.domain.model

import java.math.BigDecimal
import java.time.YearMonth
import java.util.Locale

data class BudgetLimit(val categoryId: String?, val amount: Double)

enum class BudgetStatus { ON_TRACK, NEAR_LIMIT, LIMIT_REACHED, OVER_BUDGET }

data class BudgetProgress(
    val limit: BudgetLimit,
    val spent: Double,
    val fraction: Float,
    val status: BudgetStatus
)

fun calculateBudgetProgress(
    month: YearMonth,
    limits: List<BudgetLimit>,
    expenses: List<Expense>
): List<BudgetProgress> {
    val spending = expenses.asSequence()
        .filter { YearMonth.from(it.date) == month && it.amount.isFinite() }
        .groupingBy { it.category.id.uppercase(Locale.ROOT) }
        .fold(BigDecimal.ZERO) { total, expense -> total + BigDecimal.valueOf(expense.amount) }
    val total = spending.values.fold(BigDecimal.ZERO, BigDecimal::add)
    val warningThreshold = BigDecimal("0.8")

    return limits.filter { it.amount.isFinite() && it.amount > 0 }.map { limit ->
        val spent = limit.categoryId?.let { spending[it.uppercase(Locale.ROOT)] ?: BigDecimal.ZERO }
            ?: total
        val limitAmount = BigDecimal.valueOf(limit.amount)
        val status = when {
            spent > limitAmount -> BudgetStatus.OVER_BUDGET
            spent.compareTo(limitAmount) == 0 -> BudgetStatus.LIMIT_REACHED
            spent >= limitAmount * warningThreshold -> BudgetStatus.NEAR_LIMIT
            else -> BudgetStatus.ON_TRACK
        }
        BudgetProgress(limit, spent.toDouble(), (spent.toDouble() / limit.amount).toFloat(), status)
    }
}
