package com.bytecoder.lurora.backend.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.content.FileProvider
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.io.File

class SharingUtilsTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var packageManager: PackageManager
    
    @Mock
    private lateinit var file: File
    
    @Mock
    private lateinit var resolveInfo: ResolveInfo
    
    private lateinit var sharingUtils: SharingUtils
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        sharingUtils = SharingUtils()
        
        // Mock context and package manager
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.packageName).thenReturn("com.bytecoder.lurora")
    }
    
    @Test
    fun `shareFile should start activity when file exists`() {
        // Given
        val filePath = "/storage/test.mp3"
        val mimeType = "audio/mpeg"
        
        // Mock file exists
        mockStatic(File::class.java).use { mockedFile ->
            mockedFile.`when`<File> { File(filePath) }.thenReturn(file)
            whenever(file.exists()).thenReturn(true)
            
            // Mock FileProvider
            mockStatic(FileProvider::class.java).use { mockedFileProvider ->
                val mockUri = mock<android.net.Uri>()
                mockedFileProvider.`when`<android.net.Uri> { 
                    FileProvider.getUriForFile(any(), any(), any()) 
                }.thenReturn(mockUri)
                
                // Mock package manager query
                whenever(packageManager.queryIntentActivities(any(), any()))
                    .thenReturn(listOf(resolveInfo))
                
                // When
                sharingUtils.shareFile(context, filePath, mimeType)
                
                // Then
                verify(context).startActivity(any())
            }
        }
    }
    
    @Test
    fun `shareFile should not start activity when file does not exist`() {
        // Given
        val filePath = "/storage/nonexistent.mp3"
        val mimeType = "audio/mpeg"
        
        // Mock file does not exist
        mockStatic(File::class.java).use { mockedFile ->
            mockedFile.`when`<File> { File(filePath) }.thenReturn(file)
            whenever(file.exists()).thenReturn(false)
            
            // When
            sharingUtils.shareFile(context, filePath, mimeType)
            
            // Then
            verify(context, never()).startActivity(any())
        }
    }
    
    @Test
    fun `shareMultipleFiles should start activity when files exist`() {
        // Given
        val filePaths = listOf("/storage/test1.mp3", "/storage/test2.mp4")
        
        // Mock files exist
        mockStatic(File::class.java).use { mockedFile ->
            val mockFile1 = mock<File>()
            val mockFile2 = mock<File>()
            
            mockedFile.`when`<File> { File(filePaths[0]) }.thenReturn(mockFile1)
            mockedFile.`when`<File> { File(filePaths[1]) }.thenReturn(mockFile2)
            
            whenever(mockFile1.exists()).thenReturn(true)
            whenever(mockFile2.exists()).thenReturn(true)
            
            // Mock FileProvider
            mockStatic(FileProvider::class.java).use { mockedFileProvider ->
                val mockUri1 = mock<android.net.Uri>()
                val mockUri2 = mock<android.net.Uri>()
                
                mockedFileProvider.`when`<android.net.Uri> { 
                    FileProvider.getUriForFile(eq(context), any(), eq(mockFile1)) 
                }.thenReturn(mockUri1)
                
                mockedFileProvider.`when`<android.net.Uri> { 
                    FileProvider.getUriForFile(eq(context), any(), eq(mockFile2)) 
                }.thenReturn(mockUri2)
                
                // Mock package manager query
                whenever(packageManager.queryIntentActivities(any(), any()))
                    .thenReturn(listOf(resolveInfo))
                
                // When
                sharingUtils.shareMultipleFiles(context, filePaths)
                
                // Then
                verify(context).startActivity(any())
            }
        }
    }
    
    @Test
    fun `shareMultipleFiles should not start activity when no files exist`() {
        // Given
        val filePaths = listOf("/storage/nonexistent1.mp3", "/storage/nonexistent2.mp4")
        
        // Mock files do not exist
        mockStatic(File::class.java).use { mockedFile ->
            val mockFile1 = mock<File>()
            val mockFile2 = mock<File>()
            
            mockedFile.`when`<File> { File(filePaths[0]) }.thenReturn(mockFile1)
            mockedFile.`when`<File> { File(filePaths[1]) }.thenReturn(mockFile2)
            
            whenever(mockFile1.exists()).thenReturn(false)
            whenever(mockFile2.exists()).thenReturn(false)
            
            // When
            sharingUtils.shareMultipleFiles(context, filePaths)
            
            // Then
            verify(context, never()).startActivity(any())
        }
    }
    
    @Test
    fun `shareFile should use correct intent action and type`() {
        // Given
        val filePath = "/storage/test.pdf"
        val mimeType = "application/pdf"
        
        // Mock file exists
        mockStatic(File::class.java).use { mockedFile ->
            mockedFile.`when`<File> { File(filePath) }.thenReturn(file)
            whenever(file.exists()).thenReturn(true)
            
            // Mock FileProvider
            mockStatic(FileProvider::class.java).use { mockedFileProvider ->
                val mockUri = mock<android.net.Uri>()
                mockedFileProvider.`when`<android.net.Uri> { 
                    FileProvider.getUriForFile(any(), any(), any()) 
                }.thenReturn(mockUri)
                
                // Mock package manager query
                whenever(packageManager.queryIntentActivities(any(), any()))
                    .thenReturn(listOf(resolveInfo))
                
                // When
                sharingUtils.shareFile(context, filePath, mimeType)
                
                // Then
                argumentCaptor<Intent>().apply {
                    verify(context).startActivity(capture())
                    val intent = firstValue
                    assertEquals("Should use ACTION_SEND", Intent.ACTION_SEND, intent.action)
                    assertEquals("Should set correct MIME type", mimeType, intent.type)
                    assertTrue("Should add FLAG_GRANT_READ_URI_PERMISSION", 
                        intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                }
            }
        }
    }
    
    @Test
    fun `shareMultipleFiles should use ACTION_SEND_MULTIPLE`() {
        // Given
        val filePaths = listOf("/storage/test1.mp3", "/storage/test2.mp3")
        
        // Mock files exist
        mockStatic(File::class.java).use { mockedFile ->
            val mockFile1 = mock<File>()
            val mockFile2 = mock<File>()
            
            mockedFile.`when`<File> { File(filePaths[0]) }.thenReturn(mockFile1)
            mockedFile.`when`<File> { File(filePaths[1]) }.thenReturn(mockFile2)
            
            whenever(mockFile1.exists()).thenReturn(true)
            whenever(mockFile2.exists()).thenReturn(true)
            
            // Mock FileProvider
            mockStatic(FileProvider::class.java).use { mockedFileProvider ->
                val mockUri1 = mock<android.net.Uri>()
                val mockUri2 = mock<android.net.Uri>()
                
                mockedFileProvider.`when`<android.net.Uri> { 
                    FileProvider.getUriForFile(eq(context), any(), eq(mockFile1)) 
                }.thenReturn(mockUri1)
                
                mockedFileProvider.`when`<android.net.Uri> { 
                    FileProvider.getUriForFile(eq(context), any(), eq(mockFile2)) 
                }.thenReturn(mockUri2)
                
                // Mock package manager query
                whenever(packageManager.queryIntentActivities(any(), any()))
                    .thenReturn(listOf(resolveInfo))
                
                // When
                sharingUtils.shareMultipleFiles(context, filePaths)
                
                // Then
                argumentCaptor<Intent>().apply {
                    verify(context).startActivity(capture())
                    val intent = firstValue
                    assertEquals("Should use ACTION_SEND_MULTIPLE", Intent.ACTION_SEND_MULTIPLE, intent.action)
                    assertEquals("Should set MIME type to */*", "*/*", intent.type)
                }
            }
        }
    }
}