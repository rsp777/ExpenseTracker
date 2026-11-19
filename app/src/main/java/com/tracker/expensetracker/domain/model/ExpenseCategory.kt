package com.tracker.expensetracker.domain.model

data class ExpenseCategory(
    val id: String,
    val displayName: String,
    val colorHex: Long,
    val iconName: String = "Category",
    val isDefault: Boolean = false
) {
    val name: String
        get() = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExpenseCategory) return false
        return id.equals(other.id, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return id.lowercase().hashCode()
    }

    companion object {
        val FOOD = ExpenseCategory("FOOD", "Food & Dining", 0xFFFF7043, "Restaurant", true)
        val TRANSPORT = ExpenseCategory("TRANSPORT", "Transportation", 0xFF42A5F5, "DirectionsCar", true)
        val SHOPPING = ExpenseCategory("SHOPPING", "Shopping", 0xFFAB47BC, "ShoppingCart", true)
        val BILLS = ExpenseCategory("BILLS", "Bills & Utilities", 0xFFEF5350, "Receipt", true)
        val ENTERTAINMENT = ExpenseCategory("ENTERTAINMENT", "Entertainment", 0xFFFFCA28, "Movie", true)
        val HEALTH = ExpenseCategory("HEALTH", "Health & Fitness", 0xFF26A69A, "FitnessCenter", true)
        val EDUCATION = ExpenseCategory("EDUCATION", "Education", 0xFF7E57C2, "School", true)
        val OTHER = ExpenseCategory("OTHER", "Other", 0xFF78909C, "Category", true)

        val DEFAULT_CATEGORIES: List<ExpenseCategory> = listOf(
            FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, HEALTH, EDUCATION, OTHER
        )

        fun values(): Array<ExpenseCategory> = DEFAULT_CATEGORIES.toTypedArray()

        val entries: List<ExpenseCategory> = DEFAULT_CATEGORIES

        fun valueOf(value: String): ExpenseCategory = fromNameOrId(value)

        fun fromNameOrId(nameOrId: String): ExpenseCategory {
            return DEFAULT_CATEGORIES.firstOrNull {
                it.id.equals(nameOrId, ignoreCase = true) || it.displayName.equals(nameOrId, ignoreCase = true)
            } ?: ExpenseCategory(
                id = nameOrId.trim(),
                displayName = nameOrId.trim(),
                colorHex = 0xFF78909C,
                iconName = "Category",
                isDefault = false
            )
        }
    }
}
