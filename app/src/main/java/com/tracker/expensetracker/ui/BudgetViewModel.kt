package com.tracker.expensetracker.ui

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.BudgetLimit
import com.tracker.expensetracker.domain.model.BudgetProgress
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.calculateBudgetProgress
import com.tracker.expensetracker.domain.repository.BudgetRepository
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class BudgetUiState(
    val month: YearMonth = YearMonth.now(),
    val progress: List<BudgetProgress> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val currency: AppCurrency = AppCurrency.USD
)

class BudgetViewModel(
    private val budgets: BudgetRepository,
    expenses: ExpenseRepository,
    preferences: UserPreferencesRepository
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())

    val state = combine(
        month,
        budgets.observeLimits(),
        expenses.getAllExpenses(),
        expenses.getAllCategories(),
        preferences.selectedCurrency
    ) { month, limits, expenses, categories, currency ->
        BudgetUiState(month, calculateBudgetProgress(month, limits, expenses), categories, currency)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun refreshMonth() {
        month.value = YearMonth.now()
    }

    suspend fun saveLimit(categoryId: String?, amount: Double): Boolean = try {
        budgets.setLimit(BudgetLimit(categoryId, amount))
        true
    } catch (_: SQLiteException) {
        false
    }

    suspend fun removeLimit(categoryId: String?): Boolean = try {
        budgets.removeLimit(categoryId)
        true
    } catch (_: SQLiteException) {
        false
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication
                BudgetViewModel(
                    application.container.budgetRepository,
                    application.container.expenseRepository,
                    application.container.userPreferencesRepository
                )
            }
        }
    }
}
