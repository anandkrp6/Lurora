package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.random.Random

/**
 * ViewModel for Downloads screen
 * Manages download queue, progress tracking, and file operations
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    // TODO: Inject download manager service
    // private val downloadManager: DownloadManager
) : ViewModel() {
    
    // State flows for UI
    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()
    
    private val _sortOption = MutableStateFlow(DownloadSortOption.NEWEST_FIRST)
    val sortOption: StateFlow<DownloadSortOption> = _sortOption.asStateFlow()
    
    private val _filterOptions = MutableStateFlow(DownloadFilter())
    val filterOptions: StateFlow<DownloadFilter> = _filterOptions.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _storageInfo = MutableStateFlow(StorageInfo(
        totalSpace = 64_000_000_000L, // 64 GB
        availableSpace = 32_000_000_000L, // 32 GB  
        usedSpace = 32_000_000_000L // 32 GB
    ))
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()
    
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()
    
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    private val _downloadSettings = MutableStateFlow(DownloadSettings())
    val downloadSettings: StateFlow<DownloadSettings> = _downloadSettings.asStateFlow()
    
    init {
        loadDownloads()
        updateStorageInfo()
    }
    
    // Download management
    fun loadDownloads() {
        viewModelScope.launch {
            // TODO: Replace with actual service call
            // val downloads = downloadManager.getAllDownloads()
            val mockDownloads = generateMockDownloads()
            _downloadItems.value = applySorting(mockDownloads)
        }
    }
    
    fun startDownload(url: String, title: String, platform: String) {
        viewModelScope.launch {
            // TODO: Implement actual download start
            // downloadManager.startDownload(url, title, platform)
            loadDownloads()
        }
    }
    
    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            // TODO: Implement actual pause
            // downloadManager.pauseDownload(downloadId)
            updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
        }
    }
    
    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            // TODO: Implement actual resume
            // downloadManager.resumeDownload(downloadId)
            updateDownloadStatus(downloadId, DownloadStatus.IN_PROGRESS)
        }
    }
    
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            // TODO: Implement actual cancel
            // downloadManager.cancelDownload(downloadId)
            updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
        }
    }
    
    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            // TODO: Implement actual retry
            // downloadManager.retryDownload(downloadId)
            updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
        }
    }
    
    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            // TODO: Implement actual delete with file cleanup
            // downloadManager.deleteDownload(downloadId)
            _downloadItems.value = _downloadItems.value.filter { it.id != downloadId }
        }
    }
    
    private fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
        val currentItems = _downloadItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == downloadId }
        if (itemIndex != -1) {
            currentItems[itemIndex] = currentItems[itemIndex].copy(status = status)
            _downloadItems.value = applySorting(currentItems)
        }
    }
    
    // Batch operations
    fun pauseSelected() {
        viewModelScope.launch {
            selectedItems.value.forEach { downloadId ->
                pauseDownload(downloadId)
            }
            clearSelection()
        }
    }
    
    fun resumeSelected() {
        viewModelScope.launch {
            selectedItems.value.forEach { downloadId ->
                resumeDownload(downloadId)
            }
            clearSelection()
        }
    }
    
    fun cancelSelected() {
        viewModelScope.launch {
            selectedItems.value.forEach { downloadId ->
                cancelDownload(downloadId)
            }
            clearSelection()
        }
    }
    
    fun deleteSelected() {
        viewModelScope.launch {
            selectedItems.value.forEach { downloadId ->
                deleteDownload(downloadId)
            }
            clearSelection()
        }
    }
    
    // Selection management
    fun enterSelectionMode(initialId: String) {
        _isSelectionMode.value = true
        _selectedItems.value = setOf(initialId)
    }
    
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }
    
    fun toggleSelection(downloadId: String) {
        val current = _selectedItems.value.toMutableSet()
        if (downloadId in current) {
            current.remove(downloadId)
        } else {
            current.add(downloadId)
        }
        _selectedItems.value = current
        
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }
    
    fun selectAll() {
        _selectedItems.value = _downloadItems.value.map { it.id }.toSet()
    }
    
    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }
    
    // Filtering and sorting
    fun setSortOption(option: DownloadSortOption) {
        _sortOption.value = option
        _downloadItems.value = applySorting(_downloadItems.value)
    }
    
    fun setFilterOptions(filter: DownloadFilter) {
        _filterOptions.value = filter
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    private fun applySorting(items: List<DownloadItem>): List<DownloadItem> {
        return when (_sortOption.value) {
            DownloadSortOption.NEWEST_FIRST -> items.sortedByDescending { it.downloadStarted }
            DownloadSortOption.OLDEST_FIRST -> items.sortedBy { it.downloadStarted }
            DownloadSortOption.NAME_AZ -> items.sortedBy { it.fileName.lowercase() }
            DownloadSortOption.NAME_ZA -> items.sortedByDescending { it.fileName.lowercase() }
            DownloadSortOption.SIZE_LARGE_FIRST -> items.sortedByDescending { it.fileSize }
            DownloadSortOption.SIZE_SMALL_FIRST -> items.sortedBy { it.fileSize }
            DownloadSortOption.STATUS -> items.sortedBy { it.status.ordinal }
        }
    }
    
    // Storage management
    private fun updateStorageInfo() {
        viewModelScope.launch {
            // TODO: Implement actual storage info retrieval
            // val info = storageManager.getStorageInfo()
            val mockInfo = StorageInfo(
                totalSpace = 64L * 1024 * 1024 * 1024, // 64GB
                usedSpace = 45L * 1024 * 1024 * 1024,   // 45GB
                availableSpace = 19L * 1024 * 1024 * 1024 // 19GB
            )
            _storageInfo.value = mockInfo
        }
    }
    
    // Settings management
    fun updateSettings(settings: DownloadSettings) {
        _downloadSettings.value = settings
        // TODO: Persist settings
        // settingsRepository.saveDownloadSettings(settings)
    }
    
    // Progress tracking (would be called by download service)
    fun updateDownloadProgress(downloadId: String, downloadedSize: Long, speed: Long, eta: Long) {
        val currentItems = _downloadItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.id == downloadId }
        if (itemIndex != -1) {
            val item = currentItems[itemIndex]
            currentItems[itemIndex] = item.copy(
                downloadedSize = downloadedSize,
                downloadSpeed = speed,
                eta = eta,
                status = if (downloadedSize >= item.fileSize) DownloadStatus.COMPLETED else DownloadStatus.IN_PROGRESS
            )
            _downloadItems.value = applySorting(currentItems)
        }
    }
    
    // Mock data generation
    private fun generateMockDownloads(): List<DownloadItem> {
        val platforms = listOf("YouTube", "Soundcloud", "Vimeo", "Dailymotion", "Local")
        val fileTypes = listOf("mp4", "mp3", "mkv", "wav", "flac")
        val statuses = DownloadStatus.values()
        
        return (1..20).map { i ->
            val platform = platforms.random()
            val fileType = fileTypes.random()
            val status = statuses.random()
            val fileSize = Random.nextLong(10L * 1024 * 1024, 2L * 1024 * 1024 * 1024)
            val downloadedSize = when (status) {
                DownloadStatus.COMPLETED -> fileSize
                DownloadStatus.IN_PROGRESS -> Random.nextLong((fileSize * 0.1).toLong(), (fileSize * 0.9).toLong())
                DownloadStatus.PAUSED -> Random.nextLong((fileSize * 0.2).toLong(), (fileSize * 0.8).toLong())
                else -> 0L
            }
            
            DownloadItem(
                id = "download_$i",
                fileName = "Video_${i}.${fileType}",
                originalTitle = "Amazing Video Content Title Number ${i}",
                sourcePlatform = platform,
                sourceUrl = "https://${platform.lowercase()}.com/watch?v=example${i}",
                fileType = fileType,
                fileSize = fileSize,
                downloadedSize = downloadedSize,
                downloadSpeed = if (status == DownloadStatus.IN_PROGRESS) Random.nextLong(100L * 1024, 5L * 1024 * 1024) else 0L,
                eta = if (status == DownloadStatus.IN_PROGRESS) ((fileSize - downloadedSize) / 1024 / 1024).coerceAtMost(3600) else 0L,
                status = status,
                downloadStarted = Date(System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L)),
                downloadPath = "/storage/emulated/0/Lurora/Downloads/Video_${i}.${fileType}",
                downloadCompleted = if (status == DownloadStatus.COMPLETED) Date(System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000L) + (2 * 60 * 60 * 1000L)) else null,
                error = if (status == DownloadStatus.FAILED) "Network error occurred" else null,
                thumbnail = null
            )
        }
    }
}