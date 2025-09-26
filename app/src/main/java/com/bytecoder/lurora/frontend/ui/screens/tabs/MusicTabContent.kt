package com.bytecoder.lurora.frontend.ui.screens.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bytecoder.lurora.frontend.navigation.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTabContent(
    currentSection: MusicSection,
    onSectionChange: (MusicSection) -> Unit,
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption,
    modifier: Modifier = Modifier
) {
    val sections = MusicSection.values()
    val pagerState = rememberPagerState(
        initialPage = sections.indexOf(currentSection),
        pageCount = { sections.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentSection) {
        val targetPage = sections.indexOf(currentSection)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (sections[page]) {
                MusicSection.LIBRARY -> {
                    MusicLibraryContent(sortOption, filterOption, viewOption)
                }
                MusicSection.PLAYLIST -> {
                    MusicPlaylistContent(sortOption, filterOption, viewOption)
                }
                MusicSection.FAVORITES -> {
                    MusicFavoritesContent(sortOption, filterOption, viewOption)
                }
            }
        }
    }
}

@Composable
fun MusicLibraryContent(
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
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Music Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Songs, Albums, Artists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your music collection will appear here",
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
fun MusicPlaylistContent(
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
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Music Playlists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create and manage your music playlists",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MusicFavoritesContent(
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
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Favorite Music",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your liked songs collection",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicTabContentPreview() {
    MaterialTheme {
        MusicTabContent(
            currentSection = MusicSection.LIBRARY,
            onSectionChange = { },
            sortOption = SortOption.ARTIST_ASC,
            filterOption = FilterOption.ALL,
            viewOption = ViewOption.LIST
        )
    }
}