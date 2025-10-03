package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.ui.components.MediaThumbnailImage
import com.bytecoder.lurora.frontend.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

/**
 * History Screen with tabbed interface for different media types
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onPlayMedia: (MediaItem, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val historyEntries by viewModel.historyEntries.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val historySettings by viewModel.historySettings.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf("Videos", "Audio", "Online")
    val mediaTypeFilters = listOf(
        setOf(MediaType.VIDEO),
        setOf(MediaType.AUDIO),
        setOf(MediaType.VIDEO, MediaType.AUDIO) // Online can be both
    )
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top app bar with back navigation
        TopAppBar(
            title = { 
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        // Search and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search history...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                } else null,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            // Action buttons
            Row {
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
        
        // Selection mode controls
        if (isSelectionMode) {
            SelectionModeBar(
                selectedCount = selectedItems.size,
                onSelectAll = viewModel::selectAll,
                onDeselectAll = viewModel::clearSelection,
                onDelete = { viewModel.deleteSelected() },
                onExit = { viewModel.exitSelectionMode() }
            )
        }
        
        // Tab bar
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        
        // Content pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val filteredEntries = historyEntries.filter { entry ->
                val matchesMediaType = entry.mediaItem.mediaType in mediaTypeFilters[pageIndex]
                val matchesSource = when (pageIndex) {
                    2 -> entry.source != HistorySource.LOCAL // Online tab
                    else -> true // Video and Audio tabs show all sources
                }
                val matchesSearch = if (searchQuery.isEmpty()) true else {
                    entry.mediaItem.title.contains(searchQuery, ignoreCase = true) ||
                    entry.mediaItem.artist?.contains(searchQuery, ignoreCase = true) == true
                }
                matchesMediaType && matchesSource && matchesSearch
            }
            
            if (filteredEntries.isEmpty()) {
                EmptyHistoryView(
                    message = when {
                        searchQuery.isNotEmpty() -> "No results found for \"$searchQuery\""
                        pageIndex == 0 -> "No video history yet"
                        pageIndex == 1 -> "No audio history yet"
                        else -> "No online media history yet"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredEntries,
                        key = { it.id }
                    ) { entry ->
                        HistoryItemCard(
                            entry = entry,
                            isSelected = entry.id in selectedItems,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(entry.id)
                                } else {
                                    onPlayMedia(entry.mediaItem, entry.lastPosition)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.enterSelectionMode(entry.id)
                                }
                            },
                            onRemove = { viewModel.removeEntry(entry.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showSortDialog) {
        SortOptionsDialog(
            currentSort = sortOption,
            onSortSelected = { viewModel.setSortOption(it) },
            onDismiss = { showSortDialog = false }
        )
    }
    
    if (showFilterDialog) {
        FilterOptionsDialog(
            currentFilter = filterOptions,
            onFilterChanged = { viewModel.setFilterOptions(it) },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = { 
                viewModel.clearHistory()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
    
    if (showSettingsDialog) {
        HistorySettingsDialog(
            settings = historySettings,
            onSettingsChanged = { viewModel.updateSettings(it) },
            onClearHistory = { showClearDialog = true },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun HistoryItemCard(
    entry: HistoryEntry,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection checkbox or thumbnail
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.align(Alignment.Top)
                )
            }
            
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                MediaThumbnailImage(
                    mediaItem = entry.mediaItem,
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
                // Title
                Text(
                    text = entry.mediaItem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Artist/Channel
                entry.mediaItem.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Duration and file info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(entry.mediaItem.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    entry.mediaItem.size.let { size ->
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = formatFileSize(size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Source indicator for online content
                    if (entry.source != HistorySource.LOCAL) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = entry.source.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Last played and progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRelativeTime(entry.lastPlayedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (entry.playbackProgress > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { entry.playbackProgress },
                                modifier = Modifier.width(60.dp).height(2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Text(
                                text = "${(entry.playbackProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Actions
            if (!isSelectionMode) {
                Column {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionModeBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Default.Close, contentDescription = "Exit selection")
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Row {
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
                TextButton(onClick = onDeselectAll) {
                    Text("Deselect All")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Dialog components would be implemented here
@Composable
private fun SortOptionsDialog(
    currentSort: HistorySortOption,
    onSortSelected: (HistorySortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column {
                HistorySortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onSortSelected(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = { 
                                onSortSelected(option)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                HistorySortOption.RECENT_FIRST -> "Recent first"
                                HistorySortOption.OLDEST_FIRST -> "Oldest first"
                                HistorySortOption.ALPHABETICAL -> "A to Z"
                                HistorySortOption.REVERSE_ALPHABETICAL -> "Z to A"
                                HistorySortOption.MOST_PLAYED -> "Most played"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FilterOptionsDialog(
    currentFilter: HistoryFilter,
    onFilterChanged: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementation for filter options
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Options") },
        text = { Text("Filter options will be implemented here") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear History") },
        text = { Text("This will permanently delete all history entries. This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HistorySettingsDialog(
    settings: HistorySettings,
    onSettingsChanged: (HistorySettings) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    // Implementation for history settings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("History Settings") },
        text = { Text("Settings options will be implemented here") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// Utility functions
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> String.format("%.1f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.1f KB", kb)
    }
}

private fun formatRelativeTime(dateTime: Date): String {
    val now = Date()
    val diffMs = now.time - dateTime.time
    val diffMinutes = diffMs / (1000 * 60)
    
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 24 * 60 -> "${diffMinutes / 60}h ago"
        diffMinutes < 7 * 24 * 60 -> "${diffMinutes / (24 * 60)}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(dateTime)
    }
}