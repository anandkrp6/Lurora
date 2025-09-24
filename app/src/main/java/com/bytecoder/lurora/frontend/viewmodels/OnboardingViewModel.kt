package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.utils.PermissionManager
import com.bytecoder.lurora.storage.preferences.AppPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferencesManager: AppPreferencesManager,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<OnboardingNavigationEvent?>(null)
    val navigationEvent: StateFlow<OnboardingNavigationEvent?> = _navigationEvent.asStateFlow()

    fun onGetStarted() {
        viewModelScope.launch {
            // Mark onboarding as completed
            appPreferencesManager.setOnboardingCompleted()
            
            // Navigate to main activity
            _navigationEvent.value = OnboardingNavigationEvent.NavigateToMain
        }
    }

    fun onSkipOnboarding() {
        viewModelScope.launch {
            // Mark onboarding as completed even if skipped
            appPreferencesManager.setOnboardingCompleted()
            
            // Navigate to main activity
            _navigationEvent.value = OnboardingNavigationEvent.NavigateToMain
        }
    }

    fun onThemeSelected(isDarkTheme: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setDarkTheme(isDarkTheme)
            _uiState.value = _uiState.value.copy(selectedTheme = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT)
        }
    }

    fun onNavigationEventHandled() {
        _navigationEvent.value = null
    }

    fun setCurrentPage(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }
}

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedTheme: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false
)

sealed class OnboardingNavigationEvent {
    object NavigateToMain : OnboardingNavigationEvent()
    object NavigateToPermissions : OnboardingNavigationEvent()
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}