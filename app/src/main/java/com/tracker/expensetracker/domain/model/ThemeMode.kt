package com.tracker.expensetracker.domain.model

enum class ThemeMode(val displayName: String) {
    SYSTEM("Auto (System)"),
    LIGHT("Light Mode"),
    DARK("Dark Mode");

    companion object {
        fun fromName(name: String): ThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}
