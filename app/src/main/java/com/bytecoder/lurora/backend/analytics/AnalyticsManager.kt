package com.bytecoder.lurora.backend.analytics

import android.content.Context
import android.os.Build
import com.bytecoder.lurora.backend.data.database.dao.AnalyticsDao
import com.bytecoder.lurora.backend.data.database.entity.AnalyticsEvent
import com.bytecoder.lurora.backend.performance.PerformanceMonitor
import com.bytecoder.lurora.backend.security.SecurityAuditLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive analytics and monitoring system
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val context: Context,
    private val analyticsDao: AnalyticsDao,
    private val performanceMonitor: PerformanceMonitor,
    private val securityAuditLogger: SecurityAuditLogger
) {
    
    companion object {
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
        private const val BATCH_SIZE = 50
        private const val FLUSH_INTERVAL_MS = 60 * 1000L // 1 minute
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val eventQueue = mutableListOf<AnalyticsEvent>()
    private val sessionData = ConcurrentHashMap<String, Any>()
    
    // Session tracking
    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0L
    private var lastActivityTime: Long = 0L
    
    // User behavior tracking
    private val _userBehavior = MutableStateFlow(UserBehaviorMetrics())
    val userBehavior: StateFlow<UserBehaviorMetrics> = _userBehavior.asStateFlow()
    
    // App usage statistics
    private val _usageStats = MutableStateFlow(UsageStatistics())
    val usageStats: StateFlow<UsageStatistics> = _usageStats.asStateFlow()
    
    init {
        startAnalyticsSystem()
    }
    
    /**
     * Start the analytics system
     */
    private fun startAnalyticsSystem() {
        startSession()
        
        // Start periodic data flushing
        coroutineScope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                flushEvents()
                updateUsageStatistics()
            }
        }
        
        // Track app lifecycle
        trackEvent(AnalyticsEventType.APP_START, mapOf(
            "app_version" to getAppVersion(),
            "device_model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "session_id" to (currentSessionId ?: "unknown")
        ))
    }
    
    /**
     * Track an analytics event
     */
    fun trackEvent(
        eventType: AnalyticsEventType,
        properties: Map<String, Any> = emptyMap(),
        value: Double? = null
    ) {
        val event = AnalyticsEvent(
            eventType = eventType.name,
            properties = formatProperties(properties),
            value = value,
            sessionId = currentSessionId ?: generateSessionId(),
            timestamp = System.currentTimeMillis(),
            userId = getUserId(),
            deviceInfo = getDeviceInfo()
        )
        
        synchronized(eventQueue) {
            eventQueue.add(event)
        }
        
        updateUserBehavior(eventType, properties)
        updateLastActivity()
        
        // Flush if queue is full
        if (eventQueue.size >= BATCH_SIZE) {
            coroutineScope.launch { flushEvents() }
        }
    }
    
    /**
     * Track screen view
     */
    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap()) {
        trackEvent(AnalyticsEventType.SCREEN_VIEW, properties + mapOf("screen_name" to screenName))
    }
    
    /**
     * Track user action
     */
    fun trackUserAction(action: String, target: String? = null, properties: Map<String, Any> = emptyMap()) {
        trackEvent(AnalyticsEventType.USER_ACTION, properties + buildMap {
            put("action", action)
            target?.let { put("target", it) }
        })
    }
    
    /**
     * Track media playback
     */
    fun trackMediaPlayback(
        mediaType: String,
        duration: Long? = null,
        source: String? = null,
        properties: Map<String, Any> = emptyMap()
    ) {
        trackEvent(AnalyticsEventType.MEDIA_PLAYBACK, properties + buildMap {
            put("media_type", mediaType)
            duration?.let { put("duration", it) }
            source?.let { put("source", it) }
        })
    }
    
    /**
     * Track download events
     */
    fun trackDownload(
        url: String,
        fileSize: Long? = null,
        downloadTime: Long? = null,
        success: Boolean = true,
        properties: Map<String, Any> = emptyMap()
    ) {
        trackEvent(AnalyticsEventType.DOWNLOAD, properties + buildMap {
            put("url", url)
            put("success", success)
            fileSize?.let { put("file_size", it) }
            downloadTime?.let { put("download_time", it) }
        })
    }
    
    /**
     * Track error events
     */
    fun trackError(
        errorType: String,
        errorMessage: String,
        stackTrace: String? = null,
        isFatal: Boolean = false,
        properties: Map<String, Any> = emptyMap()
    ) {
        val errorEvent = AnalyticsEventType.ERROR
        
        trackEvent(errorEvent, properties + buildMap {
            put("error_type", errorType)
            put("error_message", errorMessage)
            put("is_fatal", isFatal)
            stackTrace?.let { put("stack_trace", it) }
        })
        
        // Also log to security audit for critical errors
        if (isFatal) {
            securityAuditLogger.logSecurityEvent(
                SecurityAuditLogger.SecurityEvent.SECURITY_VIOLATION,
                "Fatal error occurred: $errorMessage",
                SecurityAuditLogger.SecurityLevel.CRITICAL,
                mapOf("error_type" to errorType)
            )
        }
    }
    
    /**
     * Track performance metrics
     */
    fun trackPerformance(
        metric: String,
        value: Double,
        unit: String? = null,
        properties: Map<String, Any> = emptyMap()
    ) {
        trackEvent(AnalyticsEventType.PERFORMANCE, properties + buildMap {
            put("metric", metric)
            put("value", value)
            unit?.let { put("unit", it) }
        }, value)
    }
    
    /**
     * Track feature usage
     */
    fun trackFeatureUsage(
        featureName: String,
        action: String,
        properties: Map<String, Any> = emptyMap()
    ) {
        trackEvent(AnalyticsEventType.FEATURE_USAGE, properties + mapOf(
            "feature_name" to featureName,
            "action" to action
        ))
    }
    
    /**
     * Start a new session
     */
    fun startSession() {
        currentSessionId = generateSessionId()
        sessionStartTime = System.currentTimeMillis()
        lastActivityTime = sessionStartTime
        
        trackEvent(AnalyticsEventType.SESSION_START)
    }
    
    /**
     * End current session
     */
    fun endSession() {
        currentSessionId?.let { sessionId ->
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            
            trackEvent(AnalyticsEventType.SESSION_END, mapOf(
                "session_duration" to sessionDuration,
                "session_id" to sessionId
            ))
            
            coroutineScope.launch { flushEvents() }
        }
        
        currentSessionId = null
    }
    
    /**
     * Check and handle session timeout
     */
    fun checkSessionTimeout() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastActivityTime > SESSION_TIMEOUT_MS) {
            endSession()
            startSession()
        }
    }
    
    /**
     * Get analytics dashboard data
     */
    suspend fun getDashboardData(): AnalyticsDashboard {
        val events = analyticsDao.getRecentEvents(limit = 1000)
        val sessions = analyticsDao.getRecentSessions()
        val errors = analyticsDao.getErrorEvents(limit = 100)
        
        return AnalyticsDashboard(
            totalEvents = events.size,
            totalSessions = sessions.size,
            totalErrors = errors.size,
            averageSessionDuration = calculateAverageSessionDuration(sessions),
            topScreens = getTopScreens(events),
            topActions = getTopUserActions(events),
            errorRate = calculateErrorRate(events, errors),
            performanceMetrics = getPerformanceMetrics(events),
            userRetention = calculateUserRetention()
        )
    }
    
    /**
     * Export analytics data for analysis
     */
    suspend fun exportAnalyticsData(format: ExportFormat = ExportFormat.JSON): String {
        val events = analyticsDao.getAllEvents()
        
        return when (format) {
            ExportFormat.JSON -> exportAsJson(events)
            ExportFormat.CSV -> exportAsCsv(events)
        }
    }
    
    /**
     * Generate analytics report
     */
    suspend fun generateReport(
        startDate: Date,
        endDate: Date,
        reportType: ReportType = ReportType.COMPREHENSIVE
    ): AnalyticsReport {
        val events = analyticsDao.getEventsByDateRange(startDate, endDate)
        
        return when (reportType) {
            ReportType.COMPREHENSIVE -> generateComprehensiveReport(events, startDate, endDate)
            ReportType.PERFORMANCE -> generatePerformanceReport(events, startDate, endDate)
            ReportType.USER_BEHAVIOR -> generateUserBehaviorReport(events, startDate, endDate)
            ReportType.ERRORS -> generateErrorReport(events, startDate, endDate)
        }
    }
    
    /**
     * Flush events to database
     */
    private suspend fun flushEvents() {
        val eventsToFlush = synchronized(eventQueue) {
            if (eventQueue.isEmpty()) return
            
            val events = eventQueue.toList()
            eventQueue.clear()
            events
        }
        
        try {
            analyticsDao.insertEvents(eventsToFlush)
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsManager", "Failed to flush events", e)
            // Re-add events to queue for retry
            synchronized(eventQueue) {
                eventQueue.addAll(0, eventsToFlush)
            }
        }
    }
    
    /**
     * Update user behavior metrics
     */
    private fun updateUserBehavior(eventType: AnalyticsEventType, properties: Map<String, Any>) {
        val currentBehavior = _userBehavior.value
        
        _userBehavior.value = currentBehavior.copy(
            totalActions = currentBehavior.totalActions + 1,
            lastActionTime = System.currentTimeMillis(),
            actionFrequency = currentBehavior.actionFrequency + mapOf(eventType.name to 1),
            sessionDuration = if (currentSessionId != null) {
                System.currentTimeMillis() - sessionStartTime
            } else currentBehavior.sessionDuration
        )
    }
    
    /**
     * Update usage statistics
     */
    private suspend fun updateUsageStatistics() {
        val recentEvents = analyticsDao.getRecentEvents(limit = 1000)
        val sessions = analyticsDao.getRecentSessions()
        
        _usageStats.value = UsageStatistics(
            dailyActiveUsers = calculateDAU(),
            weeklyActiveUsers = calculateWAU(),
            monthlyActiveUsers = calculateMAU(),
            averageSessionDuration = calculateAverageSessionDuration(sessions),
            totalSessions = sessions.size,
            crashRate = calculateCrashRate(recentEvents),
            retentionRate = calculateRetentionRate()
        )
    }
    
    /**
     * Update last activity time
     */
    private fun updateLastActivity() {
        lastActivityTime = System.currentTimeMillis()
    }
    
    // Helper methods for calculations and formatting
    private fun generateSessionId(): String = "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    
    private fun getUserId(): String = "user_${android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)}"
    
    private fun getDeviceInfo(): String = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun formatProperties(properties: Map<String, Any>): String {
        return properties.entries.joinToString(";") { "${it.key}=${it.value}" }
    }
    
    // Placeholder implementations for complex calculations
    private fun calculateAverageSessionDuration(sessions: List<AnalyticsEvent>): Long = 0L
    private fun getTopScreens(events: List<AnalyticsEvent>): List<String> = emptyList()
    private fun getTopUserActions(events: List<AnalyticsEvent>): List<String> = emptyList()
    private fun calculateErrorRate(events: List<AnalyticsEvent>, errors: List<AnalyticsEvent>): Float = 0f
    private fun getPerformanceMetrics(events: List<AnalyticsEvent>): Map<String, Double> = emptyMap()
    private fun calculateUserRetention(): Float = 0f
    private fun calculateDAU(): Int = 0
    private fun calculateWAU(): Int = 0
    private fun calculateMAU(): Int = 0
    private fun calculateCrashRate(events: List<AnalyticsEvent>): Float = 0f
    private fun calculateRetentionRate(): Float = 0f
    
    private fun exportAsJson(events: List<AnalyticsEvent>): String = "{}" // Implement JSON export
    private fun exportAsCsv(events: List<AnalyticsEvent>): String = "" // Implement CSV export
    
    private fun generateComprehensiveReport(events: List<AnalyticsEvent>, startDate: Date, endDate: Date): AnalyticsReport = AnalyticsReport("Comprehensive Report", "")
    private fun generatePerformanceReport(events: List<AnalyticsEvent>, startDate: Date, endDate: Date): AnalyticsReport = AnalyticsReport("Performance Report", "")
    private fun generateUserBehaviorReport(events: List<AnalyticsEvent>, startDate: Date, endDate: Date): AnalyticsReport = AnalyticsReport("User Behavior Report", "")
    private fun generateErrorReport(events: List<AnalyticsEvent>, startDate: Date, endDate: Date): AnalyticsReport = AnalyticsReport("Error Report", "")
    
    // Data classes
    data class UserBehaviorMetrics(
        val totalActions: Int = 0,
        val lastActionTime: Long = 0L,
        val actionFrequency: Map<String, Int> = emptyMap(),
        val sessionDuration: Long = 0L
    )
    
    data class UsageStatistics(
        val dailyActiveUsers: Int = 0,
        val weeklyActiveUsers: Int = 0,
        val monthlyActiveUsers: Int = 0,
        val averageSessionDuration: Long = 0L,
        val totalSessions: Int = 0,
        val crashRate: Float = 0f,
        val retentionRate: Float = 0f
    )
    
    data class AnalyticsDashboard(
        val totalEvents: Int,
        val totalSessions: Int,
        val totalErrors: Int,
        val averageSessionDuration: Long,
        val topScreens: List<String>,
        val topActions: List<String>,
        val errorRate: Float,
        val performanceMetrics: Map<String, Double>,
        val userRetention: Float
    )
    
    data class AnalyticsReport(
        val title: String,
        val content: String
    )
    
    enum class AnalyticsEventType {
        APP_START,
        APP_BACKGROUND,
        APP_FOREGROUND,
        SESSION_START,
        SESSION_END,
        SCREEN_VIEW,
        USER_ACTION,
        MEDIA_PLAYBACK,
        DOWNLOAD,
        ERROR,
        CRASH,
        PERFORMANCE,
        FEATURE_USAGE,
        SETTINGS_CHANGED,
        PERMISSION_GRANTED,
        PERMISSION_DENIED
    }
    
    enum class ExportFormat {
        JSON,
        CSV
    }
    
    enum class ReportType {
        COMPREHENSIVE,
        PERFORMANCE,
        USER_BEHAVIOR,
        ERRORS
    }
}