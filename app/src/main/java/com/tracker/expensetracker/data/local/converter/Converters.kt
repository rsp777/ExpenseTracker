package com.tracker.expensetracker.data.local.converter

import androidx.room.TypeConverter
import com.tracker.expensetracker.domain.model.ExpenseCategory
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun fromCategory(category: ExpenseCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toCategory(name: String?): ExpenseCategory? {
        return name?.let {
            try {
                ExpenseCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ExpenseCategory.OTHER
            }
        }
    }
}
