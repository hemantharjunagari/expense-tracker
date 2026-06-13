package com.spendless.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.spendless.app.core.data.datastore.PreferencesDataStore
import com.spendless.app.ui.navigation.NavGraph
import com.spendless.app.ui.theme.SpendLessTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeFlow = preferencesDataStore.themeMode.collectAsState(initial = "system")
            val themeStyleFlow = preferencesDataStore.themeStyle.collectAsState(initial = "standard")
            val onboardingCompleteState = preferencesDataStore.isOnboardingComplete.collectAsState(initial = null)
            val biometricEnabledState = preferencesDataStore.isBiometricEnabled.collectAsState(initial = null)

            var isAuthenticated by remember { mutableStateOf(false) }

            val onboardingComplete = onboardingCompleteState.value
            val biometricEnabled = biometricEnabledState.value

            LaunchedEffect(onboardingComplete, biometricEnabled) {
                if (onboardingComplete == true && biometricEnabled == true) {
                    showBiometricPrompt(
                        onSuccess = { isAuthenticated = true },
                        onError = {}
                    )
                } else if (onboardingComplete != null && biometricEnabled != null) {
                    isAuthenticated = true
                }
            }

            val isDarkTheme = when (themeFlow.value) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            SpendLessTheme(
                themeMode = themeFlow.value,
                themeStyle = themeStyleFlow.value
            ) {
                val backgroundModifier = if (themeStyleFlow.value == "glass") {
                    val gradient = if (isDarkTheme) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F1026),
                                Color(0xFF1D1B4C),
                                Color(0xFF090A1A)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE8F0FE),
                                Color(0xFFFCE4EC),
                                Color(0xFFECEFF1)
                            )
                        )
                    }
                    Modifier
                        .fillMaxSize()
                        .background(gradient)
                } else {
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = backgroundModifier
                    ) {
                        if (onboardingComplete != null && biometricEnabled != null) {
                            if (isAuthenticated) {
                                val startDest = if (onboardingComplete) {
                                    com.spendless.app.ui.navigation.Screen.Dashboard.route
                                } else {
                                    com.spendless.app.ui.navigation.Screen.Onboarding.route
                                }
                                NavGraph(startDestination = startDest)
                            } else {
                                // Display a clean, Nothing OS-inspired lock screen/placeholder while authenticating
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "SPENDLESS SECURE",
                                            style = com.spendless.app.ui.theme.DotMatrixLabel.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        TextButton(
                                            onClick = {
                                                showBiometricPrompt(
                                                    onSuccess = { isAuthenticated = true },
                                                    onError = {}
                                                )
                                            }
                                        ) {
                                            Text(
                                                "Tap to Unlock",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun showBiometricPrompt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricManager = BiometricManager.from(this)

        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onSuccess() // Skip biometric if not available
            return
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onError("Authentication failed")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SpendLess")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }
}
