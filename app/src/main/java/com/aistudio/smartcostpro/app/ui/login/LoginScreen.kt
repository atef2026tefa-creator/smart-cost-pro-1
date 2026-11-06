package com.aistudio.smartcostpro.app.ui.login

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.smartcostpro.app.R
import com.aistudio.smartcostpro.app.data.auth.BiometricHelper
import com.aistudio.smartcostpro.app.ui.theme.BrandColors
import com.google.android.gms.auth.api.identity.Identity

/**
 * Login screen — Stage 3.3 multi-stage flow.
 *
 * Stages:
 *  1. **Login** — email + password (or Google, or biometric, or guest).
 *  2. **Register** — sign up with email + password (twice for confirmation).
 *  3. **ForgotPassword** — enter email to receive OTP.
 *  4. **OtpVerification** — enter the 6-digit code (10-minute validity).
 *  5. **ResetPassword** — enter new password twice, must match.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate up when the user signs in.
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    val brandGradient = Brush.verticalGradient(
        colors = listOf(
            BrandColors.PrimaryDark,
            BrandColors.Primary,
            BrandColors.PrimaryLight.copy(alpha = 0.85f)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brandGradient)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppHeader()
                Spacer(Modifier.height(24.dp))
                when (state.stage) {
                    LoginStage.LOGIN -> LoginStageCard(viewModel, state)
                    LoginStage.REGISTER -> RegisterStageCard(viewModel, state)
                    LoginStage.FORGOT_PASSWORD -> ForgotPasswordStageCard(viewModel, state)
                    LoginStage.OTP_VERIFICATION -> OtpVerificationStageCard(viewModel, state)
                    LoginStage.RESET_PASSWORD -> ResetPasswordStageCard(viewModel, state)
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = "Smart Cost Pro",
            modifier = Modifier.size(96.dp)
        )
    }
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.login_app_name),
            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        Surface(
            color = BrandColors.Accent,
            shape = MaterialTheme.shapes.small,
            contentColor = Color.White
        ) {
            Text(
                text = stringResource(R.string.login_badge),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.login_tagline),
        color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp
    )
}

@Composable
private fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun LoginStageCard(viewModel: LoginViewModel, state: LoginUiState) {
    AuthCard {
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.login_email_label)) },
            placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
            singleLine = true,
            isError = state.emailError != null,
            supportingText = state.emailError?.let { { Text(stringResource(R.string.login_empty_email)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label)) },
            placeholder = { Text(stringResource(R.string.login_password_placeholder)) },
            singleLine = true,
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { { Text(stringResource(R.string.login_empty_password)) } },
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = viewModel::showForgotPassword) {
                Text(stringResource(R.string.login_forgot_password))
            }
        }
        Button(
            onClick = viewModel::submitLogin,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.login_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Biometric button (only if available on the device)
        if (state.biometricAvailable) {
            val activity = LocalContext.current as? FragmentActivity
            OutlinedButton(
                onClick = {
                    if (activity != null) {
                        val helper = (activity as com.aistudio.smartcostpro.app.MainActivity).biometricHelper
                        helper.prompt(
                            activity = activity,
                            title = "تسجيل الدخول بالبصمة",
                            subtitle = "استخدم بصمتك للدخول إلى Smart Cost Pro",
                            onSuccess = { viewModel.onBiometricSuccess() },
                            onFailure = { /* error handling done by BiometricPrompt */ }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.login_biometric))
            }
        }

        // Google Sign-In
        GoogleSignInButton(viewModel = viewModel)

        // "أو" divider
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text(
                text = stringResource(R.string.login_or),
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }

        OutlinedButton(
            onClick = viewModel::signInAsGuest,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(stringResource(R.string.login_guest))
        }

        FormErrorAndSuccess(state)

        // Footer link to register
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.login_no_account_question),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            TextButton(onClick = viewModel::showRegister) {
                Text(stringResource(R.string.login_create_account))
            }
        }
    }
}

@Composable
private fun RegisterStageCard(viewModel: LoginViewModel, state: LoginUiState) {
    AuthCard {
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.login_email_label)) },
            singleLine = true,
            isError = state.emailError != null,
            supportingText = state.emailError?.let { { Text(stringResource(R.string.login_empty_email)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label)) },
            singleLine = true,
            isError = state.passwordError != null,
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = { Text(stringResource(R.string.register_confirm_password)) },
            singleLine = true,
            isError = state.passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        state.passwordError?.let {
            if (it == "PASSWORD_MISMATCH") Text(stringResource(R.string.register_password_mismatch), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            if (it == "WEAK_PASSWORD") Text(stringResource(R.string.register_weak_password), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Button(
            onClick = viewModel::submitRegister,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.register_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        FormErrorAndSuccess(state)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = viewModel::showLogin) {
                Text(stringResource(R.string.login_already_have_account))
            }
        }
    }
}

