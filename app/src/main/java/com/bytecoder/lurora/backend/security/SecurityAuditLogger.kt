package com.bytecoder.lurora.backend.security

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bytecoder.lurora.backend.data.database.dao.SecurityAuditDao
import com.bytecoder.lurora.backend.data.database.entity.SecurityAuditLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security audit logger for tracking security-related events
 */
@Singleton
class SecurityAuditLogger @Inject constructor(
    private val context: Context,
    private val securityAuditDao: SecurityAuditDao,
    private val securityManager: SecurityManager
) {
    
    enum class SecurityEvent {
        FILE_ACCESS,
        FILE_ENCRYPTION,
        FILE_DECRYPTION,
        PERMISSION_REQUEST,
        PERMISSION_GRANTED,
        PERMISSION_DENIED,
        UNAUTHORIZED_ACCESS,
        FILE_SHARING,
        DOWNLOAD_STARTED,
        DOWNLOAD_COMPLETED,
        DATA_EXPORT,
        DATA_IMPORT,
        SECURITY_VIOLATION,
        LOGIN_ATTEMPT,
        APP_START,
        APP_BACKGROUND,
        SETTINGS_CHANGED,
        FILE_DELETION,
        SUSPICIOUS_ACTIVITY
    }
    
    enum class SecurityLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Log a security event
     * @param event The type of security event
     * @param description Description of the event
     * @param level Security level of the event
     * @param metadata Additional metadata about the event
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logSecurityEvent(
        event: SecurityEvent,
        description: String,
        level: SecurityLevel = SecurityLevel.INFO,
        metadata: Map<String, String> = emptyMap()
    ) {
        coroutineScope.launch {
            try {
                val auditLog = SecurityAuditLog(
                    timestamp = Date(),
                    event = event.name,
                    description = description,
                    level = level.name,
                    userId = getCurrentUserId(),
                    deviceInfo = getDeviceInfo(),
                    appVersion = getAppVersion(),
                    metadata = formatMetadata(metadata)
                )
                
                securityAuditDao.insertAuditLog(auditLog)
                
                // If it's a critical event, also store it securely
                if (level == SecurityLevel.CRITICAL) {
                    securityManager.storeSecureData(
                        "critical_event_${System.currentTimeMillis()}",
                        formatCriticalEvent(auditLog)
                    )
                }
                
            } catch (e: Exception) {
                // Fallback logging - write to system log if database fails
                android.util.Log.e("SecurityAudit", "Failed to log security event: $event", e)
            }
        }
    }
    
    /**
     * Log file access event
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logFileAccess(filePath: String, accessType: String) {
        logSecurityEvent(
            event = SecurityEvent.FILE_ACCESS,
            description = "File accessed: $filePath",
            metadata = mapOf(
                "filePath" to filePath,
                "accessType" to accessType,
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }
    
    /**
     * Log permission request
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logPermissionRequest(permission: String, granted: Boolean) {
        val event = if (granted) SecurityEvent.PERMISSION_GRANTED else SecurityEvent.PERMISSION_DENIED
        val level = if (granted) SecurityLevel.INFO else SecurityLevel.WARNING
        
        logSecurityEvent(
            event = event,
            description = "Permission $permission was ${if (granted) "granted" else "denied"}",
            level = level,
            metadata = mapOf(
                "permission" to permission,
                "granted" to granted.toString()
            )
        )
    }
    
    /**
     * Log unauthorized access attempt
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logUnauthorizedAccess(resource: String, reason: String) {
        logSecurityEvent(
            event = SecurityEvent.UNAUTHORIZED_ACCESS,
            description = "Unauthorized access attempted to: $resource",
            level = SecurityLevel.ERROR,
            metadata = mapOf(
                "resource" to resource,
                "reason" to reason,
                "severity" to "high"
            )
        )
    }
    
    /**
     * Log file sharing event
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logFileSharing(filePath: String, shareMethod: String) {
        logSecurityEvent(
            event = SecurityEvent.FILE_SHARING,
            description = "File shared: $filePath via $shareMethod",
            metadata = mapOf(
                "filePath" to filePath,
                "shareMethod" to shareMethod
            )
        )
    }
    
    /**
     * Log download events
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logDownloadEvent(event: SecurityEvent, url: String, filePath: String? = null) {
        logSecurityEvent(
            event = event,
            description = "Download ${event.name.lowercase()} for: $url",
            metadata = buildMap {
                put("url", url)
                filePath?.let { put("filePath", it) }
            }
        )
    }
    
    /**
     * Log security violation
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logSecurityViolation(violationType: String, details: String) {
        logSecurityEvent(
            event = SecurityEvent.SECURITY_VIOLATION,
            description = "Security violation detected: $violationType",
            level = SecurityLevel.CRITICAL,
            metadata = mapOf(
                "violationType" to violationType,
                "details" to details,
                "actionRequired" to "immediate_review"
            )
        )
    }
    
    /**
     * Log suspicious activity
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun logSuspiciousActivity(activity: String, riskScore: Int) {
        val level = when (riskScore) {
            in 0..30 -> SecurityLevel.INFO
            in 31..60 -> SecurityLevel.WARNING
            in 61..80 -> SecurityLevel.ERROR
            else -> SecurityLevel.CRITICAL
        }
        
        logSecurityEvent(
            event = SecurityEvent.SUSPICIOUS_ACTIVITY,
            description = "Suspicious activity detected: $activity",
            level = level,
            metadata = mapOf(
                "activity" to activity,
                "riskScore" to riskScore.toString()
            )
        )
    }
    
    /**
     * Get recent security events
     */
    suspend fun getRecentSecurityEvents(limit: Int = 100): List<SecurityAuditLog> {
        return securityAuditDao.getRecentAuditLogs(limit)
    }
    
    /**
     * Get critical security events
     */
    suspend fun getCriticalSecurityEvents(): List<SecurityAuditLog> {
        return securityAuditDao.getAuditLogsByLevel(SecurityLevel.CRITICAL.name)
    }
    
    /**
     * Clean up old audit logs (keep last 30 days)
     */
    suspend fun cleanupOldLogs() {
        val thirtyDaysAgo = Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000))
        securityAuditDao.deleteOldAuditLogs(thirtyDaysAgo)
    }
    
    /**
     * Export security logs for analysis
     */
    suspend fun exportSecurityLogs(): String {
        val logs = securityAuditDao.getAllAuditLogs()
        return logs.joinToString("\n") { log ->
            "${log.timestamp},${log.event},${log.level},${log.description},${log.metadata}"
        }
    }
    
    private fun getCurrentUserId(): String {
        // For now, return device ID or a generated user ID
        return securityManager.getSecureData("user_id") 
            ?: securityManager.generateSecureToken(16).also {
                securityManager.storeSecureData("user_id", it)
            }
    }
    
    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})"
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun formatMetadata(metadata: Map<String, String>): String {
        return metadata.entries.joinToString(";") { "${it.key}=${it.value}" }
    }
    
    private fun formatCriticalEvent(auditLog: SecurityAuditLog): String {
        return "CRITICAL|${auditLog.timestamp}|${auditLog.event}|${auditLog.description}|${auditLog.metadata}"
    }
}