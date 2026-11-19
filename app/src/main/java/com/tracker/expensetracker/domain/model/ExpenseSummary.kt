package com.tracker.expensetracker.domain.model

data class ExpenseSummary(
    val totalSpent: Double = 0.0,
    val totalCount: Int = 0,
    val categoryTotals: Map<ExpenseCategory, Double> = emptyMap(),
    val highestCategory: ExpenseCategory? = null
)
