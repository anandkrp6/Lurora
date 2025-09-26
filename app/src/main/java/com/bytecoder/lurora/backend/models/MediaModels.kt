package com.bytecoder.lurora.backend.models

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Core media item representing any playable content
 */
@Immutable
data class MediaItem(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val duration: Long = 0L,
    val mediaType: MediaType,
    val albumArtUri: Uri? = null,
    val subtitleUri: Uri? = null,
    val mimeType: String? = null,
    val size: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPosition: Long = 0L,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Type of media content
 */
enum class MediaType {
    VIDEO, AUDIO
}

/**
 * Playback state information
 */
@Immutable
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val currentMediaItemIndex: Int = 0,
    val totalItems: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Repeat modes for playback
 */
enum class RepeatMode {
    OFF, ONE, ALL
}

/**
 * Media queue for managing playback order
 */
@Immutable
data class MediaQueue(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val shuffledIndices: List<Int>? = null,
    val originalQueue: List<MediaItem>? = null
) {
    val currentItem: MediaItem?
        get() = items.getOrNull(currentIndex)
    
    val hasNext: Boolean
        get() = currentIndex < items.lastIndex
    
    val hasPrevious: Boolean
        get() = currentIndex > 0
    
    val size: Int
        get() = items.size
    
    fun isEmpty(): Boolean = items.isEmpty()
    
    fun isNotEmpty(): Boolean = items.isNotEmpty()
}

/**
 * Playlist model for organizing media collections
 */
@Immutable
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val coverArtUri: Uri? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isSystemPlaylist: Boolean = false,
    val playCount: Int = 0,
    val totalDuration: Long = 0L
) {
    val itemCount: Int
        get() = mediaItems.size
    
    val isEmpty: Boolean
        get() = mediaItems.isEmpty()
}

/**
 * Audio effect configuration
 */
data class AudioEffect(
    val type: Type,
    val enabled: Boolean = false,
    val strength: Float = 0.5f,
    val parameters: Map<String, Float> = emptyMap()
) {
    enum class Type {
        BASS_BOOST,
        VIRTUALIZER, 
        REVERB,
        ECHO,
        PITCH_SHIFT,
        SPEED_CHANGE,
        NOISE_REDUCTION,
        VOICE_ENHANCEMENT
    }
    
    val hasStrengthControl: Boolean
        get() = when (type) {
            Type.BASS_BOOST, Type.VIRTUALIZER, Type.REVERB, 
            Type.ECHO, Type.NOISE_REDUCTION, Type.VOICE_ENHANCEMENT -> true
            Type.PITCH_SHIFT, Type.SPEED_CHANGE -> false
        }
}

/**
 * Equalizer band configuration
 */
data class EqualizerBand(
    val frequency: Float,
    val gain: Float = 0f,
    val bandIndex: Int
)

/**
 * Visualizer data for frequency spectrum display
 */
data class VisualizerData(
    val frequencies: FloatArray,
    val waveform: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as VisualizerData
        
        if (!frequencies.contentEquals(other.frequencies)) return false
        if (!waveform.contentEquals(other.waveform)) return false
        if (timestamp != other.timestamp) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = frequencies.contentHashCode()
        result = 31 * result + waveform.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Lyrics line with timing information
 */
data class LyricLine(
    val text: String,
    val startTime: Long,
    val endTime: Long = startTime + 3000L // Default 3 second duration
)

/**
 * Video-specific models
 */
data class VideoTrack(
    val id: String,
    val title: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

data class AudioTrack(
    val id: String,
    val title: String,
    val language: String? = null,
    val channels: Int = 2,
    val isSelected: Boolean = false
)

data class SubtitleTrack(
    val id: String,
    val title: String,
    val language: String? = null,
    val uri: Uri? = null,
    val isSelected: Boolean = false
)

data class Chapter(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val thumbnailUri: Uri? = null
)