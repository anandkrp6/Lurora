package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.frontend.navigation.*
import com.bytecoder.lurora.storage.preferences.AppPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferencesManager: AppPreferencesManager
) : ViewModel() {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSearchDialog = MutableStateFlow(false)
    val showSearchDialog: StateFlow<Boolean> = _showSearchDialog.asStateFlow()

    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog: StateFlow<Boolean> = _showSortDialog.asStateFlow()

    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()

    private val _showViewDialog = MutableStateFlow(false)
    val showViewDialog: StateFlow<Boolean> = _showViewDialog.asStateFlow()

    private val _showVideoPlayer = MutableStateFlow(false)
    val showVideoPlayer: StateFlow<Boolean> = _showVideoPlayer.asStateFlow()

    init {
        loadSavedPreferences()
    }

    private fun loadSavedPreferences() {
        viewModelScope.launch {
            // Load last used tab (default to VIDEO if not saved)
            // This would be implemented with DataStore preferences
            // For now, keeping default state
        }
    }

    // Tab Navigation
    fun selectTab(tab: MainTab) {
        _navigationState.value = _navigationState.value.copy(currentTab = tab)
        saveLastUsedTab(tab)
    }

    private fun saveLastUsedTab(tab: MainTab) {
        viewModelScope.launch {
            // Save the last used tab to preferences
            // Implementation would use DataStore
        }
    }

    // Section Navigation within tabs
    fun selectVideoSection(section: VideoSection) {
        _navigationState.value = _navigationState.value.copy(currentVideoSection = section)
    }

    fun selectMusicSection(section: MusicSection) {
        _navigationState.value = _navigationState.value.copy(currentMusicSection = section)
    }

    fun selectOnlineSection(section: OnlineSection) {
        _navigationState.value = _navigationState.value.copy(currentOnlineSection = section)
    }

    fun selectMoreTab(moreTab: MoreTab) {
        _navigationState.value = _navigationState.value.copy(currentMoreTab = moreTab)
    }

    // Get current section based on active tab
    fun getCurrentSection(): String {
        return when (_navigationState.value.currentTab) {
            MainTab.VIDEO -> _navigationState.value.currentVideoSection.title
            MainTab.MUSIC -> _navigationState.value.currentMusicSection.title
            MainTab.ONLINE -> _navigationState.value.currentOnlineSection.title
            MainTab.MORE -> _navigationState.value.currentMoreTab.title
        }
    }

    // Sort Options
    fun setSortOption(sortOption: SortOption) {
        val currentState = _navigationState.value
        val updatedState = when (currentState.currentTab) {
            MainTab.VIDEO -> currentState.copy(videoSortOption = sortOption)
            MainTab.MUSIC -> currentState.copy(musicSortOption = sortOption)
            MainTab.ONLINE -> currentState.copy(onlineSortOption = sortOption)
            MainTab.MORE -> currentState // More doesn't have sort
        }
        _navigationState.value = updatedState
        saveSortPreference(currentState.currentTab, sortOption)
    }

    private fun saveSortPreference(tab: MainTab, sortOption: SortOption) {
        viewModelScope.launch {
            // Save sort preference for the specific tab
            // Implementation would use DataStore
        }
    }

    fun getCurrentSortOption(): SortOption {
        return when (_navigationState.value.currentTab) {
            MainTab.VIDEO -> _navigationState.value.videoSortOption
            MainTab.MUSIC -> _navigationState.value.musicSortOption
            MainTab.ONLINE -> _navigationState.value.onlineSortOption
            MainTab.MORE -> SortOption.NAME_ASC // Default fallback
        }
    }

    // Filter Options
    fun setFilterOption(filterOption: FilterOption) {
        val currentState = _navigationState.value
        val updatedState = when (currentState.currentTab) {
            MainTab.VIDEO -> currentState.copy(videoFilterOption = filterOption)
            MainTab.MUSIC -> currentState.copy(musicFilterOption = filterOption)
            MainTab.ONLINE -> currentState.copy(onlineFilterOption = filterOption)
            MainTab.MORE -> currentState
        }
        _navigationState.value = updatedState
    }

    fun getCurrentFilterOption(): FilterOption {
        return when (_navigationState.value.currentTab) {
            MainTab.VIDEO -> _navigationState.value.videoFilterOption
            MainTab.MUSIC -> _navigationState.value.musicFilterOption
            MainTab.ONLINE -> _navigationState.value.onlineFilterOption
            MainTab.MORE -> FilterOption.ALL // Default fallback
        }
    }

    // View Options
    fun setViewOption(viewOption: ViewOption) {
        val currentState = _navigationState.value
        val updatedState = when (currentState.currentTab) {
            MainTab.VIDEO -> currentState.copy(videoViewOption = viewOption)
            MainTab.MUSIC -> currentState.copy(musicViewOption = viewOption)
            MainTab.ONLINE -> currentState.copy(onlineViewOption = viewOption)
            MainTab.MORE -> currentState
        }
        _navigationState.value = updatedState
    }

    fun getCurrentViewOption(): ViewOption {
        return when (_navigationState.value.currentTab) {
            MainTab.VIDEO -> _navigationState.value.videoViewOption
            MainTab.MUSIC -> _navigationState.value.musicViewOption
            MainTab.ONLINE -> _navigationState.value.onlineViewOption
            MainTab.MORE -> ViewOption.LIST // Default fallback
        }
    }

    // Search
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    // Dialog State Management
    fun showSearchDialog() {
        _showSearchDialog.value = true
    }

    fun hideSearchDialog() {
        _showSearchDialog.value = false
    }

    fun showSortDialog() {
        _showSortDialog.value = true
    }

    fun hideSortDialog() {
        _showSortDialog.value = false
    }

    fun showFilterDialog() {
        _showFilterDialog.value = true
    }

    fun hideFilterDialog() {
        _showFilterDialog.value = false
    }

    fun showViewDialog() {
        _showViewDialog.value = true
    }

    fun hideViewDialog() {
        _showViewDialog.value = false
    }

    // Navigation within sections (for swipe navigation)
    fun navigateToNextSection() {
        when (_navigationState.value.currentTab) {
            MainTab.VIDEO -> {
                val sections = VideoSection.values()
                val currentIndex = sections.indexOf(_navigationState.value.currentVideoSection)
                val nextIndex = (currentIndex + 1) % sections.size
                selectVideoSection(sections[nextIndex])
            }
            MainTab.MUSIC -> {
                val sections = MusicSection.values()
                val currentIndex = sections.indexOf(_navigationState.value.currentMusicSection)
                val nextIndex = (currentIndex + 1) % sections.size
                selectMusicSection(sections[nextIndex])
            }
            MainTab.ONLINE -> {
                val sections = OnlineSection.values()
                val currentIndex = sections.indexOf(_navigationState.value.currentOnlineSection)
                val nextIndex = (currentIndex + 1) % sections.size
                _navigationState.value = _navigationState.value.copy(currentOnlineSection = sections[nextIndex])
            }
            MainTab.MORE -> {
                val tabs = MoreTab.values()
                val currentIndex = tabs.indexOf(_navigationState.value.currentMoreTab)
                val nextIndex = (currentIndex + 1) % tabs.size
                selectMoreTab(tabs[nextIndex])
            }
        }
    }

    fun navigateToPreviousSection() {
        when (_navigationState.value.currentTab) {
            MainTab.VIDEO -> {
                val sections = VideoSection.values()
                val currentIndex = sections.indexOf(_navigationState.value.currentVideoSection)
                val previousIndex = if (currentIndex == 0) sections.size - 1 else currentIndex - 1
                selectVideoSection(sections[previousIndex])
            }
            MainTab.MUSIC -> {
                val sections = MusicSection.values()
                val currentIndex = sections.indexOf(_navigationState.value.currentMusicSection)
                val previousIndex = if (currentIndex == 0) sections.size - 1 else currentIndex - 1
                selectMusicSection(sections[previousIndex])
            }
            MainTab.ONLINE -> {
                val sections = OnlineSection.values()
                val currentIndex = sections.indexOf(_navigationState.value.currentOnlineSection)
                val previousIndex = if (currentIndex == 0) sections.size - 1 else currentIndex - 1
                _navigationState.value = _navigationState.value.copy(currentOnlineSection = sections[previousIndex])
            }
            MainTab.MORE -> {
                val tabs = MoreTab.values()
                val currentIndex = tabs.indexOf(_navigationState.value.currentMoreTab)
                val previousIndex = if (currentIndex == 0) tabs.size - 1 else currentIndex - 1
                selectMoreTab(tabs[previousIndex])
            }
        }
    }

    // Advanced Navigation Functions
    fun navigateToScreen(screen: NavigationScreen) {
        val updatedState = NavigationHelper.navigateToScreen(_navigationState.value, screen)
        _navigationState.value = updatedState
    }

    fun pushToNavigationStack(screen: NavigationScreen) {
        val newStack = NavigationHelper.pushToStack(_navigationState.value.navigationStack, screen)
        val newBreadcrumbs = NavigationHelper.generateBreadcrumbs(newStack)
        _navigationState.value = _navigationState.value.copy(
            navigationStack = newStack,
            breadcrumbs = newBreadcrumbs
        )
    }

    fun popFromNavigationStack(): NavigationScreen? {
        val currentStack = _navigationState.value.navigationStack
        if (currentStack.isEmpty()) return null
        
        val newStack = NavigationHelper.popFromStack(currentStack)
        val newBreadcrumbs = NavigationHelper.generateBreadcrumbs(newStack)
        val poppedScreen = currentStack.lastOrNull()
        
        _navigationState.value = _navigationState.value.copy(
            navigationStack = newStack,
            breadcrumbs = newBreadcrumbs
        )
        
        return poppedScreen
    }

    fun generateBreadcrumbs(): List<BreadcrumbItem> {
        return NavigationHelper.generateBreadcrumbs(_navigationState.value.navigationStack)
    }

    fun handleDeepLink(url: String): Boolean {
        val screen = DeepLinkPatterns.parseDeepLink(url)
        return if (screen != null) {
            navigateToScreen(screen)
            true
        } else {
            false
        }
    }

    fun getCurrentNavigationStack(): List<NavigationScreen> {
        return _navigationState.value.navigationStack
    }

    fun clearNavigationStack() {
        _navigationState.value = _navigationState.value.copy(navigationStack = emptyList())
    }

    // MoreTab Feature Support
    fun getCurrentMoreTabFeatures(): Set<String> {
        return NavigationHelper.getMoreTabFeatures(_navigationState.value.currentMoreTab)
    }

    fun isMoreTabFeatureEnabled(feature: String): Boolean {
        return getCurrentMoreTabFeatures().contains(feature)
    }

    // Individual MoreTab feature handlers
    fun refreshMoreTabContent() {
        val currentTab = _navigationState.value.currentMoreTab
        if (currentTab.hasRefresh) {
            // Refresh logic for the specific tab
            // Implementation will be added when creating individual tab screens
        }
    }

    fun exportMoreTabData() {
        val currentTab = _navigationState.value.currentMoreTab
        if (currentTab.hasExport) {
            // Export logic for the specific tab
            // Implementation will be added when creating individual tab screens
        }
    }

    fun openMoreTabSettings() {
        val currentTab = _navigationState.value.currentMoreTab
        if (currentTab.hasSettings) {
            // Settings logic for the specific tab
            // Implementation will be added when creating individual tab screens
        }
    }

    // Video Player Navigation
    fun showVideoPlayer() {
        _showVideoPlayer.value = true
    }
    
    fun hideVideoPlayer() {
        _showVideoPlayer.value = false
    }
}