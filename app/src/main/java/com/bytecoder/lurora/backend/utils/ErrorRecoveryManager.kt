package com.bytecoder.lurora.backend.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bytecoder.lurora.backend.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Error recovery and crash management system
 */
@Singleton
class ErrorRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("error_recovery", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    private val _lastError = MutableStateFlow<RecoveryError?>(null)
    val lastError: StateFlow<RecoveryError?> = _lastError.asStateFlow()

    /**
     * Global exception handler for coroutines
     */
    val globalExceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleException(exception)
    }

    init {
        checkForCrashRecovery()
    }

    /**
     * Save current app state for crash recovery
     */
    fun saveAppState(state: AppState) {
        try {
            _appState.value = state
            val stateJson = json.encodeToString(state)
            prefs.edit().putString("last_app_state", stateJson).apply()
        } catch (e: Exception) {
            Log.e("ErrorRecovery", "Failed to save app state", e)
        }
    }

    /**
     * Load saved app state after crash
     */
    fun loadSavedAppState(): AppState? {
        return try {
            val stateJson = prefs.getString("last_app_state", null)
            stateJson?.let { json.decodeFromString<AppState>(it) }
        } catch (e: Exception) {
            Log.e("ErrorRecovery", "Failed to load saved app state", e)
            null
        }
    }

    /**
     * Check if app crashed and handle recovery
     */
    private fun checkForCrashRecovery() {
        val wasCrashed = prefs.getBoolean("app_crashed", false)
        if (wasCrashed) {
            // App crashed, attempt recovery
            Log.i("ErrorRecovery", "Detected app crash, attempting recovery...")
            val savedState = loadSavedAppState()
            savedState?.let {
                _appState.value = it
                recordError(RecoveryError.CRASH_RECOVERY("App recovered from crash"))
            }
            // Clear crash flag
            prefs.edit().putBoolean("app_crashed", false).apply()
        }
    }

    /**
     * Mark app as running (call this in onResume)
     */
    fun markAppRunning() {
        prefs.edit().putBoolean("app_crashed", true).apply()
    }

    /**
     * Mark app as properly closed (call this in onPause/onDestroy)
     */
    fun markAppClosed() {
        prefs.edit().putBoolean("app_crashed", false).apply()
    }

    /**
     * Handle exceptions and provide recovery options
     */
    fun handleException(exception: Throwable) {
        Log.e("ErrorRecovery", "Handling exception", exception)
        
        val error = when (exception) {
            is SecurityException -> RecoveryError.PERMISSION_DENIED(exception.message ?: "Permission denied")
            is java.io.FileNotFoundException -> RecoveryError.FILE_NOT_FOUND(exception.message ?: "File not found")
            is java.net.UnknownHostException -> RecoveryError.NETWORK_ERROR("No internet connection")
            is java.net.SocketTimeoutException -> RecoveryError.NETWORK_TIMEOUT("Network timeout")
            is OutOfMemoryError -> RecoveryError.MEMORY_ERROR("Out of memory")
            else -> RecoveryError.UNKNOWN_ERROR(exception.message ?: "Unknown error occurred")
        }
        
        recordError(error)
    }

    /**
     * Record error for user notification
     */
    private fun recordError(error: RecoveryError) {
        _lastError.value = error
        Log.w("ErrorRecovery", "Error recorded: ${error.message}")
    }

    /**
     * Clear last error
     */
    fun clearLastError() {
        _lastError.value = null
    }

    /**
     * Update network state
     */
    fun updateNetworkState(isConnected: Boolean) {
        _networkState.value = if (isConnected) NetworkState.CONNECTED else NetworkState.DISCONNECTED
    }

    /**
     * Clean up corrupted cache files
     */
    fun cleanupCorruptedFiles() {
        try {
            val cacheDir = File(context.cacheDir, "media_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.length() == 0L || !file.canRead()) {
                        file.delete()
                        Log.i("ErrorRecovery", "Deleted corrupted cache file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ErrorRecovery", "Failed to cleanup corrupted files", e)
        }
    }

    /**
     * Handle low memory conditions
     */
    fun handleLowMemory() {
        Log.w("ErrorRecovery", "Low memory detected, clearing caches")
        cleanupCorruptedFiles()
        System.gc() // Suggest garbage collection
        recordError(RecoveryError.MEMORY_WARNING("Low memory detected, caches cleared"))
    }

    /**
     * Retry operation with exponential backoff
     */
    suspend fun <T> retryOperation(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        var delay = initialDelayMs
        
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(operation())
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(delay)
                    delay *= 2 // Exponential backoff
                }
            }
        }
        
        lastException?.let { handleException(it) }
        return Result.failure(lastException ?: Exception("Operation failed after $maxRetries retries"))
    }
}

/**
 * App error types
 */
sealed class RecoveryError(val message: String) {
    data class PERMISSION_DENIED(val details: String) : RecoveryError("Permission denied: $details")
    data class FILE_NOT_FOUND(val details: String) : RecoveryError("File not found: $details")
    data class NETWORK_ERROR(val details: String) : RecoveryError("Network error: $details")
    data class NETWORK_TIMEOUT(val details: String) : RecoveryError("Network timeout: $details")
    data class MEMORY_ERROR(val details: String) : RecoveryError("Memory error: $details")
    data class MEMORY_WARNING(val details: String) : RecoveryError("Memory warning: $details")
    data class CRASH_RECOVERY(val details: String) : RecoveryError("Crash recovery: $details")
    data class UNKNOWN_ERROR(val details: String) : RecoveryError("Unknown error: $details")
}

/**
 * Network state
 */
enum class NetworkState {
    UNKNOWN, CONNECTED, DISCONNECTED
}

/**
 * App state for crash recovery
 */
@kotlinx.serialization.Serializable
data class AppState(
    val currentTab: String = "VIDEO",
    val currentVideoSection: String = "LIBRARY",
    val currentMusicSection: String = "LIBRARY",
    val lastPlayedMediaId: String? = null,
    val lastPlaybackPosition: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)