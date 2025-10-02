package com.bytecoder.lurora.backend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a security audit log entry
 */
@Entity(tableName = "security_audit_logs")
data class SecurityAuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    /**
     * Timestamp when the event occurred
     */
    val timestamp: Date,
    
    /**
     * Type of security event (e.g., FILE_ACCESS, PERMISSION_REQUEST)
     */
    val event: String,
    
    /**
     * Human-readable description of the event
     */
    val description: String,
    
    /**
     * Security level of the event (INFO, WARNING, ERROR, CRITICAL)
     */
    val level: String,
    
    /**
     * User ID associated with the event
     */
    val userId: String,
    
    /**
     * Device information
     */
    val deviceInfo: String,
    
    /**
     * Application version when event occurred
     */
    val appVersion: String,
    
    /**
     * Additional metadata as key-value pairs (serialized as string)
     */
    val metadata: String = ""
)