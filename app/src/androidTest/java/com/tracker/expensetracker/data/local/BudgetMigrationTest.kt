package com.tracker.expensetracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tracker.expensetracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BudgetMigrationTest {
    @Test
    fun versionThreeDataSurvivesAndNewBudgetsCanBeSaved() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "budget-migration-${UUID.randomUUID()}.db"
        val day = LocalDate.of(2026, 11, 24)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { legacy ->
                legacy.execSQL(
                    """
                    CREATE TABLE expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                legacy.execSQL(
                    """
                    CREATE TABLE categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        colorHex INTEGER NOT NULL,
                        iconName TEXT NOT NULL,
                        isDefault INTEGER NOT NULL,
                        isArchived INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                legacy.execSQL(
                    "INSERT INTO expenses VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(1L, "Existing purchase", 12.0, "TRAVEL", day.toEpochDay(), "Original notes")
                )
                legacy.execSQL(
                    "INSERT INTO categories VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf("TRAVEL", "Travel", 0xFF42A5F5, "Flight", 0, 1)
                )
                legacy.version = 3
            }

            val db = Room.databaseBuilder(context, ExpenseDatabase::class.java, name)
                .addMigrations(ExpenseDatabase.MIGRATION_3_4)
                .build()
            try {
                val expense = checkNotNull(db.expenseDao().getExpenseById(1))
                assertEquals("Existing purchase", expense.title)
                assertEquals(12.0, expense.amount, 0.0)
                assertEquals(day, expense.date)
                assertEquals("Original notes", expense.notes)
                assertEquals(true, db.categoryDao().getCategoryById("TRAVEL")?.isArchived)

                val budgets = db.budgetDao()
                assertTrue(budgets.observeLimits().first().isEmpty())
                budgets.upsert(BudgetEntity("", 100.0))
                budgets.upsert(BudgetEntity("TRAVEL", 25.0))
                assertEquals(listOf(BudgetEntity("", 100.0), BudgetEntity("TRAVEL", 25.0)), budgets.observeLimits().first())
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
