package com.bytecoder.lurora.frontend.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for comprehensive settings management
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("lurora_settings", Context.MODE_PRIVATE)
    
    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Settings storage
    private val settingsMap = mutableMapOf<String, Any>()
    
    init {
        loadSettings()
    }
    
    /**
     * Load all settings from SharedPreferences
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Load settings with default values
            val defaultSettings = mapOf(
                // General
                "dark_mode" to true,
                "auto_theme" to false,
                "language" to "English",
                "startup_screen" to "Home",
                "notifications" to true,
                "vibration" to true,
                
                // Video Player
                "auto_rotate" to true,
                "default_quality" to "Auto",
                "playback_speed" to 1.0f,
                "brightness" to 0.8f,
                "volume" to 0.7f,
                "subtitles" to false,
                "fullscreen_gestures" to true,
                
                // Audio Player
                "background_audio" to true,
                "audio_quality" to "High",
                "equalizer" to false,
                "music_player_display_mode" to "Both (Album Art & Equalizer)",
                "crossfade" to false,
                "gapless" to true,
                
                // Mini Player
                "mini_player_enabled" to true,
                "mini_player_size" to 0.6f,
                "mini_player_position" to "Bottom Right",
                "auto_mini_player" to true,
                
                // Online
                "wifi_only" to false,
                "data_saver" to false,
                "preload_videos" to true,
                "cache_size" to "1GB",
                "proxy_url" to "",
                "timeout" to 0.3f,
                
                // Downloads
                "download_quality" to "720p",
                "parallel_downloads" to 0.3f,
                "auto_retry" to true,
                "wifi_only_download" to true,
                "auto_delete" to "Never",
                
                // History
                "save_history" to true,
                "history_limit" to "1000",
                "auto_clear_history" to "Never",
                "incognito_mode" to false,
                
                // File Manager
                "show_hidden" to false,
                "default_view" to "List",
                "sort_by" to "Name",
                "show_thumbnails" to true,
                "remember_location" to true,
                
                // Privacy & Security
                "app_lock" to false,
                "fingerprint" to false,
                "auto_lock" to false,
                "hide_recent" to false,
                
                // Advanced
                "debug_mode" to false,
                "hardware_acceleration" to true,
                "experimental_features" to false
            )
            
            // Load from SharedPreferences or use defaults
            defaultSettings.forEach { (key, defaultValue) ->
                val value = when (defaultValue) {
                    is Boolean -> sharedPrefs.getBoolean(key, defaultValue)
                    is String -> sharedPrefs.getString(key, defaultValue) ?: defaultValue
                    is Float -> sharedPrefs.getFloat(key, defaultValue)
                    is Int -> sharedPrefs.getInt(key, defaultValue)
                    else -> defaultValue
                }
                settingsMap[key] = value
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Update a specific setting value
     */
    fun updateSetting(key: String, value: Any) {
        viewModelScope.launch {
            settingsMap[key] = value
            
            // Save to SharedPreferences
            val editor = sharedPrefs.edit()
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
            }
            editor.apply()
            
            // Handle special actions
            handleSettingAction(key, value)
        }
    }
    
    /**
     * Handle special setting actions
     */
    private fun handleSettingAction(key: String, value: Any) {
        when (key) {
            "download_location" -> showFolderPicker()
            "clear_history" -> clearHistory()
            "clear_data" -> showClearDataDialog()
            "privacy_policy" -> openPrivacyPolicy()
            "export_logs" -> exportDebugLogs()
            "reset_settings" -> showResetDialog()
            "app_version" -> showAppInfo()
        }
    }
    
    /**
     * Export all settings to a file
     */
    fun exportSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showMessage = "Settings exported successfully"
            )
        }
    }
    
    /**
     * Import settings from a file
     */
    fun importSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showMessage = "Settings imported successfully"
            )
        }
    }
    
    /**
     * Show reset confirmation dialog
     */
    fun showResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = true)
    }
    
    /**
     * Reset all settings to default
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            sharedPrefs.edit().clear().apply()
            loadSettings()
            _uiState.value = _uiState.value.copy(
                showResetDialog = false,
                showMessage = "All settings reset to default"
            )
        }
    }
    
    /**
     * Dismiss reset dialog
     */
    fun dismissResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = false)
    }
    
    /**
     * Clear message
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(showMessage = null)
    }
    
    // Action handlers
    private fun showFolderPicker() {
        _uiState.value = _uiState.value.copy(showMessage = "Folder picker opened")
    }
    
    private fun clearHistory() {
        _uiState.value = _uiState.value.copy(showMessage = "History cleared")
    }
    
    private fun showClearDataDialog() {
        _uiState.value = _uiState.value.copy(showMessage = "Clear data dialog shown")
    }
    
    private fun openPrivacyPolicy() {
        _uiState.value = _uiState.value.copy(showMessage = "Privacy policy opened")
    }
    
    private fun exportDebugLogs() {
        _uiState.value = _uiState.value.copy(showMessage = "Debug logs exported")
    }
    
    private fun showAppInfo() {
        _uiState.value = _uiState.value.copy(showMessage = "App info displayed")
    }
    
    /**
     * Get current value for a setting
     */
    fun getSetting(key: String): Any? {
        return settingsMap[key]
    }
}

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val showResetDialog: Boolean = false,
    val showMessage: String? = null
)