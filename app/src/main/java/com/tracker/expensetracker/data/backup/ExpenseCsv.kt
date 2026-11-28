package com.tracker.expensetracker.data.backup

import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.AppCurrency
import java.io.Writer
import java.math.BigDecimal

internal fun Writer.writeExpenseCsv(expenses: List<ExpenseEntity>, currency: AppCurrency) {
    // Excel uses the BOM to detect UTF-8 in a CSV opened directly.
    write("\uFEFFDate,Title,Amount,Currency,Category,Notes\r\n")
    for (expense in expenses) {
        val amount = BigDecimal.valueOf(expense.amount).stripTrailingZeros().toPlainString()
        write(
            listOf(
                expense.date.toString(),
                csvText(expense.title),
                amount,
                currency.code,
                csvText(expense.category.displayName),
                csvText(expense.notes),
            ).joinToString(",")
        )
        write("\r\n")
    }
}

private fun csvText(value: String): String {
    val startsFormula = value.trimStart().firstOrNull()?.let { it in "=+-@＝＋－＠" } == true
    val startsControl = value.firstOrNull()?.let { it in "\t\r\n" } == true
    val safe = if (startsFormula || startsControl) "'$value" else value
    return "\"" + safe.replace("\"", "\"\"") + "\""
}
