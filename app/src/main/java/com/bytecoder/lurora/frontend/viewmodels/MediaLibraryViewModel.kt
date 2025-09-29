package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.backend.repositories.MediaRepository
import com.bytecoder.lurora.backend.utils.ErrorRecoveryManager
import com.bytecoder.lurora.backend.utils.PerformanceManager
import com.bytecoder.lurora.frontend.navigation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val errorRecoveryManager: ErrorRecoveryManager,
    private val performanceManager: PerformanceManager
) : ViewModel() {

    // Use repository flows directly
    val videoFiles: StateFlow<List<MediaItem>> = mediaRepository.getVideoFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val audioFiles: StateFlow<List<MediaItem>> = mediaRepository.getAudioFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteFiles: StateFlow<List<MediaItem>> = mediaRepository.getFavoriteFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOption = MutableStateFlow(com.bytecoder.lurora.frontend.navigation.SortOption.NAME_ASC)
    val sortOption: StateFlow<com.bytecoder.lurora.frontend.navigation.SortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(com.bytecoder.lurora.frontend.navigation.FilterOption.ALL)
    val filterOption: StateFlow<com.bytecoder.lurora.frontend.navigation.FilterOption> = _filterOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        scanMediaFiles()
        observeErrors()
    }

    private fun observeErrors() {
        viewModelScope.launch {
            errorRecoveryManager.lastError.collect { error ->
                _lastError.value = error?.message
            }
        }
    }

    fun scanMediaFiles() {
        viewModelScope.launch(errorRecoveryManager.globalExceptionHandler) {
            _isLoading.value = true
            
            val result = errorRecoveryManager.retryOperation(maxRetries = 3) {
                mediaRepository.scanAndUpdateMediaFiles()
            }
            
            result.onFailure { exception ->
                _lastError.value = "Failed to scan media files: ${exception.message}"
            }
            
            _isLoading.value = false
        }
    }

    fun refreshLibrary() {
        clearError()
        scanMediaFiles()
    }

    fun setSortOption(option: com.bytecoder.lurora.frontend.navigation.SortOption) {
        _sortOption.value = option
    }

    fun setFilterOption(option: com.bytecoder.lurora.frontend.navigation.FilterOption) {
        _filterOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _lastError.value = null
        errorRecoveryManager.clearLastError()
    }

    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch(errorRecoveryManager.globalExceptionHandler) {
            mediaRepository.toggleFavorite(mediaItem)
        }
    }

    fun updatePlaybackInfo(mediaItem: MediaItem, position: Long) {
        viewModelScope.launch(errorRecoveryManager.globalExceptionHandler) {
            mediaRepository.updatePlaybackInfo(mediaItem, position)
        }
    }

    fun getFilteredVideos(): StateFlow<List<MediaItem>> {
        return combine(
            videoFiles,
            _sortOption,
            _filterOption,
            _searchQuery
        ) { videos, sort, filter, search ->
            applyFiltersAndSort(videos, sort, filter, search)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getFilteredAudios(): StateFlow<List<MediaItem>> {
        return combine(
            audioFiles,
            _sortOption,
            _filterOption,
            _searchQuery
        ) { audios, sort, filter, search ->
            applyFiltersAndSort(audios, sort, filter, search)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun preloadThumbnails(items: List<MediaItem>) {
        val uris = items.mapNotNull { it.albumArtUri }
        performanceManager.preloadImages(uris)
    }

    override fun onCleared() {
        super.onCleared()
        performanceManager.cleanup()
    }

    private fun applyFiltersAndSort(
        items: List<MediaItem>,
        sort: com.bytecoder.lurora.frontend.navigation.SortOption,
        filter: com.bytecoder.lurora.frontend.navigation.FilterOption,
        search: String
    ): List<MediaItem> {
        var filtered = items

        // Apply search filter
        if (search.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.title.contains(search, ignoreCase = true) ||
                item.artist?.contains(search, ignoreCase = true) == true ||
                item.album?.contains(search, ignoreCase = true) == true
            }
        }

        // Apply file type filter
        filtered = when (filter) {
            com.bytecoder.lurora.frontend.navigation.FilterOption.ALL -> filtered
            com.bytecoder.lurora.frontend.navigation.FilterOption.RECENT -> filtered.sortedByDescending { it.dateAdded }.take(50)
            com.bytecoder.lurora.frontend.navigation.FilterOption.FAVORITES -> filtered.filter { it.isFavorite }
            com.bytecoder.lurora.frontend.navigation.FilterOption.DOWNLOADED -> filtered
        }

        // Apply sorting
        filtered = when (sort) {
            com.bytecoder.lurora.frontend.navigation.SortOption.NAME_ASC -> filtered.sortedBy { it.title }
            com.bytecoder.lurora.frontend.navigation.SortOption.NAME_DESC -> filtered.sortedByDescending { it.title }
            com.bytecoder.lurora.frontend.navigation.SortOption.DATE_ADDED_ASC -> filtered.sortedBy { it.dateAdded }
            com.bytecoder.lurora.frontend.navigation.SortOption.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAdded }
            com.bytecoder.lurora.frontend.navigation.SortOption.SIZE_ASC -> filtered.sortedBy { it.size }
            com.bytecoder.lurora.frontend.navigation.SortOption.SIZE_DESC -> filtered.sortedByDescending { it.size }
            com.bytecoder.lurora.frontend.navigation.SortOption.DURATION_ASC -> filtered.sortedBy { it.duration }
            com.bytecoder.lurora.frontend.navigation.SortOption.DURATION_DESC -> filtered.sortedByDescending { it.duration }
            com.bytecoder.lurora.frontend.navigation.SortOption.ARTIST_ASC -> filtered.sortedBy { it.artist ?: "" }
            com.bytecoder.lurora.frontend.navigation.SortOption.ARTIST_DESC -> filtered.sortedByDescending { it.artist ?: "" }
            com.bytecoder.lurora.frontend.navigation.SortOption.ALBUM_ASC -> filtered.sortedBy { it.album ?: "" }
            com.bytecoder.lurora.frontend.navigation.SortOption.ALBUM_DESC -> filtered.sortedByDescending { it.album ?: "" }
        }

        return filtered
    }
}