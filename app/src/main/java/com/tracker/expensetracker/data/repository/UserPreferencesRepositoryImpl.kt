package com.tracker.expensetracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.tracker.expensetracker.domain.model.AppCurrency
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

    private fun loadInitialCurrency(): AppCurrency {
        val code = prefs.getString(KEY_CURRENCY_CODE, AppCurrency.USD.code) ?: AppCurrency.USD.code
        return AppCurrency.fromCode(code)
    }

    override suspend fun setCurrency(currency: AppCurrency) {
        prefs.edit().putString(KEY_CURRENCY_CODE, currency.code).apply()
        _selectedCurrency.value = currency
    }

    companion object {
        private const val PREFS_NAME = "expense_tracker_preferences"
        private const val KEY_CURRENCY_CODE = "selected_currency_code"
    }
}
