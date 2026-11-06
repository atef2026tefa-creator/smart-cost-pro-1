package com.aistudio.smartcostpro.app.data.oracle

import com.aistudio.smartcostpro.app.domain.model.Expense
import com.aistudio.smartcostpro.app.domain.model.MonthlyIncome
import com.aistudio.smartcostpro.app.domain.model.BankCertificate
import com.aistudio.smartcostpro.app.domain.model.CarTrip
import com.aistudio.smartcostpro.app.domain.model.Student
import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.VehicleFuel
import com.aistudio.smartcostpro.app.domain.model.VehicleMaintenance
import com.aistudio.smartcostpro.app.domain.model.VehicleOil
import com.aistudio.smartcostpro.app.domain.model.VehicleAccident
import com.aistudio.smartcostpro.app.domain.model.CurrentAccountTransaction
import com.aistudio.smartcostpro.app.domain.model.Budget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Oracle backend client — Stage 3.3 placeholder.
 *
 * **Per the project owner's spec:** the financial data (expenses, income,
 * certificates, students, etc.) lives on the user's own Oracle server
 * (1-core CPU, 6 GB RAM). Authentication is handled by the same server
 * via JWT + bcrypt (see `AuthRepository`).
 *
 * This is the placeholder interface the app talks to for any non-auth data.
 * The actual REST endpoint URLs and payloads are owned by the project owner
 * and will be filled in when their Oracle backend is exposed via REST.
 *
 * Until then, the local Room database remains the source of truth and this
 * client is a no-op — see [SyncRepository] for the offline-first flow.
 *
 * **Setup checklist for the project owner:**
 *  1. Stand up an HTTP listener on the Oracle server (e.g. `node-oracledb`,
 *     `oracle-rest-data-services` (ORDS), or any REST framework).
 *  2. Expose endpoints matching the methods below (`GET /api/expenses`, etc.).
 *  3. Set the base URL in `Constants.ORACLE_BASE_URL`.
 *  4. Implement this client using the existing Retrofit interface
 *     (`com.aistudio.smartcostpro.app.data.remote.api.*`) — those are already wired but
 *     currently pointed at a mock. Swap the BASE_URL constant.
 *
 * See `oracle/README.md` for the OpenAPI-style contract suggestion.
 */
@Singleton
class OracleClient @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    /**
     * Pushes local Room data to the Oracle server for the logged-in user.
     *
     * Returns true if the sync succeeded. Currently a no-op (Stage 3.3
     * placeholder); the implementation will use the Retrofit interfaces
     * already defined in `com.aistudio.smartcostpro.app.data.remote.api`.
     */
    suspend fun pushToOracle(userId: String): Boolean {
        // TODO: implement when Oracle REST is exposed.
        return true
    }

    /**
     * Pulls fresh data from the Oracle server into the local Room database.
     */
    suspend fun pullFromOracle(userId: String): Boolean {
        // TODO: implement when Oracle REST is exposed.
        return true
    }

    /**
     * Returns true if the Oracle backend is reachable for the active session.
     */
    fun isAvailable(): Flow<Boolean> = flowOf(false)
}

/**
 * Sync orchestration — coordinates Room (offline-first) with Oracle (push/pull).
 *
 * The app **always reads from Room** so it works offline. When the device
 * comes online and a user is signed in, the [SyncRepository] pushes local
 * changes to Oracle and pulls fresh data. This is the same pattern used
 * by the original SMART COST reference app.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val oracleClient: OracleClient
) {
    suspend fun push(userId: String): Boolean = oracleClient.pushToOracle(userId)
    suspend fun pull(userId: String): Boolean = oracleClient.pullFromOracle(userId)
    fun isAvailable(): Flow<Boolean> = oracleClient.isAvailable()
}
