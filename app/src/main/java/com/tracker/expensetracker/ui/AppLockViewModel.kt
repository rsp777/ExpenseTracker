package com.tracker.expensetracker.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.expensetracker.domain.model.AppLockMode
import com.tracker.expensetracker.domain.model.AppLockSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class AppLockAuthentication { UNLOCK, ENABLE, DISABLE }

internal enum class AppLockMessage { UNAVAILABLE, AUTHENTICATION_ERROR, SETTINGS_ERROR }

internal data class AppLockUiState(
    val session: AppLockSession = AppLockSession(),
    val authentication: AppLockAuthentication? = null,
    val saving: Boolean = false,
    val message: AppLockMessage? = null,
) {
    val busy: Boolean get() = authentication != null || saving
    val protectWindow: Boolean
        get() = session.isLoading || session.isEnabled || authentication == AppLockAuthentication.ENABLE || saving
}

internal class AppLockViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(AppLockUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val mode = withContext(Dispatchers.IO) {
                val saved = preferences.getString(KEY_MODE, AppLockMode.OFF.name)
                AppLockMode.entries.firstOrNull { it.name == saved } ?: AppLockMode.IMMEDIATE
            }
            _state.update { it.copy(session = it.session.load(mode)) }
        }
    }

    fun onForeground(now: Long) {
        _state.update { it.copy(session = it.session.onForeground(now)) }
    }

    fun onBackground(now: Long, isConfigurationChange: Boolean) {
        _state.update { it.copy(session = it.session.onBackground(now, isConfigurationChange)) }
    }

    fun beginAuthentication(action: AppLockAuthentication): Boolean {
        val state = _state.value
        val session = state.session
        if (state.busy || session.isLoading) return false
        val allowed = when (action) {
            AppLockAuthentication.UNLOCK -> session.isEnabled && session.requiresUnlock
            AppLockAuthentication.ENABLE -> session.canShowContent && !session.isEnabled
            AppLockAuthentication.DISABLE -> session.canShowContent && session.isEnabled
        }
        if (allowed) _state.value = state.copy(authentication = action, message = null)
        return allowed
    }

    fun authenticationSucceeded() {
        when (_state.value.authentication) {
            AppLockAuthentication.UNLOCK -> {
                _state.update { it.copy(session = it.session.afterAuthentication(), authentication = null) }
            }
            AppLockAuthentication.ENABLE -> saveMode(AppLockMode.IMMEDIATE)
            AppLockAuthentication.DISABLE -> saveMode(AppLockMode.OFF)
            null -> Unit
        }
    }

    fun authenticationEnded(message: AppLockMessage? = null) {
        _state.update {
            if (it.authentication == null) it else it.copy(authentication = null, message = message)
        }
    }

    fun changeTimeout(mode: AppLockMode) {
        val state = _state.value
        if (state.busy || !state.session.canShowContent || !state.session.isEnabled ||
            mode == AppLockMode.OFF || mode == state.session.mode
        ) return
        saveMode(mode)
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun saveMode(mode: AppLockMode) {
        _state.update { it.copy(authentication = null, saving = true, message = null) }
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { preferences.edit().putString(KEY_MODE, mode.name).commit() }
            _state.update {
                if (saved) it.copy(session = it.session.changeMode(mode), saving = false)
                else it.copy(saving = false, message = AppLockMessage.SETTINGS_ERROR)
            }
        }
    }

    private companion object {
        const val KEY_MODE = "mode"
    }
}
