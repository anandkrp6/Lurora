package com.bytecoder.lurora.frontend.ui.screens.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bytecoder.lurora.frontend.navigation.*
import com.bytecoder.lurora.frontend.viewmodels.MediaLibraryViewModel
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.frontend.ui.components.MediaThumbnailImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTabContent(
    currentSection: MusicSection,
    onSectionChange: (MusicSection) -> Unit,
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption,
    onMusicClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MediaLibraryViewModel = hiltViewModel()
) {
    val sections = MusicSection.values()
    val pagerState = rememberPagerState(
        initialPage = sections.indexOf(currentSection),
        pageCount = { sections.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Update ViewModel sort/filter options when they change
    LaunchedEffect(sortOption) {
        viewModel.setSortOption(sortOption)
    }
    
    LaunchedEffect(filterOption) {
        viewModel.setFilterOption(filterOption)
    }

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
                    MusicLibraryContent(
                        audioFiles = audioFiles,
                        isLoading = isLoading,
                        sortOption = sortOption,
                        filterOption = filterOption,
                        viewOption = viewOption,
                        onMusicClick = onMusicClick,
                        onRefresh = { viewModel.refreshLibrary() }
                    )
                }
                MusicSection.PLAYLIST -> {
                    MusicPlaylistContent(sortOption, filterOption, viewOption)
                }
                MusicSection.FAVORITES -> {
                    MusicFavoritesContent(
                        audioFiles = audioFiles.filter { it.isFavorite },
                        isLoading = isLoading,
                        sortOption = sortOption,
                        filterOption = filterOption,
                        viewOption = viewOption,
                        onMusicClick = onMusicClick
                    )
                }
            }
        }
    }
}

@Composable
fun MusicLibraryContent(
    audioFiles: List<MediaItem>,
    isLoading: Boolean,
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption,
    onMusicClick: (MediaItem) -> Unit,
    onRefresh: () -> Unit
) {
    if (isLoading && audioFiles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Scanning for music...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else if (audioFiles.isEmpty()) {
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
                    text = "No music found",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Try adding some music files to your device",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Button(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
    } else {
        when (viewOption) {
            ViewOption.LIST -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audio ->
                        MusicListItem(
                            audio = audio,
                            onClick = { onMusicClick(audio) }
                        )
                    }
                }
            }
            ViewOption.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audio ->
                        MusicGridItem(
                            audio = audio,
                            onClick = { onMusicClick(audio) }
                        )
                    }
                }
            }
            ViewOption.COMPACT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audio ->
                        MusicCompactItem(
                            audio = audio,
                            onClick = { onMusicClick(audio) }
                        )
                    }
                }
            }
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
    audioFiles: List<MediaItem>,
    isLoading: Boolean,
    sortOption: SortOption,
    filterOption: FilterOption,
    viewOption: ViewOption,
    onMusicClick: (MediaItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (audioFiles.isEmpty()) {
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
                    text = "No Favorite Music",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mark songs as favorite to see them here",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        when (viewOption) {
            ViewOption.LIST -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audio ->
                        MusicListItem(
                            audio = audio,
                            onClick = { onMusicClick(audio) }
                        )
                    }
                }
            }
            ViewOption.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audio ->
                        MusicGridItem(
                            audio = audio,
                            onClick = { onMusicClick(audio) }
                        )
                    }
                }
            }
            ViewOption.COMPACT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(audioFiles, key = { it.id }) { audio ->
                        MusicCompactItem(
                            audio = audio,
                            onClick = { onMusicClick(audio) }
                        )
                    }
                }
            }
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
            viewOption = ViewOption.LIST,
            onMusicClick = { }
        )
    }
}

// Music item components

@Composable
private fun MusicListItem(
    audio: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                MediaThumbnailImage(
                    mediaItem = audio,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackIconSize = 24.dp
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                audio.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                audio.album?.let { album ->
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(audio.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Play button
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play audio",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MusicGridItem(
    audio: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Album art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    MediaThumbnailImage(
                        mediaItem = audio,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        fallbackIconSize = 48.dp
                    )
                }
                
                // Play icon overlay
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .padding(4.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                audio.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicCompactItem(
    audio: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    audio.artist?.let { artist ->
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Text(
                        text = formatDuration(audio.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play audio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Utility functions
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    
    return "%.1f %s".format(
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}