package com.tracker.expensetracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tracker.expensetracker.domain.model.AppLockMode
import com.tracker.expensetracker.domain.model.AppLockSession
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLockGateDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun removesSensitiveContentUntilAuthenticationSucceedsAndWhenRelocked() {
        val locked = AppLockSession().load(AppLockMode.IMMEDIATE)
        val state = mutableStateOf(AppLockUiState())
        var requests = 0
        compose.setContent {
            MaterialTheme {
                AppLockGate(state.value, onUnlock = { requests++ }, onOpenSecuritySettings = {}) {
                    Text("Synthetic transaction")
                }
            }
        }

        compose.onNodeWithText("Synthetic transaction").assertDoesNotExist()
        compose.runOnIdle { state.value = AppLockUiState(session = locked) }
        compose.onNodeWithText("Expense Tracker is locked").assertExists()
        compose.onNodeWithText("Unlock").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithText("Synthetic transaction").assertDoesNotExist()

        compose.runOnIdle { state.value = AppLockUiState(session = locked.afterAuthentication()) }
        compose.onNodeWithText("Synthetic transaction").assertExists()
        compose.onNodeWithText("Expense Tracker is locked").assertDoesNotExist()

        compose.runOnIdle {
            val backgrounded = state.value.session.onBackground(1_000, isConfigurationChange = false)
            state.value = AppLockUiState(session = backgrounded.onForeground(1_001))
        }
        compose.onNodeWithText("Synthetic transaction").assertDoesNotExist()
        compose.onNodeWithText("Expense Tracker is locked").assertExists()
    }
}
