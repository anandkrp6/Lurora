package com.bytecoder.lurora.backend.models

import android.os.Build
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
    val explanation: String, // Why this permission is needed
    val category: PermissionCategory,
    val requiresSpecialHandling: Boolean = false, // For permissions that need special handling like All Files Access
    val minApiLevel: Int = 23, // Minimum API level where this permission is applicable
    val maxApiLevel: Int = Int.MAX_VALUE, // Maximum API level where this permission is applicable
    val alternativePermissions: List<String> = emptyList() // Alternative permissions that are granted together
)

/**
 * Permission status
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    LIMITED, // For partial access (e.g., selected photos only)
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
 * Permission categories for grouping
 */
enum class PermissionCategory(val displayName: String) {
    STORAGE("Storage & Files"),
    MEDIA("Media Access"),
    NETWORK("Network"),
    AUDIO("Audio & Media Playback"),
    BLUETOOTH("Bluetooth"),
    NOTIFICATIONS("Notifications"),
    PHONE("Phone State"),
    SYSTEM("System & Overlay")
}

/**
 * Predefined app permissions
 */
object AppPermissions {
    val READ_EXTERNAL_STORAGE = AppPermission(
        id = "read_external_storage",
        name = "Access files and media",
        description = "Browse and play your music, videos and files",
        importance = PermissionImportance.CRITICAL,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE,
        explanation = "Required to access and play media files from your device storage",
        category = PermissionCategory.STORAGE,
        minApiLevel = 23,
        maxApiLevel = 32 // Deprecated in API 33+
    )
    
    val WRITE_EXTERNAL_STORAGE = AppPermission(
        id = "write_external_storage",
        name = "Modify files and media",
        description = "Download and save media files to your device",
        importance = PermissionImportance.HIGH,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        explanation = "Required to save downloaded files and export media",
        category = PermissionCategory.STORAGE,
        minApiLevel = 23,
        maxApiLevel = 32 // Limited functionality in API 29+, deprecated in API 33+
    )
    
    val MANAGE_EXTERNAL_STORAGE = AppPermission(
        id = "manage_external_storage",
        name = "All files access",
        description = "Access all files and folders on your device",
        importance = PermissionImportance.HIGH,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        explanation = "Enables the built-in file manager and complete media library access",
        category = PermissionCategory.STORAGE,
        requiresSpecialHandling = true,
        minApiLevel = 30 // Available from API 30+
    )
    
    val READ_MEDIA_AUDIO = AppPermission(
        id = "read_media_audio",
        name = "Music and audio",
        description = "Access music and audio files",
        importance = PermissionImportance.CRITICAL,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.READ_MEDIA_AUDIO,
        explanation = "Required to access and play audio files from your device",
        category = PermissionCategory.MEDIA,
        minApiLevel = 33 // Available from API 33+
    )
    
    // Combined Photos and Videos permission (since they're granted together)
    val READ_MEDIA_VISUAL = AppPermission(
        id = "read_media_visual",
        name = "Photos and videos",
        description = "Access photos, videos and images",
        importance = PermissionImportance.CRITICAL,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.READ_MEDIA_VIDEO,
        explanation = "Required to access video files and display album art thumbnails",
        category = PermissionCategory.MEDIA,
        minApiLevel = 33, // Available from API 33+
        alternativePermissions = listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    )
    
    val BLUETOOTH_CONNECT = AppPermission(
        id = "bluetooth_connect",
        name = "Nearby devices",
        description = "Connect to paired Bluetooth devices",
        importance = PermissionImportance.MEDIUM,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.BLUETOOTH_CONNECT,
        explanation = "Required for connecting to Bluetooth audio devices",
        category = PermissionCategory.BLUETOOTH,
        minApiLevel = 31 // Available from API 31+
    )
    
    val POST_NOTIFICATIONS = AppPermission(
        id = "post_notifications",
        name = "Notifications",
        description = "Show notifications for playback controls and updates",
        importance = PermissionImportance.HIGH,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.POST_NOTIFICATIONS,
        explanation = "Shows media controls and download progress in notification panel",
        category = PermissionCategory.NOTIFICATIONS,
        minApiLevel = 33 // Required from API 33+
    )
    
    val READ_PHONE_STATE = AppPermission(
        id = "read_phone_state",
        name = "Phone",
        description = "Pause playback during phone calls",
        importance = PermissionImportance.MEDIUM,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.READ_PHONE_STATE,
        explanation = "Automatically pauses media during phone calls",
        category = PermissionCategory.PHONE,
        minApiLevel = 23 // Available from API 23+
    )
    
    val SYSTEM_ALERT_WINDOW = AppPermission(
        id = "system_alert_window",
        name = "Display over other apps",
        description = "Show floating controls over other apps",
        importance = PermissionImportance.LOW,
        status = PermissionStatus.NOT_REQUESTED,
        androidPermission = android.Manifest.permission.SYSTEM_ALERT_WINDOW,
        explanation = "Enables floating media controls that appear over other apps",
        category = PermissionCategory.SYSTEM,
        isRequired = false,
        requiresSpecialHandling = true,
        minApiLevel = 23 // Available from API 23+
    )
    
    fun getAllPermissions() = listOf(
        POST_NOTIFICATIONS,        // 1. Notifications (on top)
        READ_MEDIA_AUDIO,         // 2. Music and audio
        READ_MEDIA_VISUAL,        // 3. Photos and videos (combined)
        READ_EXTERNAL_STORAGE,    // 4. Access files and media
        WRITE_EXTERNAL_STORAGE,   // 5. Modify files and media
        MANAGE_EXTERNAL_STORAGE,  // 6. All files access (will be filtered by version)
        BLUETOOTH_CONNECT,        // 7. Nearby devices
        READ_PHONE_STATE,         // 8. Phone
        SYSTEM_ALERT_WINDOW       // 9. Display over other apps (last)
    )
    
    /**
     * Get permissions applicable for the current device's Android version
     */
    fun getApplicablePermissions(): List<AppPermission> {
        val currentApiLevel = Build.VERSION.SDK_INT
        return getAllPermissions().filter { permission ->
            currentApiLevel >= permission.minApiLevel && currentApiLevel <= permission.maxApiLevel
        }
    }
    
    /**
     * Get permissions for a specific API level (useful for testing)
     */
    fun getPermissionsForApiLevel(apiLevel: Int): List<AppPermission> {
        return getAllPermissions().filter { permission ->
            apiLevel >= permission.minApiLevel && apiLevel <= permission.maxApiLevel
        }
    }
    
    /**
     * Get permissions grouped by category
     */
    fun getPermissionsByCategory(): Map<PermissionCategory, List<AppPermission>> {
        return getAllPermissions().groupBy { it.category }
    }
    
    /**
     * Get permission by Android permission string
     */
    fun getPermissionByAndroidPermission(androidPermission: String): AppPermission? {
        return getAllPermissions().find { it.androidPermission == androidPermission }
    }
    
    /**
     * Get required permissions only
     */
    fun getRequiredPermissions(): List<AppPermission> {
        return getAllPermissions().filter { it.isRequired }
    }
}