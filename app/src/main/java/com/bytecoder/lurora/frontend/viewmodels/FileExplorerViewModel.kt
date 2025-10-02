package com.bytecoder.lurora.frontend.viewmodels

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.FileSystemItem
import com.bytecoder.lurora.backend.models.FileOperation
import com.bytecoder.lurora.backend.models.FileSortOption
import com.bytecoder.lurora.backend.models.FileViewType
import com.bytecoder.lurora.backend.models.FileTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.bytecoder.lurora.backend.utils.SharingUtils
import javax.inject.Inject

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class StorageDevice(
        val name: String,
        val path: String,
        val isRemovable: Boolean,
        val isEmulated: Boolean,
        val totalSpace: Long,
        val freeSpace: Long
    )

    var uiState by mutableStateOf(FileExplorerUiState())
        private set

    private var clipboardItems = listOf<FileSystemItem>()
    private var clipboardOperation: FileOperation? = null
    
    private var _storageDevices by mutableStateOf<List<StorageDevice>>(emptyList())
    val storageDevices: List<StorageDevice> get() = _storageDevices

    // Exposed properties for the UI to observe
    val currentPath: String get() = uiState.currentPath
    val fileItems: List<FileSystemItem> get() = uiState.items
    val isLoading: Boolean get() = uiState.isLoading
    val selectedItems: List<FileSystemItem> get() = uiState.selectedItems
    val isSelectionMode: Boolean get() = uiState.selectedItems.isNotEmpty()
    val viewMode: FileViewType get() = uiState.viewType
    val sortOption: FileSortOption get() = uiState.sortOption
    val showHiddenFiles: Boolean get() = uiState.showHiddenFiles
    val searchQuery: String get() = uiState.searchQuery
    val isSearchMode: Boolean get() = uiState.isSearchMode
    val searchResults: List<FileSystemItem> get() = uiState.searchResults
    val storageLocations: List<String> get() = _storageDevices.map { it.path }

    init {
        initializeStorageDevices()
        navigateToDirectory(getExternalStorageDirectory())
    }

    private fun getExternalStorageDirectory(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    private fun initializeStorageDevices() {
        viewModelScope.launch {
            try {
                val devices = withContext(Dispatchers.IO) {
                    getAllStorageDevices()
                }
                _storageDevices = devices
            } catch (e: Exception) {
                // Fallback to basic external storage
                _storageDevices = listOf(
                    StorageDevice(
                        name = "Internal Storage",
                        path = Environment.getExternalStorageDirectory().absolutePath,
                        isRemovable = false,
                        isEmulated = true,
                        totalSpace = 0L,
                        freeSpace = 0L
                    )
                )
            }
        }
    }

    private fun getAllStorageDevices(): List<StorageDevice> {
        val devices = mutableListOf<StorageDevice>()
        
        try {
            // Add primary external storage (internal storage)
            val primaryStorage = Environment.getExternalStorageDirectory()
            if (primaryStorage != null && primaryStorage.exists()) {
                devices.add(
                    StorageDevice(
                        name = "Internal Storage",
                        path = primaryStorage.absolutePath,
                        isRemovable = false,
                        isEmulated = Environment.isExternalStorageEmulated(),
                        totalSpace = primaryStorage.totalSpace,
                        freeSpace = primaryStorage.freeSpace
                    )
                )
            }

            // Add external SD card and USB storage
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                
                // Get all external file directories
                val externalDirs = context.getExternalFilesDirs(null)
                externalDirs?.forEachIndexed { index, dir ->
                    if (dir != null && index > 0) { // Skip first one (already added as internal)
                        val root = getRootPath(dir.absolutePath)
                        if (root.isNotEmpty() && !devices.any { it.path == root }) {
                            val rootFile = File(root)
                            if (rootFile.exists() && rootFile.canRead()) {
                                devices.add(
                                    StorageDevice(
                                        name = if (Environment.isExternalStorageRemovable(rootFile)) "SD Card" else "External Storage",
                                        path = root,
                                        isRemovable = Environment.isExternalStorageRemovable(rootFile),
                                        isEmulated = false,
                                        totalSpace = rootFile.totalSpace,
                                        freeSpace = rootFile.freeSpace
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Try to find additional removable storage by checking common mount points
            val commonMountPoints = listOf(
                "/storage/sdcard1",
                "/storage/extsdcard",
                "/storage/sdcard0/external_sdcard",
                "/mnt/extsdcard",
                "/mnt/sdcard/external_sd",
                "/sdcard/sd",
                "/mnt/external_sd",
                "/storage/external_SD",
                "/storage/ext_sd",
                "/storage/removable/sdcard1",
                "/data/sdext",
                "/data/sdext2",
                "/data/sdext3",
                "/data/sdext4"
            )

            commonMountPoints.forEach { mountPoint ->
                val file = File(mountPoint)
                if (file.exists() && file.canRead() && !devices.any { it.path == mountPoint }) {
                    devices.add(
                        StorageDevice(
                            name = "Removable Storage",
                            path = mountPoint,
                            isRemovable = true,
                            isEmulated = false,
                            totalSpace = file.totalSpace,
                            freeSpace = file.freeSpace
                        )
                    )
                }
            }

        } catch (e: Exception) {
            // Fallback handling
        }

        return devices.distinctBy { it.path }
    }

    private fun getRootPath(path: String): String {
        // Extract root path from Android/data/... path structure
        val parts = path.split("/Android/")
        return if (parts.size > 1) parts[0] else path
    }

    fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val items = withContext(Dispatchers.IO) {
                    loadDirectoryContents(path)
                }
                uiState = uiState.copy(
                    currentPath = path,
                    items = items,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Failed to load directory: ${e.message}"
                )
            }
        }
    }

    fun navigateUp() {
        val currentDir = File(uiState.currentPath)
        val parentDir = currentDir.parentFile
        if (parentDir != null && parentDir.canRead()) {
            navigateToDirectory(parentDir.absolutePath)
        }
    }

    fun refreshCurrentDirectory() {
        navigateToDirectory(uiState.currentPath)
    }

    fun toggleViewType() {
        uiState = uiState.copy(
            viewType = if (uiState.viewType == FileViewType.LIST) FileViewType.GRID else FileViewType.LIST
        )
    }

    fun updateSortOption(sortOption: FileSortOption) {
        uiState = uiState.copy(sortOption = sortOption)
        refreshCurrentDirectory()
    }

    fun toggleShowHiddenFiles() {
        uiState = uiState.copy(showHiddenFiles = !uiState.showHiddenFiles)
        refreshCurrentDirectory()
    }

    fun selectItem(item: FileSystemItem) {
        val currentSelection = uiState.selectedItems.toMutableList()
        if (currentSelection.contains(item)) {
            currentSelection.remove(item)
        } else {
            currentSelection.add(item)
        }
        uiState = uiState.copy(selectedItems = currentSelection)
    }

    fun selectAllItems() {
        uiState = uiState.copy(selectedItems = uiState.items)
    }

    fun clearSelection() {
        uiState = uiState.copy(selectedItems = emptyList())
    }

    fun copyItems(items: List<FileSystemItem>) {
        clipboardItems = items
        clipboardOperation = FileOperation.COPY
        clearSelection()
    }

    fun cutItems(items: List<FileSystemItem>) {
        clipboardItems = items
        clipboardOperation = FileOperation.MOVE
        clearSelection()
    }

    fun pasteItems() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (clipboardOperation) {
                        FileOperation.COPY -> {
                            clipboardItems.forEach { item ->
                                copyFileOrDirectory(File(item.path), File(uiState.currentPath, item.name))
                            }
                        }
                        FileOperation.MOVE -> {
                            clipboardItems.forEach { item ->
                                moveFileOrDirectory(File(item.path), File(uiState.currentPath, item.name))
                            }
                        }
                        else -> return@withContext
                    }
                }
                clearClipboard()
                refreshCurrentDirectory()
            } catch (e: Exception) {
                uiState = uiState.copy(error = "Failed to paste items: ${e.message}")
            }
        }
    }

    fun deleteItems(items: List<FileSystemItem>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    items.forEach { item ->
                        val file = File(item.path)
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                    }
                }
                clearSelection()
                refreshCurrentDirectory()
            } catch (e: Exception) {
                uiState = uiState.copy(error = "Failed to delete items: ${e.message}")
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val newFolder = File(uiState.currentPath, name)
                    if (!newFolder.exists()) {
                        newFolder.mkdirs()
                    }
                }
                refreshCurrentDirectory()
            } catch (e: Exception) {
                uiState = uiState.copy(error = "Failed to create folder: ${e.message}")
            }
        }
    }

    fun renameItem(item: FileSystemItem, newName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val oldFile = File(item.path)
                    val newFile = File(oldFile.parent, newName)
                    oldFile.renameTo(newFile)
                }
                refreshCurrentDirectory()
            } catch (e: Exception) {
                uiState = uiState.copy(error = "Failed to rename item: ${e.message}")
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    // Additional methods expected by the UI
    fun exitSelectionMode() {
        clearSelection()
    }

    fun selectAll() {
        selectAllItems()
    }

    fun copySelected() {
        if (uiState.selectedItems.isNotEmpty()) {
            copyItems(uiState.selectedItems)
        }
    }

    fun cutSelected() {
        if (uiState.selectedItems.isNotEmpty()) {
            cutItems(uiState.selectedItems)
        }
    }

    fun shareSelected() {
        // Implementation would handle sharing selected items
        clearSelection()
    }

    fun deleteSelected() {
        if (uiState.selectedItems.isNotEmpty()) {
            deleteItems(uiState.selectedItems)
        }
    }

    fun toggleSearchMode() {
        uiState = uiState.copy(
            isSearchMode = !uiState.isSearchMode,
            searchQuery = if (!uiState.isSearchMode) "" else uiState.searchQuery,
            searchResults = if (!uiState.isSearchMode) emptyList() else uiState.searchResults
        )
    }

    fun setSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
        if (query.isNotBlank()) {
            performSearch(query)
        } else {
            uiState = uiState.copy(searchResults = emptyList())
        }
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val results = withContext(Dispatchers.IO) {
                    searchFiles(uiState.currentPath, query)
                }
                uiState = uiState.copy(
                    searchResults = results,
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = "Search failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    private fun searchFiles(directory: String, query: String): List<FileSystemItem> {
        val results = mutableListOf<FileSystemItem>()
        val dir = File(directory)
        
        try {
            dir.walkTopDown()
                .filter { file ->
                    file.name.contains(query, ignoreCase = true) &&
                    (uiState.showHiddenFiles || !file.name.startsWith("."))
                }
                .take(100) // Limit results for performance
                .forEach { file ->
                    results.add(
                        FileSystemItem(
                            path = file.absolutePath,
                            name = file.name,
                            isDirectory = file.isDirectory,
                            size = if (file.isFile) file.length() else 0L,
                            lastModified = Date(file.lastModified())
                        )
                    )
                }
        } catch (e: SecurityException) {
            // Handle permission denied
        }
        
        return results.sortedBy { it.name }
    }
    
    // Sharing functionality
    fun shareFile(fileItem: FileSystemItem) {
        SharingUtils.shareFile(context, fileItem)
    }
    
    fun shareSelectedFiles() {
        if (uiState.selectedItems.isNotEmpty()) {
            SharingUtils.shareFiles(context, uiState.selectedItems)
        }
    }
    
    fun shareFiles(fileItems: List<FileSystemItem>) {
        SharingUtils.shareFiles(context, fileItems)
    }

    fun navigateToRoot() {
        navigateToDirectory(getExternalStorageDirectory())
    }

    fun toggleViewMode() {
        toggleViewType()
    }

    fun setSortOption(option: FileSortOption) {
        updateSortOption(option)
    }

    fun toggleHiddenFiles() {
        toggleShowHiddenFiles()
    }

    fun navigateToPath(path: String) {
        navigateToDirectory(path)
    }

    fun navigateToStorage(storage: String) {
        navigateToDirectory(storage)
    }

    fun enterSelectionMode(itemPath: String) {
        val item = uiState.items.find { it.path == itemPath }
        if (item != null) {
            selectItem(item)
        }
    }

    fun toggleSelection(itemPath: String) {
        val item = uiState.items.find { it.path == itemPath }
        if (item != null) {
            selectItem(item)
        }
    }

    fun clipboardHasItems(): Boolean {
        return hasClipboardItems()
    }

    fun pasteFromClipboard() {
        pasteItems()
    }

    fun getClipboardCount(): Int {
        return getClipboardItemCount()
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val newFile = File(uiState.currentPath, name)
                    if (!newFile.exists()) {
                        newFile.createNewFile()
                    }
                }
                refreshCurrentDirectory()
            } catch (e: Exception) {
                uiState = uiState.copy(error = "Failed to create file: ${e.message}")
            }
        }
    }

    fun deleteFile(item: FileSystemItem) {
        deleteItems(listOf(item))
    }

    fun renameFile(item: FileSystemItem, newName: String) {
        renameItem(item, newName)
    }

    private fun clearClipboard() {
        clipboardItems = emptyList()
        clipboardOperation = null
    }

    private fun copyFileOrDirectory(source: File, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { file ->
                copyFileOrDirectory(file, File(destination, file.name))
            }
        } else {
            source.copyTo(destination, overwrite = true)
        }
    }

    private fun moveFileOrDirectory(source: File, destination: File) {
        copyFileOrDirectory(source, destination)
        if (source.isDirectory) {
            source.deleteRecursively()
        } else {
            source.delete()
        }
    }

    private fun loadDirectoryContents(path: String): List<FileSystemItem> {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
            return emptyList()
        }

        val files = directory.listFiles() ?: return emptyList()
        
        return files
            .filter { file ->
                if (uiState.showHiddenFiles) true else !file.name.startsWith(".")
            }
            .map { file ->
                FileSystemItem(
                    path = file.absolutePath,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = Date(file.lastModified())
                )
            }
            .let { items ->
                sortFiles(items, uiState.sortOption)
            }
    }

    private fun sortFiles(files: List<FileSystemItem>, sortOption: FileSortOption): List<FileSystemItem> {
        return when (sortOption) {
            FileSortOption.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            FileSortOption.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            FileSortOption.SIZE_ASC -> files.sortedBy { it.size }
            FileSortOption.SIZE_DESC -> files.sortedByDescending { it.size }
            FileSortOption.DATE_ASC -> files.sortedBy { it.lastModified }
            FileSortOption.DATE_DESC -> files.sortedByDescending { it.lastModified }
            FileSortOption.TYPE_ASC -> files.sortedBy { 
                if (it.isDirectory) "folder" else it.name.substringAfterLast(".", "unknown")
            }
            FileSortOption.TYPE_DESC -> files.sortedByDescending { 
                if (it.isDirectory) "folder" else it.name.substringAfterLast(".", "unknown")
            }
        }.let { sortedFiles ->
            // Always show directories first
            sortedFiles.sortedBy { if (it.isDirectory) 0 else 1 }
        }
    }

    fun getStorageInfo(context: Context): StorageInfo {
        val externalDir = context.getExternalFilesDir(null) ?: File("/storage/emulated/0")
        val totalSpace = externalDir.totalSpace
        val freeSpace = externalDir.freeSpace
        val usedSpace = totalSpace - freeSpace

        return StorageInfo(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace
        )
    }

    fun isMediaFile(item: FileSystemItem): Boolean {
        val extension = item.name.substringAfterLast(".", "")
        return FileTypes.isMediaFile(extension)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }

    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }

    fun canNavigateUp(): Boolean {
        val currentDir = File(uiState.currentPath)
        val parentDir = currentDir.parentFile
        return parentDir != null && parentDir.canRead()
    }

    fun hasClipboardItems(): Boolean {
        return clipboardItems.isNotEmpty()
    }

    fun getClipboardOperation(): FileOperation? {
        return clipboardOperation
    }

    fun getClipboardItemCount(): Int {
        return clipboardItems.size
    }

    data class StorageInfo(
        val totalSpace: Long,
        val freeSpace: Long,
        val usedSpace: Long
    )
}

data class FileExplorerUiState(
    val currentPath: String = "",
    val items: List<FileSystemItem> = emptyList(),
    val selectedItems: List<FileSystemItem> = emptyList(),
    val viewType: FileViewType = FileViewType.LIST,
    val sortOption: FileSortOption = FileSortOption.NAME_ASC,
    val showHiddenFiles: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val searchResults: List<FileSystemItem> = emptyList()
)