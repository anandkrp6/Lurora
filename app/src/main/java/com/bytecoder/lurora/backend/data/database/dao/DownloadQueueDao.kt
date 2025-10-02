package com.bytecoder.lurora.backend.data.database.dao

import androidx.room.*
import com.bytecoder.lurora.backend.data.database.entity.DownloadQueue
import com.bytecoder.lurora.backend.download.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for download queue
 */
@Dao
interface DownloadQueueDao {
    
    /**
     * Insert a new download into the queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadQueue)
    
    /**
     * Update an existing download
     */
    @Update
    suspend fun updateDownload(download: DownloadQueue)
    
    /**
     * Delete a download from the queue
     */
    @Delete
    suspend fun deleteDownload(download: DownloadQueue)
    
    /**
     * Get download by ID
     */
    @Query("SELECT * FROM download_queue WHERE id = :downloadId")
    suspend fun getDownloadById(downloadId: String): DownloadQueue?
    
    /**
     * Get download by ID as Flow for reactive updates
     */
    @Query("SELECT * FROM download_queue WHERE id = :downloadId")
    fun getDownloadFlow(downloadId: String): Flow<DownloadQueue?>
    
    /**
     * Get all downloads
     */
    @Query("SELECT * FROM download_queue ORDER BY priority DESC, createdAt ASC")
    suspend fun getAllDownloads(): List<DownloadQueue>
    
    /**
     * Get all downloads as Flow
     */
    @Query("SELECT * FROM download_queue ORDER BY priority DESC, createdAt ASC")
    fun getAllDownloadsFlow(): Flow<List<DownloadQueue>>
    
    /**
     * Get downloads by status
     */
    @Query("SELECT * FROM download_queue WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getDownloadsByStatus(status: DownloadStatus): List<DownloadQueue>
    
    /**
     * Get downloads by status as Flow
     */
    @Query("SELECT * FROM download_queue WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getDownloadsByStatusFlow(status: DownloadStatus): Flow<List<DownloadQueue>>
    
    /**
     * Get queued downloads (ready to start)
     */
    @Query("SELECT * FROM download_queue WHERE status = :status AND scheduledTime <= :currentTime ORDER BY priority DESC, createdAt ASC")
    suspend fun getQueuedDownloads(
        status: DownloadStatus = DownloadStatus.QUEUED,
        currentTime: Long = System.currentTimeMillis()
    ): List<DownloadQueue>
    
    /**
     * Get active downloads (currently downloading)
     */
    @Query("SELECT * FROM download_queue WHERE status = :status")
    suspend fun getActiveDownloads(status: DownloadStatus = DownloadStatus.DOWNLOADING): List<DownloadQueue>
    
    /**
     * Get completed downloads
     */
    @Query("SELECT * FROM download_queue WHERE status = :status ORDER BY completedAt DESC")
    suspend fun getCompletedDownloads(status: DownloadStatus = DownloadStatus.COMPLETED): List<DownloadQueue>
    
    /**
     * Get failed downloads
     */
    @Query("SELECT * FROM download_queue WHERE status = :status ORDER BY lastUpdated DESC")
    suspend fun getFailedDownloads(status: DownloadStatus = DownloadStatus.FAILED): List<DownloadQueue>
    
    /**
     * Get downloads requiring retry
     */
    @Query("SELECT * FROM download_queue WHERE status = :status AND retryCount < maxRetries")
    suspend fun getDownloadsForRetry(status: DownloadStatus = DownloadStatus.FAILED): List<DownloadQueue>
    
    /**
     * Update download progress
     */
    @Query("UPDATE download_queue SET downloadedBytes = :downloadedBytes, currentSpeed = :currentSpeed, lastUpdated = :timestamp WHERE id = :downloadId")
    suspend fun updateDownloadProgress(
        downloadId: String,
        downloadedBytes: Long,
        currentSpeed: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Update download status
     */
    @Query("UPDATE download_queue SET status = :status, lastUpdated = :timestamp WHERE id = :downloadId")
    suspend fun updateDownloadStatus(
        downloadId: String,
        status: DownloadStatus,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Mark download as started
     */
    @Query("UPDATE download_queue SET status = :status, startedAt = :timestamp, lastUpdated = :timestamp WHERE id = :downloadId")
    suspend fun markDownloadStarted(
        downloadId: String,
        status: DownloadStatus = DownloadStatus.DOWNLOADING,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Mark download as completed
     */
    @Query("UPDATE download_queue SET status = :status, completedAt = :timestamp, lastUpdated = :timestamp WHERE id = :downloadId")
    suspend fun markDownloadCompleted(
        downloadId: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Mark download as failed with error message
     */
    @Query("UPDATE download_queue SET status = :status, errorMessage = :errorMessage, lastUpdated = :timestamp WHERE id = :downloadId")
    suspend fun markDownloadFailed(
        downloadId: String,
        errorMessage: String,
        status: DownloadStatus = DownloadStatus.FAILED,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Get count of downloads by status
     */
    @Query("SELECT COUNT(*) FROM download_queue WHERE status = :status")
    suspend fun getDownloadCountByStatus(status: DownloadStatus): Int
    
    /**
     * Get total downloaded bytes
     */
    @Query("SELECT SUM(downloadedBytes) FROM download_queue WHERE status = :status")
    suspend fun getTotalDownloadedBytes(status: DownloadStatus = DownloadStatus.COMPLETED): Long?
    
    /**
     * Get downloads by URL (to check for duplicates)
     */
    @Query("SELECT * FROM download_queue WHERE url = :url")
    suspend fun getDownloadsByUrl(url: String): List<DownloadQueue>
    
    /**
     * Delete completed downloads older than specified time
     */
    @Query("DELETE FROM download_queue WHERE status = :status AND completedAt < :cutoffTime")
    suspend fun deleteOldCompletedDownloads(
        cutoffTime: Long,
        status: DownloadStatus = DownloadStatus.COMPLETED
    )
    
    /**
     * Delete failed downloads older than specified time
     */
    @Query("DELETE FROM download_queue WHERE status = :status AND lastUpdated < :cutoffTime")
    suspend fun deleteOldFailedDownloads(
        cutoffTime: Long,
        status: DownloadStatus = DownloadStatus.FAILED
    )
    
    /**
     * Clear all downloads
     */
    @Query("DELETE FROM download_queue")
    suspend fun clearAllDownloads()
    
    /**
     * Get download statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN status = 'QUEUED' THEN 1 END) as queued,
            COUNT(CASE WHEN status = 'DOWNLOADING' THEN 1 END) as downloading,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
            COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed,
            COUNT(CASE WHEN status = 'PAUSED' THEN 1 END) as paused,
            COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled,
            SUM(CASE WHEN status = 'COMPLETED' THEN downloadedBytes ELSE 0 END) as totalDownloadedBytes
        FROM download_queue
    """)
    suspend fun getDownloadStatistics(): DownloadStatistics
    
    /**
     * Get recent downloads (last 24 hours)
     */
    @Query("SELECT * FROM download_queue WHERE createdAt > :since ORDER BY createdAt DESC")
    suspend fun getRecentDownloads(since: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000): List<DownloadQueue>
    
    /**
     * Search downloads by filename or URL
     */
    @Query("SELECT * FROM download_queue WHERE fileName LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchDownloads(query: String): List<DownloadQueue>
}

/**
 * Data class for download statistics
 */
data class DownloadStatistics(
    val total: Int,
    val queued: Int,
    val downloading: Int,
    val completed: Int,
    val failed: Int,
    val paused: Int,
    val cancelled: Int,
    val totalDownloadedBytes: Long
)