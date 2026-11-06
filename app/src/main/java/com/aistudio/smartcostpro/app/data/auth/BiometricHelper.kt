package com.aistudio.smartcostpro.app.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Biometric helper — Stage 3.3.
 *
 * Wraps AndroidX BiometricPrompt so the UI code stays simple. The activity
 * invoking the prompt must be a [FragmentActivity] (which `AppCompatActivity`
 * is, so `MainActivity` qualifies).
 *
 * Class 3 (Strong) biometrics are required for the financial-app context:
 * finger / face / iris (the strongest class available on the device).
 */
@Singleton
class BiometricHelper @Inject constructor(
    private val context: Context
) {

    /**
     * Returns true if the device can authenticate the user with a Class 3
     * (Strong) biometric — i.e. fingerprint or face is enrolled and the
     * hardware is available.
     */
    fun isAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Prompts the user for biometric authentication.
     *
     * @param activity The hosting [FragmentActivity] (MainActivity).
     * @param title The prompt title (e.g. "تسجيل الدخول بالبصمة").
     * @param subtitle The prompt subtitle (e.g. "استخدم بصمتك للدخول إلى Smart Cost Pro").
     * @param onSuccess Called when the user authenticated successfully.
     * @param onFailure Called with an error message when the user cancelled,
     *   failed too many times, or no biometric is enrolled.
     */
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                // Called for each failed attempt — we don't surface this
                // individually; the prompt UI itself shows the "try again"
                // message. The error callback above fires on hard errors
                // (lockout, cancel, etc.).
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("إلغاء") // For non-crypto authentication
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }
}
