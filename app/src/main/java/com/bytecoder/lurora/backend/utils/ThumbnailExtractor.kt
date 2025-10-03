package com.bytecoder.lurora.backend.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for extracting thumbnails from video and audio files
 */
@Singleton
class ThumbnailExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ThumbnailExtractor"
        private const val THUMBNAIL_CACHE_DIR = "thumbnails"
        private const val VIDEO_FRAME_TIME_US = 1_000_000L // 1 second
    }

    private val cacheDir by lazy {
        File(context.cacheDir, THUMBNAIL_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Extract video thumbnail from file path
     */
    suspend fun getVideoThumbnail(filePath: String, mediaId: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, "video_thumb_$mediaId.jpg")
                
                // Return cached thumbnail if exists
                if (cacheFile.exists()) {
                    return@withContext Uri.fromFile(cacheFile)
                }

                val retriever = MediaMetadataRetriever()
                val bitmap = try {
                    retriever.setDataSource(filePath)
                    retriever.getFrameAtTime(
                        VIDEO_FRAME_TIME_US,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract video frame from $filePath", e)
                    null
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to release MediaMetadataRetriever", e)
                    }
                }

                bitmap?.let { bmp ->
                    if (saveBitmapToCache(bmp, cacheFile)) {
                        Uri.fromFile(cacheFile)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting video thumbnail for $filePath", e)
                null
            }
        }
    }

    /**
     * Extract album art from audio file
     */
    suspend fun getAlbumArt(filePath: String, mediaId: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, "album_art_$mediaId.jpg")
                
                // Return cached album art if exists
                if (cacheFile.exists()) {
                    return@withContext Uri.fromFile(cacheFile)
                }

                val retriever = MediaMetadataRetriever()
                val bitmap = try {
                    retriever.setDataSource(filePath)
                    retriever.embeddedPicture?.let { artBytes ->
                        BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract album art from $filePath", e)
                    null
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to release MediaMetadataRetriever", e)
                    }
                }

                bitmap?.let { bmp ->
                    if (saveBitmapToCache(bmp, cacheFile)) {
                        Uri.fromFile(cacheFile)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting album art for $filePath", e)
                null
            }
        }
    }

    /**
     * Save bitmap to cache file
     */
    private fun saveBitmapToCache(bitmap: Bitmap, cacheFile: File): Boolean {
        return try {
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                true
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save bitmap to cache", e)
            false
        }
    }

    /**
     * Check if file exists and is readable
     */
    fun isFileAccessible(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.canRead() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear thumbnail cache
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear thumbnail cache", e)
            }
        }
    }
}