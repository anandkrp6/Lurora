package com.bytecoder.lurora.backend.ui.main.fileExplorer

import com.bytecoder.lurora.backend.data.database.entity.FileSystemItem
import com.bytecoder.lurora.backend.utils.SharingUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class FileExplorerViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var sharingUtils: SharingUtils
    
    private lateinit var viewModel: FileExplorerViewModel
    private lateinit var testDispatcher: TestDispatcher
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        viewModel = FileExplorerViewModel(context, sharingUtils)
    }
    
    @Test
    fun `initial state should be correct`() = runTest {
        // Then
        val state = viewModel.uiState.first()
        assertTrue("Should not be loading initially", !state.isLoading)
        assertTrue("Files list should be empty initially", state.files.isEmpty())
        assertTrue("Search query should be empty initially", state.searchQuery.isEmpty())
        assertTrue("Search results should be empty initially", state.searchResults.isEmpty())
        assertFalse("Should not be searching initially", state.isSearching)
    }
    
    @Test
    fun `updateSearchQuery should update search query in state`() = runTest {
        // Given
        val query = "test query"
        
        // When
        viewModel.updateSearchQuery(query)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertEquals("Search query should be updated", query, state.searchQuery)
    }
    
    @Test
    fun `clearSearch should reset search state`() = runTest {
        // Given - Set up search state
        viewModel.updateSearchQuery("test")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertTrue("Search query should be empty", state.searchQuery.isEmpty())
        assertTrue("Search results should be empty", state.searchResults.isEmpty())
        assertFalse("Should not be searching", state.isSearching)
    }
    
    @Test
    fun `shareFile should call sharingUtils with correct parameters`() = runTest {
        // Given
        val fileItem = FileSystemItem(
            id = 1,
            name = "test.mp3",
            path = "/storage/emulated/0/Music/test.mp3",
            isDirectory = false,
            size = 1024,
            lastModified = System.currentTimeMillis(),
            extension = "mp3",
            mimeType = "audio/mpeg"
        )
        
        // When
        viewModel.shareFile(fileItem)
        
        // Then
        verify(sharingUtils).shareFile(context, fileItem.path, fileItem.mimeType ?: "*/*")
    }
    
    @Test
    fun `shareMultipleFiles should call sharingUtils with correct file paths`() = runTest {
        // Given
        val fileItems = listOf(
            FileSystemItem(
                id = 1,
                name = "test1.mp3",
                path = "/storage/test1.mp3",
                isDirectory = false,
                size = 1024,
                lastModified = System.currentTimeMillis(),
                extension = "mp3",
                mimeType = "audio/mpeg"
            ),
            FileSystemItem(
                id = 2,
                name = "test2.mp4",
                path = "/storage/test2.mp4",
                isDirectory = false,
                size = 2048,
                lastModified = System.currentTimeMillis(),
                extension = "mp4",
                mimeType = "video/mp4"
            )
        )
        
        // When
        viewModel.shareMultipleFiles(fileItems)
        
        // Then
        val expectedPaths = fileItems.map { it.path }
        verify(sharingUtils).shareMultipleFiles(context, expectedPaths)
    }
    
    @Test
    fun `navigateToDirectory should update current path`() = runTest {
        // Given
        val newPath = "/storage/emulated/0/Music"
        
        // When
        viewModel.navigateToDirectory(newPath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Current path should be updated", newPath, viewModel.currentPath)
    }
    
    @Test
    fun `navigateUp should move to parent directory`() = runTest {
        // Given
        val initialPath = "/storage/emulated/0/Music/Albums"
        val expectedParentPath = "/storage/emulated/0/Music"
        viewModel.navigateToDirectory(initialPath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.navigateUp()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Should navigate to parent directory", expectedParentPath, viewModel.currentPath)
    }
    
    @Test
    fun `navigateUp from root should not change path`() = runTest {
        // Given
        val rootPath = "/"
        viewModel.navigateToDirectory(rootPath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.navigateUp()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Should remain at root", rootPath, viewModel.currentPath)
    }
    
    @Test
    fun `refresh should reload current directory`() = runTest {
        // Given
        val testPath = "/storage/emulated/0/Music"
        viewModel.navigateToDirectory(testPath)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Current path should remain same", testPath, viewModel.currentPath)
        // Note: In a real test, we would verify that the files are reloaded
    }
    
    @Test
    fun `createTestFileHierarchy should create mock file structure`() = runTest {
        // When
        val hierarchy = viewModel.createTestFileHierarchy()
        
        // Then
        assertTrue("Should create some files", hierarchy.isNotEmpty())
        assertTrue("Should contain both files and directories", 
            hierarchy.any { it.isDirectory } && hierarchy.any { !it.isDirectory })
        
        // Verify music files are created
        val musicFiles = hierarchy.filter { it.extension in listOf("mp3", "wav", "flac") }
        assertTrue("Should contain music files", musicFiles.isNotEmpty())
        
        // Verify video files are created
        val videoFiles = hierarchy.filter { it.extension in listOf("mp4", "avi", "mkv") }
        assertTrue("Should contain video files", videoFiles.isNotEmpty())
        
        // Verify document files are created
        val documentFiles = hierarchy.filter { it.extension in listOf("pdf", "txt", "doc") }
        assertTrue("Should contain document files", documentFiles.isNotEmpty())
    }
}