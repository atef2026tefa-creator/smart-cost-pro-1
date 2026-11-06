package com.aistudio.smartcostpro.app.data.remote.api

import com.aistudio.smartcostpro.app.data.remote.dto.BackupDto
import com.aistudio.smartcostpro.app.data.remote.dto.CertificateDto
import com.aistudio.smartcostpro.app.data.remote.dto.ExpenseDto
import com.aistudio.smartcostpro.app.data.remote.dto.IncomeDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit interface for the spec's REST API.
 *
 * Stage 1 doesn't actually hit the network — the [com.aistudio.smartcostpro.app.data.remote.MockApiService]
 * implements these endpoints in-process so the wiring is real but no
 * traffic goes out. The spec explicitly notes the production API is disabled
 * until authentication is configured, which is exactly the behavior the mock
 * reproduces.
 *
 * The endpoints match the spec table verbatim:
 *
 *   GET    /api/expenses            list all expenses
 *   POST   /api/expenses            create an expense
 *   PUT    /api/expenses/:id        update an expense
 *   DELETE /api/expenses/:id        delete an expense
 *   GET    /api/income              list monthly income rows
 *   GET    /api/certificates        list certificates
 *   GET    /api/backup              export a backup payload
 *   POST   /api/backup/restore     restore a backup payload
 */
interface ExpenseApi {
    @GET("api/expenses")
    suspend fun list(): Response<List<ExpenseDto>>

    @POST("api/expenses")
    suspend fun create(@Body body: ExpenseDto): Response<ExpenseDto>

    @PUT("api/expenses/{id}")
    suspend fun update(@Path("id") id: String, @Body body: ExpenseDto): Response<ExpenseDto>

    @DELETE("api/expenses/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}

interface IncomeApi {
    @GET("api/income")
    suspend fun list(): Response<List<IncomeDto>>
}

interface CertificateApi {
    @GET("api/certificates")
    suspend fun list(): Response<List<CertificateDto>>
}

interface BackupApi {
    @GET("api/backup")
    suspend fun export(): Response<BackupDto>

    @POST("api/backup/restore")
    suspend fun restore(@Body body: BackupDto): Response<Unit>
}
