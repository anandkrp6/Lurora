package com.bytecoder.lurora.frontend.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.frontend.navigation.*
import com.bytecoder.lurora.frontend.ui.components.*
import com.bytecoder.lurora.frontend.ui.screens.tabs.*
import com.bytecoder.lurora.frontend.viewmodels.MainViewModel
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.MusicPlayerViewModel
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.backend.models.FileSystemItem
import com.bytecoder.lurora.backend.models.SupportedFormats
import android.net.Uri
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navigationState by viewModel.navigationState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showSearchDialog by viewModel.showSearchDialog.collectAsStateWithLifecycle()
    val showSortDialog by viewModel.showSortDialog.collectAsStateWithLifecycle()
    val showFilterDialog by viewModel.showFilterDialog.collectAsStateWithLifecycle()
    val showViewDialog by viewModel.showViewDialog.collectAsStateWithLifecycle()
    val showVideoPlayer by viewModel.showVideoPlayer.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    // Get instances of media player ViewModels
    val videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel()
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()

    // Media click handlers
    val onVideoClick = { mediaItem: MediaItem ->
        videoPlayerViewModel.playVideo(mediaItem)
        viewModel.showVideoPlayer()
    }

    val onMusicClick = { mediaItem: MediaItem ->
        musicPlayerViewModel.playMediaItem(mediaItem)
        musicPlayerViewModel.expandPlayer()
    }

    // Handle back button behavior
    BackHandler {
        // Custom back button behavior - exit app when on default sections
        val isOnDefaultSection = when (navigationState.currentTab) {
            MainTab.VIDEO -> navigationState.currentVideoSection == VideoSection.LIBRARY
            MainTab.MUSIC -> navigationState.currentMusicSection == MusicSection.LIBRARY
            MainTab.ONLINE -> navigationState.currentOnlineSection == OnlineSection.BROWSE
            MainTab.MORE -> navigationState.currentMoreTab == MoreTab.HISTORY
        }
        
        if (!isOnDefaultSection) {
            // Navigate to default section of current tab
            when (navigationState.currentTab) {
                MainTab.VIDEO -> viewModel.selectVideoSection(VideoSection.LIBRARY)
                MainTab.MUSIC -> viewModel.selectMusicSection(MusicSection.LIBRARY)
                MainTab.ONLINE -> viewModel.selectOnlineSection(OnlineSection.BROWSE)
                MainTab.MORE -> viewModel.selectMoreTab(MoreTab.HISTORY)
            }
        } else {
            // Exit app
            (context as? android.app.Activity)?.finish()
        }
    }

    Scaffold(
        topBar = {
            LuroraTopBar(
                currentTab = navigationState.currentTab,
                onSearchClick = { viewModel.showSearchDialog() },
                onSortClick = { viewModel.showSortDialog() },
                onFilterClick = { viewModel.showFilterDialog() },
                onViewOptionClick = { viewModel.showViewDialog() }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        selected = navigationState.currentTab == tab,
                        onClick = { 
                            viewModel.selectTab(tab)
                            // No special handling for Online tab - let it remember current section
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Content with Horizontal Swipe Navigation
            when (navigationState.currentTab) {
                MainTab.VIDEO -> {
                    VideoTabContent(
                        currentSection = navigationState.currentVideoSection,
                        onSectionChange = { viewModel.selectVideoSection(it) },
                        sortOption = navigationState.videoSortOption,
                        filterOption = navigationState.videoFilterOption,
                        viewOption = navigationState.videoViewOption,
                        onVideoClick = onVideoClick
                    )
                }
                MainTab.MUSIC -> {
                    MusicTabContent(
                        currentSection = navigationState.currentMusicSection,
                        onSectionChange = { viewModel.selectMusicSection(it) },
                        sortOption = navigationState.musicSortOption,
                        filterOption = navigationState.musicFilterOption,
                        viewOption = navigationState.musicViewOption,
                        onMusicClick = onMusicClick
                    )
                }
                MainTab.ONLINE -> {
                    OnlineTabContent(
                        currentSection = navigationState.currentOnlineSection,
                        onSectionChange = { section ->
                            viewModel.selectOnlineSection(section)
                        },
                        sortOption = navigationState.onlineSortOption,
                        filterOption = navigationState.onlineFilterOption,
                        viewOption = navigationState.onlineViewOption
                    )
                }
                MainTab.MORE -> {
                    // Check if we're navigating to a specific screen within MORE tab
                    val lastScreen = navigationState.navigationStack.lastOrNull()
                    when {
                        lastScreen is NavigationScreen.MoreTabPage -> {
                            // Show the specific screen
                            MoreTabScreenRouter(
                                currentMoreTab = lastScreen.moreTab,
                                onNavigateBack = { viewModel.popFromNavigationStack() },
                                onPlayMedia = { media, position -> 
                                    // Handle media playback from More tab sections
                                    when (media.mediaType) {
                                        MediaType.VIDEO -> {
                                            videoPlayerViewModel.playVideo(media)
                                            viewModel.showVideoPlayer()
                                        }
                                        MediaType.AUDIO -> {
                                            musicPlayerViewModel.playMediaItem(media)
                                            musicPlayerViewModel.expandPlayer()
                                        }
                                    }
                                },
                                onOpenFile = { file -> 
                                    // Handle file opening from More tab sections
                                    // Convert FileSystemItem to MediaItem if it's a media file
                                    val mediaItem = convertFileToMediaItem(file)
                                    if (mediaItem != null) {
                                        when (mediaItem.mediaType) {
                                            MediaType.VIDEO -> {
                                                videoPlayerViewModel.playVideo(mediaItem)
                                                viewModel.showVideoPlayer()
                                            }
                                            MediaType.AUDIO -> {
                                                musicPlayerViewModel.playMediaItem(mediaItem)
                                                musicPlayerViewModel.expandPlayer()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        else -> {
                            // Show tab list
                            MoreTabContent(
                                currentMoreTab = navigationState.currentMoreTab,
                                onMoreTabChange = { moreTab ->
                                    viewModel.navigateToScreen(NavigationScreen.MoreTabPage(moreTab))
                                }
                            )
                        }
                    }
                }
            }
            
            // Music Player Overlay - Always available
            MusicPlayerScreen(
                modifier = Modifier.fillMaxSize()
            )
            
            // Video Player Overlay - Shown when video is selected
            if (showVideoPlayer) {
                VideoPlayerScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = videoPlayerViewModel,
                    onBack = { viewModel.hideVideoPlayer() }
                )
            }
        }
    }

    // Dialogs
    if (showSortDialog) {
        SortDialog(
            currentTab = navigationState.currentTab,
            currentSort = viewModel.getCurrentSortOption(),
            onSortSelected = { 
                viewModel.setSortOption(it)
                viewModel.hideSortDialog()
            },
            onDismiss = { viewModel.hideSortDialog() }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = viewModel.getCurrentFilterOption(),
            onFilterSelected = { 
                viewModel.setFilterOption(it)
                viewModel.hideFilterDialog()
            },
            onDismiss = { viewModel.hideFilterDialog() }
        )
    }

    if (showViewDialog) {
        ViewOptionsDialog(
            currentView = viewModel.getCurrentViewOption(),
            onViewSelected = { 
                viewModel.setViewOption(it)
                viewModel.hideViewDialog()
            },
            onDismiss = { viewModel.hideViewDialog() }
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            query = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            onDismiss = { 
                viewModel.hideSearchDialog()
                viewModel.clearSearchQuery()
            },
            onSearch = { 
                // Handle search logic
                viewModel.hideSearchDialog()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSearch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search") },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSearch,
                enabled = query.isNotBlank()
            ) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Helper function to convert FileSystemItem to MediaItem for media playback
 */
private fun convertFileToMediaItem(fileItem: FileSystemItem): MediaItem? {
    // Only convert if it's a media file with media info
    val mediaInfo = fileItem.mediaInfo ?: return null
    
    // Get file extension to determine if supported
    val extension = fileItem.name.substringAfterLast('.', "").lowercase()
    if (!SupportedFormats.isSupported(extension)) {
        return null
    }
    
    return MediaItem(
        id = fileItem.path,
        uri = Uri.parse("file://${fileItem.path}"),
        title = fileItem.name.substringBeforeLast('.'),
        duration = mediaInfo.duration,
        mediaType = mediaInfo.mediaType,
        size = fileItem.size,
        dateAdded = fileItem.lastModified.time,
        mimeType = SupportedFormats.getMimeType(extension),
        metadata = mapOf(
            "file_path" to fileItem.path,
            "extension" to extension,
            "resolution" to (mediaInfo.resolution ?: ""),
            "codec" to (mediaInfo.codec ?: ""),
            "bitrate" to mediaInfo.bitrate.toString(),
            "frame_rate" to mediaInfo.frameRate.toString()
        )
    )
}