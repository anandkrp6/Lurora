package com.bytecoder.lurora.backend.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager {
    
    companion object {
        // Permission request codes
        const val PERMISSION_REQUEST_CODE = 1001
        
        // All required permissions
        val REQUIRED_PERMISSIONS = arrayOf(
            // Storage and file access
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            
            // Network
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            
            // Media playback
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
            
            // Audio and haptic
            Manifest.permission.VIBRATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            
            // Bluetooth
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            
            // Notifications
            Manifest.permission.POST_NOTIFICATIONS,
            
            // Phone state for call handling
            Manifest.permission.READ_PHONE_STATE,
            
            // Overlay controls (optional)
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
        
        // Runtime permissions that need to be requested
        val RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return getRequiredPermissionsForVersion().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get permissions required for current Android version
     */
    private fun getRequiredPermissionsForVersion(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.READ_PHONE_STATE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ (API 31+)
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.READ_PHONE_STATE
                )
            }
            else -> {
                // Below Android 12
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE
                )
            }
        }
    }
    
    /**
     * Get missing permissions that need to be requested
     */
    fun getMissingPermissions(context: Context): Array<String> {
        return getRequiredPermissionsForVersion().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    /**
     * Request all missing permissions
     */
    fun requestPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions,
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Check if specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Handle permission request result
     */
    fun onPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onAllGranted: () -> Unit,
        onSomeRejected: (rejectedPermissions: List<String>) -> Unit
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val rejectedPermissions = mutableListOf<String>()
            
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    rejectedPermissions.add(permission)
                }
            }
            
            if (rejectedPermissions.isEmpty()) {
                onAllGranted()
            } else {
                onSomeRejected(rejectedPermissions)
            }
        }
    }
    
    /**
     * Check if permission should show rationale
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * Check if All Files Access permission is granted (Android 11+)
     */
    fun isAllFilesAccessGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not needed for Android 10 and below
        }
    }
    
    /**
     * Request All Files Access permission (opens Settings)
     */
    fun requestAllFilesAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            }
        }
    }
}