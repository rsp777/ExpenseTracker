package com.tracker.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tracker.expensetracker.data.local.converter.Converters
import com.tracker.expensetracker.data.local.dao.BudgetDao
import com.tracker.expensetracker.data.local.dao.CategoryDao
import com.tracker.expensetracker.data.local.dao.ExpenseDao
import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.ExpenseCategory

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `displayName` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                for (cat in ExpenseCategory.DEFAULT_CATEGORIES) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO `categories` (`id`, `displayName`, `colorHex`, `iconName`, `isDefault`) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>(cat.id, cat.displayName, cat.colorHex, cat.iconName, if (cat.isDefault) 1 else 0)
                    )
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` " +
                        "(`scopeId` TEXT NOT NULL PRIMARY KEY, `amount` REAL NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            for (cat in ExpenseCategory.DEFAULT_CATEGORIES) {
                                db.execSQL(
                                    "INSERT OR IGNORE INTO `categories` (`id`, `displayName`, `colorHex`, `iconName`, `isDefault`) VALUES (?, ?, ?, ?, ?)",
                                    arrayOf<Any>(cat.id, cat.displayName, cat.colorHex, cat.iconName, if (cat.isDefault) 1 else 0)
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
