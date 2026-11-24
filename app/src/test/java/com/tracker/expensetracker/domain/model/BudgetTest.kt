package com.tracker.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class BudgetTest {
    private val month = YearMonth.of(2026, 11)
    private val overall = BudgetLimit(null, 100.0)
    private val food = BudgetLimit(ExpenseCategory.FOOD.id, 50.0)

    @Test
    fun totalsOnlyIncludeExpensesFromTheSelectedMonth() {
        val expenses = listOf(
            expense(30.0),
            expense(20.0, ExpenseCategory.TRANSPORT),
            expense(80.0).copy(date = month.minusMonths(1).atEndOfMonth()),
            expense(15.0).copy(date = month.plusMonths(1).atDay(1))
        )

        val budgets = calculateBudgetProgress(month, listOf(overall, food), expenses)

        assertEquals(listOf(50.0, 30.0), budgets.map { it.spent })
        assertEquals(listOf(BudgetStatus.ON_TRACK, BudgetStatus.ON_TRACK), budgets.map { it.status })
    }

    @Test
    fun warningAndLimitThresholdsAreInclusiveAndOverspendIsSeparate() {
        val cases = listOf(
            39.99 to BudgetStatus.ON_TRACK,
            40.0 to BudgetStatus.NEAR_LIMIT,
            49.99 to BudgetStatus.NEAR_LIMIT,
            50.0 to BudgetStatus.LIMIT_REACHED,
            50.01 to BudgetStatus.OVER_BUDGET
        )

        cases.forEach { (amount, expected) ->
            val result = calculateBudgetProgress(month, listOf(food), listOf(expense(amount))).single()
            assertEquals(expected, result.status)
        }
    }

    @Test
    fun categoryNamesAndArchivedStateDoNotAffectMembershipAndDecimalTotalsRemainExact() {
        val archived = ExpenseCategory.FOOD.copy(id = "food", displayName = "Dining", isArchived = true)
        val budget = BudgetLimit("FOOD", 0.30)

        val result = calculateBudgetProgress(
            month, listOf(budget), listOf(expense(0.10, archived), expense(0.20))
        ).single()

        assertEquals(0.30, result.spent, 0.0)
        assertEquals(BudgetStatus.LIMIT_REACHED, result.status)
    }

    private fun expense(amount: Double, category: ExpenseCategory = ExpenseCategory.FOOD) =
        Expense(title = "Sample", amount = amount, category = category, date = month.atDay(1))
}
