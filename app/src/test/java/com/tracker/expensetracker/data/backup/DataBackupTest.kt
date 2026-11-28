package com.tracker.expensetracker.data.backup

import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ThemeMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter
import java.security.GeneralSecurityException
import java.time.LocalDate

class DataBackupTest {
    private val coffee = ExpenseEntity(
        id = 1,
        title = "Coffee",
        amount = 4.5,
        category = ExpenseCategory.FOOD,
        date = LocalDate.of(2026, 11, 28),
    )
    private val settings = BackupPreferences(AppCurrency.INR, CustomAppTheme.DYNAMIC, ThemeMode.SYSTEM)

    @Test
    fun csvUsesSpreadsheetCompatibleNumbersAndEscapesUntrustedText() {
        val expense = coffee.copy(title = " =SUM(1,2)", notes = "₹50, \"cash\"\nsecond line")
        val csv = StringWriter().apply { writeExpenseCsv(listOf(expense), AppCurrency.INR) }.toString()

        assertEquals(
            "\uFEFFDate,Title,Amount,Currency,Category,Notes\r\n" +
                "2026-11-28,\"' =SUM(1,2)\",4.5,INR,\"Food & Dining\",\"₹50, \"\"cash\"\"\nsecond line\"\r\n",
            csv,
        )
    }

    @Test
    fun restoreKeepsLocalConflictsAndOnlyAddsMissingCopies() {
        val localCategory = CategoryEntity("COFFEE", "Local label", 0xFF000000)
        val remoteCategory = localCategory.copy(id = "coffee", displayName = "Backup label")
        val localBudget = BudgetEntity("COFFEE", 100.0)
        val local = BackupSnapshot(listOf(coffee), listOf(localCategory), listOf(localBudget), settings)
        val backup = BackupSnapshot(
            expenses = listOf(coffee.copy(id = 8), coffee.copy(id = 9), coffee.copy(title = "Lunch")),
            categories = listOf(remoteCategory),
            budgets = listOf(BudgetEntity("coffee", 250.0), BudgetEntity("FOOD", 200.0)),
            preferences = settings,
        )

        val plan = planRestore(local, backup)

        assertEquals(listOf(coffee.copy(id = 0), coffee.copy(id = 0, title = "Lunch")), plan.expenses)
        assertTrue(plan.categories.isEmpty())
        assertEquals(listOf(BudgetEntity("FOOD", 200.0)), plan.budgets)
        val after = local.copy(expenses = local.expenses + plan.expenses, budgets = local.budgets + plan.budgets)
        assertEquals(RestorePlan(emptyList(), emptyList(), emptyList()), planRestore(after, backup))
    }

    @Test
    fun encryptedPayloadUsesFreshParametersAndRejectsTampering() {
        val plaintext = "sample expense metadata".toByteArray()
        val password = "sample Passphrase 123".toCharArray()
        val first = BackupEncryption.encrypt(plaintext, password)
        val second = BackupEncryption.encrypt(plaintext, password)

        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.iv.contentEquals(second.iv))
        assertArrayEquals(plaintext, BackupEncryption.decrypt(first, password))
        assertThrows(GeneralSecurityException::class.java) {
            BackupEncryption.decrypt(first, "different passphrase".toCharArray())
        }

        first.ciphertext[0] = (first.ciphertext[0].toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) {
            BackupEncryption.decrypt(first, password)
        }
    }
}
