package com.tracker.expensetracker.domain.repository

import com.tracker.expensetracker.domain.model.AppCurrency
import kotlinx.coroutines.flow.StateFlow

interface UserPreferencesRepository {
    val selectedCurrency: StateFlow<AppCurrency>
    suspend fun setCurrency(currency: AppCurrency)
}
