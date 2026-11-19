package com.tracker.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tracker.expensetracker.domain.model.ExpenseCategory

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val colorHex: Long,
    val iconName: String = "Category",
    val isDefault: Boolean = false
) {
    fun toDomainModel(): ExpenseCategory = ExpenseCategory(
        id = id,
        displayName = displayName,
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault
    )

    companion object {
        fun fromDomainModel(category: ExpenseCategory): CategoryEntity = CategoryEntity(
            id = category.id,
            displayName = category.displayName,
            colorHex = category.colorHex,
            iconName = category.iconName,
            isDefault = category.isDefault
        )
    }
}
