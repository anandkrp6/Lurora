package com.bytecoder.lurora.backend.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance optimization manager for efficient resource usage
 */
@Singleton
class PerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val imageCache = ConcurrentHashMap<String, Bitmap>()
    private val metadataCache = ConcurrentHashMap<String, Any>()
    private val loadingJobs = ConcurrentHashMap<String, Job>()
    
    /**
     * Optimized image loader with caching
     */
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
    
    /**
     * Lazy loading with pagination support
     */
    class LazyLoadingPager<T>(
        private val pageSize: Int = 20,
        private val loadPage: suspend (page: Int, size: Int) -> List<T>
    ) {
        private val _items = MutableStateFlow<List<T>>(emptyList())
        val items: StateFlow<List<T>> = _items.asStateFlow()
        
        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
        
        private val _hasMoreItems = MutableStateFlow(true)
        val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()
        
        private var currentPage = 0
        
        suspend fun loadNextPage() {
            if (_isLoading.value || !_hasMoreItems.value) return
            
            _isLoading.value = true
            try {
                val newItems = loadPage(currentPage, pageSize)
                _items.value = _items.value + newItems
                _hasMoreItems.value = newItems.size == pageSize
                currentPage++
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
        
        fun reset() {
            _items.value = emptyList()
            currentPage = 0
            _hasMoreItems.value = true
        }
    }
    
    /**
     * Background task executor with lifecycle management
     */
    class BackgroundTaskManager {
        private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val activeTasks = ConcurrentHashMap<String, Job>()
        
        fun <T> executeTask(
            taskId: String,
            task: suspend () -> T,
            onResult: (Result<T>) -> Unit = {}
        ) {
            // Cancel existing task with same ID
            activeTasks[taskId]?.cancel()
            
            val job = backgroundScope.launch {
                try {
                    val result = task()
                    withContext(Dispatchers.Main) {
                        onResult(Result.success(result))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(Result.failure(e))
                    }
                } finally {
                    activeTasks.remove(taskId)
                }
            }
            
            activeTasks[taskId] = job
        }
        
        fun cancelTask(taskId: String) {
            activeTasks[taskId]?.cancel()
            activeTasks.remove(taskId)
        }
        
        fun cancelAllTasks() {
            activeTasks.values.forEach { it.cancel() }
            activeTasks.clear()
        }
    }
    
    /**
     * Memory-efficient thumbnail cache
     */
    class ThumbnailCache(private val maxSize: Int = 100) {
        private val cache = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
        
        @Synchronized
        fun get(key: String): Bitmap? {
            return cache[key]
        }
        
        @Synchronized
        fun put(key: String, bitmap: Bitmap) {
            if (cache.size >= maxSize) {
                cache.remove(cache.keys.first())
            }
            cache[key] = bitmap
        }
        
        @Synchronized
        fun clear() {
            cache.values.forEach { 
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
            cache.clear()
        }
        
        @Synchronized
        fun size(): Int = cache.size
    }
    
    private val thumbnailCache = ThumbnailCache()
    private val backgroundTaskManager = BackgroundTaskManager()
    
    /**
     * Load thumbnail with caching
     */
    suspend fun loadThumbnail(uri: Uri): Bitmap? {
        val cacheKey = uri.toString()
        
        // Check cache first
        thumbnailCache.get(cacheKey)?.let { return it }
        
        return try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(200, 200) // Thumbnail size
                .build()
                
            val drawable = imageLoader.execute(request).drawable
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            
            bitmap?.let { 
                thumbnailCache.put(cacheKey, it)
            }
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Preload images for better performance
     */
    fun preloadImages(uris: List<Uri>) {
        backgroundTaskManager.executeTask<Unit>("preload_images", {
            uris.forEach { uri ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .build()
                    imageLoader.enqueue(request)
                } catch (e: Exception) {
                    // Ignore individual failures
                }
            }
        })
    }
    
    /**
     * Memory cleanup when low memory detected
     */
    fun onLowMemory() {
        thumbnailCache.clear()
        imageLoader.memoryCache?.clear()
        System.gc()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        backgroundTaskManager.cancelAllTasks()
        thumbnailCache.clear()
        imageLoader.shutdown()
    }
}

/**
 * Composable for lazy loading with visibility detection
 */
@Composable
fun <T> LazyLoadingItem(
    item: T,
    onVisible: () -> Unit = {},
    content: @Composable (T) -> Unit
) {
    LaunchedEffect(item) {
        onVisible()
    }
    
    content(item)
}

/**
 * Memory-efficient image loading composable
 */
@Composable
fun rememberOptimizedImageLoader(): ImageLoader {
    val context = LocalContext.current
    
    return remember {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}

/**
 * Performance monitoring utilities
 */
object PerformanceMonitor {
    
    /**
     * Measure execution time of a block
     */
    inline fun <T> measureTime(tag: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val endTime = System.currentTimeMillis()
            android.util.Log.d("Performance", "$tag took ${endTime - startTime}ms")
        }
    }
    
    /**
     * Log memory usage
     */
    fun logMemoryUsage(tag: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val percentUsed = (usedMemory * 100) / maxMemory
        
        android.util.Log.d("Memory", "$tag - Used: ${usedMemory / 1024 / 1024}MB, Max: ${maxMemory / 1024 / 1024}MB, Percent: $percentUsed%")
    }
}

/**
 * Debounced state for search and filtering
 */
@Composable
fun <T> rememberDebouncedState(
    initialValue: T,
    delayMillis: Long = 300L
): MutableState<T> {
    val state = remember { mutableStateOf(initialValue) }
    val debouncedState = remember { mutableStateOf(initialValue) }
    
    LaunchedEffect(state.value) {
        delay(delayMillis)
        debouncedState.value = state.value
    }
    
    return object : MutableState<T> {
        override var value: T
            get() = debouncedState.value
            set(value) { state.value = value }
        
        override fun component1(): T = value
        override fun component2(): (T) -> Unit = { value = it }
    }
}