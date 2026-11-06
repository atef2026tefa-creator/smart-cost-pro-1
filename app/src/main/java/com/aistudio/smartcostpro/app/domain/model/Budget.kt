package com.aistudio.smartcostpro.app.domain.model

import kotlinx.serialization.Serializable

/**
 * User-set monthly budget.
 *
 * Stored under `smartcost_budget` in the reference app. The dashboard uses
 * this to display budget-vs-actuals and warn when expense ratio exceeds 90%.
 */
@Serializable
data class Budget(
    val month: String, // YYYY-MM
    val total: Double = 0.0,
    val house: Double = 0.0,
    val medical: Double = 0.0,
    val work: Double = 0.0,
    val personal: Double = 0.0,
    val fuel: Double = 0.0,
    val maintenance: Double = 0.0,
    val oils: Double = 0.0,
    val education: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * The persisted section ordering for the sections menu.
 *
 * Stored under `smartcost_expenses_sections_order`. The spec allows the user
 * to drag-and-drop sections to reorder them. The default order is:
 *   1. vehicle
 *   2. income_certs
 *   3. education
 *   4. house
 *   5. personal
 *   6. reports
 */
@Serializable
data class SectionOrder(
    val sections: List<SectionType>
)

/**
 * The set of "sections" the user can navigate to from the sections menu.
 *
 * Stage 1 includes all sections as route entries, but only `dashboard`,
 * `sections_menu`, `settings`, and a few subsection stubs are wired up —
 * the rest will be implemented in subsequent stages.
 */
@Serializable
enum class SectionType(val id: String) {
    Dashboard("dashboard"),
    SectionsMenu("sections_menu"),
    House("house"),
    Work("work"),
    Personal("personal"),
    Vehicle("vehicle"),
    Education("education"),
    IncomeCerts("income_certs"),
    Reports("reports"),
    CarIncome("car_income"),
    Associations("associations"),
    CurrentAccount("current_account"),
    Settings("settings");

    companion object {
        /**
         * The default ordering documented in the spec:
         *   1. Vehicle
         *   2. IncomeCerts
         *   3. Education
         *   4. House
         *   5. Personal
         *   6. Reports
         *
         * Stage 3 fix: added Work, Associations, CarIncome, CurrentAccount
         * so all section screens are reachable from the sections menu (per
         * code review — these 4 were previously unreachable).
         */
        val DEFAULT_ORDER: List<SectionType> = listOf(
            Vehicle,
            IncomeCerts,
            Education,
            House,
            Work,
            Personal,
            Associations,
            CarIncome,
            CurrentAccount,
            Reports
        )

        fun fromId(id: String?): SectionType? = id?.let { v ->
            entries.firstOrNull { it.id == v }
        }
    }
}
