package com.tracker.expensetracker.data.local.converter

import com.tracker.expensetracker.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun categoryRoundTripPreservesPunctuationAndUnicode() {
        val category = ExpenseCategory(
            id = "WORK:::TRAVEL|東京",
            displayName = "Work:::Travel | 東京",
            colorHex = 0xFF2E7D32,
            iconName = "Flight|Train:::Day",
            isDefault = true
        )

        assertSnapshot(category, converters.fromCategory(category))
    }

    @Test
    fun readsLegacyCategoryIdsAndStandardSnapshots() {
        assertSnapshot(ExpenseCategory.FOOD, "FOOD")
        val category = ExpenseCategory("HOME_OFFICE", "Home Office", 0xFF2E7D32, "Work")
        assertSnapshot(category, legacyValue(category))
    }

    @Test
    fun recoversLegacyCustomCategoriesWithColons() {
        val labels = listOf("Work:::Travel", "Straße ::: Travel", "Team:", ":Family", ":::")
        for (label in labels) {
            val category = ExpenseCategory(
                id = ExpenseCategory.customIdForName(label),
                displayName = label,
                colorHex = 0xFF2E7D32,
                iconName = "Flight"
            )
            assertSnapshot(category, legacyValue(category))
        }
    }

    private fun assertSnapshot(expected: ExpenseCategory, stored: String?) {
        val actual = checkNotNull(converters.toCategory(stored))
        assertEquals(expected.id, actual.id)
        assertEquals(expected.displayName, actual.displayName)
        assertEquals(expected.colorHex, actual.colorHex)
        assertEquals(expected.iconName, actual.iconName)
        assertEquals(expected.isDefault, actual.isDefault)
    }

    private fun legacyValue(category: ExpenseCategory): String =
        listOf(category.id, category.displayName, category.colorHex, category.iconName, category.isDefault)
            .joinToString(":::")
}
