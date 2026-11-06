package com.aistudio.smartcostpro.app.util

/**
 * App-wide constants.
 *
 * Centralises the keys, default values and configuration the spec
 * (`SMART-COST-Android-Logic-Spec.md`) documented as references, so
 * they are easy to find and audit.
 */
object Constants {
    // ───────────────────────────── Storage keys ─────────────────────────────
    // Matching the reference app's local storage keys so any future
    // re-import from the original SMART COST JSON works untouched.
    const val STORAGE_EXPENSES_CACHE = "smartcost_expenses_cache"
    const val STORAGE_BUDGET = "smartcost_budget"
    const val STORAGE_MONTHLY_INCOME = "smartcost_monthly_income"
    const val STORAGE_BANK_CERTIFICATES = "smartcost_bank_certificates"
    const val STORAGE_VEHICLE_ACCIDENTS = "smartcost_vehicle_accidents"
    const val STORAGE_STUDENTS = "smartcost_students"
    const val STORAGE_EXPENSES_SECTIONS_ORDER = "smartcost_expenses_sections_order"
    const val STORAGE_PERSONAL_ASSOCIATIONS = "smartcost_personal_associations"
    const val STORAGE_CAR_INCOME_TRIPS = "smartcost_car_income_trips"

    // ──────────────────────── DataStore / Preferences ────────────────────────
    const val PREFS_NAME = "cost_app_pro_prefs"
    const val PREF_KEY_LANGUAGE = "language"
    const val PREF_KEY_CURRENCY = "currency"
    const val PREF_KEY_THEME = "theme"
    const val PREF_KEY_DEFAULT_SCREEN = "default_screen"

    // ──────────────────────────── Defaults ──────────────────────────────────
    const val DEFAULT_LANGUAGE = "ar"
    const val DEFAULT_CURRENCY = "EGP"
    const val DEFAULT_THEME = "dark"
    const val DEFAULT_SCREEN = "expenses"

    // ────────────────────────── Database ────────────────────────────────────
    const val DATABASE_NAME = "cost_app_pro.db"

    // ────────────────────────── Network (Stage 3.3.2 — Oracle backend) ───────────────────────────────────
    /**
     * The base URL of your Smart Cost Pro REST API.
     *
     * Stage 3.3.2: this is now build-type-aware.
     *   - Debug builds connect to the local backend on the dev PC (via the Android
     *     emulator's 10.0.2.2 alias). Cleartext HTTP is allowed only for 10.0.2.2 /
     *     localhost / 127.0.0.1 in `xml/network_security_config.xml`.
     *   - Release builds MUST use HTTPS — Android 9+ blocks cleartext by default,
     *     and the Play Store rejects apps that allow it globally.
     *
     * When you deploy the backend to your Oracle VM, replace the release URL with
     * your DuckDNS domain (e.g. `https://smart-cost-pro.duckdns.org/`).
     */
    val BACKEND_URL: String = if (com.aistudio.smartcostpro.app.BuildConfig.DEBUG) {
        "http://10.0.2.2:3001/"  // Android emulator → host PC's localhost:3001
    } else {
        "https://REPLACE_ME_WITH_YOUR_DOMAIN/"  // ← e.g. https://smart-cost-pro.duckdns.org/
    }
    /** Backwards-compat aliases — all point to BACKEND_URL. */
    val BASE_URL: String get() = BACKEND_URL
    val ORACLE_BASE_URL: String get() = BACKEND_URL
    const val NETWORK_TIMEOUT_SECONDS = 30L

    // ────────────────────────── Validation ──────────────────────────────────
    const val MIN_AMOUNT = 0.1
    const val AMOUNT_STEP = 0.01
    const val MAX_NOTES_LENGTH = 2000
    const val NATIONAL_ID_LENGTH = 14

    // ────────────────────────── Backup ──────────────────────────────────────
    const val BACKUP_SCHEMA_VERSION = 2
    const val BACKUP_FILE_PREFIX = "cost_app_pro_backup_"
    const val BACKUP_FILE_EXTENSION = ".json"
}

/**
 * Supported application languages.
 * Codes match the values used by the original SMART COST app.
 */
enum class AppLocale(val code: String, val englishName: String, val nativeName: String) {
    Arabic("ar", "Arabic", "العربية"),
    English("en", "English", "English");

    companion object {
        fun fromCode(code: String?): AppLocale = when (code?.lowercase()) {
            "en" -> English
            else -> Arabic // default
        }
    }
}

/**
 * Supported currencies (subset of the spec).
 * Symbol is the ISO 4217 code; we don't use currency symbols to keep numbers readable.
 */
enum class AppCurrency(val code: String, val displayName: String) {
    EGP("EGP", "Egyptian Pound"),
    SAR("SAR", "Saudi Riyal"),
    AED("AED", "UAE Dirham"),
    USD("USD", "US Dollar"),
    EUR("EUR", "Euro");

    companion object {
        fun fromCode(code: String?): AppCurrency = when (code?.uppercase()) {
            "SAR" -> SAR
            "AED" -> AED
            "USD" -> USD
            "EUR" -> EUR
            else -> EGP
        }
    }
}
