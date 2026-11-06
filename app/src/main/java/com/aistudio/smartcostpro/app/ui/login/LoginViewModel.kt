package com.aistudio.smartcostpro.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.smartcostpro.app.data.auth.AuthException
import com.aistudio.smartcostpro.app.data.auth.AuthRepository
import com.aistudio.smartcostpro.app.data.auth.AuthSession
import com.aistudio.smartcostpro.app.data.auth.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which sub-screen is active in the login flow.
 *
 * Per the project owner's spec (Stage 3.3), the login experience has 5 phases:
 *  1. **Login** — email + password (or Google, or biometric, or guest).
 *  2. **Register** — sign up with email + password.
 *  3. **ForgotPassword** — enter email to receive OTP.
 *  4. **OtpVerification** — enter the 6-digit code (10-minute validity).
 *  5. **ResetPassword** — enter new password twice, must match.
 */
enum class LoginStage { LOGIN, REGISTER, FORGOT_PASSWORD, OTP_VERIFICATION, RESET_PASSWORD }

/**
 * Login UI state. Covers all 5 stages via [stage].
 */
data class LoginUiState(
    val stage: LoginStage = LoginStage.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val otp: String = "",
    val passwordVisible: Boolean = false,
    val newPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val otpError: String? = null,
    val newPasswordError: String? = null,
    val formError: String? = null,
    val formSuccess: String? = null,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    val devOtpHint: String? = null, // shown in dev builds to make testing easier
    val signedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        // Check if biometric is available on this device.
        _state.update { it.copy(biometricAvailable = biometricHelper.isAvailable()) }
    }

    // ─── Stage navigation ────────────────────────────────────────────────

    fun showRegister() {
        _state.update {
            it.copy(
                stage = LoginStage.REGISTER,
                email = "", password = "",
                emailError = null, passwordError = null, formError = null
            )
        }
    }

    fun showLogin() {
        _state.update {
            it.copy(
                stage = LoginStage.LOGIN,
                emailError = null, passwordError = null, formError = null
            )
        }
    }

    fun showForgotPassword() {
        _state.update {
            it.copy(
                stage = LoginStage.FORGOT_PASSWORD,
                emailError = null, formError = null
            )
        }
    }

    fun showOtpVerification() {
        _state.update {
            it.copy(
                stage = LoginStage.OTP_VERIFICATION,
                otp = "", otpError = null, formError = null
            )
        }
    }

    fun showResetPassword() {
        _state.update {
            it.copy(
                stage = LoginStage.RESET_PASSWORD,
                newPassword = "", newPasswordConfirm = "",
                newPasswordError = null, formError = null
            )
        }
    }

    // ─── Field setters ───────────────────────────────────────────────────

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, passwordError = null, formError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, newPasswordError = null, formError = null) }
    }

    fun onNewPasswordConfirmChange(value: String) {
        _state.update { it.copy(newPasswordConfirm = value, newPasswordError = null, formError = null) }
    }

    fun onOtpChange(value: String) {
        // Only accept up to 6 digits.
        val filtered = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(otp = filtered, otpError = null, formError = null) }
    }

    fun onPasswordVisibilityToggle() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onNewPasswordVisibilityToggle() {
        _state.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    // ─── Stage actions ───────────────────────────────────────────────────

    /**
     * Sign in with email + password.
     */
    fun submitLogin() {
        val s = _state.value
        if (s.isSubmitting) return
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = "EMPTY_EMAIL") }
            return
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(passwordError = "EMPTY_PASSWORD") }
            return
        }
        runAuth(s.email, s.password) { email, pw -> authRepository.signIn(email, pw) }
    }

    /**
     * Sign up — creates a new account with email + password.
     * Confirms the two passwords match.
     */
    fun submitRegister() {
        val s = _state.value
        if (s.isSubmitting) return
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = "EMPTY_EMAIL") }
            return
        }
        if (s.password.length < 6) {
            _state.update { it.copy(passwordError = "WEAK_PASSWORD") }
            return
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(passwordError = "PASSWORD_MISMATCH") }
            return
        }
        runAuth(s.email, s.password) { email, pw -> authRepository.signUp(email, pw) }
    }

    /**
     * Forgot-password — sends an OTP email to the entered address.
     */
    fun submitForgotPassword() {
        val s = _state.value
        if (s.isSubmitting) return
        if (s.email.isBlank()) {
            _state.update { it.copy(emailError = "EMPTY_EMAIL") }
            return
        }
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            // Stage 3.3.1 (Oracle backend): single endpoint generates + emails OTP.
            val otpResult = authRepository.generateAndSendOtp(s.email)
            otpResult.fold(
                onSuccess = { otp ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            stage = LoginStage.OTP_VERIFICATION,
                            // In dev the server returns the OTP for testing; in prod it's empty.
                            devOtpHint = if (com.aistudio.smartcostpro.app.BuildConfig.DEBUG && otp.isNotEmpty()) otp else null,
                            formSuccess = "OTP_SENT"
                        )
                    }
                },
                onFailure = { e ->
                    val code = (e as? AuthException)?.code ?: "OTP_FAILED"
                    _state.update { it.copy(isSubmitting = false, formError = code) }
                }
            )
        }
    }

    /**
     * Verify the 6-digit OTP the user entered.
     *
     * Stage 3.3.1: We just advance to the ResetPassword stage — the actual OTP +
     * new password are sent to the server in submitResetPassword(), which atomically
     * verifies the OTP and updates the password. This is safer than splitting the
     * verification from the password update (no race condition window).
     */
    fun submitOtpVerification() {
        val s = _state.value
        if (s.isSubmitting) return
        if (s.otp.length != 6) {
            _state.update { it.copy(otpError = "OTP_INVALID") }
            return
        }
        // Advance to reset-password stage — the OTP + new password are sent together.
        _state.update { it.copy(stage = LoginStage.RESET_PASSWORD) }
    }

    /**
     * Reset password — sends the OTP + new password to the server, which
     * atomically verifies the OTP and updates the password using bcrypt.
     *
     * Calls `verifyOtpAndResetPassword` Cloud Function equivalent on Oracle
     * backend (`POST /api/auth/verify-otp-and-reset`).
     */
    fun submitResetPassword() {
        val s = _state.value
        if (s.isSubmitting) return
        if (s.newPassword.length < 6) {
            _state.update { it.copy(newPasswordError = "WEAK_PASSWORD") }
            return
        }
        if (s.newPassword != s.newPasswordConfirm) {
            _state.update { it.copy(newPasswordError = "PASSWORD_MISMATCH") }
            return
        }
        if (s.otp.length != 6) {
            _state.update { it.copy(otpError = "OTP_INVALID") }
            return
        }
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            authRepository.verifyOtpAndResetPassword(s.email, s.otp, s.newPassword).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            formSuccess = "PASSWORD_RESET_SUCCESS",
                            stage = LoginStage.LOGIN,
                            password = "", confirmPassword = "",
                            newPassword = "", newPasswordConfirm = "",
                            otp = ""
                        )
                    }
                },
                onFailure = { e ->
                    val code = (e as? AuthException)?.code ?: "RESET_FAILED"
                    _state.update { it.copy(isSubmitting = false, formError = code) }
                }
            )
        }
    }

    /**
     * Google Sign-In — exchanges the ID token from the Google SDK for a
     * Firebase credential.
     */
    fun signInWithGoogle(idToken: String) {
        val s = _state.value
        if (s.isSubmitting) return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { session ->
                    _state.update { it.copy(isSubmitting = false, signedIn = true) }
                    _events.emit(LoginEvent.SignedIn(session))
                },
                onFailure = { e ->
                    val code = (e as? AuthException)?.code ?: "GOOGLE_FAILED"
                    _state.update { it.copy(isSubmitting = false, formError = code) }
                }
            )
        }
    }

    /**
     * Sign in as a guest (no credentials, local only).
     */
    fun signInAsGuest() {
        val s = _state.value
        if (s.isSubmitting) return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            authRepository.signInAsGuest().fold(
                onSuccess = { session ->
                    _state.update { it.copy(isSubmitting = false, signedIn = true) }
                    _events.emit(LoginEvent.SignedIn(session))
                },
                onFailure = {
                    _state.update { it.copy(isSubmitting = false, formError = "GUEST_FAILED") }
                }
            )
        }
    }

    /**
     * Trigger the biometric prompt. The actual prompt UI is in the activity;
     * the result comes back via [BiometricHelper.prompt]'s callbacks.
     */
    fun onBiometricSuccess() {
        // Treat biometric like a successful sign-in.
        _state.update { it.copy(signedIn = true) }
    }

    private fun runAuth(
        email: String,
        password: String,
        block: suspend (String, String) -> Result<AuthSession>
    ) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            block(email, password).fold(
                onSuccess = { session ->
                    _state.update { it.copy(isSubmitting = false, signedIn = true) }
                    _events.emit(LoginEvent.SignedIn(session))
                },
                onFailure = { e ->
                    val code = (e as? AuthException)?.code ?: "AUTH_FAILED"
                    _state.update { it.copy(isSubmitting = false, formError = code) }
                }
            )
        }
    }
}

sealed interface LoginEvent {
    data class SignedIn(val session: AuthSession) : LoginEvent
}
