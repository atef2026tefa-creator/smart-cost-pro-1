package com.aistudio.smartcostpro.app.data.remote

import com.aistudio.smartcostpro.app.data.remote.api.BackupApi
import com.aistudio.smartcostpro.app.data.remote.api.CertificateApi
import com.aistudio.smartcostpro.app.data.remote.api.ExpenseApi
import com.aistudio.smartcostpro.app.data.remote.api.IncomeApi
import com.aistudio.smartcostpro.app.data.remote.dto.BackupDto
import com.aistudio.smartcostpro.app.data.remote.dto.CertificateDto
import com.aistudio.smartcostpro.app.data.remote.dto.ExpenseDto
import com.aistudio.smartcostpro.app.data.remote.dto.IncomeDto
import kotlinx.coroutines.delay
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * In-process implementations of the API interfaces.
 *
 * The spec is explicit: the production API is disabled until authentication
 * is configured (the original SMART-COST code printed:
 *   `SMARTCOST API is disabled in production until authentication is configured.`)
 * We mirror that by returning empty lists / 200 OK on safe calls so the
 * ViewModel can exercise the full code path without leaking any data.
 *
 * Stage 2 will swap these mocks for real Retrofit implementations; the
 * repository layer above is already designed so this swap is one-line.
 *
 * The Kotlin compiler would not allow three `list()` overrides with the same
 * JVM signature inside a single class, so each interface gets its own
 * [Singleton] implementation here.
 */

private const val ARTIFICIAL_LATENCY_MS = 50L

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockApiMarker

@Singleton
@MockApiMarker
class MockExpenseApi @Inject constructor() : ExpenseApi {
    override suspend fun list(): Response<List<ExpenseDto>> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(emptyList())
    }

    override suspend fun create(body: ExpenseDto): Response<ExpenseDto> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(body)
    }

    override suspend fun update(id: String, body: ExpenseDto): Response<ExpenseDto> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(body.copy(id = id))
    }

    override suspend fun delete(id: String): Response<Unit> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(Unit)
    }
}

@Singleton
@MockApiMarker
class MockIncomeApi @Inject constructor() : IncomeApi {
    override suspend fun list(): Response<List<IncomeDto>> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(emptyList())
    }
}

@Singleton
@MockApiMarker
class MockCertificateApi @Inject constructor() : CertificateApi {
    override suspend fun list(): Response<List<CertificateDto>> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(emptyList())
    }
}

@Singleton
@MockApiMarker
class MockBackupApi @Inject constructor() : BackupApi {
    override suspend fun export(): Response<BackupDto> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(
            BackupDto(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                expenses = emptyList(),
                income = emptyList(),
                certificates = emptyList(),
                carIncome = emptyList()
            )
        )
    }

    override suspend fun restore(body: BackupDto): Response<Unit> {
        delay(ARTIFICIAL_LATENCY_MS)
        return Response.success(Unit)
    }
}
