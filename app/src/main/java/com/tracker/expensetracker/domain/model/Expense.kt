package com.tracker.expensetracker.domain.model

import java.time.LocalDate

data class Expense(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: LocalDate = LocalDate.now(),
    val notes: String = ""
)
