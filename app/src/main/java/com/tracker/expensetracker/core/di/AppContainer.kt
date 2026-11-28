package com.tracker.expensetracker.core.di

import android.content.Context
import com.tracker.expensetracker.data.backup.DataBackupRepository
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.repository.BudgetRepositoryImpl
import com.tracker.expensetracker.data.repository.ExpenseRepositoryImpl
import com.tracker.expensetracker.data.repository.UserPreferencesRepositoryImpl
import com.tracker.expensetracker.domain.repository.BudgetRepository
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository
import com.tracker.expensetracker.notifications.BudgetAlertWorker

interface AppContainer {
    val dataBackupRepository: DataBackupRepository
    val budgetRepository: BudgetRepository
    val expenseRepository: ExpenseRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: ExpenseDatabase by lazy {
        ExpenseDatabase.getInstance(context)
    }

    private val scheduleBudgetAlerts = { BudgetAlertWorker.schedule(context) }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database.budgetDao(), scheduleBudgetAlerts)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao(),
            onExpensesChanged = scheduleBudgetAlerts
        )
    }

    override val userPreferencesRepository: UserPreferencesRepositoryImpl by lazy {
        UserPreferencesRepositoryImpl(context)
    }

    override val dataBackupRepository: DataBackupRepository by lazy {
        DataBackupRepository(context, database, expenseRepository, userPreferencesRepository, scheduleBudgetAlerts)
    }
}
