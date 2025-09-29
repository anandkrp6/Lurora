package com.bytecoder.lurora.backend.models

import androidx.compose.runtime.Immutable
import android.net.Uri
import java.util.Date

/**
 * History entry for media playback
 */
@Immutable
data class HistoryEntry(
    val id: String,
    val mediaItem: MediaItem,
    val lastPlayedAt: Date,
    val playbackProgress: Float = 0f, // 0.0 to 1.0
    val lastPosition: Long = 0L, // milliseconds
    val playCount: Int = 1,
    val source: HistorySource = HistorySource.LOCAL,
    val isPrivateSession: Boolean = false
)

/**
 * Source of the history entry
 */
enum class HistorySource {
    LOCAL,
    YOUTUBE,
    SPOTIFY,
    SOUNDCLOUD,
    OTHER_ONLINE
}

/**
 * History sorting options
 */
enum class HistorySortOption {
    RECENT_FIRST,
    OLDEST_FIRST,
    ALPHABETICAL,
    REVERSE_ALPHABETICAL,
    MOST_PLAYED
}

/**
 * History filter options
 */
data class HistoryFilter(
    val mediaTypes: Set<MediaType> = MediaType.values().toSet(),
    val sources: Set<HistorySource> = HistorySource.values().toSet(),
    val includePrivateSessions: Boolean = true,
    val dateRange: DateRange? = null
)

/**
 * Date range for filtering
 */
@Immutable
data class DateRange(
    val start: Date,
    val end: Date
)

/**
 * History settings
 */
@Immutable
data class HistorySettings(
    val retentionDays: Int = 30, // -1 for unlimited
    val trackPrivateSessions: Boolean = false,
    val autoCleanupBrokenLinks: Boolean = true,
    val enableHistory: Boolean = true
)