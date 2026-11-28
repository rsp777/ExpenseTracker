package com.tracker.expensetracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.repository.UserPreferencesRepositoryImpl
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class DataBackupRepository(
    private val context: Context,
    private val database: ExpenseDatabase,
    private val expenses: ExpenseRepository,
    private val preferences: UserPreferencesRepositoryImpl,
    private val onExpensesChanged: () -> Unit,
) {
    data class RestoreResult(val expenses: Int, val categories: Int, val budgets: Int)

    suspend fun saveCsv(destination: Uri) = withContext(Dispatchers.IO) {
        val entries = expenses.getAllExpenses().first().map(ExpenseEntity::fromDomainModel)
        writeTo(destination) { output ->
            val writer = output.bufferedWriter()
            writer.writeExpenseCsv(entries, preferences.selectedCurrency.value)
            writer.flush()
        }
    }

    suspend fun createShareableCsv(): Uri = withContext(Dispatchers.IO) {
        val entries = expenses.getAllExpenses().first().map(ExpenseEntity::fromDomainModel)
        val directory = File(context.cacheDir, "expense_exports")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException()
        val expiry = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        directory.listFiles()?.forEach { if (it.isFile && it.lastModified() < expiry) it.delete() }
        val file = File.createTempFile("expenses-", ".csv", directory)
        try {
            file.bufferedWriter().use { it.writeExpenseCsv(entries, preferences.selectedCurrency.value) }
            FileProvider.getUriForFile(context, "${context.packageName}.expense-exports", file)
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    suspend fun saveBackup(destination: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction { readSnapshot() }
        val document = BackupCodec.encode(snapshot, password)
        writeTo(destination) { it.write(document) }
    }

    suspend fun restoreBackup(source: Uri, password: CharArray): RestoreResult = withContext(Dispatchers.IO) {
        val backup = BackupCodec.decode(readDocument(source), password)
        val plan = database.withTransaction {
            val changes = planRestore(readSnapshot(), backup)
            database.categoryDao().insertCategories(changes.categories)
            database.expenseDao().insertRestoredExpenses(changes.expenses)
            for (budget in changes.budgets) database.budgetDao().upsert(budget)
            changes
        }
        preferences.restorePreferences(
            backup.preferences.currency,
            backup.preferences.theme,
            backup.preferences.themeMode,
        )
        if (plan.expenses.isNotEmpty() || plan.budgets.isNotEmpty()) onExpensesChanged()
        RestoreResult(plan.expenses.size, plan.categories.size, plan.budgets.size)
    }

    private suspend fun readSnapshot() = BackupSnapshot(
        expenses = database.expenseDao().getAllExpenses().first(),
        categories = database.categoryDao().getAllCategories().first(),
        budgets = database.budgetDao().observeLimits().first(),
        preferences = BackupPreferences(
            preferences.selectedCurrency.value,
            preferences.selectedTheme.value,
            preferences.themeMode.value,
        ),
    )

    private fun writeTo(uri: Uri, write: (OutputStream) -> Unit) {
        require(uri.scheme == "content")
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
        stream.use(write)
    }

    private fun readDocument(uri: Uri): ByteArray {
        require(uri.scheme == "content")
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException()
        return input.use { stream ->
            val bytes = ByteArrayOutputStream()
            val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = stream.read(chunk)
                if (count == -1) break
                if (bytes.size() + count > BackupCodec.MAX_DOCUMENT_BYTES) throw BackupTooLargeException()
                bytes.write(chunk, 0, count)
            }
            bytes.toByteArray()
        }
    }
}
