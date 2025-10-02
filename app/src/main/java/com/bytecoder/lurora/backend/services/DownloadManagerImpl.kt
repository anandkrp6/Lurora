package com.bytecoder.lurora.backend.services

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.work.*
import com.bytecoder.lurora.backend.database.LuroraDatabase
import com.bytecoder.lurora.backend.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DownloadManager using Android WorkManager for background downloads
 */
@Singleton
class DownloadManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LuroraDatabase,
    private val workManager: WorkManager
) : DownloadManager {
    
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    
    override fun getAllDownloads(): Flow<List<DownloadItem>> {
        // For now, return mock data. In real implementation, this would query the database
        return flow {
            emit(generateMockDownloads())
        }
    }
    
    override suspend fun startDownload(url: String, title: String, platform: String): Result<String> {
        return try {
            val downloadId = UUID.randomUUID().toString()
            
            // Create download work request
            val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(
                    "download_id" to downloadId,
                    "url" to url,
                    "title" to title,
                    "platform" to platform
                ))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag("download_$downloadId")
                .build()
            
            workManager.enqueue(downloadWorkRequest)
            
            Result.success(downloadId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun pauseDownload(downloadId: String): Result<Unit> {
        return try {
            workManager.cancelAllWorkByTag("download_$downloadId")
            updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumeDownload(downloadId: String): Result<Unit> {
        return try {
            // Get the download item and restart the work
            val downloadItem = getDownloadById(downloadId)
            if (downloadItem != null) {
                startDownload(downloadItem.sourceUrl, downloadItem.fileName, downloadItem.sourcePlatform)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelDownload(downloadId: String): Result<Unit> {
        return try {
            workManager.cancelAllWorkByTag("download_$downloadId")
            updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun retryDownload(downloadId: String): Result<Unit> {
        return try {
            val downloadItem = getDownloadById(downloadId)
            if (downloadItem != null) {
                updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
                startDownload(downloadItem.sourceUrl, downloadItem.fileName, downloadItem.sourcePlatform)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteDownload(downloadId: String): Result<Unit> {
        return try {
            // Cancel any ongoing work
            workManager.cancelAllWorkByTag("download_$downloadId")
            
            // Delete the file if it exists
            val downloadItem = getDownloadById(downloadId)
            downloadItem?.let { item ->
                val file = File(item.downloadPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            
            // Remove from database (mock implementation)
            // In real implementation: database.downloadDao().deleteDownload(downloadId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteDownloads(downloadIds: List<String>): Result<Unit> {
        return try {
            downloadIds.forEach { id ->
                deleteDownload(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getDownloadProgress(downloadId: String): Flow<DownloadProgress> {
        return _downloadProgress.map { progressMap ->
            progressMap[downloadId] ?: DownloadProgress(
                downloadId = downloadId,
                downloadedBytes = 0L,
                totalBytes = 0L,
                speed = 0L,
                eta = 0L
            )
        }
    }
    
    override suspend fun getStorageInfo(): StorageInfo {
        return try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val stat = StatFs(downloadDir.path)
            
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - availableBytes
            
            StorageInfo(
                totalSpace = totalBytes,
                usedSpace = usedBytes,
                availableSpace = availableBytes
            )
        } catch (e: Exception) {
            // Fallback to mock data
            StorageInfo(
                totalSpace = 64L * 1024 * 1024 * 1024, // 64GB
                usedSpace = 45L * 1024 * 1024 * 1024,   // 45GB
                availableSpace = 19L * 1024 * 1024 * 1024 // 19GB
            )
        }
    }
    
    private suspend fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
        // In real implementation, update the database
        // database.downloadDao().updateDownloadStatus(downloadId, status)
    }
    
    private suspend fun getDownloadById(downloadId: String): DownloadItem? {
        // In real implementation, query the database
        // return database.downloadDao().getDownloadById(downloadId)
        return generateMockDownloads().find { it.id == downloadId }
    }
    
    // Mock data for testing
    private fun generateMockDownloads(): List<DownloadItem> {
        return listOf(
            DownloadItem(
                id = "1",
                fileName = "Movie_2024_4K.mp4",
                originalTitle = "Movie 2024 4K",
                fileType = "mp4",
                fileSize = 2_500_000_000L,
                downloadedSize = 1_800_000_000L,
                status = DownloadStatus.IN_PROGRESS,
                sourceUrl = "https://example.com/movie.mp4",
                sourcePlatform = "YouTube",
                downloadPath = "/storage/emulated/0/Download/Movie_2024_4K.mp4",
                downloadStarted = Date(System.currentTimeMillis() - 1800000),
                downloadCompleted = null,
                downloadSpeed = 2_500_000L,
                eta = 280000L
            ),
            DownloadItem(
                id = "2", 
                fileName = "Song_Collection.zip",
                originalTitle = "Song Collection",
                fileType = "zip",
                fileSize = 150_000_000L,
                downloadedSize = 150_000_000L,
                status = DownloadStatus.COMPLETED,
                sourceUrl = "https://example.com/songs.zip",
                sourcePlatform = "SoundCloud",
                downloadPath = "/storage/emulated/0/Download/Song_Collection.zip",
                downloadStarted = Date(System.currentTimeMillis() - 7200000),
                downloadCompleted = Date(System.currentTimeMillis() - 3600000),
                downloadSpeed = 0L,
                eta = 0L
            ),
            DownloadItem(
                id = "3",
                fileName = "Podcast_Episode_045.mp3",
                originalTitle = "Podcast Episode 045",
                fileType = "mp3",
                fileSize = 75_000_000L,
                downloadedSize = 25_000_000L,
                status = DownloadStatus.PAUSED,
                sourceUrl = "https://example.com/podcast.mp3",
                sourcePlatform = "Spotify",
                downloadPath = "/storage/emulated/0/Download/Podcast_Episode_045.mp3",
                downloadStarted = Date(System.currentTimeMillis() - 900000),
                downloadCompleted = null,
                downloadSpeed = 0L,
                eta = 0L
            )
        )
    }
}

/**
 * WorkManager Worker for handling downloads
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        val url = inputData.getString("url") ?: return Result.failure()
        val title = inputData.getString("title") ?: return Result.failure()
        val platform = inputData.getString("platform") ?: return Result.failure()
        
        return try {
            // Simulate download progress
            // In real implementation, this would use OkHttp or similar to download the file
            for (progress in 0..100 step 10) {
                setProgress(workDataOf("progress" to progress))
                kotlinx.coroutines.delay(1000) // Simulate download time
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}