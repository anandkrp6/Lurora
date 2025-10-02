package com.bytecoder.lurora.backend.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bytecoder.lurora.backend.data.database.entity.SecurityAuditLog
import java.util.Date

/**
 * Data Access Object for security audit logs
 */
@Dao
interface SecurityAuditDao {
    
    /**
     * Insert a new security audit log entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(auditLog: SecurityAuditLog)
    
    /**
     * Insert multiple security audit log entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(auditLogs: List<SecurityAuditLog>)
    
    /**
     * Get recent audit logs ordered by timestamp descending
     */
    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAuditLogs(limit: Int): List<SecurityAuditLog>
    
    /**
     * Get all audit logs
     */
    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<SecurityAuditLog>
    
    /**
     * Get audit logs by security level
     */
    @Query("SELECT * FROM security_audit_logs WHERE level = :level ORDER BY timestamp DESC")
    suspend fun getAuditLogsByLevel(level: String): List<SecurityAuditLog>
    
    /**
     * Get audit logs by event type
     */
    @Query("SELECT * FROM security_audit_logs WHERE event = :event ORDER BY timestamp DESC")
    suspend fun getAuditLogsByEvent(event: String): List<SecurityAuditLog>
    
    /**
     * Get audit logs by user ID
     */
    @Query("SELECT * FROM security_audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAuditLogsByUser(userId: String): List<SecurityAuditLog>
    
    /**
     * Get audit logs within a date range
     */
    @Query("SELECT * FROM security_audit_logs WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getAuditLogsByDateRange(startDate: Date, endDate: Date): List<SecurityAuditLog>
    
    /**
     * Get audit logs for today
     */
    @Query("SELECT * FROM security_audit_logs WHERE date(timestamp/1000, 'unixepoch') = date('now') ORDER BY timestamp DESC")
    suspend fun getTodayAuditLogs(): List<SecurityAuditLog>
    
    /**
     * Search audit logs by description
     */
    @Query("SELECT * FROM security_audit_logs WHERE description LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    suspend fun searchAuditLogs(searchTerm: String): List<SecurityAuditLog>
    
    /**
     * Get count of audit logs by level
     */
    @Query("SELECT COUNT(*) FROM security_audit_logs WHERE level = :level")
    suspend fun getAuditLogCountByLevel(level: String): Int
    
    /**
     * Get count of audit logs by event type
     */
    @Query("SELECT COUNT(*) FROM security_audit_logs WHERE event = :event")
    suspend fun getAuditLogCountByEvent(event: String): Int
    
    /**
     * Delete audit logs older than specified date
     */
    @Query("DELETE FROM security_audit_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldAuditLogs(cutoffDate: Date)
    
    /**
     * Delete all audit logs
     */
    @Query("DELETE FROM security_audit_logs")
    suspend fun deleteAllAuditLogs()
    
    /**
     * Delete audit logs by level
     */
    @Query("DELETE FROM security_audit_logs WHERE level = :level")
    suspend fun deleteAuditLogsByLevel(level: String)
    
    /**
     * Get latest audit log for a specific event type
     */
    @Query("SELECT * FROM security_audit_logs WHERE event = :event ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAuditLogByEvent(event: String): SecurityAuditLog?
    
    /**
     * Get audit log statistics summary
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN level = 'INFO' THEN 1 END) as info_count,
            COUNT(CASE WHEN level = 'WARNING' THEN 1 END) as warning_count,
            COUNT(CASE WHEN level = 'ERROR' THEN 1 END) as error_count,
            COUNT(CASE WHEN level = 'CRITICAL' THEN 1 END) as critical_count
        FROM security_audit_logs
    """)
    suspend fun getAuditLogStatistics(): AuditLogStatistics
    
    /**
     * Get most frequent events
     */
    @Query("""
        SELECT event, COUNT(*) as count 
        FROM security_audit_logs 
        GROUP BY event 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getMostFrequentEvents(limit: Int = 10): List<EventFrequency>
    
    /**
     * Get events in the last N hours
     */
    @Query("SELECT * FROM security_audit_logs WHERE timestamp > :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentEvents(sinceTimestamp: Date): List<SecurityAuditLog>
}

/**
 * Data class for audit log statistics
 */
data class AuditLogStatistics(
    val total: Int,
    val info_count: Int,
    val warning_count: Int,
    val error_count: Int,
    val critical_count: Int
)

/**
 * Data class for event frequency statistics
 */
data class EventFrequency(
    val event: String,
    val count: Int
)