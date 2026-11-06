package com.aistudio.smartcostpro.app.domain.model

/**
 * The eight unified expense types used by the system.
 *
 * **Critical:** the spec forbids collapsing any of these into a single
 * "expense" type; the unified totals in the dashboard rely on this enum.
 *
 * @param code The stable code persisted in the DB / JSON.
 * @param storageKey The key used by the reference app for sections order.
 */
enum class ExpenseType(val code: String, val storageKey: String) {
    House("house", "house"),
    Medical("medical", "medical"),
    Work("work", "work"),
    Personal("personal", "personal"),
    Student("student", "student"),
    VehicleFuel("vehicle_fuel", "fuel"),
    VehicleMaint("vehicle_maint", "maintenance"),
    VehicleOil("vehicle_oil", "oils");

    companion object {
        /**
         * Returns the [ExpenseType] matching the given code, or null when
         * the code is unknown — the spec is strict about not inventing new types.
         */
        fun fromCode(code: String?): ExpenseType? = code?.let { c ->
            entries.firstOrNull { it.code == c }
        }

        /** All eight types in their canonical order. */
        val ALL: List<ExpenseType> = entries.toList()

        /** The eight types used by the dashboard expense totals. */
        val DASHBOARD_TYPES: List<ExpenseType> = ALL
    }
}
