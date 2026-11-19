package com.tracker.expensetracker.domain.model

enum class CustomAppTheme(
    val id: String,
    val displayName: String,
    val previewColorHex: Long
) {
    DYNAMIC("DYNAMIC", "Material Dynamic (System)", 0xFF006C4C),
    EMERALD_FINANCE("EMERALD", "Emerald Mint", 0xFF006C4C),
    SAPPHIRE_BUSINESS("SAPPHIRE", "Sapphire Blue", 0xFF0061A4),
    AMETHYST_ROYAL("AMETHYST", "Amethyst Violet", 0xFF7E4E9E),
    SUNSET_COPPER("COPPER", "Sunset Copper", 0xFF9E4822),
    MIDNIGHT_OBSIDIAN("MIDNIGHT", "Midnight Obsidian", 0xFF355E6B);

    companion object {
        fun fromId(id: String): CustomAppTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DYNAMIC
        }
    }
}
