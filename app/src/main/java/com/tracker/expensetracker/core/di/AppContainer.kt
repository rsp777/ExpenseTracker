package com.tracker.expensetracker.core.di

import android.content.Context
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.repository.ExpenseRepositoryImpl
import com.tracker.expensetracker.data.repository.UserPreferencesRepositoryImpl
import com.tracker.expensetracker.domain.repository.ExpenseRepository
import com.tracker.expensetracker.domain.repository.UserPreferencesRepository

interface AppContainer {
    val expenseRepository: ExpenseRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: ExpenseDatabase by lazy {
        ExpenseDatabase.getInstance(context)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao()
        )
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(context)
    }
}