@Composable
private fun ForgotPasswordStageCard(viewModel: LoginViewModel, state: LoginUiState) {
    AuthCard {
        Text(
            text = stringResource(R.string.forgot_password_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.forgot_password_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.login_email_label)) },
            singleLine = true,
            isError = state.emailError != null,
            supportingText = state.emailError?.let { { Text(stringResource(R.string.login_empty_email)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = viewModel::submitForgotPassword,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.forgot_password_send_otp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        state.formSuccess?.let {
            if (it == "OTP_SENT") Text(stringResource(R.string.otp_sent_success), color = BrandColors.Success, fontSize = 13.sp)
        }
        FormErrorAndSuccess(state)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = viewModel::showLogin) {
                Text(stringResource(R.string.login_back_to_login))
            }
        }
    }
}

@Composable
private fun OtpVerificationStageCard(viewModel: LoginViewModel, state: LoginUiState) {
    AuthCard {
        Text(
            text = stringResource(R.string.otp_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.otp_subtitle, state.email),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        // Dev hint shows the OTP locally for testing (only in debug builds).
        state.devOtpHint?.let { hint ->
            Text(
                text = "(DEV) OTP: $hint",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        OutlinedTextField(
            value = state.otp,
            onValueChange = viewModel::onOtpChange,
            label = { Text(stringResource(R.string.otp_label)) },
            placeholder = { Text("123456") },
            singleLine = true,
            isError = state.otpError != null,
            supportingText = state.otpError?.let { { Text(stringResource(R.string.otp_invalid)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = viewModel::submitOtpVerification,
            enabled = !state.isSubmitting && state.otp.length == 6,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.otp_verify), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = stringResource(R.string.otp_validity_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        FormErrorAndSuccess(state)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = viewModel::showForgotPassword) {
                Text(stringResource(R.string.otp_resend))
            }
        }
    }
}

@Composable
private fun ResetPasswordStageCard(viewModel: LoginViewModel, state: LoginUiState) {
    AuthCard {
        Text(
            text = stringResource(R.string.reset_password_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.reset_password_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        OutlinedTextField(
            value = state.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            label = { Text(stringResource(R.string.reset_password_new)) },
            singleLine = true,
            isError = state.newPasswordError != null,
            visualTransformation = if (state.newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::onNewPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (state.newPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.newPasswordConfirm,
            onValueChange = viewModel::onNewPasswordConfirmChange,
            label = { Text(stringResource(R.string.reset_password_confirm)) },
            singleLine = true,
            isError = state.newPasswordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        state.newPasswordError?.let {
            if (it == "WEAK_PASSWORD") Text(stringResource(R.string.register_weak_password), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            if (it == "PASSWORD_MISMATCH") Text(stringResource(R.string.register_password_mismatch), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Button(
            onClick = viewModel::submitResetPassword,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.reset_password_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        FormErrorAndSuccess(state)
    }
}

@Composable
private fun FormErrorAndSuccess(state: LoginUiState) {
    state.formError?.let { code ->
        val resId = when (code) {
            "EMPTY_EMAIL" -> R.string.login_empty_email
            "EMPTY_PASSWORD" -> R.string.login_empty_password
            "INVALID_CREDENTIALS", "USER_NOT_FOUND" -> R.string.login_invalid_credentials
            "EMAIL_EXISTS" -> R.string.register_email_exists
            "WEAK_PASSWORD" -> R.string.register_weak_password
            "OTP_INVALID" -> R.string.otp_invalid
            "OTP_EXPIRED" -> R.string.otp_expired
            "OTP_NOT_FOUND" -> R.string.otp_not_found
            "OTP_USED" -> R.string.otp_used
            "PASSWORD_MISMATCH" -> R.string.register_password_mismatch
            "OTP_SENT" -> R.string.otp_sent_success
            "PASSWORD_RESET_SUCCESS" -> R.string.reset_password_success
            else -> R.string.auth_unknown_error
        }
        Text(stringResource(resId), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun GoogleSignInButton(viewModel: LoginViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            try {
                val credential = Identity.getSignInClient(context)
                    .getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: Exception) {
                // ignored — the form error will surface
            }
        }
    }

    OutlinedButton(
        onClick = {
            val activity = context as? ComponentActivity ?: return@OutlinedButton
            val signInClient = Identity.getSignInClient(context)
            val signInRequest = com.google.android.gms.auth.api.identity.SignInRequest.Builder()
                .setGoogleIdTokenRequestOptions(
                    com.google.android.gms.auth.api.identity.GoogleIdTokenRequestOptions.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId("388782080421-re43u53oessa3t0ke72a3gs3po6kbjvq.apps.googleusercontent.com") // From google-services.json → oauth_client → client_type 3
                        .build()
                )
                .build()
            try {
                val pending = signInClient.getSignInIntent(signInRequest)
                launcher.launch(IntentSenderRequest.Builder(pending.intentSenderRequest).build())
            } catch (_: Exception) { /* surface in form error */ }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        // Google "G" logo as text (avoids bundling the official asset).
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.login_google))
    }
}
