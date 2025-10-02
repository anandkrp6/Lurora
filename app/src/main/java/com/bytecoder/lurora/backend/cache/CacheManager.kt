package com.bytecoder.lurora.backend.cache

import android.content.Context
import android.graphics.Bitmap
import androidx.collection.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache manager for improved performance with multi-level caching
 */
@Singleton
class CacheManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val MEMORY_CACHE_SIZE = 1024 * 1024 * 32 // 32MB
        private const val DISK_CACHE_SIZE = 1024L * 1024L * 100L // 100MB
        private const val CACHE_VERSION = 1
        private const val BITMAP_CACHE_SIZE = 20 // Number of bitmaps to cache
    }
    
    // Memory caches
    private val memoryCache = LruCache<String, Any>(MEMORY_CACHE_SIZE)
    private val bitmapCache = LruCache<String, Bitmap>(BITMAP_CACHE_SIZE)
    
    // Disk cache directory
    private val diskCacheDir by lazy {
        File(context.cacheDir, "lurora_cache").apply {
            if (!exists()) mkdirs()
        }
    }
    
    // Cache statistics
    private val _cacheStats = MutableStateFlow(CacheStatistics())
    val cacheStats: StateFlow<CacheStatistics> = _cacheStats.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Cache a bitmap with automatic size management
     */
    fun cacheBitmap(key: String, bitmap: Bitmap) {
        try {
            bitmapCache.put(key, bitmap)
            updateStats(added = true, type = "bitmap")
        } catch (e: Exception) {
            // Cache full or other error, log but don't crash
            android.util.Log.w("CacheManager", "Failed to cache bitmap: $key", e)
        }
    }
    
    /**
     * Get cached bitmap
     */
    fun getCachedBitmap(key: String): Bitmap? {
        return try {
            val bitmap = bitmapCache.get(key)
            if (bitmap != null) {
                updateStats(hit = true, type = "bitmap")
            } else {
                updateStats(miss = true, type = "bitmap")
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Failed to get cached bitmap: $key", e)
            updateStats(miss = true, type = "bitmap")
            null
        }
    }
    
    /**
     * Cache data in memory
     */
    fun <T> cacheInMemory(key: String, data: T) {
        try {
            memoryCache.put(key, data as Any)
            updateStats(added = true, type = "memory")
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Failed to cache in memory: $key", e)
        }
    }
    
    /**
     * Get data from memory cache
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getFromMemoryCache(key: String): T? {
        return try {
            val data = memoryCache.get(key) as? T
            if (data != null) {
                updateStats(hit = true, type = "memory")
            } else {
                updateStats(miss = true, type = "memory")
            }
            data
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Failed to get from memory cache: $key", e)
            updateStats(miss = true, type = "memory")
            null
        }
    }
    
    /**
     * Cache data to disk asynchronously
     */
    suspend fun cacheToDisk(key: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val fileName = generateCacheFileName(key)
            val file = File(diskCacheDir, fileName)
            
            file.writeBytes(data)
            updateStats(added = true, type = "disk")
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Failed to cache to disk: $key", e)
        }
    }
    
    /**
     * Get data from disk cache
     */
    suspend fun getFromDiskCache(key: String): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            val fileName = generateCacheFileName(key)
            val file = File(diskCacheDir, fileName)
            
            if (file.exists()) {
                updateStats(hit = true, type = "disk")
                file.readBytes()
            } else {
                updateStats(miss = true, type = "disk")
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("CacheManager", "Failed to get from disk cache: $key", e)
            updateStats(miss = true, type = "disk")
            null
        }
    }
    
    /**
     * Cache text data to disk
     */
    suspend fun cacheTextToDisk(key: String, text: String) {
        cacheToDisk(key, text.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Get text data from disk cache
     */
    suspend fun getTextFromDiskCache(key: String): String? {
        return getFromDiskCache(key)?.toString(Charsets.UTF_8)
    }
    
    /**
     * Check if data exists in any cache layer
     */
    suspend fun exists(key: String): Boolean {
        return getFromMemoryCache<Any>(key) != null || getFromDiskCache(key) != null
    }
    
    /**
     * Remove item from all caches
     */
    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        // Remove from memory caches
        memoryCache.remove(key)
        bitmapCache.remove(key)
        
        // Remove from disk cache
        val fileName = generateCacheFileName(key)
        val file = File(diskCacheDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        
        updateStats(removed = true)
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        // Clear memory caches
        memoryCache.evictAll()
        bitmapCache.evictAll()
        
        // Clear disk cache
        diskCacheDir.listFiles()?.forEach { it.delete() }
        
        _cacheStats.value = CacheStatistics()
    }
    
    /**
     * Clear expired cache entries
     */
    suspend fun clearExpired(maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - maxAgeMillis
        
        diskCacheDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
                updateStats(removed = true)
            }
        }
    }
    
    /**
     * Get cache size information
     */
    suspend fun getCacheSizeInfo(): CacheSizeInfo = withContext(Dispatchers.IO) {
        val diskSize = diskCacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
        
        val memorySize = calculateMemoryCacheSize()
        
        return@withContext CacheSizeInfo(
            diskSizeBytes = diskSize,
            memorySizeBytes = memorySize,
            diskFileCount = diskCacheDir.listFiles()?.size ?: 0,
            memoryItemCount = memoryCache.size() + bitmapCache.size()
        )
    }
    
    /**
     * Optimize cache by removing least recently used items if over size limit
     */
    suspend fun optimizeCache() = withContext(Dispatchers.IO) {
        val sizeInfo = getCacheSizeInfo()
        
        // If disk cache is over limit, remove oldest files
        if (sizeInfo.diskSizeBytes > DISK_CACHE_SIZE) {
            val files = diskCacheDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            var currentSize = sizeInfo.diskSizeBytes
            
            for (file in files) {
                if (currentSize <= DISK_CACHE_SIZE * 0.8) break // Leave 20% headroom
                
                currentSize -= file.length()
                file.delete()
                updateStats(removed = true)
            }
        }
    }
    
    /**
     * Preload frequently accessed data
     */
    suspend fun preloadData(keys: List<String>, dataProvider: suspend (String) -> ByteArray?) {
        coroutineScope.launch {
            keys.forEach { key ->
                if (!exists(key)) {
                    try {
                        dataProvider(key)?.let { data ->
                            cacheToDisk(key, data)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CacheManager", "Failed to preload data for key: $key", e)
                    }
                }
            }
        }
    }
    
    private fun generateCacheFileName(key: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val hash = md5.digest(key.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun calculateMemoryCacheSize(): Long {
        var size = 0L
        
        // Approximate memory usage (this is a rough estimation)
        size += memoryCache.size() * 1024 // Assume average 1KB per item
        
        // Calculate bitmap cache size more accurately
        bitmapCache.snapshot().values.forEach { bitmap ->
            size += bitmap.byteCount
        }
        
        return size
    }
    
    private fun updateStats(
        hit: Boolean = false,
        miss: Boolean = false,
        added: Boolean = false,
        removed: Boolean = false,
        type: String = ""
    ) {
        val currentStats = _cacheStats.value
        _cacheStats.value = currentStats.copy(
            totalHits = currentStats.totalHits + if (hit) 1 else 0,
            totalMisses = currentStats.totalMisses + if (miss) 1 else 0,
            totalAdds = currentStats.totalAdds + if (added) 1 else 0,
            totalRemovals = currentStats.totalRemovals + if (removed) 1 else 0,
            lastAccessTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Get cache hit rate
     */
    fun getHitRate(): Float {
        val stats = _cacheStats.value
        val total = stats.totalHits + stats.totalMisses
        return if (total > 0) stats.totalHits.toFloat() / total else 0f
    }
    
    data class CacheStatistics(
        val totalHits: Long = 0,
        val totalMisses: Long = 0,
        val totalAdds: Long = 0,
        val totalRemovals: Long = 0,
        val lastAccessTime: Long = System.currentTimeMillis()
    )
    
    data class CacheSizeInfo(
        val diskSizeBytes: Long,
        val memorySizeBytes: Long,
        val diskFileCount: Int,
        val memoryItemCount: Int
    ) {
        val totalSizeBytes: Long get() = diskSizeBytes + memorySizeBytes
        
        fun getFormattedDiskSize(): String = formatBytes(diskSizeBytes)
        fun getFormattedMemorySize(): String = formatBytes(memorySizeBytes)
        fun getFormattedTotalSize(): String = formatBytes(totalSizeBytes)
        
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                bytes >= 1024 -> "${bytes / 1024} KB"
                else -> "$bytes B"
            }
        }
    }
}