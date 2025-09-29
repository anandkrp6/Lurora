package com.bytecoder.lurora.backend.models

import androidx.compose.runtime.Immutable

/**
 * Permission information
 */
@Immutable
data class AppPermission(
    val id: String,
    val name: String,
    val description: String,
    val importance: PermissionImportance,
    val status: PermissionStatus,
    val androidPermission: String, // Android manifest permission string
    val isRequired: Boolean = true,
    val explanation: String // Why this permission is needed
)

/**
 * Permission status
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    LIMITED, // For permissions that are partially granted
    NOT_REQUESTED
}

/**
 * Permission importance level
 */
enum class PermissionImportance {
    CRITICAL, // App won't function without this
    HIGH, // Major features require this
    MEDIUM, // Some features require this
    LOW // Optional features only
}

/**
 * Predefined app permissions
 */
object AppPermissions {
    val VIDEO_ACCESS = AppPermission(
        id = "video_access",
        name = "Video & Picture Access",
        description = "Read video files and generate thumbnails",
        importance = PermissionImportance.CRITICAL,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.READ_EXTERNAL_STORAGE",
        explanation = "Required to access and play video files from your device storage"
    )
    
    val AUDIO_ACCESS = AppPermission(
        id = "audio_access",
        name = "Audio Access",
        description = "Read audio files and metadata",
        importance = PermissionImportance.CRITICAL,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.READ_EXTERNAL_STORAGE",
        explanation = "Required to access and play audio files from your device storage"
    )
    
    val ALL_FILES_ACCESS = AppPermission(
        id = "all_files_access",
        name = "All Files Access",
        description = "Complete file system access for file manager",
        importance = PermissionImportance.HIGH,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.MANAGE_EXTERNAL_STORAGE",
        explanation = "Enables the built-in file manager and complete media library access"
    )
    
    val INTERNET_ACCESS = AppPermission(
        id = "internet_access",
        name = "Internet Access",
        description = "Stream online content and download files",
        importance = PermissionImportance.HIGH,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.INTERNET",
        explanation = "Required for streaming online media and downloading content"
    )
    
    val NOTIFICATIONS = AppPermission(
        id = "notifications",
        name = "Notifications",
        description = "Background playback controls and download progress",
        importance = PermissionImportance.MEDIUM,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.POST_NOTIFICATIONS",
        explanation = "Shows media controls and download progress in notification panel"
    )
    
    val WRITE_STORAGE = AppPermission(
        id = "write_storage",
        name = "Storage Write Access",
        description = "Save downloads and export files",
        importance = PermissionImportance.HIGH,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.WRITE_EXTERNAL_STORAGE",
        explanation = "Required to save downloaded files and export media"
    )
    
    val WAKE_LOCK = AppPermission(
        id = "wake_lock",
        name = "Keep Screen On",
        description = "Prevent screen from turning off during playback",
        importance = PermissionImportance.LOW,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = "android.permission.WAKE_LOCK",
        explanation = "Keeps the screen on during video playback for better user experience"
    )
    
    fun getAllPermissions() = listOf(
        VIDEO_ACCESS,
        AUDIO_ACCESS,
        ALL_FILES_ACCESS,
        INTERNET_ACCESS,
        NOTIFICATIONS,
        WRITE_STORAGE,
        WAKE_LOCK
    )
}