package com.bytecoder.lurora.backend.services

import com.bytecoder.lurora.backend.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface for download management operations
 */
interface DownloadManager {
    /**
     * Get all downloads as a flow
     */
    fun getAllDownloads(): Flow<List<DownloadItem>>
    
    /**
     * Start a new download
     */
    suspend fun startDownload(url: String, title: String, platform: String): Result<String>
    
    /**
     * Pause an active download
     */
    suspend fun pauseDownload(downloadId: String): Result<Unit>
    
    /**
     * Resume a paused download
     */
    suspend fun resumeDownload(downloadId: String): Result<Unit>
    
    /**
     * Cancel an active download
     */
    suspend fun cancelDownload(downloadId: String): Result<Unit>
    
    /**
     * Retry a failed download
     */
    suspend fun retryDownload(downloadId: String): Result<Unit>
    
    /**
     * Delete a download and its associated files
     */
    suspend fun deleteDownload(downloadId: String): Result<Unit>
    
    /**
     * Delete multiple downloads
     */
    suspend fun deleteDownloads(downloadIds: List<String>): Result<Unit>
    
    /**
     * Get download progress for a specific download
     */
    fun getDownloadProgress(downloadId: String): Flow<DownloadProgress>
    
    /**
     * Get storage information
     */
    suspend fun getStorageInfo(): StorageInfo
}