package com.tracker.expensetracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.ThemeMode
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepositoryImpl(
    context: Context
) : UserPreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedCurrency = MutableStateFlow(loadInitialCurrency())
    override val selectedCurrency: StateFlow<AppCurrency> = _selectedCurrency.asStateFlow()

    private val _selectedTheme = MutableStateFlow(loadInitialTheme())
    override val selectedTheme: StateFlow<CustomAppTheme> = _selectedTheme.asStateFlow()

    private val _themeMode = MutableStateFlow(loadInitialThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadInitialCurrency(): AppCurrency {
        val code = prefs.getString(KEY_CURRENCY_CODE, AppCurrency.USD.code) ?: AppCurrency.USD.code
        return AppCurrency.fromCode(code)
    }

    private fun loadInitialTheme(): CustomAppTheme {
        val themeId = prefs.getString(KEY_THEME_ID, CustomAppTheme.DYNAMIC.id) ?: CustomAppTheme.DYNAMIC.id
        return CustomAppTheme.fromId(themeId)
    }

    private fun loadInitialThemeMode(): ThemeMode {
        val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return ThemeMode.fromName(modeName)
    }

    override suspend fun setCurrency(currency: AppCurrency) {
        prefs.edit().putString(KEY_CURRENCY_CODE, currency.code).apply()
        _selectedCurrency.value = currency
    }

    override suspend fun setTheme(theme: CustomAppTheme) {
        prefs.edit().putString(KEY_THEME_ID, theme.id).apply()
        _selectedTheme.value = theme
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "expense_tracker_preferences"
        private const val KEY_CURRENCY_CODE = "selected_currency_code"
        private const val KEY_THEME_ID = "selected_theme_id"
        private const val KEY_THEME_MODE = "selected_theme_mode"
    }
}
