package com.lockself.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lockself.app.ui.navigation.LockSelfNavGraph
import com.lockself.app.ui.navigation.NavRoute
import com.lockself.app.ui.theme.LockSelfTheme

class MainActivity : FragmentActivity() {

    private val app get() = LockSelfApp.instance
    private var backgroundTimestamp: Long = 0L
    private var currentNavController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            currentNavController = navController

            LockSelfTheme(darkTheme = app.preferences.darkTheme) {
                LockSelfNavGraph(
                    navController = navController,
                    repository = app.repository,
                    crypto = app.cryptoManager,
                    preferences = app.preferences,
                    clipboardHelper = app.clipboardHelper,
                    onLaunchBiometricPrompt = {
                        launchBiometricPrompt(
                            onSuccess = {
                                if (app.cryptoManager.unlockWithBiometric()) {
                                    app.preferences.resetReminderCounter()
                                    navController.navigate(NavRoute.VaultMain.route) {
                                        popUpTo(NavRoute.Lock.route) { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(this, "Biometric authentication failed to restore key", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = { err ->
                                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Only mark background timestamp if not temporarily launching an SAF file picker or biometric prompt
        if (!app.isSafPickerActive && !app.isBiometricPromptActive) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    override fun onResume() {
        super.onResume()

        if (app.isSafPickerActive || app.isBiometricPromptActive) {
            backgroundTimestamp = 0L
            return
        }

        if (backgroundTimestamp != 0L && app.cryptoManager.isVaultUnlocked()) {
            val elapsedSeconds = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            val timeoutSeconds = app.preferences.lockTimeoutSeconds

            if (app.preferences.lockOnMinimize || (timeoutSeconds > 0 && elapsedSeconds >= timeoutSeconds)) {
                app.cryptoManager.lockVault()
                app.clipboardHelper.onVaultLocked()
                currentNavController?.navigate(NavRoute.Lock.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        backgroundTimestamp = 0L
    }

    private fun launchBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric authentication is not available or not enrolled on this device")
            return
        }

        app.isBiometricPromptActive = true
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    app.isBiometricPromptActive = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    app.isBiometricPromptActive = false
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Kept active for subsequent finger touches
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LockSelf")
            .setSubtitle("Confirm your biometric credential to unlock your vault")
            .setNegativeButtonText("Use Master Password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        runCatching {
            biometricPrompt.authenticate(promptInfo)
        }.onFailure {
            app.isBiometricPromptActive = false
            onError("Unable to launch biometric prompt: ${it.localizedMessage}")
        }
    }
}
