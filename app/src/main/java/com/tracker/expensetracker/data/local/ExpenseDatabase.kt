package com.tracker.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tracker.expensetracker.data.local.converter.Converters
import com.tracker.expensetracker.data.local.dao.ExpenseDao
import com.tracker.expensetracker.data.local.entity.ExpenseEntity

@Database(entities = [ExpenseEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getInstance(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
