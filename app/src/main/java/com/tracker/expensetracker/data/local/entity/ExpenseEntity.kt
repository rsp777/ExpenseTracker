package com.tracker.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tracker.expensetracker.domain.model.Expense
import com.tracker.expensetracker.domain.model.ExpenseCategory
import java.time.LocalDate

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: LocalDate,
    val notes: String = ""
) {
    fun toDomainModel(): Expense = Expense(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = date,
        notes = notes
    )

    companion object {
        fun fromDomainModel(expense: Expense): ExpenseEntity = ExpenseEntity(
            id = expense.id,
            title = expense.title,
            amount = expense.amount,
            category = expense.category,
            date = expense.date,
            notes = expense.notes
        )
    }
}
