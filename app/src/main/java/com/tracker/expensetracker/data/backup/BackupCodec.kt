package com.tracker.expensetracker.data.backup

import com.tracker.expensetracker.data.local.entity.BudgetEntity
import com.tracker.expensetracker.data.local.entity.CategoryEntity
import com.tracker.expensetracker.data.local.entity.ExpenseEntity
import com.tracker.expensetracker.domain.model.AppCurrency
import com.tracker.expensetracker.domain.model.CustomAppTheme
import com.tracker.expensetracker.domain.model.ExpenseCategory
import com.tracker.expensetracker.domain.model.ThemeMode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Base64

internal class InvalidBackupException : IOException()
internal class UnsupportedBackupVersionException : IOException()
internal class BackupTooLargeException : IOException()

internal object BackupCodec {
    const val MAX_DOCUMENT_BYTES = 16 * 1024 * 1024
    private const val FORMAT = "expense-tracker-backup"
    private const val VERSION = 1

    fun encode(snapshot: BackupSnapshot, password: CharArray): ByteArray {
        val payload = JSONObject()
            .put("expenses", snapshot.expenses.toJson(::expenseToJson))
            .put("categories", snapshot.categories.toJson { categoryToJson(it.toDomainModel()) })
            .put("budgets", snapshot.budgets.toJson {
                JSONObject().put("scopeId", it.scopeId).put("amount", it.amount)
            })
            .put("preferences", JSONObject()
                .put("currency", snapshot.preferences.currency.code)
                .put("theme", snapshot.preferences.theme.id)
                .put("themeMode", snapshot.preferences.themeMode.name))
            .toString().toByteArray(Charsets.UTF_8)
        val encrypted = try {
            if (payload.size > MAX_DOCUMENT_BYTES * 3 / 4) throw BackupTooLargeException()
            BackupEncryption.encrypt(payload, password)
        } finally {
            payload.fill(0)
        }
        val encoder = Base64.getEncoder()
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("salt", encoder.encodeToString(encrypted.salt))
            .put("iv", encoder.encodeToString(encrypted.iv))
            .put("ciphertext", encoder.encodeToString(encrypted.ciphertext))
            .toString().toByteArray(Charsets.UTF_8).also {
                if (it.size > MAX_DOCUMENT_BYTES) throw BackupTooLargeException()
            }
    }

    fun decode(document: ByteArray, password: CharArray): BackupSnapshot {
        if (document.size > MAX_DOCUMENT_BYTES) throw BackupTooLargeException()
        val encrypted = validate {
            val envelope = parseObject(document)
            require(envelope.string("format") == FORMAT)
            if (envelope.integer("version") != VERSION.toLong()) {
                throw UnsupportedBackupVersionException()
            }
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(envelope.string("salt"))
            val iv = decoder.decode(envelope.string("iv"))
            val ciphertext = decoder.decode(envelope.string("ciphertext"))
            require(salt.size == BackupEncryption.SALT_BYTES)
            require(iv.size == BackupEncryption.IV_BYTES)
            require(ciphertext.size > 16)
            BackupEncryption.Encrypted(salt, iv, ciphertext)
        }
        val payload = BackupEncryption.decrypt(encrypted, password)
        return try {
            validate { decodePayload(parseObject(payload)) }
        } finally {
            payload.fill(0)
        }
    }

    private fun decodePayload(json: JSONObject): BackupSnapshot {
        val expenses = json.getJSONArray("expenses").mapObjects(::expenseFromJson)
        val categories = json.getJSONArray("categories").mapObjects {
            CategoryEntity.fromDomainModel(categoryFromJson(it))
        }
        val budgets = json.getJSONArray("budgets").mapObjects {
            BudgetEntity(it.nonBlank("scopeId"), it.positiveAmount())
        }
        require(expenses.map { it.id }.distinct().size == expenses.size)
        require(categories.map { normalizedId(it.id) }.distinct().size == categories.size)
        require(budgets.map { normalizedId(it.scopeId) }.distinct().size == budgets.size)

        val settings = json.getJSONObject("preferences")
        val currency = settings.string("currency")
        val theme = settings.string("theme")
        val themeMode = settings.string("themeMode")
        val preferences = BackupPreferences(
            currency = requireNotNull(AppCurrency.SUPPORTED_CURRENCIES.find { it.code == currency }),
            theme = requireNotNull(CustomAppTheme.entries.find { it.id == theme }),
            themeMode = requireNotNull(ThemeMode.entries.find { it.name == themeMode }),
        )
        return BackupSnapshot(expenses, categories, budgets, preferences)
    }

    private fun expenseToJson(expense: ExpenseEntity) = JSONObject()
        .put("id", expense.id)
        .put("title", expense.title)
        .put("amount", expense.amount)
        .put("category", categoryToJson(expense.category))
        .put("date", expense.date.toString())
        .put("notes", expense.notes)

    private fun expenseFromJson(json: JSONObject) = ExpenseEntity(
        id = json.integer("id").also { require(it > 0) },
        title = json.nonBlank("title"),
        amount = json.positiveAmount(),
        category = categoryFromJson(json.getJSONObject("category")),
        date = LocalDate.parse(json.string("date")),
        notes = json.string("notes"),
    )

    private fun categoryToJson(category: ExpenseCategory) = JSONObject()
        .put("id", category.id)
        .put("displayName", category.displayName)
        .put("colorHex", category.colorHex)
        .put("iconName", category.iconName)
        .put("default", category.isDefault)
        .put("archived", category.isArchived)

    private fun categoryFromJson(json: JSONObject) = ExpenseCategory(
        id = json.nonBlank("id"),
        displayName = json.nonBlank("displayName"),
        colorHex = json.integer("colorHex").also { require(it in Int.MIN_VALUE.toLong()..0xFFFFFFFFL) },
        iconName = json.nonBlank("iconName"),
        isDefault = json.get("default") as? Boolean ?: throw InvalidBackupException(),
        isArchived = json.get("archived") as? Boolean ?: throw InvalidBackupException(),
    )

    private fun parseObject(bytes: ByteArray): JSONObject {
        val tokens = JSONTokener(bytes.toString(Charsets.UTF_8))
        val result = tokens.nextValue() as? JSONObject ?: throw InvalidBackupException()
        require(tokens.nextClean() == '\u0000')
        return result
    }

    private fun JSONObject.string(key: String): String =
        get(key) as? String ?: throw InvalidBackupException()

    private fun JSONObject.nonBlank(key: String): String = string(key).also { require(it.isNotBlank()) }

    private fun JSONObject.integer(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> throw InvalidBackupException()
    }

    private fun JSONObject.positiveAmount(): Double =
        (get("amount") as? Number)?.toDouble()?.takeIf { it.isFinite() && it > 0 }
            ?: throw InvalidBackupException()

    private fun <T> List<T>.toJson(transform: (T) -> JSONObject) = JSONArray().apply {
        for (item in this@toJson) put(transform(item))
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { transform(getJSONObject(it)) }

    private inline fun <T> validate(parse: () -> T): T {
        try {
            return parse()
        } catch (_: JSONException) {
            throw InvalidBackupException()
        } catch (_: IllegalArgumentException) {
            throw InvalidBackupException()
        } catch (_: DateTimeParseException) {
            throw InvalidBackupException()
        }
    }
}
