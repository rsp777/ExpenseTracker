package com.tracker.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.RecurrenceCadence
import com.tracker.expensetracker.domain.model.RecurringExpense
import com.tracker.expensetracker.domain.model.RecurringExpenseDetails
import java.time.LocalDate

@Entity(tableName = "recurring_expenses", indices = [Index(value = ["paused", "nextDate"])])
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val startDate: LocalDate,
    val notes: String,
    val cadence: String,
    val nextDate: LocalDate,
    val lastPostedDate: LocalDate? = null,
    val paused: Boolean = false,
) {
    fun toDomain() = RecurringExpense(
        id,
        RecurringExpenseDetails(title, amount, category, startDate, notes, RecurrenceCadence.valueOf(cadence)),
        nextDate,
        paused,
    )

    fun toExpense() = ExpenseEntity(
        title = title,
        amount = amount,
        category = category,
        date = nextDate,
        notes = notes,
    )
}
