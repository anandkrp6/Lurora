package com.bytecoder.lurora.backend.ui.more.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var context: Context
    
    private lateinit var viewModel: PermissionsViewModel
    private lateinit var testDispatcher: TestDispatcher
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        viewModel = PermissionsViewModel(context)
    }
    
    @Test
    fun `initial state should be correct`() = runTest {
        // Then
        val state = viewModel.uiState.first()
        assertTrue("Should not be loading initially", !state.isLoading)
        assertTrue("Should have some permissions", state.permissions.isNotEmpty())
        assertFalse("Should not show rationale initially", state.showRationale)
        assertNull("No permission should need rationale initially", state.rationalePermission)
    }
    
    @Test
    fun `permissions should include required permissions`() = runTest {
        // Then
        val state = viewModel.uiState.first()
        val permissionNames = state.permissions.map { it.permission }
        
        // Check for essential permissions
        assertTrue("Should include READ_EXTERNAL_STORAGE", 
            permissionNames.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE))
        assertTrue("Should include WRITE_EXTERNAL_STORAGE", 
            permissionNames.contains(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertTrue("Should include CAMERA", 
            permissionNames.contains(android.Manifest.permission.CAMERA))
        assertTrue("Should include RECORD_AUDIO", 
            permissionNames.contains(android.Manifest.permission.RECORD_AUDIO))
    }
    
    @Test
    fun `permissions should include Android 13+ permissions when appropriate`() = runTest {
        // Given - Mock Android 13+ (API 33+)
        // Note: This test would need reflection or PowerMock to properly test SDK version
        
        // Then
        val state = viewModel.uiState.first()
        val permissionNames = state.permissions.map { it.permission }
        
        // These permissions should be included for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertTrue("Should include READ_MEDIA_AUDIO for Android 13+", 
                permissionNames.contains(android.Manifest.permission.READ_MEDIA_AUDIO))
            assertTrue("Should include READ_MEDIA_VIDEO for Android 13+", 
                permissionNames.contains(android.Manifest.permission.READ_MEDIA_VIDEO))
            assertTrue("Should include READ_MEDIA_IMAGES for Android 13+", 
                permissionNames.contains(android.Manifest.permission.READ_MEDIA_IMAGES))
        }
    }
    
    @Test
    fun `checkPermission should return correct status when granted`() {
        // Given
        val permission = android.Manifest.permission.CAMERA
        whenever(context.checkSelfPermission(permission))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        
        // When
        val isGranted = viewModel.checkPermission(permission)
        
        // Then
        assertTrue("Permission should be granted", isGranted)
    }
    
    @Test
    fun `checkPermission should return correct status when denied`() {
        // Given
        val permission = android.Manifest.permission.CAMERA
        whenever(context.checkSelfPermission(permission))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        
        // When
        val isGranted = viewModel.checkPermission(permission)
        
        // Then
        assertFalse("Permission should be denied", isGranted)
    }
    
    @Test
    fun `refreshPermissions should update permission states`() = runTest {
        // Given
        whenever(context.checkSelfPermission(android.Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        whenever(context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        
        // When
        viewModel.refreshPermissions()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        val cameraPermission = state.permissions.find { 
            it.permission == android.Manifest.permission.CAMERA 
        }
        val storagePermission = state.permissions.find { 
            it.permission == android.Manifest.permission.READ_EXTERNAL_STORAGE 
        }
        
        assertNotNull("Camera permission should exist", cameraPermission)
        assertNotNull("Storage permission should exist", storagePermission)
        assertTrue("Camera permission should be granted", cameraPermission?.isGranted == true)
        assertFalse("Storage permission should be denied", storagePermission?.isGranted == true)
    }
    
    @Test
    fun `showRationaleDialog should update state correctly`() = runTest {
        // Given
        val permission = android.Manifest.permission.CAMERA
        
        // When
        viewModel.showRationaleDialog(permission)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertTrue("Should show rationale", state.showRationale)
        assertEquals("Should set rationale permission", permission, state.rationalePermission)
    }
    
    @Test
    fun `dismissRationaleDialog should clear rationale state`() = runTest {
        // Given - Set up rationale state
        viewModel.showRationaleDialog(android.Manifest.permission.CAMERA)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.dismissRationaleDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertFalse("Should not show rationale", state.showRationale)
        assertNull("Should clear rationale permission", state.rationalePermission)
    }
    
    @Test
    fun `permission categories should be properly defined`() = runTest {
        // Then
        val state = viewModel.uiState.first()
        val permissions = state.permissions
        
        // Check that permissions have proper categories
        val storagePermissions = permissions.filter { it.category == "Storage" }
        val mediaPermissions = permissions.filter { it.category == "Media" }
        val hardwarePermissions = permissions.filter { it.category == "Hardware" }
        val locationPermissions = permissions.filter { it.category == "Location" }
        val notificationPermissions = permissions.filter { it.category == "Notifications" }
        
        assertTrue("Should have storage permissions", storagePermissions.isNotEmpty())
        assertTrue("Should have media permissions", mediaPermissions.isNotEmpty())
        assertTrue("Should have hardware permissions", hardwarePermissions.isNotEmpty())
        
        // Check that essential permissions are categorized correctly
        val readStorage = permissions.find { 
            it.permission == android.Manifest.permission.READ_EXTERNAL_STORAGE 
        }
        assertEquals("READ_EXTERNAL_STORAGE should be in Storage category", 
            "Storage", readStorage?.category)
        
        val camera = permissions.find { 
            it.permission == android.Manifest.permission.CAMERA 
        }
        assertEquals("CAMERA should be in Hardware category", 
            "Hardware", camera?.category)
    }
    
    @Test
    fun `all permissions should have descriptions`() = runTest {
        // Then
        val state = viewModel.uiState.first()
        val permissions = state.permissions
        
        permissions.forEach { permission ->
            assertFalse("Permission ${permission.permission} should have a description",
                permission.description.isBlank())
            assertFalse("Permission ${permission.permission} should have a name",
                permission.name.isBlank())
        }
    }
}