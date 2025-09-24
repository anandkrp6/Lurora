package com.bytecoder.lurora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.frontend.ui.screens.SplashScreen
import com.bytecoder.lurora.frontend.ui.theme.LuroraTheme
import com.bytecoder.lurora.storage.preferences.AppPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    @Inject
    lateinit var appPreferencesManager: AppPreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LuroraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
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
                // First launch - show onboarding
                Intent(this, OnboardingActivity::class.java)
            }
            else -> {
                // Not first launch or onboarding completed - go to main
                Intent(this, MainActivity::class.java)
            }
        }
        
        startActivity(intent)
        finish()
    }
}