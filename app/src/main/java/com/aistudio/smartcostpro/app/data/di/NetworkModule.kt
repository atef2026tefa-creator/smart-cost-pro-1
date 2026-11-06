package com.aistudio.smartcostpro.app.data.di

import com.aistudio.smartcostpro.app.data.remote.MockApiMarker
import com.aistudio.smartcostpro.app.data.remote.MockBackupApi
import com.aistudio.smartcostpro.app.data.remote.MockCertificateApi
import com.aistudio.smartcostpro.app.data.remote.MockExpenseApi
import com.aistudio.smartcostpro.app.data.remote.MockIncomeApi
import com.aistudio.smartcostpro.app.data.remote.api.BackupApi
import com.aistudio.smartcostpro.app.data.remote.api.CertificateApi
import com.aistudio.smartcostpro.app.data.remote.api.ExpenseApi
import com.aistudio.smartcostpro.app.data.remote.api.IncomeApi
import com.aistudio.smartcostpro.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that wires the network layer.
 *
 * Stage 1 binds the **mock** implementations directly — the [Retrofit] is
 * still constructed (so Stage 2 can swap the bindings to real Retrofit
 * interfaces by simply removing the @MockApiMarker providers) but the
 * ViewModel will receive the mocks.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // ───────── Mock bindings (Stage 1) ─────────

    @Provides
    @Singleton
    @MockApiMarker
    fun provideMockExpenseApi(): ExpenseApi = MockExpenseApi()

    @Provides
    @Singleton
    @MockApiMarker
    fun provideMockIncomeApi(): IncomeApi = MockIncomeApi()

    @Provides
    @Singleton
    @MockApiMarker
    fun provideMockCertificateApi(): CertificateApi = MockCertificateApi()

    @Provides
    @Singleton
    @MockApiMarker
    fun provideMockBackupApi(): BackupApi = MockBackupApi()
}
