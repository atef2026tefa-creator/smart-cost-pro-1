package com.aistudio.smartcostpro.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters for the JSON blobs the reference app stores as
 * free-form JSON strings.
 *
 * Currently no entity column requires a custom converter — all blob columns
 * are kept as plain `String?` to mirror the spec exactly. This file is kept
 * as a placeholder so future stages can add converters for typed arrays
 * (e.g. for `IncomeSource` lists, `MedicalDetails`, etc.) without re-touching
 * the database class.
 */
class Converters {

    /**
     * Lenient JSON encoder used across all blob columns. Tolerates missing
     * fields and unknown keys, which is important when restoring backups
     * created by the original SMART COST app (whose payload we may not have
     * explicitly versioned).
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    inline fun <reified T> toJson(value: T?): String? = value?.let { json.encodeToString(it) }

    inline fun <reified T> fromJson(value: String?): T? = value?.let {
        try {
            json.decodeFromString<T>(it)
        } catch (_: Exception) {
            null
        }
    }
}
