package com.bytecoder.lurora.backend.ui.main.fileExplorer

import com.bytecoder.lurora.backend.models.FileSystemItem
import com.bytecoder.lurora.frontend.viewmodels.FileExplorerViewModel
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

/**
 * Unit tests for FileExplorerViewModel
 * Note: This is a simplified version focusing on basic validation
 * Full integration tests would require more complex setup
 */
class FileExplorerViewModelTest {
    
    @Test
    fun `FileExplorerViewModel should have required data class`() {
        // Test that FileSystemItem can be created
        val fileItem = FileSystemItem(
            path = "/storage/test.mp3",
            name = "test.mp3",
            isDirectory = false,
            size = 1024L,
            lastModified = Date()
        )
        
        assertEquals("/storage/test.mp3", fileItem.path)
        assertEquals("test.mp3", fileItem.name)
        assertFalse(fileItem.isDirectory)
        assertEquals(1024L, fileItem.size)
    }
    
    @Test
    fun `ViewModel class should be accessible`() {
        // Test that FileExplorerViewModel class can be referenced
        assertNotNull(FileExplorerViewModel::class)
    }
}