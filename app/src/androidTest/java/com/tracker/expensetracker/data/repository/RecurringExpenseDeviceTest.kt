package com.tracker.expensetracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.RecurrenceCadence
import com.tracker.expensetracker.domain.model.RecurringExpenseDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecurringExpenseDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val firstDate = LocalDate.of(2026, 1, 31)
    private val template = RecurringExpenseDetails(
        "Synthetic subscription",
        12.50,
        ExpenseCategory.ENTERTAINMENT,
        firstDate,
        "Test only",
        RecurrenceCadence.MONTHLY,
    )

    @Test
    fun concurrentChecksPostEachDueOccurrenceOnceAndAdvanceTheSchedule() = runBlocking {
        withDatabase { db, repository ->
            assertTrue(repository.save(null, template, firstDate))
            val today = LocalDate.of(2026, 3, 31)
            val counts = coroutineScope {
                List(2) { async(Dispatchers.Default) { repository.postDueExpenses(today) } }.awaitAll()
            }

            assertEquals(3, counts.sum())
            assertEquals(0, repository.postDueExpenses(today))
            assertEquals(
                listOf(today, LocalDate.of(2026, 2, 28), firstDate),
                db.expenseDao().getAllExpenses().first().map { it.date },
            )
            assertEquals(LocalDate.of(2026, 4, 30), repository.observeSchedules().first().single().nextDate)
        }
    }

    @Test
    fun pauseResumeEditAndCancelManageFutureEntries() = runBlocking {
        withDatabase { db, repository ->
            val daily = template.copy(cadence = RecurrenceCadence.DAILY)
            assertTrue(repository.save(null, daily, firstDate))
            assertEquals(1, repository.postDueExpenses(firstDate))
            val id = repository.observeSchedules().first().single().id

            assertTrue(repository.setPaused(id, true, firstDate))
            val resumedOn = firstDate.plusDays(4)
            assertEquals(0, repository.postDueExpenses(resumedOn))
            assertTrue(repository.setPaused(id, false, resumedOn))
            assertEquals(1, repository.postDueExpenses(resumedOn))

            val edited = daily.copy(title = "Edited schedule", amount = 19.0)
            assertTrue(repository.save(id, edited, resumedOn))
            assertEquals(0, repository.postDueExpenses(resumedOn))
            assertEquals(1, repository.postDueExpenses(resumedOn.plusDays(1)))
            assertTrue(repository.cancel(id))
            assertEquals(0, repository.postDueExpenses(resumedOn.plusWeeks(1)))

            val history = db.expenseDao().getAllExpenses().first()
            assertEquals(listOf(resumedOn.plusDays(1), resumedOn, firstDate), history.map { it.date })
            assertEquals("Edited schedule", history.first().title)
            assertEquals(19.0, history.first().amount, 0.0)
        }
    }

    @Test
    fun version4UpgradePreservesExistingTransactionsCategoriesAndBudgets() = runBlocking {
        val name = "recurring-migration-${UUID.randomUUID()}"
        val opening = Room.databaseBuilder(context, ExpenseDatabase::class.java, name).build()
        val category = CategoryEntity("QA_TEST", "Synthetic custom category", 0xFF147287)
        val expense = ExpenseEntity(title = "Existing expense", amount = 42.0, category = category.toDomainModel(), date = firstDate)
        try {
            try {
                opening.expenseDao().insertExpense(expense)
                opening.categoryDao().insertCategory(category)
                opening.budgetDao().upsert(BudgetEntity("QA_TEST", 100.0))
                val legacySchema = opening.openHelper.writableDatabase
                legacySchema.execSQL("DROP TABLE recurring_expenses")
                legacySchema.version = 4
            } finally {
                opening.close()
            }
            val migrated = Room.databaseBuilder(context, ExpenseDatabase::class.java, name)
                .addMigrations(ExpenseDatabase.MIGRATION_4_5)
                .build()
            try {
                assertEquals(listOf(expense.copy(id = 1)), migrated.expenseDao().getAllExpenses().first())
                assertEquals(category, migrated.categoryDao().getCategoryById("QA_TEST"))
                assertEquals(listOf(BudgetEntity("QA_TEST", 100.0)), migrated.budgetDao().observeLimits().first())
                val repository = RecurringExpenseRepositoryImpl(migrated)
                assertTrue(repository.observeSchedules().first().isEmpty())
                assertTrue(repository.save(null, template, firstDate))
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }

    private suspend fun withDatabase(
        block: suspend (ExpenseDatabase, RecurringExpenseRepositoryImpl) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java).build()
        try {
            block(database, RecurringExpenseRepositoryImpl(database))
        } finally {
            database.close()
        }
    }
}
