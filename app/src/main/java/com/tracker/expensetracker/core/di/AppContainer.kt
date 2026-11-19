package com.tracker.expensetracker.core.di

import android.content.Context
import com.tracker.expensetracker.data.local.ExpenseDatabase
import com.tracker.expensetracker.data.repository.ExpenseRepositoryImpl
import com.tracker.expensetracker.domain.repository.ExpenseRepository

interface AppContainer {
    val expenseRepository: ExpenseRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: ExpenseDatabase by lazy {
        ExpenseDatabase.getInstance(context)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(database.expenseDao())
    }
}
