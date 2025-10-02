package com.bytecoder.lurora.backend.data.database.dao

import androidx.room.*
import com.bytecoder.lurora.backend.data.database.entity.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for analytics events
 */
@Dao
interface AnalyticsDao {
    
    /**
     * Insert a single analytics event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnalyticsEvent)
    
    /**
     * Insert multiple analytics events
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<AnalyticsEvent>)
    
    /**
     * Get all analytics events
     */
    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<AnalyticsEvent>
    
    /**
     * Get recent analytics events
     */
    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 100): List<AnalyticsEvent>
    
    /**
     * Get events by type
     */
    @Query("SELECT * FROM analytics_events WHERE eventType = :eventType ORDER BY timestamp DESC")
    suspend fun getEventsByType(eventType: String): List<AnalyticsEvent>
    
    /**
     * Get events by session ID
     */
    @Query("SELECT * FROM analytics_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsBySession(sessionId: String): List<AnalyticsEvent>
    
    /**
     * Get events by user ID
     */
    @Query("SELECT * FROM analytics_events WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getEventsByUser(userId: String): List<AnalyticsEvent>
    
    /**
     * Get events within date range
     */
    @Query("SELECT * FROM analytics_events WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getEventsByDateRange(startDate: Date, endDate: Date): List<AnalyticsEvent>
    
    /**
     * Get error events
     */
    @Query("SELECT * FROM analytics_events WHERE eventType = 'ERROR' OR eventType = 'CRASH' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getErrorEvents(limit: Int = 100): List<AnalyticsEvent>
    
    /**
     * Get session events (session start/end)
     */
    @Query("SELECT * FROM analytics_events WHERE eventType IN ('SESSION_START', 'SESSION_END') ORDER BY timestamp DESC")
    suspend fun getRecentSessions(): List<AnalyticsEvent>
    
    /**
     * Get screen view events
     */
    @Query("SELECT * FROM analytics_events WHERE eventType = 'SCREEN_VIEW' ORDER BY timestamp DESC")
    suspend fun getScreenViewEvents(): List<AnalyticsEvent>
    
    /**
     * Get user action events
     */
    @Query("SELECT * FROM analytics_events WHERE eventType = 'USER_ACTION' ORDER BY timestamp DESC")
    suspend fun getUserActionEvents(): List<AnalyticsEvent>
    
    /**
     * Get performance events
     */
    @Query("SELECT * FROM analytics_events WHERE eventType = 'PERFORMANCE' ORDER BY timestamp DESC")
    suspend fun getPerformanceEvents(): List<AnalyticsEvent>
    
    /**
     * Get events for today
     */
    @Query("SELECT * FROM analytics_events WHERE date(timestamp/1000, 'unixepoch') = date('now') ORDER BY timestamp DESC")
    suspend fun getTodayEvents(): List<AnalyticsEvent>
    
    /**
     * Get events for last 7 days
     */
    @Query("SELECT * FROM analytics_events WHERE timestamp > :sevenDaysAgo ORDER BY timestamp DESC")
    suspend fun getLastWeekEvents(sevenDaysAgo: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000): List<AnalyticsEvent>
    
    /**
     * Get events for last 30 days
     */
    @Query("SELECT * FROM analytics_events WHERE timestamp > :thirtyDaysAgo ORDER BY timestamp DESC")
    suspend fun getLastMonthEvents(thirtyDaysAgo: Long = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000): List<AnalyticsEvent>
    
    /**
     * Count events by type
     */
    @Query("SELECT eventType, COUNT(*) as count FROM analytics_events GROUP BY eventType ORDER BY count DESC")
    suspend fun getEventCountsByType(): List<EventCount>
    
    /**
     * Count events by session
     */
    @Query("SELECT sessionId, COUNT(*) as count FROM analytics_events GROUP BY sessionId ORDER BY count DESC")
    suspend fun getEventCountsBySession(): List<SessionEventCount>
    
    /**
     * Get unique users count
     */
    @Query("SELECT COUNT(DISTINCT userId) FROM analytics_events")
    suspend fun getUniqueUsersCount(): Int
    
    /**
     * Get unique sessions count
     */
    @Query("SELECT COUNT(DISTINCT sessionId) FROM analytics_events")
    suspend fun getUniqueSessionsCount(): Int
    
    /**
     * Get events with specific property
     */
    @Query("SELECT * FROM analytics_events WHERE properties LIKE '%' || :property || '%' ORDER BY timestamp DESC")
    suspend fun getEventsWithProperty(property: String): List<AnalyticsEvent>
    
    /**
     * Get events by screen
     */
    @Query("SELECT * FROM analytics_events WHERE screen = :screen ORDER BY timestamp DESC")
    suspend fun getEventsByScreen(screen: String): List<AnalyticsEvent>
    
    /**
     * Search events by properties or metadata
     */
    @Query("SELECT * FROM analytics_events WHERE properties LIKE '%' || :query || '%' OR metadata LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchEvents(query: String): List<AnalyticsEvent>
    
    /**
     * Delete events older than specified date
     */
    @Query("DELETE FROM analytics_events WHERE timestamp < :cutoffDate")
    suspend fun deleteOldEvents(cutoffDate: Long)
    
    /**
     * Delete all events
     */
    @Query("DELETE FROM analytics_events")
    suspend fun deleteAllEvents()
    
    /**
     * Delete events by type
     */
    @Query("DELETE FROM analytics_events WHERE eventType = :eventType")
    suspend fun deleteEventsByType(eventType: String)
    
    /**
     * Delete events by session
     */
    @Query("DELETE FROM analytics_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsBySession(sessionId: String)
    
    /**
     * Get analytics summary
     */
    @Query("""
        SELECT 
            COUNT(*) as totalEvents,
            COUNT(DISTINCT sessionId) as totalSessions,
            COUNT(DISTINCT userId) as totalUsers,
            COUNT(CASE WHEN eventType = 'ERROR' OR eventType = 'CRASH' THEN 1 END) as errorCount,
            MIN(timestamp) as firstEventTime,
            MAX(timestamp) as lastEventTime
        FROM analytics_events
    """)
    suspend fun getAnalyticsSummary(): AnalyticsSummary
    
    /**
     * Get most active users
     */
    @Query("SELECT userId, COUNT(*) as eventCount FROM analytics_events GROUP BY userId ORDER BY eventCount DESC LIMIT :limit")
    suspend fun getMostActiveUsers(limit: Int = 10): List<UserActivity>
    
    /**
     * Get most popular screens
     */
    @Query("SELECT screen, COUNT(*) as viewCount FROM analytics_events WHERE eventType = 'SCREEN_VIEW' AND screen IS NOT NULL GROUP BY screen ORDER BY viewCount DESC LIMIT :limit")
    suspend fun getMostPopularScreens(limit: Int = 10): List<ScreenPopularity>
    
    /**
     * Get event flow for a session
     */
    @Query("SELECT eventType, timestamp, screen FROM analytics_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSessionEventFlow(sessionId: String): List<EventFlow>
    
    /**
     * Get daily event counts
     */
    @Query("SELECT date(timestamp/1000, 'unixepoch') as date, COUNT(*) as count FROM analytics_events GROUP BY date ORDER BY date DESC LIMIT :days")
    suspend fun getDailyEventCounts(days: Int = 30): List<DailyCount>
    
    /**
     * Get hourly event counts for today
     */
    @Query("SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour, COUNT(*) as count FROM analytics_events WHERE date(timestamp/1000, 'unixepoch') = date('now') GROUP BY hour ORDER BY hour")
    suspend fun getHourlyEventCounts(): List<HourlyCount>
}

/**
 * Data classes for query results
 */
data class EventCount(
    val eventType: String,
    val count: Int
)

data class SessionEventCount(
    val sessionId: String,
    val count: Int
)

data class AnalyticsSummary(
    val totalEvents: Int,
    val totalSessions: Int,
    val totalUsers: Int,
    val errorCount: Int,
    val firstEventTime: Long,
    val lastEventTime: Long
)

data class UserActivity(
    val userId: String,
    val eventCount: Int
)

data class ScreenPopularity(
    val screen: String,
    val viewCount: Int
)

data class EventFlow(
    val eventType: String,
    val timestamp: Long,
    val screen: String?
)

data class DailyCount(
    val date: String,
    val count: Int
)

data class HourlyCount(
    val hour: String,
    val count: Int
)