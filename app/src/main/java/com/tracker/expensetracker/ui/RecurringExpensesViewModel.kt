package com.tracker.expensetracker.ui

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.R
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.RecurringExpense
import com.tracker.expensetracker.domain.model.RecurringExpenseDetails
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.domain.repository.RecurringExpenseRepository
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository
import com.tracker.expensetracker.notifications.RecurringExpenseWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

internal data class RecurringExpenseEditor(val previous: RecurringExpense? = null)

internal data class RecurringExpenseInteraction(
    val editor: RecurringExpenseEditor? = null,
    val busy: Boolean = false,
    val message: Int? = null,
)

internal data class RecurringExpensesUiState(
    val schedules: List<RecurringExpense> = emptyList(),
    val categories: List<ExpenseCategory> = ExpenseCategory.DEFAULT_CATEGORIES,
    val currency: AppCurrency = AppCurrency.USD,
    val interaction: RecurringExpenseInteraction = RecurringExpenseInteraction(),
)

internal class RecurringExpensesViewModel(
    private val repository: RecurringExpenseRepository,
    expenses: ExpenseRepository,
    preferences: UserPreferencesRepository,
    private val requestCheck: () -> Unit,
) : ViewModel() {
    private val interaction = MutableStateFlow(RecurringExpenseInteraction())

    val state = combine(
        repository.observeSchedules(),
        expenses.getAllCategories(),
        preferences.selectedCurrency,
        interaction,
    ) { schedules, categories, currency, controls ->
        RecurringExpensesUiState(schedules, categories, currency, controls)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringExpensesUiState())

    fun openEditor(schedule: RecurringExpense? = null) {
        interaction.update {
            if (it.busy) it else it.copy(editor = RecurringExpenseEditor(schedule), message = null)
        }
    }

    fun dismissEditor() {
        interaction.update { if (it.busy) it else it.copy(editor = null, message = null) }
    }

    fun save(details: RecurringExpenseDetails) {
        val editor = interaction.value.editor ?: return
        updateSchedule(closeEditor = true, runCheck = true) {
            repository.save(editor.previous?.id, details, LocalDate.now())
        }
    }

    fun setPaused(id: Long, paused: Boolean) = updateSchedule(runCheck = !paused) {
        repository.setPaused(id, paused, LocalDate.now())
    }

    fun cancel(id: Long) = updateSchedule { repository.cancel(id) }

    fun clearMessage() {
        interaction.update { it.copy(message = null) }
    }

    private fun updateSchedule(
        closeEditor: Boolean = false,
        runCheck: Boolean = false,
        update: suspend () -> Boolean,
    ) {
        if (interaction.value.busy) return
        interaction.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val saved = try {
                update()
            } catch (_: SQLiteException) {
                false
            }
            interaction.update {
                it.copy(
                    busy = false,
                    editor = if (saved && closeEditor) null else it.editor,
                    message = if (saved) null else R.string.recurring_save_error,
                )
            }
            if (saved && runCheck) requestCheck()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication
                RecurringExpensesViewModel(
                    app.container.recurringExpenseRepository,
                    app.container.expenseRepository,
                    app.container.userPreferencesRepository,
                ) { RecurringExpenseWorker.afterScheduleChange(app) }
            }
        }
    }
}
