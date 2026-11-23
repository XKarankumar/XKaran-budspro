package com.example.system

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricLockHelper {

    fun isBiometricOrCredentialAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bm = context.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager
            val res = bm?.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (res == BiometricManager.BIOMETRIC_SUCCESS) return true
        }
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    fun authenticate(
        context: Context,
        title: String = "Unlock KARAN BUDS",
        subtitle: String = "Verify fingerprint or device credential to continue",
        preferBiometric: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(context)
            val cancellationSignal = CancellationSignal()
            val builder = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val authenticators = if (preferBiometric) {
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                } else {
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                }
                builder.setAllowedAuthenticators(authenticators)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            } else {
                builder.setNegativeButton("Cancel", executor) { _, _ ->
                    onError("Authentication cancelled")
                }
            }

            val prompt = builder.build()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        onError(errString?.toString() ?: "Authentication cancelled")
                    }

                    override fun onAuthenticationFailed() {
                        onError("Fingerprint not recognized. Try again.")
                    }
                }
            )
        } else {
            onSuccess()
        }
    }
}
