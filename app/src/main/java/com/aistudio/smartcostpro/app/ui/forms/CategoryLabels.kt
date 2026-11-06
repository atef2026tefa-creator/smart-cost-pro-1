package com.aistudio.smartcostpro.app.ui.forms

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aistudio.smartcostpro.app.R

/**
 * Localized labels for the spec's categories.
 *
 * The codes live in [com.aistudio.smartcostpro.app.domain.model.Categories]; this file
 * only handles the UI side (resolving a code to a localized string).
 *
 * If a code has no localized label yet (e.g. a future category added by
 * Stage 3), we fall back to the raw code so the UI never crashes.
 */
@Composable
fun localizedCategoryLabel(code: String): String {
    val resId = categoryLabelRes(code)
    return if (resId != 0) stringResource(resId) else code
}

/**
 * Returns the string resource id for the given category code, or 0 when
 * the code has no localized label yet.
 */
fun categoryLabelRes(code: String): Int = when (code) {
    // House
    "breakfast" -> R.string.cat_house_breakfast
    "lunch" -> R.string.cat_house_lunch
    "dinner" -> R.string.cat_house_dinner
    "electrical_plumbing_paint" -> R.string.cat_house_electrical
    "home_appliances" -> R.string.cat_house_appliances
    "furniture" -> R.string.cat_house_furniture
    "bills" -> R.string.cat_house_bills
    "cleaning_supplies" -> R.string.cat_house_cleaning
    // Work
    "office_supplies" -> R.string.cat_work_office
    "equipment_maintenance" -> R.string.cat_work_maintenance
    "software_subscriptions" -> R.string.cat_work_subscriptions
    "hospitality_meetings" -> R.string.cat_work_hospitality
    "work_transport" -> R.string.cat_work_transport
    "shipping_delivery" -> R.string.cat_work_shipping
    // Personal
    "food_drink" -> R.string.cat_personal_food
    "transport" -> R.string.cat_personal_transport
    "clothes" -> R.string.cat_personal_clothes
    "health_personal_care" -> R.string.cat_personal_health
    "entertainment" -> R.string.cat_personal_entertainment
    "gifts" -> R.string.cat_personal_gifts
    "personal_subscriptions" -> R.string.cat_personal_subscriptions
    "bills_communications" -> R.string.cat_personal_bills
    // Vehicle
    "gasoline_92" -> R.string.cat_vehicle_gasoline_92
    "gasoline_95" -> R.string.cat_vehicle_gasoline_95
    "gasoline_80" -> R.string.cat_vehicle_gasoline_80
    "diesel" -> R.string.cat_vehicle_diesel
    "natural_gas" -> R.string.cat_vehicle_gas
    "maintenance_oils" -> R.string.cat_vehicle_maintenance
    "spare_parts_tires" -> R.string.cat_vehicle_spare_parts
    "wash_cleaning" -> R.string.cat_vehicle_wash
    // Education
    "private_lessons" -> R.string.cat_edu_lessons
    "school_fees" -> R.string.cat_edu_school_fees
    "books_notes" -> R.string.cat_edu_books
    "school_bus" -> R.string.cat_edu_bus
    "supplies" -> R.string.cat_edu_supplies
    "personal_allowance" -> R.string.cat_edu_allowance
    // Common
    "other" -> R.string.cat_other
    // Medical
    "consultation" -> R.string.cat_medical_consultation
    "medication" -> R.string.cat_medical_medication
    "lab_test" -> R.string.cat_medical_lab
    "surgery" -> R.string.cat_medical_surgery
    "emergency" -> R.string.cat_medical_emergency
    "follow_up" -> R.string.cat_medical_followup
    else -> 0
}

/**
 * Returns the localized label for a payment method code.
 */
@Composable
fun localizedPaymentMethod(code: String): String {
    val resId = when (code) {
        "cash" -> R.string.payment_cash
        "card" -> R.string.payment_card
        "transfer" -> R.string.payment_transfer
        "wallet" -> R.string.payment_wallet
        "cheque" -> R.string.payment_cheque
        else -> 0
    }
    return if (resId != 0) stringResource(resId) else code
}

/**
 * Returns the localized label for an expense type code.
 */
@Composable
fun localizedExpenseType(code: String): String {
    val resId = when (code) {
        "house" -> R.string.expense_type_house
        "medical" -> R.string.expense_type_medical
        "work" -> R.string.expense_type_work
        "personal" -> R.string.expense_type_personal
        "student" -> R.string.expense_type_student
        "vehicle_fuel" -> R.string.expense_type_fuel
        "vehicle_maint" -> R.string.expense_type_maintenance
        "vehicle_oil" -> R.string.expense_type_oil
        else -> 0
    }
    return if (resId != 0) stringResource(resId) else code
}
