package com.bytecoder.lurora.backend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing an analytics event
 */
@Entity(tableName = "analytics_events")
data class AnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    /**
     * Type of analytics event
     */
    val eventType: String,
    
    /**
     * Event properties as serialized string
     */
    val properties: String = "",
    
    /**
     * Numeric value associated with the event (optional)
     */
    val value: Double? = null,
    
    /**
     * Session ID when event occurred
     */
    val sessionId: String,
    
    /**
     * Timestamp when event occurred
     */
    val timestamp: Long,
    
    /**
     * User ID associated with the event
     */
    val userId: String,
    
    /**
     * Device information
     */
    val deviceInfo: String,
    
    /**
     * Screen or context where event occurred
     */
    val screen: String? = null,
    
    /**
     * Source of the event (e.g., user, system, etc.)
     */
    val source: String = "user",
    
    /**
     * Platform information
     */
    val platform: String = "android",
    
    /**
     * App version when event occurred
     */
    val appVersion: String? = null,
    
    /**
     * Additional metadata
     */
    val metadata: String = ""
)