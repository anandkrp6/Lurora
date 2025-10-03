package com.bytecoder.lurora.backend.repositories

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.bytecoder.lurora.backend.database.*
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.backend.utils.ThumbnailExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val historyDao: HistoryDao,
    private val thumbnailExtractor: ThumbnailExtractor
) {

    /**
     * Get all video files
     */
    fun getVideoFiles(): Flow<List<MediaItem>> {
        return mediaDao.getMediaByType(MediaType.VIDEO).map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    /**
     * Get all audio files
     */
    fun getAudioFiles(): Flow<List<MediaItem>> {
        return mediaDao.getMediaByType(MediaType.AUDIO).map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    /**
     * Get favorite media files
     */
    fun getFavoriteFiles(): Flow<List<MediaItem>> {
        return mediaDao.getFavoriteMedia().map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    /**
     * Search media files
     */
    fun searchMedia(query: String): Flow<List<MediaItem>> {
        return mediaDao.searchMedia(query).map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    /**
     * Scan device for media files and update database
     */
    suspend fun scanAndUpdateMediaFiles(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val videoFiles = scanVideoFiles()
                val audioFiles = scanAudioFiles()
                
                // Clear existing data and insert new scan results
                mediaDao.clearAllMedia()
                mediaDao.insertAllMedia(videoFiles.map { it.toMediaItemEntity() })
                mediaDao.insertAllMedia(audioFiles.map { it.toMediaItemEntity() })
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Toggle favorite status for a media item
     */
    suspend fun toggleFavorite(mediaItem: MediaItem): MediaItem {
        val updatedItem = mediaItem.copy(isFavorite = !mediaItem.isFavorite)
        mediaDao.updateMedia(updatedItem.toMediaItemEntity())
        return updatedItem
    }

    /**
     * Update media item play count and last position
     */
    suspend fun updatePlaybackInfo(mediaItem: MediaItem, position: Long) {
        val updatedItem = mediaItem.copy(
            lastPosition = position,
            playCount = mediaItem.playCount + 1
        )
        mediaDao.updateMedia(updatedItem.toMediaItemEntity())
    }

    /**
     * Get media item by ID
     */
    suspend fun getMediaById(id: String): MediaItem? {
        return mediaDao.getMediaById(id)?.toMediaItem()
    }

    private fun scanVideoFiles(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        val supportedExtensions = SupportedFormats.getSupportedExtensions(MediaType.VIDEO)
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM
        )

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(displayNameColumn) ?: ""
                val title = it.getString(titleColumn) ?: displayName
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val data = it.getString(dataColumn) ?: ""
                val mimeType = it.getString(mimeTypeColumn) ?: ""
                val dateAdded = it.getLong(dateAddedColumn) * 1000L
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)

                // Check if file exists and has supported extension
                if (data.isNotEmpty() && thumbnailExtractor.isFileAccessible(data)) {
                    val extension = File(data).extension.lowercase()
                    if (supportedExtensions.contains(extension)) {
                        // Create proper file URI
                        val videoUri = Uri.fromFile(File(data))
                        
                        mediaItems.add(
                            MediaItem(
                                id = id.toString(),
                                uri = videoUri,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                mediaType = MediaType.VIDEO,
                                albumArtUri = null, // Will be loaded asynchronously by UI
                                mimeType = mimeType,
                                size = size,
                                dateAdded = dateAdded,
                                metadata = mapOf(
                                    "file_path" to data,
                                    "extension" to extension,
                                    "format_supported" to SupportedFormats.isSupported(extension).toString(),
                                    "hardware_support" to SupportedFormats.hasHardwareSupport(extension).toString(),
                                    "streaming_support" to SupportedFormats.supportsStreaming(extension).toString()
                                )
                            )
                        )
                    }
                }
            }
        }

        return mediaItems
    }

    private fun scanAudioFiles(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        val supportedExtensions = SupportedFormats.getSupportedExtensions(MediaType.AUDIO)
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val genreColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(displayNameColumn) ?: ""
                val title = it.getString(titleColumn) ?: displayName
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val data = it.getString(dataColumn) ?: ""
                val mimeType = it.getString(mimeTypeColumn) ?: ""
                val dateAdded = it.getLong(dateAddedColumn) * 1000L
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val genre = it.getString(genreColumn)
                val albumId = it.getLong(albumIdColumn)

                // Check if file exists and has supported extension
                if (data.isNotEmpty() && thumbnailExtractor.isFileAccessible(data)) {
                    val extension = File(data).extension.lowercase()
                    if (supportedExtensions.contains(extension)) {
                        // Try to get album art from MediaStore first
                        val albumArtUri: Uri? = if (albumId != 0L) {
                            ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            )
                        } else null
                        // Note: If MediaStore album art doesn't exist, UI will extract from file

                        // Create proper file URI
                        val audioUri = Uri.fromFile(File(data))

                        mediaItems.add(
                            MediaItem(
                                id = id.toString(),
                                uri = audioUri,
                                title = title,
                                artist = artist,
                                album = album,
                                genre = genre,
                                duration = duration,
                                mediaType = MediaType.AUDIO,
                                albumArtUri = albumArtUri,
                                mimeType = mimeType,
                                size = size,
                                dateAdded = dateAdded,
                                metadata = mapOf(
                                    "file_path" to data,
                                    "extension" to extension,
                                    "format_supported" to SupportedFormats.isSupported(extension).toString(),
                                    "hardware_support" to SupportedFormats.hasHardwareSupport(extension).toString(),
                                    "streaming_support" to SupportedFormats.supportsStreaming(extension).toString(),
                                    "quality" to (SupportedFormats.getFormat(extension)?.quality?.name ?: "STANDARD")
                                )
                            )
                        )
                    }
                }
            }
        }

        return mediaItems
    }
}

/**
 * Extension functions to convert between entities and domain models
 */
fun MediaItemEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        uri = Uri.parse(uri),
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        duration = duration,
        mediaType = mediaType,
        albumArtUri = albumArtUri?.let { Uri.parse(it) },
        subtitleUri = subtitleUri?.let { Uri.parse(it) },
        mimeType = mimeType,
        size = size,
        dateAdded = dateAdded.time,
        playCount = playCount,
        isFavorite = isFavorite,
        lastPosition = lastPosition,
        metadata = metadata
    )
}

fun MediaItem.toMediaItemEntity(): MediaItemEntity {
    return MediaItemEntity(
        id = id,
        uri = uri.toString(),
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        duration = duration,
        mediaType = mediaType,
        albumArtUri = albumArtUri?.toString(),
        subtitleUri = subtitleUri?.toString(),
        mimeType = mimeType,
        size = size,
        dateAdded = Date(dateAdded),
        playCount = playCount,
        isFavorite = isFavorite,
        lastPosition = lastPosition,
        metadata = metadata
    )
}