package com.bytecoder.lurora

import android.app.Application
import com.bytecoder.lurora.backend.security.SecurityManager
import com.bytecoder.lurora.backend.security.SecurityAuditLogger
import com.bytecoder.lurora.backend.analytics.AnalyticsManager
import com.bytecoder.lurora.backend.analytics.CrashReporter
import com.bytecoder.lurora.backend.performance.PerformanceMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    
    @Inject
    lateinit var securityManager: SecurityManager
    
    @Inject
    lateinit var securityAuditLogger: SecurityAuditLogger
    
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    
    @Inject
    lateinit var crashReporter: CrashReporter
    
    @Inject
    lateinit var performanceMonitor: PerformanceMonitor
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize security framework
        try {
            securityManager.initialize()
            
            // Initialize crash reporting
            crashReporter.initialize()
            
            // Start performance monitoring
            performanceMonitor.startMonitoring()
            
            // Log application start
            securityAuditLogger.logSecurityEvent(
                event = SecurityAuditLogger.SecurityEvent.APP_START,
                description = "Application started",
                level = SecurityAuditLogger.SecurityLevel.INFO
            )
            
            // Track app start in analytics
            analyticsManager.trackEvent(
                AnalyticsManager.AnalyticsEventType.APP_START,
                mapOf(
                    "app_version" to getAppVersion(),
                    "first_launch" to isFirstLaunch().toString()
                )
            )
            
        } catch (e: Exception) {
            // Fallback logging if initialization fails
            android.util.Log.e("App", "Failed to initialize app components", e)
            crashReporter.reportNonFatalException(e, "App initialization")
        }
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Log memory pressure events for security monitoring
        try {
            securityAuditLogger.logSecurityEvent(
                event = SecurityAuditLogger.SecurityEvent.APP_BACKGROUND,
                description = "Memory trim level: $level",
                level = SecurityAuditLogger.SecurityLevel.INFO,
                metadata = mapOf("trimLevel" to level.toString())
            )
            
            // Track in analytics
            analyticsManager.trackEvent(
                AnalyticsManager.AnalyticsEventType.APP_BACKGROUND,
                mapOf("trim_level" to level)
            )
            
        } catch (e: Exception) {
            android.util.Log.w("App", "Failed to log memory trim event", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        try {
            // End analytics session
            analyticsManager.endSession()
            
            // Stop performance monitoring
            performanceMonitor.stopMonitoring()
            
        } catch (e: Exception) {
            android.util.Log.w("App", "Failed to cleanup on terminate", e)
        }
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        
        if (isFirstLaunch) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
        
        return isFirstLaunch
    }
}