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
    
    val context = LocalContext.current

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
                        viewOption = navigationState.videoViewOption
                    )
                }
                MainTab.MUSIC -> {
                    MusicTabContent(
                        currentSection = navigationState.currentMusicSection,
                        onSectionChange = { viewModel.selectMusicSection(it) },
                        sortOption = navigationState.musicSortOption,
                        filterOption = navigationState.musicFilterOption,
                        viewOption = navigationState.musicViewOption
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
                                    // TODO: Implement media playback
                                },
                                onOpenFile = { file -> 
                                    // TODO: Implement file opening
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