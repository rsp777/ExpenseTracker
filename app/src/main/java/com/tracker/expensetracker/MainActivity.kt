package com.tracker.expensetracker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tracker.expensetracker.notifications.BudgetAlertWorker
import com.tracker.expensetracker.ui.AppLockAuthentication
import com.tracker.expensetracker.ui.AppLockGate
import com.tracker.expensetracker.ui.AppLockPrompt
import com.tracker.expensetracker.ui.AppLockSettingsAction
import com.tracker.expensetracker.ui.AppLockViewModel
import com.tracker.expensetracker.ui.BudgetViewModel
import com.tracker.expensetracker.ui.ExpenseScreen
import com.tracker.expensetracker.ui.ExpenseViewModel
import com.tracker.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: ExpenseViewModel by viewModels { ExpenseViewModel.Factory }
    private val budgetViewModel: BudgetViewModel by viewModels { BudgetViewModel.Factory }
    private val appLockViewModel: AppLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val appLockPrompt = AppLockPrompt(this, appLockViewModel)
        lifecycleScope.launch {
            appLockViewModel.state.map { it.protectWindow }.distinctUntilChanged().collect { secure ->
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        setContent {
            val lockState by appLockViewModel.state.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ExpenseTrackerTheme(
                customTheme = uiState.theme,
                themeMode = uiState.themeMode
            ) {
                AppLockGate(
                    state = lockState,
                    onUnlock = { appLockPrompt.authenticate(AppLockAuthentication.UNLOCK) },
                    onOpenSecuritySettings = ::openSecuritySettings,
                ) {
                    ExpenseScreen(
                        viewModel = viewModel,
                        budgetViewModel = budgetViewModel,
                        appLockAction = {
                            AppLockSettingsAction(
                                state = lockState,
                                onAuthentication = appLockPrompt::authenticate,
                                onTimeoutSelected = appLockViewModel::changeTimeout,
                                onOpenSecuritySettings = ::openSecuritySettings,
                                onClearMessage = appLockViewModel::clearMessage,
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        appLockViewModel.onForeground(SystemClock.elapsedRealtime())
        super.onStart()
    }

    override fun onStop() {
        appLockViewModel.onBackground(SystemClock.elapsedRealtime(), isChangingConfigurations)
        super.onStop()
    }

    private fun openSecuritySettings() {
        try {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        budgetViewModel.refreshMonth()
        BudgetAlertWorker.schedule(applicationContext)
    }
}
