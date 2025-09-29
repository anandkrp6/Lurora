package com.bytecoder.lurora.backend.models

import androidx.compose.runtime.Immutable
import android.net.Uri
import java.util.Date

/**
 * Download item representing a file download
 */
@Immutable
data class DownloadItem(
    val id: String,
    val fileName: String,
    val originalTitle: String,
    val fileType: String, // Extension like "mp4", "mp3", etc.
    val sourceUrl: String,
    val sourcePlatform: String = "Unknown",
    val downloadPath: String,
    val status: DownloadStatus,
    val fileSize: Long = 0L, // Total size in bytes
    val downloadedSize: Long = 0L, // Downloaded size in bytes
    val downloadSpeed: Long = 0L, // bytes per second
    val eta: Long = 0L, // seconds remaining
    val downloadStarted: Date,
    val downloadCompleted: Date? = null,
    val error: String? = null,
    val thumbnail: Uri? = null
)

/**
 * Download status types
 */
enum class DownloadStatus {
    QUEUED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Download sorting options
 */
enum class DownloadSortOption {
    NEWEST_FIRST,
    OLDEST_FIRST,
    NAME_AZ,
    NAME_ZA,
    SIZE_LARGE_FIRST,
    SIZE_SMALL_FIRST,
    STATUS
}

/**
 * Download filter options
 */
data class DownloadFilter(
    val statuses: Set<DownloadStatus> = DownloadStatus.values().toSet(),
    val fileTypes: Set<String> = emptySet(), // Empty means all types
    val platforms: Set<String> = emptySet() // Empty means all platforms
)

/**
 * Download settings
 */
@Immutable
data class DownloadSettings(
    val downloadLocation: String = "/storage/emulated/0/Download/Lurora",
    val simultaneousDownloads: Int = 3,
    val pauseOnMeteredConnection: Boolean = true,
    val autoRetryFailedDownloads: Boolean = true,
    val maxRetryAttempts: Int = 3,
    val cleanupFailedAfterDays: Int = 7,
    val showProgressNotifications: Boolean = true,
    val showCompletionNotifications: Boolean = true
)

/**
 * Storage information
 */
@Immutable
data class StorageInfo(
    val totalSpace: Long,
    val availableSpace: Long,
    val usedSpace: Long
) {
    val usagePercentage: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace) * 100f else 0f
        
    val isLowStorage: Boolean
        get() = availableSpace < (1024L * 1024L * 1024L) // Less than 1GB
}