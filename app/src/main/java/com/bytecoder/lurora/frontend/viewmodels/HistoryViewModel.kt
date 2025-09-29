package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.util.*

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)

    private val _historyEntries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val historyEntries: StateFlow<List<HistoryEntry>> = _historyEntries.asStateFlow()

    private val _sortOption = MutableStateFlow(HistorySortOption.RECENT_FIRST)
    val sortOption: StateFlow<HistorySortOption> = _sortOption.asStateFlow()

    private val _filterOptions = MutableStateFlow(HistoryFilter())
    val filterOptions: StateFlow<HistoryFilter> = _filterOptions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historySettings = MutableStateFlow(HistorySettings())
    val historySettings: StateFlow<HistorySettings> = _historySettings.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    init {
        loadHistorySettings()
        loadHistoryEntries()
        observeSortAndFilter()
    }

    private fun loadHistorySettings() {
        val retentionDays = prefs.getInt("retention_days", 30)
        val trackPrivate = prefs.getBoolean("track_private", false)
        val autoCleanup = prefs.getBoolean("auto_cleanup", true)
        val enableHistory = prefs.getBoolean("enable_history", true)
        
        _historySettings.value = HistorySettings(
            retentionDays = retentionDays,
            trackPrivateSessions = trackPrivate,
            autoCleanupBrokenLinks = autoCleanup,
            enableHistory = enableHistory
        )
    }

    private fun observeSortAndFilter() {
        combine(
            _historyEntries,
            _sortOption,
            _filterOptions,
            _searchQuery
        ) { entries, sort, filter, query ->
            applyFiltersAndSort(entries, sort, filter, query)
        }.onEach { filteredEntries ->
            // Update the displayed entries if needed
        }.launchIn(viewModelScope)
    }

    private fun applyFiltersAndSort(
        entries: List<HistoryEntry>,
        sort: HistorySortOption,
        filter: HistoryFilter,
        query: String
    ): List<HistoryEntry> {
        var filtered = entries

        // Apply media type filter
        if (filter.mediaTypes.isNotEmpty()) {
            filtered = filtered.filter { it.mediaItem.mediaType in filter.mediaTypes }
        }

        // Apply source filter
        if (filter.sources.isNotEmpty()) {
            filtered = filtered.filter { it.source in filter.sources }
        }

        // Apply private session filter
        if (!filter.includePrivateSessions) {
            filtered = filtered.filter { !it.isPrivateSession }
        }

        // Apply date range filter
        filter.dateRange?.let { dateRange ->
            filtered = filtered.filter { entry ->
                entry.lastPlayedAt.after(dateRange.start) && 
                entry.lastPlayedAt.before(dateRange.end)
            }
        }

        // Apply search query
        if (query.isNotEmpty()) {
            filtered = filtered.filter { entry ->
                entry.mediaItem.title.contains(query, ignoreCase = true) ||
                entry.mediaItem.artist?.contains(query, ignoreCase = true) == true
            }
        }

        // Apply sorting
        filtered = when (sort) {
            HistorySortOption.RECENT_FIRST -> filtered.sortedByDescending { it.lastPlayedAt }
            HistorySortOption.OLDEST_FIRST -> filtered.sortedBy { it.lastPlayedAt }
            HistorySortOption.ALPHABETICAL -> filtered.sortedBy { it.mediaItem.title }
            HistorySortOption.REVERSE_ALPHABETICAL -> filtered.sortedByDescending { it.mediaItem.title }
            HistorySortOption.MOST_PLAYED -> filtered.sortedByDescending { it.playCount }
        }

        return filtered
    }

    private fun loadHistoryEntries() {
        viewModelScope.launch {
            // For now, start with empty history
            // In a real app, this would load from a database
            _historyEntries.value = emptyList()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: HistorySortOption) {
        _sortOption.value = option
    }

    fun setFilterOptions(filter: HistoryFilter) {
        _filterOptions.value = filter
    }

    fun updateSettings(settings: HistorySettings) {
        _historySettings.value = settings
        // Save to preferences
        prefs.edit()
            .putInt("retention_days", settings.retentionDays)
            .putBoolean("track_private", settings.trackPrivateSessions)
            .putBoolean("auto_cleanup", settings.autoCleanupBrokenLinks)
            .putBoolean("enable_history", settings.enableHistory)
            .apply()
    }

    /**
     * Add a new history entry when media is played
     */
    fun addHistoryEntry(mediaItem: MediaItem, playbackPosition: Long = 0L, isPrivateSession: Boolean = false) {
        if (!_historySettings.value.enableHistory) return
        
        viewModelScope.launch {
            val currentEntries = _historyEntries.value.toMutableList()
            
            // Check if entry already exists (update instead of duplicate)
            val existingIndex = currentEntries.indexOfFirst { 
                it.mediaItem.uri == mediaItem.uri 
            }
            
            val now = Date()
            val progress = if (mediaItem.duration > 0) {
                playbackPosition.toFloat() / mediaItem.duration.toFloat()
            } else 0f
            
            if (existingIndex != -1) {
                // Update existing entry
                val existingEntry = currentEntries[existingIndex]
                currentEntries[existingIndex] = existingEntry.copy(
                    lastPlayedAt = now,
                    playbackProgress = progress,
                    lastPosition = playbackPosition,
                    playCount = existingEntry.playCount + 1
                )
            } else {
                // Add new entry
                val newEntry = HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    mediaItem = mediaItem,
                    lastPlayedAt = now,
                    playbackProgress = progress,
                    lastPosition = playbackPosition,
                    playCount = 1,
                    source = HistorySource.LOCAL,
                    isPrivateSession = isPrivateSession
                )
                currentEntries.add(0, newEntry) // Add to beginning
            }
            
            // Respect retention settings
            val retentionDays = _historySettings.value.retentionDays
            if (retentionDays > 0) {
                val cutoffTime = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
                val cutoffDate = Date(cutoffTime)
                currentEntries.removeAll { it.lastPlayedAt.before(cutoffDate) }
            }
            
            _historyEntries.value = currentEntries
        }
    }

    /**
     * Update playback progress for an existing entry
     */
    fun updatePlaybackProgress(mediaItem: MediaItem, playbackPosition: Long) {
        if (!_historySettings.value.enableHistory) return
        
        viewModelScope.launch {
            val currentEntries = _historyEntries.value.toMutableList()
            val entryIndex = currentEntries.indexOfFirst { it.mediaItem.uri == mediaItem.uri }
            
            if (entryIndex != -1) {
                val entry = currentEntries[entryIndex]
                val progress = if (mediaItem.duration > 0) {
                    playbackPosition.toFloat() / mediaItem.duration.toFloat()
                } else 0f
                
                currentEntries[entryIndex] = entry.copy(
                    playbackProgress = progress,
                    lastPosition = playbackPosition,
                    lastPlayedAt = Date()
                )
                
                _historyEntries.value = currentEntries
            }
        }
    }

    fun removeEntry(entryId: String) {
        viewModelScope.launch {
            val currentEntries = _historyEntries.value.toMutableList()
            currentEntries.removeAll { it.id == entryId }
            _historyEntries.value = currentEntries
        }
    }

    fun removeEntries(entryIds: Set<String>) {
        viewModelScope.launch {
            val currentEntries = _historyEntries.value.toMutableList()
            currentEntries.removeAll { it.id in entryIds }
            _historyEntries.value = currentEntries
            exitSelectionMode()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _historyEntries.value = emptyList()
            exitSelectionMode()
        }
    }

    fun clearHistoryByType(mediaTypes: Set<MediaType>) {
        viewModelScope.launch {
            val currentEntries = _historyEntries.value.toMutableList()
            currentEntries.removeAll { it.mediaItem.mediaType in mediaTypes }
            _historyEntries.value = currentEntries
        }
    }

    fun enterSelectionMode(initialSelection: String) {
        _isSelectionMode.value = true
        _selectedItems.value = setOf(initialSelection)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun toggleSelection(entryId: String) {
        val currentSelection = _selectedItems.value.toMutableSet()
        if (entryId in currentSelection) {
            currentSelection.remove(entryId)
        } else {
            currentSelection.add(entryId)
        }
        _selectedItems.value = currentSelection
    }

    fun selectAll() {
        val allIds = _historyEntries.value.map { it.id }.toSet()
        _selectedItems.value = allIds
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun deleteSelected() {
        val selectedIds = _selectedItems.value
        removeEntries(selectedIds)
    }
}