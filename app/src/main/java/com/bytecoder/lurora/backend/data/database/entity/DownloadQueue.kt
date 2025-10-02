package com.bytecoder.lurora.backend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bytecoder.lurora.backend.download.DownloadStatus

/**
 * Entity representing a download in the queue
 */
@Entity(tableName = "download_queue")
data class DownloadQueue(
    @PrimaryKey
    val id: String,
    
    /**
     * URL to download from
     */
    val url: String,
    
    /**
     * Target file name
     */
    val fileName: String,
    
    /**
     * Target directory path
     */
    val targetDirectory: String,
    
    /**
     * Download priority (higher number = higher priority)
     */
    val priority: Int = 0,
    
    /**
     * Whether download requires WiFi connection
     */
    val requiresWifi: Boolean = false,
    
    /**
     * Whether download requires device to be charging
     */
    val requiresCharging: Boolean = false,
    
    /**
     * Scheduled time for download (timestamp)
     */
    val scheduledTime: Long = System.currentTimeMillis(),
    
    /**
     * Maximum number of retry attempts
     */
    val maxRetries: Int = 3,
    
    /**
     * Current retry count
     */
    val retryCount: Int = 0,
    
    /**
     * Bandwidth limit in KB/s (null = no limit)
     */
    val bandwidthLimitKbps: Int? = null,
    
    /**
     * HTTP headers as JSON string
     */
    val headers: Map<String, String> = emptyMap(),
    
    /**
     * Additional metadata as JSON string
     */
    val metadata: Map<String, String> = emptyMap(),
    
    /**
     * Current download status
     */
    val status: DownloadStatus = DownloadStatus.QUEUED,
    
    /**
     * Number of bytes downloaded
     */
    val downloadedBytes: Long = 0L,
    
    /**
     * Total file size in bytes
     */
    val totalBytes: Long = 0L,
    
    /**
     * Current download speed in bytes/second
     */
    val currentSpeed: Long = 0L,
    
    /**
     * Error message if download failed
     */
    val errorMessage: String? = null,
    
    /**
     * Timestamp when download was created
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when download was started
     */
    val startedAt: Long? = null,
    
    /**
     * Timestamp when download was completed
     */
    val completedAt: Long? = null,
    
    /**
     * Timestamp of last update
     */
    val lastUpdated: Long = System.currentTimeMillis()
)