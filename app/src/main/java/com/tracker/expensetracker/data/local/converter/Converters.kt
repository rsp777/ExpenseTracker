package com.tracker.expensetracker.data.local.converter

import androidx.room.TypeConverter
import com.tracker.expensetracker.domain.model.ExpenseCategory
import java.time.LocalDate
import java.util.Base64

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
        return SNAPSHOT_PREFIX + listOf(
            encode(category.id),
            encode(category.displayName),
            category.colorHex.toString(),
            encode(category.iconName),
            category.isDefault.toString()
        ).joinToString("|")
    }

    @TypeConverter
    fun toCategory(value: String?): ExpenseCategory? {
        if (value == null) return null
        if (value.startsWith(SNAPSHOT_PREFIX)) {
            decodeSnapshot(value)?.let { return it }
        }
        return if (LEGACY_SEPARATOR in value) {
            decodeLegacySnapshot(value)
        } else {
            ExpenseCategory.fromNameOrId(value)
        }
    }

    private fun decodeSnapshot(value: String): ExpenseCategory? {
        val fields = value.removePrefix(SNAPSHOT_PREFIX).split('|')
        if (fields.size != 5) return null
        val (id, label, color, icon, isDefault) = fields
        return try {
            categoryFromFields(decode(id), decode(label), color, decode(icon), isDefault)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun decodeLegacySnapshot(value: String): ExpenseCategory {
        val snapshot = LEGACY_SNAPSHOT.matchEntire(value)
        if (snapshot != null) {
            val (names, color, icon, isDefault) = snapshot.destructured
            val separator = legacyNameSeparator(names)
            if (separator >= 0) {
                return categoryFromFields(
                    names.substring(0, separator),
                    names.substring(separator + LEGACY_SEPARATOR.length),
                    color,
                    icon,
                    isDefault
                )
            }
        }

        val fields = value.split(LEGACY_SEPARATOR)
        return categoryFromFields(
            fields[0], fields.getOrNull(1), fields.getOrNull(2), fields.getOrNull(3), fields.getOrNull(4)
        )
    }

    private fun legacyNameSeparator(names: String): Int {
        // A legacy custom category stores its normalized label as the ID.
        // Normalizing the entire prefix makes the ID and label the same length.
        val normalizedNames = ExpenseCategory.customIdForName(names)
        val candidate = (normalizedNames.length - LEGACY_SEPARATOR.length) / 2
        if (names.startsWith(LEGACY_SEPARATOR, candidate)) {
            val label = names.substring(candidate + LEGACY_SEPARATOR.length)
            if (ExpenseCategory.customIdForName(label) == names.substring(0, candidate)) {
                return candidate
            }
        }
        return names.indexOf(LEGACY_SEPARATOR)
    }

    private fun categoryFromFields(
        id: String,
        label: String?,
        color: String?,
        icon: String?,
        isDefault: String?
    ) = ExpenseCategory(
        id = id,
        displayName = label ?: id,
        colorHex = color?.toLongOrNull() ?: ExpenseCategory.OTHER.colorHex,
        iconName = icon ?: "Category",
        isDefault = isDefault?.toBooleanStrictOrNull() ?: false
    )

    private fun encode(text: String): String = ENCODER.encodeToString(text.toByteArray(Charsets.UTF_8))

    private fun decode(text: String): String = DECODER.decode(text).toString(Charsets.UTF_8)

    private companion object {
        const val SNAPSHOT_PREFIX = "category-v2|"
        const val LEGACY_SEPARATOR = ":::"
        val LEGACY_SNAPSHOT = Regex(
            """(.*):::([+-]?\d+):::([^:]*):::(true|false)""",
            RegexOption.DOT_MATCHES_ALL
        )
        val ENCODER = Base64.getUrlEncoder().withoutPadding()
        val DECODER = Base64.getUrlDecoder()
    }
}
