package com.bytecoder.lurora.backend.models

import androidx.compose.runtime.Immutable

/**
 * Supported media formats and their properties
 */
@Immutable
data class MediaFormat(
    val extension: String,
    val mimeType: String,
    val mediaType: MediaType,
    val isStreamingSupported: Boolean = false,
    val hasHardwareSupport: Boolean = false,
    val quality: MediaQuality = MediaQuality.STANDARD
)

/**
 * Media quality levels
 */
enum class MediaQuality {
    LOW, STANDARD, HIGH, ULTRA_HIGH
}

/**
 * Streaming protocol types
 */
enum class StreamingProtocol(val protocol: String, val description: String) {
    HTTP("http://", "HTTP Direct Link"),
    HTTPS("https://", "HTTPS Direct Link"),
    HLS("m3u8", "HTTP Live Streaming"),
    DASH("mpd", "Dynamic Adaptive Streaming"),
    RTMP("rtmp://", "Real-Time Messaging Protocol")
}

/**
 * Supported file formats registry
 */
object SupportedFormats {
    
    // Video formats
    val VIDEO_FORMATS = listOf(
        MediaFormat("mp4", "video/mp4", MediaType.VIDEO, true, true, MediaQuality.HIGH),
        MediaFormat("mkv", "video/x-matroska", MediaType.VIDEO, false, true, MediaQuality.ULTRA_HIGH),
        MediaFormat("avi", "video/x-msvideo", MediaType.VIDEO, false, false, MediaQuality.STANDARD),
        MediaFormat("mov", "video/quicktime", MediaType.VIDEO, true, true, MediaQuality.HIGH),
        MediaFormat("wmv", "video/x-ms-wmv", MediaType.VIDEO, false, false, MediaQuality.STANDARD),
        MediaFormat("flv", "video/x-flv", MediaType.VIDEO, true, false, MediaQuality.LOW),
        MediaFormat("3gp", "video/3gpp", MediaType.VIDEO, false, true, MediaQuality.LOW),
        MediaFormat("webm", "video/webm", MediaType.VIDEO, true, true, MediaQuality.HIGH)
    )
    
    // Audio formats
    val AUDIO_FORMATS = listOf(
        MediaFormat("mp3", "audio/mpeg", MediaType.AUDIO, true, true, MediaQuality.STANDARD),
        MediaFormat("flac", "audio/flac", MediaType.AUDIO, false, false, MediaQuality.ULTRA_HIGH),
        MediaFormat("aac", "audio/aac", MediaType.AUDIO, true, true, MediaQuality.HIGH),
        MediaFormat("ogg", "audio/ogg", MediaType.AUDIO, true, false, MediaQuality.HIGH),
        MediaFormat("wav", "audio/wav", MediaType.AUDIO, false, false, MediaQuality.ULTRA_HIGH),
        MediaFormat("m4a", "audio/mp4", MediaType.AUDIO, true, true, MediaQuality.HIGH),
        MediaFormat("wma", "audio/x-ms-wma", MediaType.AUDIO, false, false, MediaQuality.STANDARD)
    )
    
    // Subtitle formats
    val SUBTITLE_FORMATS = listOf(
        MediaFormat("srt", "application/x-subrip", MediaType.VIDEO),
        MediaFormat("vtt", "text/vtt", MediaType.VIDEO),
        MediaFormat("ass", "text/x-ass", MediaType.VIDEO),
        MediaFormat("ssa", "text/x-ssa", MediaType.VIDEO)
    )
    
    // Image formats (for album art)
    val IMAGE_FORMATS = listOf(
        MediaFormat("jpg", "image/jpeg", MediaType.AUDIO),
        MediaFormat("jpeg", "image/jpeg", MediaType.AUDIO),
        MediaFormat("png", "image/png", MediaType.AUDIO),
        MediaFormat("webp", "image/webp", MediaType.AUDIO),
        MediaFormat("gif", "image/gif", MediaType.AUDIO)
    )
    
    val ALL_FORMATS = VIDEO_FORMATS + AUDIO_FORMATS + SUBTITLE_FORMATS + IMAGE_FORMATS
    
    /**
     * Check if a file extension is supported
     */
    fun isSupported(extension: String): Boolean {
        return ALL_FORMATS.any { it.extension.equals(extension, ignoreCase = true) }
    }
    
    /**
     * Get format information by extension
     */
    fun getFormat(extension: String): MediaFormat? {
        return ALL_FORMATS.find { it.extension.equals(extension, ignoreCase = true) }
    }
    
    /**
     * Get supported extensions for a media type
     */
    fun getSupportedExtensions(mediaType: MediaType): List<String> {
        return when (mediaType) {
            MediaType.VIDEO -> VIDEO_FORMATS.map { it.extension }
            MediaType.AUDIO -> AUDIO_FORMATS.map { it.extension }
        }
    }
    
    /**
     * Check if streaming is supported for this format
     */
    fun supportsStreaming(extension: String): Boolean {
        return getFormat(extension)?.isStreamingSupported ?: false
    }
    
    /**
     * Check if hardware acceleration is supported
     */
    fun hasHardwareSupport(extension: String): Boolean {
        return getFormat(extension)?.hasHardwareSupport ?: false
    }
    
    /**
     * Get MIME type for extension
     */
    fun getMimeType(extension: String): String? {
        return getFormat(extension)?.mimeType
    }
    
    /**
     * Detect streaming protocol from URL
     */
    fun detectStreamingProtocol(url: String): StreamingProtocol? {
        return when {
            url.startsWith("rtmp://") -> StreamingProtocol.RTMP
            url.contains(".m3u8") -> StreamingProtocol.HLS
            url.contains(".mpd") -> StreamingProtocol.DASH
            url.startsWith("https://") -> StreamingProtocol.HTTPS
            url.startsWith("http://") -> StreamingProtocol.HTTP
            else -> null
        }
    }
}