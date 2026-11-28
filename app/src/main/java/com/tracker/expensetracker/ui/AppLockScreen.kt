package com.tracker.expensetracker.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tracker.expensetracker.R
import com.tracker.expensetracker.domain.model.AppLockMode

@Composable
internal fun AppLockGate(
    state: AppLockUiState,
    onUnlock: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val saveableState = rememberSaveableStateHolder()
    if (state.session.canShowContent) {
        saveableState.SaveableStateProvider("expenses", content)
    } else {
        Surface(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.session.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text(stringResource(R.string.app_lock_locked), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.app_lock_unlock_help), textAlign = TextAlign.Center)
                        Button(enabled = !state.busy, onClick = onUnlock) {
                            Text(stringResource(R.string.app_lock_unlock))
                        }
                        AppLockMessageText(state.message, onOpenSecuritySettings)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppLockSettingsAction(
    state: AppLockUiState,
    onAuthentication: (AppLockAuthentication) -> Unit,
    onTimeoutSelected: (AppLockMode) -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = {
        onClearMessage()
        visible = true
    }) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = stringResource(R.string.app_lock_title),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    if (!visible) return

    AlertDialog(
        onDismissRequest = { if (!state.busy) visible = false },
        title = { Text(stringResource(R.string.app_lock_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = state.session.isEnabled,
                        enabled = !state.busy,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            onAuthentication(if (enabled) AppLockAuthentication.ENABLE else AppLockAuthentication.DISABLE)
                        },
                    ).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.app_lock_toggle), modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.session.isEnabled,
                        enabled = !state.busy,
                        onCheckedChange = null,
                    )
                }
                Text(stringResource(R.string.app_lock_settings_help))
                if (state.session.isEnabled) {
                    Text(stringResource(R.string.app_lock_timeout), style = MaterialTheme.typography.titleSmall)
                    Column(Modifier.selectableGroup()) {
                        AppLockMode.entries.filterNot { it == AppLockMode.OFF }.forEach { mode ->
                            val selected = mode == state.session.mode
                            Row(
                                modifier = Modifier.fillMaxWidth().selectable(
                                    selected = selected,
                                    enabled = !state.busy,
                                    role = Role.RadioButton,
                                    onClick = { onTimeoutSelected(mode) },
                                ).padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, enabled = !state.busy, onClick = null)
                                Text(stringResource(mode.label))
                            }
                        }
                    }
                }
                AppLockMessageText(state.message, onOpenSecuritySettings)
            }
        },
        confirmButton = {
            TextButton(enabled = !state.busy, onClick = { visible = false }) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
private fun AppLockMessageText(message: AppLockMessage?, onOpenSettings: () -> Unit) {
    val text = when (message) {
        AppLockMessage.UNAVAILABLE -> R.string.app_lock_unavailable
        AppLockMessage.AUTHENTICATION_ERROR -> R.string.app_lock_authentication_error
        AppLockMessage.SETTINGS_ERROR -> R.string.app_lock_settings_error
        null -> return
    }
    Text(stringResource(text), color = MaterialTheme.colorScheme.error)
    if (message == AppLockMessage.UNAVAILABLE) {
        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.app_lock_security_settings)) }
    }
}

@get:StringRes
private val AppLockMode.label: Int
    get() = when (this) {
        AppLockMode.OFF -> R.string.app_lock_off
        AppLockMode.IMMEDIATE -> R.string.app_lock_immediately
        AppLockMode.THIRTY_SECONDS -> R.string.app_lock_thirty_seconds
        AppLockMode.ONE_MINUTE -> R.string.app_lock_one_minute
        AppLockMode.FIVE_MINUTES -> R.string.app_lock_five_minutes
    }
