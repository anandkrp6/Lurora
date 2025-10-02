package com.bytecoder.lurora.backend.analytics

import android.content.Context
import android.util.Log
import com.bytecoder.lurora.backend.security.SecurityAuditLogger
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash reporting and handling system
 */
@Singleton
class CrashReporter @Inject constructor(
    private val context: Context,
    private val analyticsManager: AnalyticsManager,
    private val securityAuditLogger: SecurityAuditLogger
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_CRASH_LOGS = 50
    }
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val crashLogDir: File by lazy {
        File(context.filesDir, CRASH_LOG_DIR).apply { mkdirs() }
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize crash reporting
     */
    fun initialize() {
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        // Clean up old crash logs
        coroutineScope.launch {
            cleanupOldCrashLogs()
        }
    }
    
    /**
     * Handle uncaught exceptions
     */
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            handleCrash(thread, exception)
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to handle crash", e)
        } finally {
            // Call the default handler to maintain normal crash behavior
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    /**
     * Handle application crash
     */
    private fun handleCrash(thread: Thread, exception: Throwable) {
        val crashReport = generateCrashReport(thread, exception)
        
        // Save crash report to file
        saveCrashReport(crashReport)
        
        // Track crash in analytics
        trackCrash(exception, crashReport)
        
        // Log security event for critical crashes
        securityAuditLogger.logSecurityEvent(
            SecurityAuditLogger.SecurityEvent.SECURITY_VIOLATION,
            "Application crash occurred: ${exception.message}",
            SecurityAuditLogger.SecurityLevel.CRITICAL,
            mapOf(
                "exception_type" to exception.javaClass.simpleName,
                "thread_name" to thread.name
            )
        )
        
        Log.e("CrashReporter", "Application crashed", exception)
    }
    
    /**
     * Report non-fatal exceptions
     */
    fun reportNonFatalException(exception: Throwable, context: String? = null) {
        coroutineScope.launch {
            try {
                val crashReport = generateNonFatalReport(exception, context)
                
                // Track in analytics
                analyticsManager.trackError(
                    errorType = exception.javaClass.simpleName,
                    errorMessage = exception.message ?: "Unknown error",
                    stackTrace = getStackTrace(exception),
                    isFatal = false,
                    properties = mapOf(
                        "context" to (context ?: "unknown"),
                        "thread" to Thread.currentThread().name
                    )
                )
                
                // Save to file for debugging
                saveNonFatalReport(crashReport)
                
            } catch (e: Exception) {
                Log.e("CrashReporter", "Failed to report non-fatal exception", e)
            }
        }
    }
    
    /**
     * Generate comprehensive crash report
     */
    private fun generateCrashReport(thread: Thread, exception: Throwable): CrashReport {
        return CrashReport(
            timestamp = System.currentTimeMillis(),
            exceptionType = exception.javaClass.simpleName,
            exceptionMessage = exception.message ?: "No message",
            stackTrace = getStackTrace(exception),
            threadName = thread.name,
            threadId = thread.id,
            deviceInfo = getDeviceInfo(),
            appInfo = getAppInfo(),
            memoryInfo = getMemoryInfo(),
            systemInfo = getSystemInfo(),
            isFatal = true
        )
    }
    
    /**
     * Generate non-fatal error report
     */
    private fun generateNonFatalReport(exception: Throwable, context: String?): CrashReport {
        return CrashReport(
            timestamp = System.currentTimeMillis(),
            exceptionType = exception.javaClass.simpleName,
            exceptionMessage = exception.message ?: "No message",
            stackTrace = getStackTrace(exception),
            threadName = Thread.currentThread().name,
            threadId = Thread.currentThread().id,
            deviceInfo = getDeviceInfo(),
            appInfo = getAppInfo(),
            memoryInfo = getMemoryInfo(),
            systemInfo = getSystemInfo(),
            isFatal = false,
            context = context
        )
    }
    
    /**
     * Save crash report to file
     */
    private fun saveCrashReport(crashReport: CrashReport) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val timestamp = dateFormat.format(Date(crashReport.timestamp))
            val fileName = "crash_${timestamp}_${crashReport.exceptionType}.txt"
            val file = File(crashLogDir, fileName)
            
            file.writeText(formatCrashReport(crashReport))
            
            Log.i("CrashReporter", "Crash report saved to: ${file.absolutePath}")
            
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to save crash report", e)
        }
    }
    
    /**
     * Save non-fatal error report
     */
    private fun saveNonFatalReport(crashReport: CrashReport) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val timestamp = dateFormat.format(Date(crashReport.timestamp))
            val fileName = "error_${timestamp}_${crashReport.exceptionType}.txt"
            val file = File(crashLogDir, fileName)
            
            file.writeText(formatCrashReport(crashReport))
            
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to save error report", e)
        }
    }
    
    /**
     * Track crash in analytics
     */
    private fun trackCrash(exception: Throwable, crashReport: CrashReport) {
        try {
            analyticsManager.trackError(
                errorType = exception.javaClass.simpleName,
                errorMessage = exception.message ?: "Unknown error",
                stackTrace = crashReport.stackTrace,
                isFatal = true,
                properties = mapOf(
                    "thread_name" to crashReport.threadName,
                    "thread_id" to crashReport.threadId.toString(),
                    "memory_used" to crashReport.memoryInfo.usedMemory.toString(),
                    "available_memory" to crashReport.memoryInfo.availableMemory.toString()
                )
            )
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to track crash in analytics", e)
        }
    }
    
    /**
     * Format crash report for file output
     */
    private fun formatCrashReport(crashReport: CrashReport): String {
        return buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Timestamp: ${Date(crashReport.timestamp)}")
            appendLine("Fatal: ${crashReport.isFatal}")
            appendLine("Exception Type: ${crashReport.exceptionType}")
            appendLine("Exception Message: ${crashReport.exceptionMessage}")
            crashReport.context?.let { appendLine("Context: $it") }
            appendLine()
            
            appendLine("=== THREAD INFO ===")
            appendLine("Thread Name: ${crashReport.threadName}")
            appendLine("Thread ID: ${crashReport.threadId}")
            appendLine()
            
            appendLine("=== DEVICE INFO ===")
            appendLine("Device: ${crashReport.deviceInfo.manufacturer} ${crashReport.deviceInfo.model}")
            appendLine("Android Version: ${crashReport.deviceInfo.androidVersion}")
            appendLine("API Level: ${crashReport.deviceInfo.apiLevel}")
            appendLine("Architecture: ${crashReport.deviceInfo.architecture}")
            appendLine()
            
            appendLine("=== APP INFO ===")
            appendLine("Package: ${crashReport.appInfo.packageName}")
            appendLine("Version: ${crashReport.appInfo.versionName} (${crashReport.appInfo.versionCode})")
            appendLine("Debug Build: ${crashReport.appInfo.isDebugBuild}")
            appendLine()
            
            appendLine("=== MEMORY INFO ===")
            appendLine("Used Memory: ${crashReport.memoryInfo.usedMemory} MB")
            appendLine("Available Memory: ${crashReport.memoryInfo.availableMemory} MB")
            appendLine("Total Memory: ${crashReport.memoryInfo.totalMemory} MB")
            appendLine("Max Memory: ${crashReport.memoryInfo.maxMemory} MB")
            appendLine()
            
            appendLine("=== SYSTEM INFO ===")
            appendLine("Free Storage: ${crashReport.systemInfo.freeStorage} MB")
            appendLine("Total Storage: ${crashReport.systemInfo.totalStorage} MB")
            appendLine("Battery Level: ${crashReport.systemInfo.batteryLevel}%")
            appendLine("Is Charging: ${crashReport.systemInfo.isCharging}")
            appendLine()
            
            appendLine("=== STACK TRACE ===")
            appendLine(crashReport.stackTrace)
        }
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTrace(exception: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        exception.printStackTrace(printWriter)
        return stringWriter.toString()
    }
    
    /**
     * Get device information
     */
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT,
            architecture = System.getProperty("os.arch") ?: "unknown"
        )
    }
    
    /**
     * Get app information
     */
    private fun getAppInfo(): AppInfo {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            AppInfo(
                packageName = context.packageName,
                versionName = packageInfo.versionName ?: "unknown",
                versionCode = packageInfo.longVersionCode,
                isDebugBuild = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            )
        } catch (e: Exception) {
            AppInfo("unknown", "unknown", 0, false)
        }
    }
    
    /**
     * Get memory information
     */
    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            availableMemory = memInfo.availMem / (1024 * 1024),
            totalMemory = runtime.totalMemory() / (1024 * 1024),
            maxMemory = runtime.maxMemory() / (1024 * 1024)
        )
    }
    
    /**
     * Get system information
     */
    private fun getSystemInfo(): SystemInfo {
        val statsFs = android.os.StatFs(context.filesDir.absolutePath)
        val freeStorage = statsFs.availableBytes / (1024 * 1024)
        val totalStorage = statsFs.totalBytes / (1024 * 1024)
        
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        
        return SystemInfo(
            freeStorage = freeStorage,
            totalStorage = totalStorage,
            batteryLevel = batteryLevel,
            isCharging = isCharging
        )
    }
    
    /**
     * Clean up old crash logs
     */
    private suspend fun cleanupOldCrashLogs() = withContext(Dispatchers.IO) {
        try {
            val files = crashLogDir.listFiles() ?: return@withContext
            
            if (files.size > MAX_CRASH_LOGS) {
                // Sort by last modified and delete oldest
                files.sortedBy { it.lastModified() }
                    .take(files.size - MAX_CRASH_LOGS)
                    .forEach { it.delete() }
            }
            
            // Delete files older than 30 days
            val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
            files.filter { it.lastModified() < cutoffTime }
                .forEach { it.delete() }
                
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to cleanup old crash logs", e)
        }
    }
    
    /**
     * Get all crash reports
     */
    suspend fun getCrashReports(): List<File> = withContext(Dispatchers.IO) {
        try {
            crashLogDir.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to get crash reports", e)
            emptyList()
        }
    }
    
    /**
     * Export crash reports
     */
    suspend fun exportCrashReports(): String = withContext(Dispatchers.IO) {
        try {
            val reports = getCrashReports()
            buildString {
                reports.forEach { file ->
                    appendLine("=== ${file.name} ===")
                    appendLine(file.readText())
                    appendLine()
                }
            }
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to export crash reports", e)
            "Failed to export crash reports: ${e.message}"
        }
    }
    
    // Data classes
    data class CrashReport(
        val timestamp: Long,
        val exceptionType: String,
        val exceptionMessage: String,
        val stackTrace: String,
        val threadName: String,
        val threadId: Long,
        val deviceInfo: DeviceInfo,
        val appInfo: AppInfo,
        val memoryInfo: MemoryInfo,
        val systemInfo: SystemInfo,
        val isFatal: Boolean,
        val context: String? = null
    )
    
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val apiLevel: Int,
        val architecture: String
    )
    
    data class AppInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val isDebugBuild: Boolean
    )
    
    data class MemoryInfo(
        val usedMemory: Long,
        val availableMemory: Long,
        val totalMemory: Long,
        val maxMemory: Long
    )
    
    data class SystemInfo(
        val freeStorage: Long,
        val totalStorage: Long,
        val batteryLevel: Int,
        val isCharging: Boolean
    )
}