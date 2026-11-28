package com.tracker.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.expensetracker.notifications.BudgetAlertWorker
import com.tracker.expensetracker.notifications.RecurringExpenseWorker
import com.tracker.expensetracker.ui.BudgetViewModel
import com.tracker.expensetracker.ui.ExpenseScreen
import com.tracker.expensetracker.ui.ExpenseViewModel
import com.tracker.expensetracker.ui.RecurringExpensesViewModel
import com.tracker.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels { ExpenseViewModel.Factory }
    private val budgetViewModel: BudgetViewModel by viewModels { BudgetViewModel.Factory }
    private val recurringViewModel: RecurringExpensesViewModel by viewModels { RecurringExpensesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ExpenseTrackerTheme(
                customTheme = uiState.theme,
                themeMode = uiState.themeMode
            ) {
                ExpenseScreen(
                    viewModel = viewModel,
                    budgetViewModel = budgetViewModel,
                    recurringViewModel = recurringViewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        budgetViewModel.refreshMonth()
        BudgetAlertWorker.schedule(applicationContext)
        RecurringExpenseWorker.schedule(applicationContext)
    }
}
