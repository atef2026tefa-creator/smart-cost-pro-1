package com.aistudio.smartcostpro.app.domain.model

/**
 * Per-section category catalog.
 *
 * The spec lists explicit category names for each expense section. We keep
 * the codes here (used in DB and JSON), and the UI surfaces localized names
 * through [com.aistudio.smartcostpro.app.ui.forms.CategoryLabels].
 *
 * **Spec compliance:**
 *  - Default category is the FIRST entry in each list (spec §4.1: "first
 *    category in the section").
 *  - Adding new categories requires a spec amendment — these catalogs are
 *    intentionally exhaustive and not user-editable at runtime in Stage 2.
 */
object Categories {

    // ───────────────────────── House ─────────────────────────
    /** The spec's house categories (breakfast, lunch, dinner, ...). */
    val HOUSE = listOf(
        "breakfast",
        "lunch",
        "dinner",
        "electrical_plumbing_paint",
        "home_appliances",
        "furniture",
        "bills",
        "cleaning_supplies",
        "other"
    )

    // ───────────────────────── Medical ─────────────────────────
    /**
     * The spec doesn't enumerate medical categories; we use these codes for
     * the type field of the medical record. The detailed medical fields
     * live inside `detailsJson` (see [MedicalDetails]).
     */
    val MEDICAL = listOf(
        "consultation",
        "medication",
        "lab_test",
        "surgery",
        "emergency",
        "follow_up",
        "other"
    )

    // ───────────────────────── Work ─────────────────────────
    val WORK = listOf(
        "office_supplies",
        "equipment_maintenance",
        "software_subscriptions",
        "hospitality_meetings",
        "work_transport",
        "shipping_delivery",
        "other"
    )

    // ───────────────────────── Personal ─────────────────────────
    val PERSONAL = listOf(
        "food_drink",
        "transport",
        "clothes",
        "health_personal_care",
        "entertainment",
        "gifts",
        "personal_subscriptions",
        "bills_communications",
        "other"
    )

    // ───────────────────────── Vehicle (general — used in AddExpenseModal) ─────────────────────────
    /**
     * Note: the spec warns that the generic vehicle category should NOT be
     * used to enter oil / filter records — the dedicated oil / filters form
     * must keep its own data path. This list is only for the generic
     * AddExpenseModal's vehicle options.
     */
    val VEHICLE = listOf(
        "gasoline_92",
        "gasoline_95",
        "gasoline_80",
        "diesel",
        "natural_gas",
        "maintenance_oils",
        "spare_parts_tires",
        "wash_cleaning",
        "other"
    )

    // ───────────────────────── Education ─────────────────────────
    val EDUCATION = listOf(
        "private_lessons",
        "school_fees",
        "books_notes",
        "school_bus",
        "supplies",
        "personal_allowance",
        "other"
    )

    /** Returns the categories for [type] per the spec. */
    fun forType(type: ExpenseType): List<String> = when (type) {
        ExpenseType.House -> HOUSE
        ExpenseType.Medical -> MEDICAL
        ExpenseType.Work -> WORK
        ExpenseType.Personal -> PERSONAL
        ExpenseType.Student -> EDUCATION
        ExpenseType.VehicleFuel, ExpenseType.VehicleMaint, ExpenseType.VehicleOil -> VEHICLE
    }

    /** The default category for [type] (first entry per spec §4.1). */
    fun defaultFor(type: ExpenseType): String = forType(type).first()
}
