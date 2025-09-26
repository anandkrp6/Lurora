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
import com.bytecoder.lurora.frontend.navigation.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnlineTabContent(
    currentSection: OnlineSection,
    onSectionChange: (OnlineSection) -> Unit,
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption,
    modifier: Modifier = Modifier
) {
    val sections = OnlineSection.values()
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
                OnlineSection.BROWSE -> {
                    OnlineBrowseContent(sortOption, filterOption, viewOption)
                }
                OnlineSection.PLAYLIST -> {
                    OnlinePlaylistContent(sortOption, filterOption, viewOption)
                }
                OnlineSection.FAVORITES -> {
                    OnlineFavoritesContent(sortOption, filterOption, viewOption)
                }
            }
        }
    }
}

@Composable
fun OnlineBrowseContent(
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
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Browse Online",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Discover new content online",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            // Search bar placeholder
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Search online content...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                enabled = false // Placeholder for now
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
fun OnlinePlaylistContent(
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
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Online Playlists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your online playlists and collections",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun OnlineFavoritesContent(
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
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Online Favorites",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your favorite online content",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnlineTabContentPreview() {
    MaterialTheme {
        OnlineTabContent(
            currentSection = OnlineSection.BROWSE,
            onSectionChange = { },
            sortOption = SortOption.NAME_ASC,
            filterOption = FilterOption.ALL,
            viewOption = ViewOption.GRID
        )
    }
}