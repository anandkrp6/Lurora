package com.bytecoder.lurora.storage.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

    companion object {
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE = booleanPreferencesKey("dark_theme")
        private val SHOW_SEEK_BUTTONS = booleanPreferencesKey("show_seek_buttons")
    }

    /**
     * Check if this is the first launch of the app
     */
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_LAUNCH] ?: true
    }

    /**
     * Check if onboarding has been completed
     */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Get current theme preference
     */
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: false
    }

    /**
     * Get seek buttons visibility preference
     */
    val showSeekButtons: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_SEEK_BUTTONS] ?: true
    }

    /**
     * Mark first launch as completed
     */
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }

    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
            preferences[IS_FIRST_LAUNCH] = false
        }
    }

    /**
     * Set theme preference
     */
    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = isDark
        }
    }

    /**
     * Set seek buttons visibility preference
     */
    suspend fun setShowSeekButtons(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_SEEK_BUTTONS] = show
        }
    }

    /**
     * Reset all preferences (for testing or app reset)
     */
    suspend fun resetPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}