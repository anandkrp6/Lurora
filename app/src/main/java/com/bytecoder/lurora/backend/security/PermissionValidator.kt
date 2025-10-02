package com.bytecoder.lurora.backend.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission validator for enhanced security checks
 */
@Singleton
class PermissionValidator @Inject constructor(
    private val context: Context,
    private val securityAuditLogger: SecurityAuditLogger
) {
    
    companion object {
        private val DANGEROUS_PERMISSIONS = setOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS
        )
        
        private val MEDIA_PERMISSIONS_API_33 = setOf(
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    }
    
    /**
     * Check if a permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        
        // Log permission check
        securityAuditLogger.logPermissionRequest(permission, isGranted)
        
        return isGranted
    }
    
    /**
     * Check multiple permissions
     */
    fun arePermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all { isPermissionGranted(it) }
    }
    
    /**
     * Get required permissions for the current Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // Older Android versions
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    
    /**
     * Get storage permissions for the current Android version
     */
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    /**
     * Check if all storage permissions are granted
     */
    fun hasStoragePermissions(): Boolean {
        return arePermissionsGranted(getStoragePermissions())
    }
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean {
        return isPermissionGranted(android.Manifest.permission.CAMERA)
    }
    
    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioPermission(): Boolean {
        return isPermissionGranted(android.Manifest.permission.RECORD_AUDIO)
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(): Boolean {
        return isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                isPermissionGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Not required on older versions
        }
    }
    
    /**
     * Validate file access permissions
     */
    fun validateFileAccess(filePath: String): ValidationResult {
        if (!hasStoragePermissions()) {
            securityAuditLogger.logUnauthorizedAccess(filePath, "Missing storage permissions")
            return ValidationResult.PERMISSION_DENIED
        }
        
        // Check if file path is suspicious
        if (isSuspiciousFilePath(filePath)) {
            securityAuditLogger.logSuspiciousActivity("Suspicious file path access: $filePath", 70)
            return ValidationResult.SUSPICIOUS_PATH
        }
        
        return ValidationResult.ALLOWED
    }
    
    /**
     * Validate camera access
     */
    fun validateCameraAccess(): ValidationResult {
        if (!hasCameraPermission()) {
            securityAuditLogger.logUnauthorizedAccess("camera", "Missing camera permission")
            return ValidationResult.PERMISSION_DENIED
        }
        
        return ValidationResult.ALLOWED
    }
    
    /**
     * Validate audio recording access
     */
    fun validateAudioAccess(): ValidationResult {
        if (!hasAudioPermission()) {
            securityAuditLogger.logUnauthorizedAccess("microphone", "Missing audio permission")
            return ValidationResult.PERMISSION_DENIED
        }
        
        return ValidationResult.ALLOWED
    }
    
    /**
     * Check if a file path looks suspicious
     */
    private fun isSuspiciousFilePath(filePath: String): Boolean {
        val suspiciousPatterns = listOf(
            "/system/",
            "/data/data/",
            "/proc/",
            "/dev/",
            "/root/",
            "/..",
            "../",
            ".ssh/",
            ".android_secure/",
            "/cache/",
            "/tmp/"
        )
        
        return suspiciousPatterns.any { pattern ->
            filePath.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Get missing permissions from a list
     */
    fun getMissingPermissions(permissions: Array<String>): List<String> {
        return permissions.filter { !isPermissionGranted(it) }
    }
    
    /**
     * Check if permission is dangerous (requires user approval)
     */
    fun isDangerousPermission(permission: String): Boolean {
        return permission in DANGEROUS_PERMISSIONS || 
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permission in MEDIA_PERMISSIONS_API_33)
    }
    
    /**
     * Get permission security risk level
     */
    fun getPermissionRiskLevel(permission: String): RiskLevel {
        return when (permission) {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_IMAGES -> RiskLevel.MEDIUM
            
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO -> RiskLevel.HIGH
            
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_SMS -> RiskLevel.HIGH
            
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.SEND_SMS -> RiskLevel.CRITICAL
            
            else -> RiskLevel.LOW
        }
    }
    
    /**
     * Generate permission security report
     */
    fun generateSecurityReport(): PermissionSecurityReport {
        val requiredPermissions = getRequiredPermissions()
        val grantedPermissions = requiredPermissions.filter { isPermissionGranted(it) }
        val missingPermissions = requiredPermissions.filter { !isPermissionGranted(it) }
        
        val riskScore = calculateRiskScore(grantedPermissions)
        
        return PermissionSecurityReport(
            totalRequired = requiredPermissions.size,
            granted = grantedPermissions.size,
            missing = missingPermissions.size,
            grantedPermissions = grantedPermissions,
            missingPermissions = missingPermissions,
            riskScore = riskScore,
            riskLevel = when (riskScore) {
                in 0..25 -> RiskLevel.LOW
                in 26..50 -> RiskLevel.MEDIUM
                in 51..75 -> RiskLevel.HIGH
                else -> RiskLevel.CRITICAL
            }
        )
    }
    
    private fun calculateRiskScore(grantedPermissions: List<String>): Int {
        return grantedPermissions.sumOf { permission: String ->
            when (getPermissionRiskLevel(permission)) {
                RiskLevel.LOW -> 5
                RiskLevel.MEDIUM -> 15
                RiskLevel.HIGH -> 25
                RiskLevel.CRITICAL -> 40
            }.toInt()
        }
    }
    
    enum class ValidationResult {
        ALLOWED,
        PERMISSION_DENIED,
        SUSPICIOUS_PATH,
        SECURITY_VIOLATION
    }
    
    enum class RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    data class PermissionSecurityReport(
        val totalRequired: Int,
        val granted: Int,
        val missing: Int,
        val grantedPermissions: List<String>,
        val missingPermissions: List<String>,
        val riskScore: Int,
        val riskLevel: RiskLevel
    )
}