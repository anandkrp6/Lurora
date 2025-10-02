package com.bytecoder.lurora.backend.services

import android.content.Context
import androidx.work.WorkManager
import com.bytecoder.lurora.backend.database.LuroraDatabase
import com.bytecoder.lurora.backend.models.DownloadStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class DownloadManagerImplTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var database: LuroraDatabase
    
    @Mock
    private lateinit var workManager: WorkManager
    
    private lateinit var downloadManager: DownloadManagerImpl
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        downloadManager = DownloadManagerImpl(context, database, workManager)
    }
    
    @Test
    fun `getAllDownloads should return mock downloads`() = runTest {
        // When
        val downloads = downloadManager.getAllDownloads().first()
        
        // Then
        assertTrue("Downloads list should not be empty", downloads.isNotEmpty())
        assertEquals("Should have 3 mock downloads", 3, downloads.size)
        
        // Verify first download
        val firstDownload = downloads.first()
        assertEquals("Movie_2024_4K.mp4", firstDownload.fileName)
        assertEquals(DownloadStatus.IN_PROGRESS, firstDownload.status)
    }
    
    @Test
    fun `startDownload should return success result`() = runTest {
        // Given
        val url = "https://example.com/video.mp4"
        val title = "Test Video"
        val platform = "YouTube"
        
        // When
        val result = downloadManager.startDownload(url, title, platform)
        
        // Then
        assertTrue("Start download should succeed", result.isSuccess)
        assertNotNull("Download ID should not be null", result.getOrNull())
    }
    
    @Test
    fun `pauseDownload should return success result`() = runTest {
        // Given
        val downloadId = "test-download-id"
        
        // When
        val result = downloadManager.pauseDownload(downloadId)
        
        // Then
        assertTrue("Pause download should succeed", result.isSuccess)
    }
    
    @Test
    fun `getStorageInfo should return valid storage information`() = runTest {
        // When
        val storageInfo = downloadManager.getStorageInfo()
        
        // Then
        assertTrue("Total space should be positive", storageInfo.totalSpace > 0)
        assertTrue("Available space should be positive", storageInfo.availableSpace > 0)
        assertTrue("Used space should be positive", storageInfo.usedSpace > 0)
        assertEquals(
            "Total space should equal used + available",
            storageInfo.totalSpace,
            storageInfo.usedSpace + storageInfo.availableSpace
        )
    }
}