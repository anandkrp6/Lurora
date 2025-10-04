package com.bytecoder.lurora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.bytecoder.lurora.frontend.ui.screens.onboarding.OnboardingScreen
import com.bytecoder.lurora.frontend.ui.theme.LuroraTheme
import com.bytecoder.lurora.frontend.viewmodels.OnboardingNavigationEvent
import com.bytecoder.lurora.frontend.viewmodels.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {
    
    private val viewModel: OnboardingViewModel by viewModels()
    
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
                    OnboardingContent()
                }
            }
        }
    }
    
    @Composable
    private fun OnboardingContent() {
        val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
        
        // Handle navigation events
        LaunchedEffect(navigationEvent) {
            when (navigationEvent) {
                is OnboardingNavigationEvent.NavigateToMain -> {
                    navigateToMainActivity()
                    viewModel.onNavigationEventHandled()
                }
                is OnboardingNavigationEvent.NavigateToPermissions -> {
                    viewModel.onNavigationEventHandled()
                }

                null -> {
                    // No navigation event to handle
                }
            }
        }
        
        OnboardingScreen(
            onGetStarted = {
                viewModel.onGetStarted()
            },
            onSkip = {
                viewModel.onSkipOnboarding()
            }
        )
    }
    
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}