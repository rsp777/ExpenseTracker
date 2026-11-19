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
        if (category == null) return null
        return "${category.id}:::${category.displayName}:::${category.colorHex}:::${category.iconName}:::${category.isDefault}"
    }

    @TypeConverter
    fun toCategory(name: String?): ExpenseCategory? {
        if (name == null) return null
        return if (name.contains(":::")) {
            val parts = name.split(":::")
            val id = parts.getOrNull(0) ?: "OTHER"
            val displayName = parts.getOrNull(1) ?: id
            val colorHex = parts.getOrNull(2)?.toLongOrNull() ?: 0xFF78909C
            val iconName = parts.getOrNull(3) ?: "Category"
            val isDefault = parts.getOrNull(4)?.toBooleanStrictOrNull() ?: false
            ExpenseCategory(
                id = id,
                displayName = displayName,
                colorHex = colorHex,
                iconName = iconName,
                isDefault = isDefault
            )
        } else {
            ExpenseCategory.fromNameOrId(name)
        }
    }
}
