package com.bytecoder.lurora.backend.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling and logging utility
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    private val _errors = MutableStateFlow<List<AppError>>(emptyList())
    val errors: StateFlow<List<AppError>> = _errors.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Handle and log an error
     */
    fun handleError(error: Throwable, context: String = "Unknown", showToUser: Boolean = true) {
        val appError = AppError(
            message = error.message ?: "Unknown error occurred",
            context = context,
            throwable = error,
            timestamp = System.currentTimeMillis(),
            showToUser = showToUser
        )
        
        // Log the error
        Log.e("LuroraError", "[$context] ${error.message}", error)
        
        // Add to error list if it should be shown to user
        if (showToUser) {
            val currentErrors = _errors.value.toMutableList()
            currentErrors.add(appError)
            // Keep only last 10 errors
            if (currentErrors.size > 10) {
                currentErrors.removeAt(0)
            }
            _errors.value = currentErrors
        }
    }
    
    /**
     * Handle a string error message
     */
    fun handleError(message: String, context: String = "Unknown", showToUser: Boolean = true) {
        handleError(Exception(message), context, showToUser)
    }
    
    /**
     * Clear a specific error
     */
    fun clearError(error: AppError) {
        _errors.value = _errors.value.filter { it != error }
    }
    
    /**
     * Clear all errors
     */
    fun clearAllErrors() {
        _errors.value = emptyList()
    }
    
    /**
     * Set loading state
     */
    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
    
    /**
     * Execute a suspending function with error handling
     */
    suspend fun <T> safeCall(
        context: String,
        showToUser: Boolean = true,
        action: suspend () -> T
    ): Result<T> {
        return try {
            setLoading(true)
            val result = action()
            Result.success(result)
        } catch (e: Exception) {
            handleError(e, context, showToUser)
            Result.failure(e)
        } finally {
            setLoading(false)
        }
    }
    
    /**
     * Execute a function with error handling (non-suspending)
     */
    fun <T> safeTry(
        context: String,
        showToUser: Boolean = true,
        action: () -> T
    ): Result<T> {
        return try {
            val result = action()
            Result.success(result)
        } catch (e: Exception) {
            handleError(e, context, showToUser)
            Result.failure(e)
        }
    }
}

/**
 * Application error data class
 */
data class AppError(
    val message: String,
    val context: String,
    val throwable: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val showToUser: Boolean = true,
    val id: String = "${context}_${timestamp}"
)

/**
 * Common error types for the application
 */
sealed class LuroraException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(message: String = "Network connection error", cause: Throwable? = null) : LuroraException(message, cause)
    class FileAccessError(message: String = "File access error", cause: Throwable? = null) : LuroraException(message, cause)
    class PermissionError(message: String = "Permission denied", cause: Throwable? = null) : LuroraException(message, cause)
    class MediaPlaybackError(message: String = "Media playback error", cause: Throwable? = null) : LuroraException(message, cause)
    class DatabaseError(message: String = "Database error", cause: Throwable? = null) : LuroraException(message, cause)
    class DownloadError(message: String = "Download error", cause: Throwable? = null) : LuroraException(message, cause)
    class ValidationError(message: String = "Validation error", cause: Throwable? = null) : LuroraException(message, cause)
}