package com.bytecoder.lurora.frontend.ui.screens.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytecoder.lurora.frontend.navigation.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoTabContent(
    currentSection: VideoSection,
    onSectionChange: (VideoSection) -> Unit,
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption,
    modifier: Modifier = Modifier
) {
    val sections = VideoSection.values()
    val pagerState = rememberPagerState(
        initialPage = sections.indexOf(currentSection),
        pageCount = { sections.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync pager with section changes
    LaunchedEffect(currentSection) {
        val targetPage = sections.indexOf(currentSection)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Update section when pager changes
    LaunchedEffect(pagerState.currentPage) {
        onSectionChange(sections[pagerState.currentPage])
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Section Tabs - Full Width
        TabRow(
            selectedTabIndex = sections.indexOf(currentSection),
            modifier = Modifier.fillMaxWidth()
        ) {
            sections.forEach { section ->
                Tab(
                    selected = currentSection == section,
                    onClick = {
                        onSectionChange(section)
                        coroutineScope.launch {
                            pagerState.scrollToPage(sections.indexOf(section)) // Direct scroll, no animation
                        }
                    },
                    text = {
                        Text(
                            text = section.title,
                            fontWeight = if (currentSection == section) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Content Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (sections[page]) {
                VideoSection.LIBRARY -> {
                    VideoLibraryContent(sortOption, filterOption, viewOption)
                }
                VideoSection.PLAYLIST -> {
                    VideoPlaylistContent(sortOption, filterOption, viewOption)
                }
                VideoSection.FAVORITES -> {
                    VideoFavoritesContent(sortOption, filterOption, viewOption)
                }
            }
        }
    }
}

@Composable
fun VideoLibraryContent(
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Video Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your video collection will appear here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Sort: ${sortOption.title} | Filter: ${filterOption.title} | View: ${viewOption.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun VideoPlaylistContent(
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Video Playlists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create and manage your video playlists",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun VideoFavoritesContent(
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Favorite Videos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your liked videos collection",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoTabContentPreview() {
    MaterialTheme {
        VideoTabContent(
            currentSection = VideoSection.LIBRARY,
            onSectionChange = { },
            sortOption = SortOption.NAME_ASC,
            filterOption = FilterOption.ALL,
            viewOption = ViewOption.GRID
        )
    }
}