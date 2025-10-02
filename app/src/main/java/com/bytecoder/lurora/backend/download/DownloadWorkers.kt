package com.bytecoder.lurora.backend.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for scheduled downloads
 */
@HiltWorker
class ScheduledDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadManager: AdvancedDownloadManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        
        return try {
            val success = downloadManager.startDownload(downloadId)
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            android.util.Log.e("ScheduledDownloadWorker", "Failed to start scheduled download", e)
            Result.failure()
        }
    }
}

/**
 * WorkManager worker for retry downloads
 */
@HiltWorker
class RetryDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadManager: AdvancedDownloadManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        
        return try {
            val success = downloadManager.retryDownload(downloadId)
            if (success) Result.success() else Result.failure()
        } catch (e: Exception) {
            android.util.Log.e("RetryDownloadWorker", "Failed to retry download", e)
            Result.failure()
        }
    }
}