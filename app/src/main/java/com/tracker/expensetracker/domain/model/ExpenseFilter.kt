package com.tracker.expensetracker.domain.model

enum class ExpenseFilter(val label: String) {
    ALL("All dates"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    TODAY("Today")
}
