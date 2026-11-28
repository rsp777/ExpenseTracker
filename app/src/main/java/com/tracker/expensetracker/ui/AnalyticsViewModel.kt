package com.tracker.expensetracker.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tracker.expensetracker.ExpenseTrackerApplication
import com.tracker.expensetracker.domain.model.AnalyticsRange
import com.tracker.expensetracker.domain.model.SpendingAnalytics
import com.tracker.expensetracker.domain.model.calculateSpendingAnalytics
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.THREE_MONTHS,
    val spending: SpendingAnalytics? = null
)

class AnalyticsViewModel(
    expenses: ExpenseRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val rangeName = savedState.getStateFlow("analytics_range", AnalyticsRange.THREE_MONTHS.name)
    private val today = MutableStateFlow(LocalDate.now())

    val state = combine(expenses.getAllExpenses(), rangeName, today) { entries, rangeName, today ->
        val range = AnalyticsRange.entries.firstOrNull { it.name == rangeName } ?: AnalyticsRange.THREE_MONTHS
        AnalyticsUiState(range, calculateSpendingAnalytics(entries, range, today))
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun onRangeSelected(range: AnalyticsRange) {
        refreshDate()
        savedState["analytics_range"] = range.name
    }

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication
                AnalyticsViewModel(app.container.expenseRepository, createSavedStateHandle())
            }
        }
    }
}
