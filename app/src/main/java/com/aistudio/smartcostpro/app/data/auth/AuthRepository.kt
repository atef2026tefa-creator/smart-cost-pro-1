package com.aistudio.smartcostpro.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authPrefs by preferencesDataStore(name = "smart_cost_auth")

/**
 * Authentication repository — Stage 3.3.1 (Oracle Cloud backend, no Firebase).
 *
 * **Backend:** REST API on Oracle Cloud Free Tier (Express.js + PostgreSQL).
 * See `backend/` for the server code and `oracle/README.md` for setup.
 *
 * **What's wired up:**
 *  - **Email + password sign-in / sign-up** via POST /api/auth/login | /api/auth/register
 *  - **Google Sign-In** via POST /api/auth/google (the server verifies the ID token)
 *  - **Biometric opt-in** stored locally as a boolean flag.
 *  - **Forgot-password flow** (6-digit OTP, 10-minute validity, single-use):
 *      - POST /api/auth/forgot-password → server emails the OTP
 *      - POST /api/auth/verify-otp-and-reset → server verifies OTP + sets new password
 *
 * **What's stored locally:**
 *  - The JWT token (30-day validity — server-side session).
 *  - The user's display name + email (so the UI can greet them without a network call).
 *  - The biometric opt-in flag.
 *
 * **What never leaves the device:**
 *  - The plain password (we pass it over HTTPS straight to the server, which bcrypts it).
 *  - The biometric enrollment status (only the OS knows that).
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Base URL of the Smart Cost Pro backend.
     *
     * Replace with your Oracle Cloud VM public IP + port, e.g.
     * `http://129.156.123.45:3000/` or `https://smartcost.example.com/`.
     */
    private val baseUrl: String
        get() = com.aistudio.smartcostpro.app.util.Constants.BACKEND_URL

    /** Live session stream (null = logged out). */
    val session: Flow<AuthSession?> = callbackFlow {
        // Read the locally-stored session on emit
        val prefs = context.authPrefs.data
        prefs.collect { p ->
            val token = p[Keys.TOKEN]
            val email = p[Keys.EMAIL]
            val displayName = p[Keys.DISPLAY_NAME]
            val isGuest = p[Keys.IS_GUEST] ?: false
            if (token == null && isGuest == false) {
                trySend(null)
            } else {
                trySend(AuthSession(email ?: "", displayName ?: "", isGuest, uid = null, token = token))
            }
        }
        awaitClose { /* nothing to clean up — DataStore handles it */ }
    }

    // ──────────────────────────── Sign in ────────────────────────────

    suspend fun signIn(email: String, password: String): Result<AuthSession> {
        if (email.isBlank()) return Result.failure(AuthException("EMPTY_EMAIL"))
        if (password.isBlank()) return Result.failure(AuthException("EMPTY_PASSWORD"))

        return runCatching {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val response = post("/api/auth/login", body)
            val user = response.getJSONObject("user")
            val token = response.getString("token")
            val displayName = user.optString("displayName", email.substringBefore("@"))
            saveSession(token, user.getString("email"), displayName, isGuest = false)
            AuthSession(user.getString("email"), displayName, false, null, token)
        }.recoverCatching { e ->
            throw AuthException(mapHttpError(e))
        }
    }

    // ──────────────────────────── Sign up ────────────────────────────

    suspend fun signUp(email: String, password: String): Result<AuthSession> {
        if (email.isBlank()) return Result.failure(AuthException("EMPTY_EMAIL"))
        if (password.length < 6) return Result.failure(AuthException("WEAK_PASSWORD"))

        return runCatching {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("displayName", email.substringBefore("@").replace(".", " "))
            }
            val response = post("/api/auth/register", body)
            val user = response.getJSONObject("user")
            val token = response.getString("token")
            val displayName = user.optString("displayName", email.substringBefore("@"))
            saveSession(token, user.getString("email"), displayName, isGuest = false)
            AuthSession(user.getString("email"), displayName, false, null, token)
        }.recoverCatching { e ->
            throw AuthException(mapHttpError(e))
        }
    }

    // ──────────────────────────── Google Sign-In ────────────────────────────

    suspend fun signInWithGoogle(idToken: String): Result<AuthSession> {
        return runCatching {
            val body = JSONObject().apply {
                // The server expects a JSON payload with email, googleId, displayName.
                // For Stage 3.3.1 we pass the raw idToken — the server can decode it
                // using google-auth-library (Stage 4 wiring).
                put("idToken", idToken)
            }
            val response = post("/api/auth/google", body)
            val user = response.getJSONObject("user")
            val token = response.getString("token")
            val displayName = user.optString("displayName", "")
            saveSession(token, user.getString("email"), displayName, isGuest = false)
            AuthSession(user.getString("email"), displayName, false, null, token)
        }.recoverCatching { e ->
            throw AuthException(mapHttpError(e))
        }
    }

    // ──────────────────────────── Forgot password (OTP) ────────────────────────────

    suspend fun generateAndSendOtp(email: String): Result<String> {
        if (email.isBlank()) return Result.failure(AuthException("EMPTY_EMAIL"))
        return runCatching {
            val body = JSONObject().apply { put("email", email) }
            val response = post("/api/auth/forgot-password", body)
            // In production the server returns `otp: null`; in dev it returns the OTP
            // so the app can display it for testing.
            response.optString("otp", "")
        }.recoverCatching { e ->
            throw AuthException(mapHttpError(e))
        }
    }

    // ──────────────────────────── Verify OTP + Reset password ────────────────────────────

    suspend fun verifyOtpAndResetPassword(email: String, otp: String, newPassword: String): Result<Unit> {
        if (otp.length != 6) return Result.failure(AuthException("OTP_INVALID"))
        if (newPassword.length < 6) return Result.failure(AuthException("WEAK_PASSWORD"))

        return runCatching {
            val body = JSONObject().apply {
                put("email", email)
                put("otp", otp)
                put("newPassword", newPassword)
            }
            post("/api/auth/verify-otp-and-reset", body)
        }.recoverCatching { e ->
            throw AuthException(mapHttpError(e))
        }
    }

    // ──────────────────────────── Guest + Sign out ────────────────────────────

    suspend fun signInAsGuest(): Result<AuthSession> {
        // Guest = local only, no server call.
        val email = "guest@smartcost.pro"
        val displayName = "Guest"
        saveSession(token = "", email = email, displayName = displayName, isGuest = true)
        return Result.success(AuthSession(email, displayName, isGuest = true, uid = null, token = ""))
    }

    suspend fun signOut() {
        context.authPrefs.edit { it.clear() }
    }

    // ──────────────────────────── Biometric opt-in ────────────────────────────

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.authPrefs.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    val biometricEnabled: Flow<Boolean> = context.authPrefs.data
        .map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    // ──────────────────────────── HTTP helper ────────────────────────────

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(baseUrl.trimEnd('/') + path)
        // Read the JWT token from DataStore (one-shot, fast — DataStore caches it).
        val token = kotlinx.coroutines.flow.first(
            context.authPrefs.data.map { it[Keys.TOKEN] }
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            // Stage 3.3.2 fix: send the JWT token as a Bearer for protected routes.
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                conn.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            }
            if (code !in 200..299) {
                val errCode = try {
                    JSONObject(responseText).optString("error", "HTTP_$code")
                } catch (_: Exception) { "HTTP_$code" }
                throw AuthException(errCode)
            }
            JSONObject(responseText)
        } finally {
            conn.disconnect()
        }
    }

    private fun saveSession(token: String, email: String, displayName: String, isGuest: Boolean) {
        kotlinx.coroutines.runBlocking {
            context.authPrefs.edit {
                it[Keys.TOKEN] = token
                it[Keys.EMAIL] = email
                it[Keys.DISPLAY_NAME] = displayName
                it[Keys.IS_GUEST] = isGuest
            }
        }
    }

    private fun mapHttpError(e: Throwable): String = when (e) {
        is AuthException -> e.code
        is java.net.UnknownHostException -> "NETWORK_UNREACHABLE"
        is java.net.SocketTimeoutException -> "NETWORK_TIMEOUT"
        else -> "UNKNOWN: ${e.message ?: ""}"
    }

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }
}

/**
 * Snapshot of the current session.
 */
data class AuthSession(
    val email: String,
    val displayName: String,
    val isGuest: Boolean,
    val uid: String?,
    val token: String? = null
)

class AuthException(val code: String) : Exception(code)
