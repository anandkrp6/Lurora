package com.bytecoder.lurora.backend.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.bytecoder.lurora.backend.cache.CacheManager
import com.bytecoder.lurora.backend.performance.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized media loader with caching and performance monitoring
 */
@Singleton
class OptimizedMediaLoader @Inject constructor(
    private val context: Context,
    private val cacheManager: CacheManager,
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val THUMBNAIL_SIZE = 512
        private const val METADATA_CACHE_PREFIX = "metadata_"
        private const val THUMBNAIL_CACHE_PREFIX = "thumbnail_"
        private const val ARTWORK_CACHE_PREFIX = "artwork_"
    }
    
    private val metadataRetriever = MediaMetadataRetriever()
    private val loadingJobs = mutableMapOf<String, Deferred<*>>()
    
    /**
     * Load media metadata with caching
     */
    suspend fun loadMediaMetadata(uri: Uri): MediaMetadata? = withContext(Dispatchers.IO) {
        val cacheKey = "$METADATA_CACHE_PREFIX${uri.toString()}"
        
        // Check cache first
        cacheManager.getFromMemoryCache<MediaMetadata>(cacheKey)?.let { return@withContext it }
        
        return@withContext performanceMonitor.measureOperation(
            operationName = "load_media_metadata",
            category = "media_loading"
        ) {
            try {
                metadataRetriever.setDataSource(context, uri)
                
                val metadata = MediaMetadata(
                    uri = uri,
                    title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    genre = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                    duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                    bitrate = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull(),
                    mimeType = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                    width = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                    height = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                    hasVideo = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes",
                    hasAudio = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
                )
                
                // Cache the metadata
                cacheManager.cacheInMemory(cacheKey, metadata)
                
                metadata
                
            } catch (e: Exception) {
                performanceMonitor.logPerformanceWarning(
                    "load_media_metadata",
                    "Failed to load metadata: ${e.message}",
                    mapOf("uri" to uri.toString())
                )
                null
            } finally {
                try {
                    metadataRetriever.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
        }
    }
    
    /**
     * Load media thumbnail with caching
     */
    suspend fun loadThumbnail(uri: Uri, timeUs: Long = 0): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$THUMBNAIL_CACHE_PREFIX${uri}_$timeUs"
        
        // Check memory cache first
        cacheManager.getCachedBitmap(cacheKey)?.let { return@withContext it }
        
        // Check if we're already loading this thumbnail
        loadingJobs[cacheKey]?.let { job ->
            @Suppress("UNCHECKED_CAST")
            return@withContext (job as Deferred<Bitmap?>).await()
        }
        
        // Start loading
        val job = async {
            performanceMonitor.measureOperation(
                operationName = "load_thumbnail",
                category = "media_loading"
            ) {
                try {
                    metadataRetriever.setDataSource(context, uri)
                    val bitmap = if (timeUs > 0) {
                        metadataRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } else {
                        metadataRetriever.frameAtTime
                    }
                    
                    bitmap?.let { originalBitmap ->
                        val scaledBitmap = scaleBitmap(originalBitmap, THUMBNAIL_SIZE)
                        cacheManager.cacheBitmap(cacheKey, scaledBitmap)
                        
                        // Also cache to disk for persistence
                        launch {
                            val bytes = bitmapToByteArray(scaledBitmap)
                            cacheManager.cacheToDisk(cacheKey, bytes)
                        }
                        
                        scaledBitmap
                    }
                    
                } catch (e: Exception) {
                    performanceMonitor.logPerformanceWarning(
                        "load_thumbnail",
                        "Failed to load thumbnail: ${e.message}",
                        mapOf("uri" to uri.toString(), "timeUs" to timeUs)
                    )
                    null
                } finally {
                    try {
                        metadataRetriever.release()
                    } catch (e: Exception) {
                        // Ignore release errors
                    }
                }
            }
        }
        
        loadingJobs[cacheKey] = job
        val result = job.await()
        loadingJobs.remove(cacheKey)
        
        result
    }
    
    /**
     * Load album artwork with caching
     */
    suspend fun loadAlbumArtwork(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$ARTWORK_CACHE_PREFIX${uri}"
        
        // Check memory cache first
        cacheManager.getCachedBitmap(cacheKey)?.let { return@withContext it }
        
        // Check disk cache
        cacheManager.getFromDiskCache(cacheKey)?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap?.let {
                cacheManager.cacheBitmap(cacheKey, it)
                return@withContext it
            }
        }
        
        return@withContext performanceMonitor.measureOperation(
            operationName = "load_album_artwork",
            category = "media_loading"
        ) {
            try {
                metadataRetriever.setDataSource(context, uri)
                val artworkBytes = metadataRetriever.embeddedPicture
                
                artworkBytes?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    bitmap?.let { originalBitmap ->
                        val scaledBitmap = scaleBitmap(originalBitmap, THUMBNAIL_SIZE)
                        
                        // Cache in memory and disk
                        cacheManager.cacheBitmap(cacheKey, scaledBitmap)
                        launch {
                            cacheManager.cacheToDisk(cacheKey, bitmapToByteArray(scaledBitmap))
                        }
                        
                        scaledBitmap
                    }
                }
                
            } catch (e: Exception) {
                performanceMonitor.logPerformanceWarning(
                    "load_album_artwork",
                    "Failed to load artwork: ${e.message}",
                    mapOf("uri" to uri.toString())
                )
                null
            } finally {
                try {
                    metadataRetriever.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
        }
    }
    
    /**
     * Preload media data for better performance
     */
    suspend fun preloadMedia(uris: List<Uri>) = coroutineScope {
        val jobs = uris.map { uri ->
            async {
                // Preload metadata
                loadMediaMetadata(uri)
                
                // Preload thumbnail
                loadThumbnail(uri)
                
                // Preload artwork
                loadAlbumArtwork(uri)
            }
        }
        
        jobs.awaitAll()
    }
    
    /**
     * Load media in batches for better performance
     */
    fun loadMediaBatch(uris: List<Uri>, batchSize: Int = 10): Flow<MediaLoadResult> = flow {
        uris.chunked(batchSize).forEach { batch ->
            coroutineScope {
                val results = batch.map { uri ->
                    async {
                        try {
                            val metadata = loadMediaMetadata(uri)
                            val thumbnail = loadThumbnail(uri)
                            val artwork = loadAlbumArtwork(uri)
                            
                            MediaLoadResult.Success(
                                uri = uri,
                                metadata = metadata,
                                thumbnail = thumbnail,
                                artwork = artwork
                            )
                        } catch (e: Exception) {
                            MediaLoadResult.Error(uri, e)
                        }
                    }
                }
                
                results.awaitAll().forEach { result ->
                    emit(result)
                }
            }
            
            // Small delay between batches to prevent overwhelming the system
            delay(100)
        }
    }
    
    /**
     * Clear media cache
     */
    suspend fun clearMediaCache() {
        performanceMonitor.measureOperation(
            operationName = "clear_media_cache",
            category = "cache_management"
        ) {
            cacheManager.clearAll()
        }
    }
    
    /**
     * Get cache statistics for media
     */
    suspend fun getMediaCacheStats(): MediaCacheStats {
        val sizeInfo = cacheManager.getCacheSizeInfo()
        val hitRate = cacheManager.getHitRate()
        
        return MediaCacheStats(
            totalSizeMB = sizeInfo.totalSizeBytes / (1024 * 1024),
            diskSizeMB = sizeInfo.diskSizeBytes / (1024 * 1024),
            memorySizeMB = sizeInfo.memorySizeBytes / (1024 * 1024),
            itemCount = sizeInfo.memoryItemCount + sizeInfo.diskFileCount,
            hitRate = hitRate
        )
    }
    
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        
        if (ratio >= 1.0f) return bitmap
        
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
    
    data class MediaMetadata(
        val uri: Uri,
        val title: String?,
        val artist: String?,
        val album: String?,
        val genre: String?,
        val duration: Long,
        val bitrate: Int?,
        val mimeType: String?,
        val width: Int?,
        val height: Int?,
        val hasVideo: Boolean,
        val hasAudio: Boolean
    )
    
    sealed class MediaLoadResult {
        abstract val uri: Uri
        
        data class Success(
            override val uri: Uri,
            val metadata: MediaMetadata?,
            val thumbnail: Bitmap?,
            val artwork: Bitmap?
        ) : MediaLoadResult()
        
        data class Error(
            override val uri: Uri,
            val exception: Exception
        ) : MediaLoadResult()
    }
    
    data class MediaCacheStats(
        val totalSizeMB: Long,
        val diskSizeMB: Long,
        val memorySizeMB: Long,
        val itemCount: Int,
        val hitRate: Float
    )
}