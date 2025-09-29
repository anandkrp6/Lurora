package com.bytecoder.lurora.backend.models

import androidx.compose.runtime.Immutable
import android.net.Uri
import java.util.Date

/**
 * File system item (file or directory)
 */
@Immutable
data class FileSystemItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Date,
    val isHidden: Boolean = false,
    val permissions: FilePermissions = FilePermissions(),
    val thumbnail: Uri? = null,
    val mediaInfo: FileMediaInfo? = null
)

/**
 * File permissions
 */
@Immutable
data class FilePermissions(
    val canRead: Boolean = true,
    val canWrite: Boolean = false,
    val canExecute: Boolean = false
)

/**
 * Media file information
 */
@Immutable
data class FileMediaInfo(
    val duration: Long = 0L, // milliseconds
    val resolution: String? = null, // e.g., "1920x1080"
    val codec: String? = null,
    val bitrate: Long = 0L, // bits per second
    val frameRate: Float = 0f,
    val mediaType: MediaType
)

/**
 * File view options
 */
enum class FileViewType {
    LIST,
    GRID
}

/**
 * File sorting options
 */
enum class FileSortOption {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DATE_ASC,
    DATE_DESC,
    TYPE_ASC,
    TYPE_DESC
}

/**
 * File filter options
 */
data class FileFilter(
    val showHidden: Boolean = false,
    val fileTypes: Set<String> = emptySet(), // Empty means all types
    val mediaTypes: Set<MediaType> = emptySet(), // Empty means all media types
    val sizeRange: SizeRange? = null,
    val dateRange: DateRange? = null
)

/**
 * File size range
 */
@Immutable
data class SizeRange(
    val minSize: Long, // bytes
    val maxSize: Long  // bytes
)

/**
 * File operation types
 */
enum class FileOperation {
    COPY,
    MOVE,
    DELETE,
    RENAME,
    CREATE_FOLDER,
    EXTRACT,
    COMPRESS,
    SHARE
}

/**
 * File operation result
 */
@Immutable
data class FileOperationResult(
    val operation: FileOperation,
    val success: Boolean,
    val message: String,
    val affectedFiles: List<String> = emptyList()
)

/**
 * Navigation breadcrumb
 */
@Immutable
data class BreadcrumbItem(
    val path: String,
    val name: String,
    val isRoot: Boolean = false
)

/**
 * Bookmark for frequently accessed folders
 */
@Immutable
data class FileBookmark(
    val id: String,
    val name: String,
    val path: String,
    val icon: String? = null,
    val createdAt: Date = Date()
)

/**
 * File search result
 */
@Immutable
data class FileSearchResult(
    val items: List<FileSystemItem>,
    val query: String,
    val totalFound: Int,
    val searchTime: Long // milliseconds
)

/**
 * File explorer settings
 */
@Immutable
data class FileExplorerSettings(
    val defaultView: FileViewType = FileViewType.LIST,
    val showHiddenFiles: Boolean = false,
    val defaultSortOption: FileSortOption = FileSortOption.NAME_ASC,
    val generateThumbnails: Boolean = true,
    val thumbnailQuality: ThumbnailQuality = ThumbnailQuality.MEDIUM,
    val showFileExtensions: Boolean = true,
    val confirmBeforeDelete: Boolean = true,
    val enableFilePreview: Boolean = true
)

/**
 * Thumbnail quality levels
 */
enum class ThumbnailQuality {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Common file type categories
 */
object FileTypes {
    val VIDEO_EXTENSIONS = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico")
    val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "txt", "rtf", "odt")
    val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2")
    
    fun getFileTypeCategory(extension: String): String {
        return when (extension.lowercase()) {
            in VIDEO_EXTENSIONS -> "Video"
            in AUDIO_EXTENSIONS -> "Audio"
            in IMAGE_EXTENSIONS -> "Image"
            in DOCUMENT_EXTENSIONS -> "Document"
            in ARCHIVE_EXTENSIONS -> "Archive"
            else -> "Other"
        }
    }
    
    fun isMediaFile(extension: String): Boolean {
        return extension.lowercase() in (VIDEO_EXTENSIONS + AUDIO_EXTENSIONS)
    }
}