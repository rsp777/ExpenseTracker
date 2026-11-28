package com.tracker.expensetracker.ui

import android.app.KeyguardManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.tracker.expensetracker.R

internal class AppLockPrompt(
    private val activity: FragmentActivity,
    private val viewModel: AppLockViewModel,
) {
    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.authenticationSucceeded()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val message = when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> null
                    else -> AppLockMessage.AUTHENTICATION_ERROR
                }
                viewModel.authenticationEnded(message)
            }
        },
    )

    fun authenticate(action: AppLockAuthentication) {
        if (!viewModel.beginAuthentication(action)) return
        val biometricsAvailable = BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
        val hasScreenLock = activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        if (!biometricsAvailable && !hasScreenLock) {
            viewModel.authenticationEnded(AppLockMessage.UNAVAILABLE)
            return
        }
        val title = when (action) {
            AppLockAuthentication.UNLOCK -> R.string.app_lock_unlock_title
            AppLockAuthentication.ENABLE -> R.string.app_lock_enable_title
            AppLockAuthentication.DISABLE -> R.string.app_lock_disable_title
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(title))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }
}
