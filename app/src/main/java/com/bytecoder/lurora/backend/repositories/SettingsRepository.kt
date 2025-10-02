package com.bytecoder.lurora.backend.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.bytecoder.lurora.backend.models.DownloadSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    private object PreferencesKeys {
        val DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
        val SIMULTANEOUS_DOWNLOADS = intPreferencesKey("simultaneous_downloads")
        val PAUSE_ON_METERED_CONNECTION = booleanPreferencesKey("pause_on_metered_connection")
        val AUTO_RETRY_FAILED_DOWNLOADS = booleanPreferencesKey("auto_retry_failed_downloads")
        val MAX_RETRY_ATTEMPTS = intPreferencesKey("max_retry_attempts")
        val CLEANUP_FAILED_AFTER_DAYS = intPreferencesKey("cleanup_failed_after_days")
        val SHOW_PROGRESS_NOTIFICATIONS = booleanPreferencesKey("show_progress_notifications")
        val SHOW_COMPLETION_NOTIFICATIONS = booleanPreferencesKey("show_completion_notifications")
    }
    
    val downloadSettings: Flow<DownloadSettings> = dataStore.data.map { preferences ->
        DownloadSettings(
            downloadLocation = preferences[PreferencesKeys.DOWNLOAD_LOCATION] ?: "/storage/emulated/0/Download/Lurora",
            simultaneousDownloads = preferences[PreferencesKeys.SIMULTANEOUS_DOWNLOADS] ?: 3,
            pauseOnMeteredConnection = preferences[PreferencesKeys.PAUSE_ON_METERED_CONNECTION] ?: true,
            autoRetryFailedDownloads = preferences[PreferencesKeys.AUTO_RETRY_FAILED_DOWNLOADS] ?: true,
            maxRetryAttempts = preferences[PreferencesKeys.MAX_RETRY_ATTEMPTS] ?: 3,
            cleanupFailedAfterDays = preferences[PreferencesKeys.CLEANUP_FAILED_AFTER_DAYS] ?: 7,
            showProgressNotifications = preferences[PreferencesKeys.SHOW_PROGRESS_NOTIFICATIONS] ?: true,
            showCompletionNotifications = preferences[PreferencesKeys.SHOW_COMPLETION_NOTIFICATIONS] ?: true
        )
    }
    
    suspend fun saveDownloadSettings(settings: DownloadSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_LOCATION] = settings.downloadLocation
            preferences[PreferencesKeys.SIMULTANEOUS_DOWNLOADS] = settings.simultaneousDownloads
            preferences[PreferencesKeys.PAUSE_ON_METERED_CONNECTION] = settings.pauseOnMeteredConnection
            preferences[PreferencesKeys.AUTO_RETRY_FAILED_DOWNLOADS] = settings.autoRetryFailedDownloads
            preferences[PreferencesKeys.MAX_RETRY_ATTEMPTS] = settings.maxRetryAttempts
            preferences[PreferencesKeys.CLEANUP_FAILED_AFTER_DAYS] = settings.cleanupFailedAfterDays
            preferences[PreferencesKeys.SHOW_PROGRESS_NOTIFICATIONS] = settings.showProgressNotifications
            preferences[PreferencesKeys.SHOW_COMPLETION_NOTIFICATIONS] = settings.showCompletionNotifications
        }
    }
}