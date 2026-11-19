package com.tracker.expensetracker

import android.app.Application
import com.tracker.expensetracker.core.di.AppContainer
import com.tracker.expensetracker.core.di.DefaultAppContainer

class ExpenseTrackerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
