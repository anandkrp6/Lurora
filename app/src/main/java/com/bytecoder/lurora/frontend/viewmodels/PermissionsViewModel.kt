package com.bytecoder.lurora.frontend.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.AppPermission
import com.bytecoder.lurora.backend.models.AppPermissions
import com.bytecoder.lurora.backend.models.PermissionCategory
import com.bytecoder.lurora.backend.models.PermissionStatus
import com.bytecoder.lurora.backend.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for comprehensive Permissions screen
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    private val _permissionsState = MutableStateFlow<List<AppPermission>>(emptyList())
    val permissionsState: StateFlow<List<AppPermission>> = _permissionsState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadPermissions()
    }
    
    /**
     * Load and update permissions status
     */
    fun loadAndUpdatePermissions(context: Context) {
        loadPermissions()
        updatePermissionStatus(context)
    }
    
    /**
     * Load all permissions applicable for the current device's Android version
     */
    fun loadPermissions() {
        viewModelScope.launch {
            _isLoading.value = true
            val permissions = AppPermissions.getApplicablePermissions()
            _permissionsState.value = permissions
            _isLoading.value = false
        }
    }
    
    /**
     * Update permission status for a specific permission
     */
    fun updatePermissionStatus(context: Context) {
        viewModelScope.launch {
            val currentPermissions = _permissionsState.value
            val updatedPermissions = currentPermissions.map { permission ->
                val status = when {
                    permission.androidPermission == android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val isGranted = Environment.isExternalStorageManager()
                            if (isGranted) {
                                PermissionStatus.GRANTED
                            } else {
                                PermissionStatus.DENIED
                            }
                        } else {
                            PermissionStatus.GRANTED // Not needed for older versions
                        }
                    }
                    permission.androidPermission == android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val isGranted = Settings.canDrawOverlays(context)
                            if (isGranted) {
                                PermissionStatus.GRANTED
                            } else {
                                PermissionStatus.DENIED
                            }
                        } else {
                            PermissionStatus.GRANTED // Not needed for older versions
                        }
                    }
                    // Handle combined photos/videos permission
                    permission.id == "read_media_visual" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val videoGranted = permissionManager.isPermissionGranted(context, android.Manifest.permission.READ_MEDIA_VIDEO)
                            val imagesGranted = permissionManager.isPermissionGranted(context, android.Manifest.permission.READ_MEDIA_IMAGES)
                            
                            // Check for partial access (Android 14+)
                            val hasPartialAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                try {
                                    permissionManager.isPermissionGranted(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
                                } catch (e: Exception) {
                                    false
                                }
                            } else {
                                false
                            }
                            
                            when {
                                videoGranted && imagesGranted -> PermissionStatus.GRANTED
                                hasPartialAccess -> PermissionStatus.LIMITED
                                else -> PermissionStatus.DENIED
                            }
                        } else {
                            PermissionStatus.GRANTED // Not applicable for older versions
                        }
                    }
                    else -> {
                        val isGranted = permissionManager.isPermissionGranted(context, permission.androidPermission)
                        if (isGranted) {
                            PermissionStatus.GRANTED
                        } else {
                            PermissionStatus.DENIED
                        }
                    }
                }
                permission.copy(status = status)
            }
            _permissionsState.value = updatedPermissions
        }
    }
    
    /**
     * Request special permissions that require settings navigation
     */
    fun requestSpecialPermission(context: Context, permission: AppPermission) {
        when (permission.androidPermission) {
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                requestAllFilesAccessFromContext(context)
            }
            android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                requestOverlayPermissionFromContext(context)
            }
            else -> {
                // Unrecognized special permission - do nothing
            }
        }
    }
    
    /**
     * Request All Files Access permission from context (navigates to settings)
     */
    private fun requestAllFilesAccessFromContext(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        // Fallback to general storage settings
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        // If all fails, silently ignore
                    }
                }
            }
        }
    }
    
    /**
     * Request overlay permission from context (navigates to settings)
     */
    private fun requestOverlayPermissionFromContext(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
    
    /**
     * Check if all critical permissions are granted
     */
    fun areAllCriticalPermissionsGranted(): Boolean {
        return _permissionsState.value
            .filter { it.importance == com.bytecoder.lurora.backend.models.PermissionImportance.CRITICAL }
            .all { it.status == PermissionStatus.GRANTED }
    }
    
    /**
     * Get count of granted permissions
     */
    fun getGrantedPermissionsCount(): Int {
        return _permissionsState.value
            .count { it.status == PermissionStatus.GRANTED }
    }
    
    /**
     * Get total permissions count
     */
    fun getTotalPermissionsCount(): Int {
        return _permissionsState.value.size
    }
    
    /**
     * Check if a permission should be requested (not already granted and is requestable)
     */
    fun shouldRequestPermission(context: Context, permission: AppPermission): Boolean {
        // Don't request special permissions here
        if (permission.requiresSpecialHandling) {
            return false
        }
        
        // Check if permission is already granted
        val isGranted = when (permission.id) {
            "read_media_visual" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val videoGranted = permissionManager.isPermissionGranted(context, android.Manifest.permission.READ_MEDIA_VIDEO)
                    val imagesGranted = permissionManager.isPermissionGranted(context, android.Manifest.permission.READ_MEDIA_IMAGES)
                    videoGranted && imagesGranted // Only consider fully granted
                } else {
                    true // Not applicable for older versions
                }
            }
            else -> permissionManager.isPermissionGranted(context, permission.androidPermission)
        }
        
        if (isGranted) {
            return false
        }
        
        // Check if permission is relevant for current Android version using the new filtering
        val currentApiLevel = Build.VERSION.SDK_INT
        return currentApiLevel >= permission.minApiLevel && currentApiLevel <= permission.maxApiLevel
    }
    
    /**
     * Check if we should redirect to settings instead of requesting permission
     * This happens when user has denied permission twice and Android won't show dialog anymore
     */
    fun shouldRedirectToSettings(context: Context, permission: AppPermission): Boolean {
        // Don't redirect special permissions - they always go to settings
        if (permission.requiresSpecialHandling) {
            return false
        }
        
        // Check if permission is already granted
        val isGranted = permissionManager.isPermissionGranted(context, permission.androidPermission)
        if (isGranted) {
            return false
        }
        
        // Get activity from context
        val activity = context as? Activity ?: return false
        
        // If we can't show rationale and permission is not granted, redirect to settings
        val shouldShowRationale = permissionManager.shouldShowRequestPermissionRationale(activity, permission.androidPermission)
        
        // If shouldShowRationale is false and permission is not granted,
        // it means user denied it permanently or it's the first time
        // We need additional logic to differentiate between first time and permanently denied
        // For now, if rationale is false and permission is denied, we'll still try the dialog first
        // The calling code should handle the case where dialog doesn't appear
        return false
    }
    
    /**
     * Request permission with automatic fallback to settings if needed
     */
    fun requestPermissionWithFallback(context: Context, permission: AppPermission, 
                                     onNeedDialog: () -> Unit, 
                                     onNeedSettings: () -> Unit) {
        when {
            permission.requiresSpecialHandling -> {
                // Special permissions always go to settings
                onNeedSettings()
            }
            shouldRequestPermission(context, permission) -> {
                // Try dialog first
                onNeedDialog()
            }
            else -> {
                // Permission not needed or already granted
                updatePermissionStatus(context)
            }
        }
    }
    
    /**
     * Open app settings page for manual permission management
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                // If all fails, silently ignore
            }
        }
    }
}