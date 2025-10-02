package com.bytecoder.lurora.backend.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.bytecoder.lurora.backend.data.database.entity.DownloadQueue
import com.bytecoder.lurora.backend.data.database.dao.DownloadQueueDao
import com.bytecoder.lurora.backend.security.SecurityAuditLogger
import com.bytecoder.lurora.backend.performance.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced download manager with scheduling, resume capability, and bandwidth control
 */
@Singleton
class AdvancedDownloadManager @Inject constructor(
    private val context: Context,
    private val downloadQueueDao: DownloadQueueDao,
    private val securityAuditLogger: SecurityAuditLogger,
    private val performanceMonitor: PerformanceMonitor,
    private val workManager: WorkManager
) {
    
    companion object {
        private const val CHUNK_SIZE = 8192
        private const val MAX_CONCURRENT_DOWNLOADS = 3
        private const val RETRY_DELAY_SECONDS = 30L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BANDWIDTH_CHECK_INTERVAL = 1000L // 1 second
    }
    
    private val activeDownloads = mutableMapOf<String, DownloadSession>()
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bandwidthLimiter = BandwidthLimiter()
    
    // Download statistics
    private val _downloadStats = MutableStateFlow(DownloadStatistics())
    val downloadStats: StateFlow<DownloadStatistics> = _downloadStats.asStateFlow()
    
    // Queue management
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()
    
    /**
     * Schedule a download with advanced options
     */
    suspend fun scheduleDownload(request: AdvancedDownloadRequest): String {
        val downloadId = generateDownloadId()
        
        val queueItem = DownloadQueue(
            id = downloadId,
            url = request.url,
            fileName = request.fileName,
            targetDirectory = request.targetDirectory,
            priority = request.priority,
            requiresWifi = request.requiresWifi,
            requiresCharging = request.requiresCharging,
            scheduledTime = request.scheduledTime,
            maxRetries = request.maxRetries,
            bandwidthLimitKbps = request.bandwidthLimitKbps,
            headers = request.headers,
            metadata = request.metadata,
            status = DownloadStatus.QUEUED,
            createdAt = System.currentTimeMillis()
        )
        
        downloadQueueDao.insertDownload(queueItem)
        
        // Schedule WorkManager job if needed
        if (request.scheduledTime > System.currentTimeMillis()) {
            scheduleDelayedDownload(downloadId, request.scheduledTime)
        } else {
            // Start immediately if conditions are met
            processQueueIfReady()
        }
        
        securityAuditLogger.logDownloadEvent(
            SecurityAuditLogger.SecurityEvent.DOWNLOAD_STARTED,
            request.url,
            "${request.targetDirectory}/${request.fileName}"
        )
        
        updateQueueState()
        return downloadId
    }
    
    /**
     * Start or resume a download
     */
    suspend fun startDownload(downloadId: String): Boolean {
        val queueItem = downloadQueueDao.getDownloadById(downloadId) ?: return false
        
        if (activeDownloads.containsKey(downloadId)) {
            return true // Already downloading
        }
        
        if (activeDownloads.size >= MAX_CONCURRENT_DOWNLOADS) {
            return false // Too many active downloads
        }
        
        if (!isNetworkAvailable(queueItem.requiresWifi)) {
            return false // Network requirements not met
        }
        
        if (queueItem.requiresCharging && !isBatteryCharging()) {
            return false // Charging requirement not met
        }
        
        val session = DownloadSession(
            downloadId = downloadId,
            queueItem = queueItem,
            job = downloadScope.launch { executeDownload(queueItem) }
        )
        
        activeDownloads[downloadId] = session
        updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
        updateQueueState()
        
        return true
    }
    
    /**
     * Pause a download
     */
    suspend fun pauseDownload(downloadId: String) {
        activeDownloads[downloadId]?.let { session ->
            session.job.cancel()
            activeDownloads.remove(downloadId)
            updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
            updateQueueState()
        }
    }
    
    /**
     * Cancel a download
     */
    suspend fun cancelDownload(downloadId: String) {
        activeDownloads[downloadId]?.let { session ->
            session.job.cancel()
            activeDownloads.remove(downloadId)
        }
        
        updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
        updateQueueState()
        
        // Clean up partial file
        val queueItem = downloadQueueDao.getDownloadById(downloadId)
        queueItem?.let { item ->
            val file = File(item.targetDirectory, item.fileName)
            if (file.exists()) {
                file.delete()
            }
        }
    }
    
    /**
     * Retry a failed download
     */
    suspend fun retryDownload(downloadId: String): Boolean {
        val queueItem = downloadQueueDao.getDownloadById(downloadId) ?: return false
        
        if (queueItem.retryCount >= queueItem.maxRetries) {
            return false
        }
        
        downloadQueueDao.updateDownload(queueItem.copy(
            retryCount = queueItem.retryCount + 1,
            status = DownloadStatus.QUEUED,
            errorMessage = null
        ))
        
        return startDownload(downloadId)
    }
    
    /**
     * Set global bandwidth limit
     */
    fun setBandwidthLimit(limitKbps: Int) {
        bandwidthLimiter.setGlobalLimit(limitKbps)
    }
    
    /**
     * Process download queue automatically
     */
    suspend fun processQueueIfReady() {
        val queuedDownloads = downloadQueueDao.getQueuedDownloads()
            .filter { it.scheduledTime <= System.currentTimeMillis() }
            .sortedBy { it.priority }
        
        for (download in queuedDownloads) {
            if (activeDownloads.size < MAX_CONCURRENT_DOWNLOADS) {
                startDownload(download.id)
            } else {
                break
            }
        }
    }
    
    /**
     * Get download progress
     */
    fun getDownloadProgress(downloadId: String): Flow<DownloadProgress> {
        return downloadQueueDao.getDownloadFlow(downloadId)
            .map { queueItem ->
                queueItem?.let {
                    DownloadProgress(
                        downloadId = downloadId,
                        status = it.status,
                        bytesDownloaded = it.downloadedBytes,
                        totalBytes = it.totalBytes,
                        progress = if (it.totalBytes > 0) it.downloadedBytes.toFloat() / it.totalBytes else 0f,
                        speed = calculateDownloadSpeed(downloadId),
                        estimatedTimeRemaining = calculateETA(downloadId)
                    )
                } ?: DownloadProgress(downloadId, DownloadStatus.NOT_FOUND)
            }
    }
    
    /**
     * Execute the actual download
     */
    private suspend fun executeDownload(queueItem: DownloadQueue) = withContext(Dispatchers.IO) {
        val downloadId = queueItem.id
        
        try {
            performanceMonitor.measureOperation(
                operationName = "download_file",
                category = "network"
            ) {
                val url = URL(queueItem.url)
                val connection = url.openConnection() as HttpURLConnection
                
                // Set headers
                queueItem.headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                
                // Check for resume capability
                val targetFile = File(queueItem.targetDirectory, queueItem.fileName)
                val existingBytes = if (targetFile.exists()) targetFile.length() else 0L
                
                if (existingBytes > 0) {
                    connection.setRequestProperty("Range", "bytes=$existingBytes-")
                }
                
                connection.connect()
                
                val responseCode = connection.responseCode
                val totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    existingBytes + connection.contentLengthLong
                } else {
                    connection.contentLengthLong.let { if (it < 0) 0L else it }
                }
                
                // Update total bytes in database
                downloadQueueDao.updateDownload(queueItem.copy(totalBytes = totalBytes))
                
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(targetFile, existingBytes > 0)
                
                downloadFile(downloadId, inputStream, outputStream, existingBytes, totalBytes, queueItem.bandwidthLimitKbps)
                
                // Mark as completed
                updateDownloadStatus(downloadId, DownloadStatus.COMPLETED)
                activeDownloads.remove(downloadId)
                
                securityAuditLogger.logDownloadEvent(
                    SecurityAuditLogger.SecurityEvent.DOWNLOAD_COMPLETED,
                    queueItem.url,
                    targetFile.absolutePath
                )
                
                updateQueueState()
            }
            
        } catch (e: Exception) {
            handleDownloadError(downloadId, e)
        }
    }
    
    /**
     * Download file with bandwidth limiting and progress tracking
     */
    private suspend fun downloadFile(
        downloadId: String,
        inputStream: InputStream,
        outputStream: FileOutputStream,
        startBytes: Long,
        totalBytes: Long,
        bandwidthLimitKbps: Int?
    ) {
        val buffer = ByteArray(CHUNK_SIZE)
        var downloadedBytes = startBytes
        var lastUpdateTime = System.currentTimeMillis()
        var lastUpdateBytes = downloadedBytes
        
        try {
            while (true) {
                // Check if download is cancelled
                if (!activeDownloads.containsKey(downloadId)) {
                    break
                }
                
                // Apply bandwidth limiting
                val limitKbps = bandwidthLimitKbps ?: bandwidthLimiter.getGlobalLimit()
                if (limitKbps > 0) {
                    bandwidthLimiter.waitForBandwidth(downloadId, CHUNK_SIZE)
                }
                
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                
                // Update progress every second
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= 1000) {
                    val speed = ((downloadedBytes - lastUpdateBytes) * 1000) / (currentTime - lastUpdateTime)
                    
                    downloadQueueDao.updateDownloadProgress(downloadId, downloadedBytes, speed)
                    
                    updateDownloadStatistics(downloadId, speed)
                    
                    lastUpdateTime = currentTime
                    lastUpdateBytes = downloadedBytes
                }
            }
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }
    
    /**
     * Handle download errors with retry logic
     */
    private suspend fun handleDownloadError(downloadId: String, error: Exception) {
        val queueItem = downloadQueueDao.getDownloadById(downloadId) ?: return
        
        activeDownloads.remove(downloadId)
        
        if (queueItem.retryCount < queueItem.maxRetries) {
            // Schedule retry
            val delay = RETRY_DELAY_SECONDS * (queueItem.retryCount + 1)
            
            downloadQueueDao.updateDownload(queueItem.copy(
                status = DownloadStatus.RETRY_SCHEDULED,
                errorMessage = error.message,
                retryCount = queueItem.retryCount + 1
            ))
            
            // Schedule retry with WorkManager
            scheduleRetry(downloadId, delay)
            
        } else {
            // Mark as failed
            downloadQueueDao.updateDownload(queueItem.copy(
                status = DownloadStatus.FAILED,
                errorMessage = error.message
            ))
            
            securityAuditLogger.logSecurityEvent(
                SecurityAuditLogger.SecurityEvent.DOWNLOAD_COMPLETED,
                "Download failed: ${queueItem.url}",
                SecurityAuditLogger.SecurityLevel.ERROR,
                mapOf("error" to (error.message ?: "Unknown error"))
            )
        }
        
        updateQueueState()
    }
    
    /**
     * Schedule delayed download with WorkManager
     */
    private fun scheduleDelayedDownload(downloadId: String, scheduledTime: Long) {
        val delay = scheduledTime - System.currentTimeMillis()
        if (delay <= 0) return
        
        val workRequest = OneTimeWorkRequestBuilder<ScheduledDownloadWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("download_id" to downloadId))
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * Schedule retry with WorkManager
     */
    private fun scheduleRetry(downloadId: String, delaySeconds: Long) {
        val workRequest = OneTimeWorkRequestBuilder<RetryDownloadWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(workDataOf("download_id" to downloadId))
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * Check network availability
     */
    private fun isNetworkAvailable(requiresWifi: Boolean): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            requiresWifi -> capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            else -> capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
    
    /**
     * Check if device is charging
     */
    private fun isBatteryCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.isCharging
    }
    
    /**
     * Update download status in database
     */
    private suspend fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
        val queueItem = downloadQueueDao.getDownloadById(downloadId) ?: return
        downloadQueueDao.updateDownload(queueItem.copy(status = status))
    }
    
    /**
     * Calculate download speed
     */
    private fun calculateDownloadSpeed(downloadId: String): Long {
        // Implementation depends on tracking download progress over time
        return 0L // Placeholder
    }
    
    /**
     * Calculate estimated time remaining
     */
    private fun calculateETA(downloadId: String): Long {
        // Implementation depends on current speed and remaining bytes
        return 0L // Placeholder
    }
    
    /**
     * Update download statistics
     */
    private suspend fun updateDownloadStatistics(downloadId: String, speed: Long) {
        val currentStats = _downloadStats.value
        _downloadStats.value = currentStats.copy(
            totalActiveDownloads = activeDownloads.size,
            averageSpeed = (currentStats.averageSpeed + speed) / 2,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Update queue state
     */
    private suspend fun updateQueueState() {
        val queuedCount = downloadQueueDao.getDownloadCountByStatus(DownloadStatus.QUEUED)
        val completedCount = downloadQueueDao.getDownloadCountByStatus(DownloadStatus.COMPLETED)
        val failedCount = downloadQueueDao.getDownloadCountByStatus(DownloadStatus.FAILED)
        
        _queueState.value = QueueState(
            queuedCount = queuedCount,
            activeCount = activeDownloads.size,
            completedCount = completedCount,
            failedCount = failedCount
        )
    }
    
    /**
     * Generate unique download ID
     */
    private fun generateDownloadId(): String {
        return "download_${System.currentTimeMillis()}_${(0..999).random()}"
    }
    
    data class AdvancedDownloadRequest(
        val url: String,
        val fileName: String,
        val targetDirectory: String,
        val priority: Int = 0,
        val requiresWifi: Boolean = false,
        val requiresCharging: Boolean = false,
        val scheduledTime: Long = System.currentTimeMillis(),
        val maxRetries: Int = MAX_RETRY_ATTEMPTS,
        val bandwidthLimitKbps: Int? = null,
        val headers: Map<String, String> = emptyMap(),
        val metadata: Map<String, String> = emptyMap()
    )
    
    data class DownloadSession(
        val downloadId: String,
        val queueItem: DownloadQueue,
        val job: Job
    )
    
    data class DownloadProgress(
        val downloadId: String,
        val status: DownloadStatus,
        val bytesDownloaded: Long = 0L,
        val totalBytes: Long = 0L,
        val progress: Float = 0f,
        val speed: Long = 0L,
        val estimatedTimeRemaining: Long = 0L
    )
    
    data class DownloadStatistics(
        val totalActiveDownloads: Int = 0,
        val averageSpeed: Long = 0L,
        val totalBytesDownloaded: Long = 0L,
        val lastUpdateTime: Long = System.currentTimeMillis()
    )
    
    data class QueueState(
        val queuedCount: Int = 0,
        val activeCount: Int = 0,
        val completedCount: Int = 0,
        val failedCount: Int = 0
    )
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    RETRY_SCHEDULED,
    NOT_FOUND
}