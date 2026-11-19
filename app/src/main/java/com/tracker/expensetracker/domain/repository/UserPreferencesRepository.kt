package com.tracker.expensetracker.domain.repository

import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface UserPreferencesRepository {
    val selectedCurrency: StateFlow<AppCurrency>
    val selectedTheme: StateFlow<CustomAppTheme>
    val themeMode: StateFlow<ThemeMode>

    suspend fun setCurrency(currency: AppCurrency)
    suspend fun setTheme(theme: CustomAppTheme)
    suspend fun setThemeMode(mode: ThemeMode)
}
