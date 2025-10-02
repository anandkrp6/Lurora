package com.bytecoder.lurora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        
        setContent {
            LuroraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
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

                null -> TODO()
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