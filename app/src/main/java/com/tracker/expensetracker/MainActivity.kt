package com.tracker.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tracker.expensetracker.ui.ExpenseScreen
import com.tracker.expensetracker.ui.ExpenseViewModel
import com.tracker.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels { ExpenseViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                ExpenseScreen(viewModel = viewModel)
            }
        }
    }
}
