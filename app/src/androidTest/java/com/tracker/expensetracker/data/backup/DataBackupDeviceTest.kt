package com.tracker.expensetracker.data.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.data.repository.ExpenseRepositoryImpl
import com.tracker.expensetracker.data.repository.UserPreferencesRepositoryImpl
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.GeneralSecurityException
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DataBackupDeviceTest {
    @Test
    fun restoresAnEncryptedFileIntoAnotherDatabaseWithoutOverwritingOrDuplicatingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "expense_exports").apply { mkdirs() }
        val file = File.createTempFile("backup-device-test-", ".json", directory)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.expense-exports", file)
        val prefix = UUID.randomUUID().toString()
        val sourceName = "$prefix-source"
        val destinationName = "$prefix-destination"
        val sourcePrefs = testPreferences(context, sourceName)
        val destinationPrefs = testPreferences(context, destinationName)
        val category = ExpenseCategory("TEST", "Test category", 0xFF137355)
        val archived = CategoryEntity.fromDomainModel(category.copy(isArchived = true))
        val expense = ExpenseEntity(
            title = "Synthetic expense",
            amount = 12.25,
            date = LocalDate.of(2026, 11, 28),
            category = category,
            notes = "Sample ✓",
        )
        val password = "Test Backup Passphrase".toCharArray()
        val source = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java).build()
        val destination = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java).build()
        try {
            val sourceRepo = repository(context, source, sourcePrefs)
            val destinationRepo = repository(context, destination, destinationPrefs)
            source.categoryDao().insertCategory(archived)
            source.budgetDao().upsert(BudgetEntity("TEST", 100.0))
            source.expenseDao().insertExpense(expense)
            sourcePrefs.setCurrency(AppCurrency.INR)
            sourcePrefs.setTheme(CustomAppTheme.EMERALD_FINANCE)
            sourcePrefs.setThemeMode(ThemeMode.DARK)
            val existing = expense.copy(title = "Device-local expense")
            destination.expenseDao().insertExpense(existing)

            sourceRepo.saveBackup(uri, password)
            assertFalse(file.readText().contains(expense.title))
            val wrongPassphrase = runCatching {
                destinationRepo.restoreBackup(uri, "Incorrect passphrase".toCharArray())
            }.exceptionOrNull()
            assertTrue(wrongPassphrase is GeneralSecurityException)
            assertEquals(listOf(existing.title), destination.expenseDao().getAllExpenses().first().map { it.title })
            assertEquals(AppCurrency.USD, destinationPrefs.selectedCurrency.value)

            val restored = destinationRepo.restoreBackup(uri, password)
            assertEquals(DataBackupRepository.RestoreResult(1, 1, 1), restored)
            assertEquals(setOf(existing.copy(id = 1), expense.copy(id = 2)), destination.expenseDao().getAllExpenses().first().toSet())
            assertEquals(archived, destination.categoryDao().getCategoryById("TEST"))
            assertEquals(listOf(BudgetEntity("TEST", 100.0)), destination.budgetDao().observeLimits().first())
            assertEquals(AppCurrency.INR, destinationPrefs.selectedCurrency.value)
            assertEquals(CustomAppTheme.EMERALD_FINANCE, destinationPrefs.selectedTheme.value)
            assertEquals(ThemeMode.DARK, destinationPrefs.themeMode.value)
            assertEquals(DataBackupRepository.RestoreResult(0, 0, 0), destinationRepo.restoreBackup(uri, password))
        } finally {
            source.close()
            destination.close()
            file.delete()
            context.deleteSharedPreferences(sourceName)
            context.deleteSharedPreferences(destinationName)
            password.fill('\u0000')
        }
    }

    @Test
    fun rejectsUnsupportedVersionsAndAuthenticatedInvalidPayloads() {
        val password = "Test Backup Passphrase".toCharArray()
        val versioned = """{"format":"expense-tracker-backup","version":99}"""
        assertThrows(UnsupportedBackupVersionException::class.java) {
            BackupCodec.decode(versioned.toByteArray(), password)
        }
        val encrypted = BackupEncryption.encrypt("""{"expenses":[]}""".toByteArray(), password)
        val encoder = Base64.getEncoder()
        val document = JSONObject()
            .put("format", "expense-tracker-backup")
            .put("version", 1)
            .put("salt", encoder.encodeToString(encrypted.salt))
            .put("iv", encoder.encodeToString(encrypted.iv))
            .put("ciphertext", encoder.encodeToString(encrypted.ciphertext))
        assertThrows(InvalidBackupException::class.java) {
            BackupCodec.decode(document.toString().toByteArray(), password)
        }
    }

    @Test
    fun rejectsExcessivelyNestedDocumentsBeforeParsingPayloads() {
        val nested = "{\"unused\":" + "[".repeat(1000) + "0" + "]".repeat(1000) + "}"
        assertThrows(InvalidBackupException::class.java) {
            BackupCodec.decode(nested.toByteArray(), "Test Backup Passphrase".toCharArray())
        }
    }

    private fun repository(
        context: Context,
        database: ExpenseDatabase,
        preferences: UserPreferencesRepositoryImpl,
    ) = DataBackupRepository(
        context,
        database,
        ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao()),
        preferences,
        onExpensesChanged = {},
    )

    private fun testPreferences(context: Context, name: String) = UserPreferencesRepositoryImpl(
        object : ContextWrapper(context) {
            override fun getSharedPreferences(ignored: String?, mode: Int): SharedPreferences =
                context.getSharedPreferences(name, mode)
        }
    )
}
