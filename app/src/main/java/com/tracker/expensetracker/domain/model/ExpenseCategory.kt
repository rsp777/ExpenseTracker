package com.tracker.expensetracker.domain.model

enum class ExpenseCategory(
    val displayName: String,
    val colorHex: Long
) {
    FOOD("Food & Dining", 0xFFFF7043),
    TRANSPORT("Transportation", 0xFF42A5F5),
    SHOPPING("Shopping", 0xFFAB47BC),
    BILLS("Bills & Utilities", 0xFFEF5350),
    ENTERTAINMENT("Entertainment", 0xFFFFCA28),
    HEALTH("Health & Fitness", 0xFF26A69A),
    EDUCATION("Education", 0xFF7E57C2),
    OTHER("Other", 0xFF78909C)
}
