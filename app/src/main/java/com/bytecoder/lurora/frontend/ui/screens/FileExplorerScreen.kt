package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.viewmodels.FileExplorerViewModel
import java.util.Date

/**
 * File Explorer Screen with comprehensive file management
 * Main screen shows storage devices, clicking navigates to file browser
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel = hiltViewModel(),
    onPlayMedia: (FileSystemItem) -> Unit = {},
    onOpenFile: (FileSystemItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedDevice by remember { mutableStateOf<FileExplorerViewModel.StorageDevice?>(null) }
    
    // Show storage devices list or file browser based on selection
    if (selectedDevice == null) {
        StorageDevicesMainScreen(
            viewModel = viewModel,
            onDeviceSelected = { device -> selectedDevice = device },
            modifier = modifier
        )
    } else {
        FileBrowserScreen(
            viewModel = viewModel,
            storageDevice = selectedDevice!!,
            onNavigateBack = { selectedDevice = null },
            onPlayMedia = onPlayMedia,
            onOpenFile = onOpenFile,
            modifier = modifier
        )
    }
}

/**
 * Storage Devices Main Screen - Shows only storage devices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageDevicesMainScreen(
    viewModel: FileExplorerViewModel,
    onDeviceSelected: (FileExplorerViewModel.StorageDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val storageDevices = viewModel.storageDevices
    
    Column(modifier = modifier.fillMaxSize()) {
        // App bar for storage devices
        TopAppBar(
            title = { 
                Text(
                    text = "File Explorer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        // Storage devices content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Choose Storage Device",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                Text(
                    text = "Select a storage device to browse files and folders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(storageDevices) { device ->
                StorageDeviceCard(
                    storageDevice = device,
                    onClick = { onDeviceSelected(device) }
                )
            }
            
            if (storageDevices.isEmpty()) {
                item {
                    EmptyStorageView(
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * File Browser Screen - Shows files and folders for selected storage device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileBrowserScreen(
    viewModel: FileExplorerViewModel,
    storageDevice: FileExplorerViewModel.StorageDevice,
    onNavigateBack: () -> Unit,
    onPlayMedia: (FileSystemItem) -> Unit,
    onOpenFile: (FileSystemItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Initialize the browser with the storage device path
    LaunchedEffect(storageDevice) {
        viewModel.navigateToDirectory(storageDevice.path)
    }
    
    // Direct property access from ViewModel
    val currentPath = viewModel.currentPath
    val fileItems = viewModel.fileItems
    val isLoading = viewModel.isLoading
    val selectedItems = viewModel.selectedItems
    val isSelectionMode = viewModel.isSelectionMode
    val viewMode = viewModel.viewMode
    val sortOption = viewModel.sortOption
    val showHiddenFiles = viewModel.showHiddenFiles
    val searchQuery = viewModel.searchQuery
    val isSearchMode = viewModel.isSearchMode
    val searchResults = viewModel.searchResults
    val storageLocations = viewModel.storageLocations
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf<FileSystemItem?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileSystemItem?>(null) }
    
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top app bar with navigation and controls
        if (isSelectionMode) {
            SelectionModeAppBar(
                selectedCount = selectedItems.count(),
                onExit = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAllItems() },
                onCopy = { viewModel.copyItems(selectedItems) },
                onCut = { viewModel.cutItems(selectedItems) },
                onDelete = { 
                    if (selectedItems.any()) {
                        showDeleteConfirmation = true
                    }
                },
                onShare = { viewModel.shareSelectedFiles() }
            )
        } else {
            FileBrowserAppBar(
                deviceName = storageDevice.name,
                currentPath = currentPath,
                isSearchMode = isSearchMode,
                searchQuery = searchQuery,
                canNavigateUp = viewModel.canNavigateUp(),
                isAtDeviceRoot = currentPath == storageDevice.path,
                onNavigateBack = onNavigateBack,
                onSearchToggle = { viewModel.toggleSearchMode() },
                onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                onNavigateUp = { viewModel.navigateUp() },
                onViewModeToggle = { viewModel.toggleViewType() },
                onShowSortDialog = { showSortDialog = true },
                onToggleHiddenFiles = { viewModel.toggleShowHiddenFiles() },
                onShowCreateDialog = { showCreateDialog = true }
            )
        }
        
        // Breadcrumb navigation
        if (isSearchMode.not() && isSelectionMode.not()) {
            BreadcrumbNavigation(
                currentPath = currentPath,
                storageLocations = storageLocations,
                onNavigateToPath = { viewModel.navigateToDirectory(it) },
                onNavigateToStorage = { viewModel.navigateToDirectory(it) }
            )
        }
        
        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                isSearchMode && searchResults.isEmpty() && searchQuery.isNotEmpty() -> {
                    EmptyFileView(
                        icon = Icons.Default.SearchOff,
                        message = "No files found for \"$searchQuery\"",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                (isSearchMode.not() && fileItems.isEmpty()) -> {
                    EmptyFileView(
                        icon = Icons.Default.FolderOpen,
                        message = "This folder is empty",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val itemsToShow = if (isSearchMode) searchResults else fileItems
                    
                    when (viewMode) {
                        FileViewType.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    items = itemsToShow,
                                    key = { it.path }
                                ) { item ->
                                    FileListItem(
                                        item = item,
                                        isSelected = selectedItems.any { it.path == item.path },
                                        isSelectionMode = isSelectionMode,
                                        onClick = { handleFileClick(item, viewModel, onPlayMedia, onOpenFile) },
                                        onLongClick = {
                                            if (isSelectionMode.not()) {
                                                viewModel.selectItem(item)
                                            }
                                        },
                                        onShowProperties = { showPropertiesDialog = item },
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                        FileViewType.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 120.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = itemsToShow,
                                    key = { it.path }
                                ) { item ->
                                    FileGridItem(
                                        item = item,
                                        isSelected = selectedItems.any { it.path == item.path },
                                        isSelectionMode = isSelectionMode,
                                        onClick = { handleFileClick(item, viewModel, onPlayMedia, onOpenFile) },
                                        onLongClick = {
                                            if (isSelectionMode.not()) {
                                                viewModel.selectItem(item)
                                            }
                                        },
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Floating action button for paste (when clipboard has items)
        if (viewModel.clipboardHasItems()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.pasteFromClipboard() },
                    icon = { Icon(Icons.Default.ContentPaste, contentDescription = "Paste") },
                    text = { Text("Paste ${viewModel.getClipboardCount()} items") }
                )
            }
        }
    }
    
    // Dialogs
    if (showCreateDialog) {
        CreateItemDialog(
            onCreateFolder = { name ->
                viewModel.createFolder(name)
                showCreateDialog = false
            },
            onCreateFile = { name ->
                viewModel.createFile(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    
    if (showSortDialog) {
        SortOptionsDialog(
            currentSort = sortOption,
            onSortSelected = { 
                viewModel.setSortOption(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
    
    showPropertiesDialog?.let { item ->
        FilePropertiesDialog(
            item = item,
            onDismiss = { showPropertiesDialog = null },
            onRename = { 
                showPropertiesDialog = null
                showRenameDialog = item
            },
            onDelete = {
                showPropertiesDialog = null
                viewModel.deleteFile(item)
            }
        )
    }
    
    showRenameDialog?.let { item ->
        RenameDialog(
            item = item,
            onRename = { newName ->
                viewModel.renameFile(item, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }
    
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            itemCount = selectedItems.count(),
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileExplorerAppBar(
    currentPath: String,
    isSearchMode: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToRoot: () -> Unit,
    onViewModeToggle: () -> Unit,
    onShowSortDialog: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onShowCreateDialog: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            } else {
                Text(
                    text = if (currentPath.isEmpty()) "File Explorer" else currentPath.substringAfterLast('/'),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (isSearchMode) {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Close, contentDescription = "Close search")
                }
            } else if (currentPath.isNotEmpty()) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Navigate up")
                }
            }
        },
        actions = {
            if (!isSearchMode) {
                Row {
                    IconButton(onClick = onSearchToggle) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onViewModeToggle) {
                        Icon(Icons.Default.ViewModule, contentDescription = "Toggle view mode")
                    }
                    
                    // More options menu
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort") },
                                onClick = {
                                    showMenu = false
                                    onShowSortDialog()
                                },
                                leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Show hidden files") },
                                onClick = {
                                    showMenu = false
                                    onToggleHiddenFiles()
                                },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Go to root") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToRoot()
                                },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("New") },
                                onClick = {
                                    showMenu = false
                                    onShowCreateDialog()
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )
                        }
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeAppBar(
    selectedCount: Int,
    onExit: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, contentDescription = "Exit selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onCut) {
                Icon(Icons.Default.ContentCut, contentDescription = "Cut")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun BreadcrumbNavigation(
    currentPath: String,
    storageLocations: List<String>, // Simplified to just paths
    onNavigateToPath: (String) -> Unit,
    onNavigateToStorage: (String) -> Unit // Simplified callback
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Storage locations (simplified)
        items(storageLocations) { storagePath ->
            FilterChip(
                selected = currentPath.startsWith(storagePath),
                onClick = { onNavigateToStorage(storagePath) },
                label = { 
                    Text(
                        text = storagePath.substringAfterLast("/"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        // Path segments
        if (currentPath.isNotEmpty()) {
            val pathSegments = currentPath.split("/").filter { it.isNotEmpty() }
            items(pathSegments.size) { index ->
                val segment = pathSegments[index]
                val segmentPath = "/" + pathSegments.take(index + 1).joinToString("/")
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    TextButton(
                        onClick = { onNavigateToPath(segmentPath) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = segment,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    item: FileSystemItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShowProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox or file icon
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                FileIcon(
                    item = item,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // File details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.isDirectory) {
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Text(
                        text = formatDate(item.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (FileTypes.isMediaFile(item.name.substringAfterLast("."))) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Media file",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Action button
            if (!isSelectionMode) {
                IconButton(onClick = onShowProperties) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridItem(
    item: FileSystemItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selection overlay or file icon
            Box(modifier = Modifier.weight(1f)) {
                FileIcon(
                    item = item,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
                
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                
                if (FileTypes.isMediaFile(item.name.substringAfterLast("."))) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(16.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // File name and details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                
                if (!item.isDirectory) {
                    Text(
                        text = formatFileSize(item.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileIcon(
    item: FileSystemItem,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when {
        item.isDirectory -> Icons.Default.Folder to MaterialTheme.colorScheme.primary
        item.name.substringAfterLast(".").lowercase() in listOf("mp4", "avi", "mkv", "mov", "wmv") -> 
            Icons.Default.Movie to MaterialTheme.colorScheme.secondary
        item.name.substringAfterLast(".").lowercase() in listOf("mp3", "wav", "flac", "aac", "m4a") -> 
            Icons.Default.MusicNote to MaterialTheme.colorScheme.secondary
        item.name.substringAfterLast(".").lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> 
            Icons.Default.Image to MaterialTheme.colorScheme.secondary
        item.name.substringAfterLast(".").lowercase() in listOf("pdf") -> 
            Icons.Default.PictureAsPdf to MaterialTheme.colorScheme.error
        item.name.substringAfterLast(".").lowercase() in listOf("txt", "md", "log") -> 
            Icons.Default.Description to MaterialTheme.colorScheme.tertiary
        item.name.substringAfterLast(".").lowercase() in listOf("zip", "rar", "7z", "tar") -> 
            Icons.Default.Archive to MaterialTheme.colorScheme.outline
        else -> Icons.Default.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .background(
                tint.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EmptyFileView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Helper function to handle file clicks
private fun handleFileClick(
    item: FileSystemItem,
    viewModel: FileExplorerViewModel,
    onPlayMedia: (FileSystemItem) -> Unit,
    onOpenFile: (FileSystemItem) -> Unit
) {
    when {
        viewModel.isSelectionMode -> {
            viewModel.toggleSelection(item.path)
        }
        item.isDirectory -> {
            viewModel.navigateToPath(item.path)
        }
        FileTypes.isMediaFile(item.name.substringAfterLast(".")) -> {
            onPlayMedia(item)
        }
        else -> {
            onOpenFile(item)
        }
    }
}

// Dialog implementations (simplified for brevity)
@Composable
private fun CreateItemDialog(
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isFolder by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFolder) "Create Folder" else "Create File") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isFolder,
                        onClick = { isFolder = true },
                        label = { Text("Folder") }
                    )
                    FilterChip(
                        selected = !isFolder,
                        onClick = { isFolder = false },
                        label = { Text("File") }
                    )
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        if (isFolder) {
                            onCreateFolder(name.trim())
                        } else {
                            onCreateFile(name.trim())
                        }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
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
private fun SortOptionsDialog(
    currentSort: FileSortOption,
    onSortSelected: (FileSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column {
                FileSortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = { onSortSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                FileSortOption.NAME_ASC -> "Name A-Z"
                                FileSortOption.NAME_DESC -> "Name Z-A"
                                FileSortOption.SIZE_ASC -> "Size (smallest first)"
                                FileSortOption.SIZE_DESC -> "Size (largest first)"
                                FileSortOption.DATE_ASC -> "Date (oldest first)"
                                FileSortOption.DATE_DESC -> "Date (newest first)"
                                FileSortOption.TYPE_ASC -> "Type A-Z"
                                FileSortOption.TYPE_DESC -> "Type Z-A"
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
private fun FilePropertiesDialog(
    item: FileSystemItem,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Name: ${item.name}")
                if (!item.isDirectory) {
                    Text("Size: ${formatFileSize(item.size)}")
                }
                Text("Modified: ${formatDate(item.lastModified)}")
                Text("Path: ${item.path}")
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onRename) { Text("Rename") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun RenameDialog(
    item: FileSystemItem,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(item.name) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(newName) },
                enabled = newName.isNotBlank() && newName != item.name
            ) {
                Text("Rename")
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
private fun DeleteConfirmationDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (itemCount == 1) "item" else "$itemCount items"}?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Utility functions
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> String.format("%.1f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatDate(date: Date): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

private fun formatRelativeDate(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInMinutes = diffInMillis / (1000 * 60)
    val diffInHours = diffInMinutes / 60
    val diffInDays = diffInHours / 24
    
    return when {
        diffInMinutes < 1 -> "Just now"
        diffInMinutes < 60 -> "${diffInMinutes}m ago"
        diffInHours < 24 -> "${diffInHours}h ago"
        diffInDays < 7 -> "${diffInDays}d ago"
        else -> {
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            formatter.format(date)
        }
    }
}

@Composable
private fun StorageDevicesSection(
    storageDevices: List<FileExplorerViewModel.StorageDevice>,
    onStorageClick: (FileExplorerViewModel.StorageDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Storage Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            storageDevices.forEach { storage ->
                StorageDeviceItem(
                    storageDevice = storage,
                    onClick = { onStorageClick(storage) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (storage != storageDevices.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StorageDeviceItem(
    storageDevice: FileExplorerViewModel.StorageDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedSpace = storageDevice.totalSpace - storageDevice.freeSpace
    val usagePercentage = if (storageDevice.totalSpace > 0) {
        (usedSpace.toFloat() / storageDevice.totalSpace.toFloat())
    } else 0f
    
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Storage icon
            val iconColor = when {
                storageDevice.isRemovable -> MaterialTheme.colorScheme.tertiary
                storageDevice.isEmulated -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }
            
            val storageIcon = when {
                storageDevice.isRemovable && "SD" in storageDevice.name.uppercase() -> Icons.Default.SdCard
                storageDevice.isRemovable -> Icons.Default.Usb
                else -> Icons.Default.PhoneAndroid
            }
            
            Icon(
                imageVector = storageIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Storage info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storageDevice.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (storageDevice.totalSpace > 0) {
                    Text(
                        text = "${formatFileSize(storageDevice.freeSpace)} free of ${formatFileSize(storageDevice.totalSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Storage usage bar
                    LinearProgressIndicator(
                        progress = usagePercentage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when {
                            usagePercentage > 0.9f -> MaterialTheme.colorScheme.error
                            usagePercentage > 0.75f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                } else {
                    Text(
                        text = storageDevice.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Navigation arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StorageDeviceCard(
    storageDevice: FileExplorerViewModel.StorageDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedSpace = storageDevice.totalSpace - storageDevice.freeSpace
    val usagePercentage = if (storageDevice.totalSpace > 0) {
        (usedSpace.toFloat() / storageDevice.totalSpace.toFloat())
    } else 0f
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Storage icon
            val iconColor = when {
                storageDevice.isRemovable -> MaterialTheme.colorScheme.tertiary
                storageDevice.isEmulated -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }
            
            val storageIcon = when {
                storageDevice.isRemovable && "SD" in storageDevice.name.uppercase() -> Icons.Default.SdCard
                storageDevice.isRemovable -> Icons.Default.Usb
                else -> Icons.Default.PhoneAndroid
            }
            
            Icon(
                imageVector = storageIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Storage info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storageDevice.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (storageDevice.totalSpace > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatFileSize(storageDevice.freeSpace)} free of ${formatFileSize(storageDevice.totalSpace)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Storage usage bar
                    LinearProgressIndicator(
                        progress = usagePercentage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            usagePercentage > 0.9f -> MaterialTheme.colorScheme.error
                            usagePercentage > 0.75f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                } else {
                    Text(
                        text = storageDevice.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Navigation arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyStorageView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No storage devices found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Please check your device's storage permissions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileBrowserAppBar(
    deviceName: String,
    currentPath: String,
    isSearchMode: Boolean,
    searchQuery: String,
    canNavigateUp: Boolean,
    isAtDeviceRoot: Boolean,
    onNavigateBack: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onViewModeToggle: () -> Unit,
    onShowSortDialog: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onShowCreateDialog: () -> Unit
) {
    TopAppBar(
        title = { 
            if (isSearchMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isAtDeviceRoot) {
                        Text(
                            text = currentPath.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                if (isAtDeviceRoot) {
                    onNavigateBack()
                } else {
                    onNavigateUp()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = if (isAtDeviceRoot) "Back to storage devices" else "Navigate up"
                )
            }
        },
        actions = {
            if (!isSearchMode) {
                IconButton(onClick = onViewModeToggle) {
                    Icon(Icons.Default.ViewList, contentDescription = "Toggle view mode")
                }
                IconButton(onClick = onShowSortDialog) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort options")
                }
                IconButton(onClick = onShowCreateDialog) {
                    Icon(Icons.Default.Add, contentDescription = "Create new")
                }
            }
        }
    )
}