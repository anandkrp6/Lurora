package com.bytecoder.lurora

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.frontend.ui.screens.SplashScreen
import com.bytecoder.lurora.frontend.ui.theme.LuroraTheme
import com.bytecoder.lurora.storage.preferences.AppPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    @Inject
    lateinit var appPreferencesManager: AppPreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            LuroraTheme {
                val view = LocalView.current
                val isDarkTheme = isSystemInDarkTheme()
                
                // Make system bars transparent
                LaunchedEffect(isDarkTheme) {
                    val window = (view.context as ComponentActivity).window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    
                    val windowInsetsController = WindowCompat.getInsetsController(window, view)
                    // Adapt status bar content to theme: dark content for light theme, light content for dark theme
                    windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }
                
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashContent()
                }
            }
        }
    }
    
    @Composable
    private fun SplashContent() {
        val isFirstLaunch by appPreferencesManager.isFirstLaunch.collectAsStateWithLifecycle(initialValue = true)
        val onboardingCompleted by appPreferencesManager.isOnboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
        
        SplashScreen(
            onSplashFinished = {
                navigateToNextScreen(isFirstLaunch, onboardingCompleted)
            }
        )
    }
    
    private fun navigateToNextScreen(isFirstLaunch: Boolean, onboardingCompleted: Boolean) {
        val intent = when {
            isFirstLaunch && !onboardingCompleted -> {
                Intent(this, OnboardingActivity::class.java)
            }
            else -> {
                Intent(this, MainActivity::class.java)
            }
        }
        
        startActivity(intent)
        finish()
    }
}