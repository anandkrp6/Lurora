package com.bytecoder.lurora.backend.performance

import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Performance monitor for tracking application performance metrics
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Performance metrics state
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // Active operations tracking
    private val activeOperations = ConcurrentHashMap<String, OperationTracker>()
    
    // Memory monitoring
    private var isMonitoring = false
    private val monitoringInterval = 5000L // 5 seconds
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        coroutineScope.launch {
            while (isMonitoring) {
                collectMetrics()
                delay(monitoringInterval)
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    /**
     * Start tracking an operation
     */
    fun startOperation(operationName: String, category: String = "general"): String {
        val operationId = "${operationName}_${System.currentTimeMillis()}"
        val tracker = OperationTracker(
            id = operationId,
            name = operationName,
            category = category,
            startTime = System.currentTimeMillis(),
            startMemory = getCurrentMemoryUsage()
        )
        
        activeOperations[operationId] = tracker
        return operationId
    }
    
    /**
     * End tracking an operation
     */
    fun endOperation(operationId: String) {
        activeOperations[operationId]?.let { tracker ->
            val endTime = System.currentTimeMillis()
            val endMemory = getCurrentMemoryUsage()
            
            val completedOperation = CompletedOperation(
                name = tracker.name,
                category = tracker.category,
                duration = endTime - tracker.startTime,
                memoryDelta = endMemory.usedMemoryMB - tracker.startMemory.usedMemoryMB,
                timestamp = endTime
            )
            
            addCompletedOperation(completedOperation)
            activeOperations.remove(operationId)
        }
    }
    
    /**
     * Measure operation performance with automatic tracking
     */
    suspend fun <T> measureOperation(
        operationName: String,
        category: String = "general",
        operation: suspend () -> T
    ): T {
        val operationId = startOperation(operationName, category)
        return try {
            operation()
        } finally {
            endOperation(operationId)
        }
    }
    
    /**
     * Measure synchronous operation performance
     */
    fun <T> measureSyncOperation(
        operationName: String,
        category: String = "general",
        operation: () -> T
    ): T {
        val operationId = startOperation(operationName, category)
        return try {
            operation()
        } finally {
            endOperation(operationId)
        }
    }
    
    /**
     * Track UI frame rendering time
     */
    fun trackFrameTime(frameTimeNanos: Long) {
        val frameTimeMs = frameTimeNanos / 1_000_000.0
        
        val currentMetrics = _performanceMetrics.value
        val newFrameTimes = (currentMetrics.recentFrameTimes + frameTimeMs).takeLast(100)
        
        _performanceMetrics.value = currentMetrics.copy(
            recentFrameTimes = newFrameTimes,
            averageFrameTime = newFrameTimes.average(),
            maxFrameTime = newFrameTimes.maxOrNull() ?: 0.0,
            jankyFrameCount = currentMetrics.jankyFrameCount + if (frameTimeMs > 16.67) 1 else 0
        )
    }
    
    /**
     * Log performance warning
     */
    fun logPerformanceWarning(operation: String, warning: String, metrics: Map<String, Any> = emptyMap()) {
        val currentMetrics = _performanceMetrics.value
        val newWarning = PerformanceWarning(
            operation = operation,
            warning = warning,
            timestamp = System.currentTimeMillis(),
            metrics = metrics
        )
        
        _performanceMetrics.value = currentMetrics.copy(
            warnings = (currentMetrics.warnings + newWarning).takeLast(50)
        )
        
        android.util.Log.w("PerformanceMonitor", "Performance warning in $operation: $warning")
    }
    
    /**
     * Get current memory usage
     */
    fun getCurrentMemoryUsage(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val nativeHeap = Debug.getNativeHeapSize()
        val nativeAllocated = Debug.getNativeHeapAllocatedSize()
        
        return MemoryInfo(
            usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            totalMemoryMB = runtime.totalMemory() / (1024 * 1024),
            maxMemoryMB = runtime.maxMemory() / (1024 * 1024),
            nativeHeapMB = nativeHeap / (1024 * 1024),
            nativeAllocatedMB = nativeAllocated / (1024 * 1024),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if the app is experiencing performance issues
     */
    fun isPerformanceDegraded(): Boolean {
        val metrics = _performanceMetrics.value
        
        return metrics.averageFrameTime > 20.0 || // > 20ms average frame time
                metrics.memoryInfo.usedMemoryMB > metrics.memoryInfo.maxMemoryMB * 0.9 || // > 90% memory usage
                metrics.recentOperations.any { it.duration > 5000 } // Any operation > 5 seconds
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val metrics = _performanceMetrics.value
        
        val recentOperations = metrics.recentOperations.takeLast(100)
        val averageOperationTime = recentOperations.map { it.duration }.average()
        val slowOperations = recentOperations.count { it.duration > 1000 }
        
        return PerformanceSummary(
            averageFrameTime = metrics.averageFrameTime,
            jankyFramePercentage = if (metrics.recentFrameTimes.isNotEmpty()) {
                metrics.jankyFrameCount.toDouble() / metrics.recentFrameTimes.size * 100
            } else 0.0,
            averageOperationTime = averageOperationTime,
            slowOperationCount = slowOperations,
            memoryUsagePercentage = (metrics.memoryInfo.usedMemoryMB.toDouble() / 
                                   metrics.memoryInfo.maxMemoryMB.toDouble()) * 100,
            warningCount = metrics.warnings.size,
            isHealthy = !isPerformanceDegraded()
        )
    }
    
    /**
     * Export performance data for analysis
     */
    fun exportPerformanceData(): String {
        val metrics = _performanceMetrics.value
        
        return buildString {
            appendLine("Performance Report - ${java.util.Date()}")
            appendLine("========================================")
            appendLine()
            
            appendLine("Frame Performance:")
            appendLine("  Average Frame Time: ${String.format("%.2f", metrics.averageFrameTime)}ms")
            appendLine("  Max Frame Time: ${String.format("%.2f", metrics.maxFrameTime)}ms")
            appendLine("  Janky Frames: ${metrics.jankyFrameCount}")
            appendLine()
            
            appendLine("Memory Usage:")
            appendLine("  Used: ${metrics.memoryInfo.usedMemoryMB}MB")
            appendLine("  Total: ${metrics.memoryInfo.totalMemoryMB}MB")
            appendLine("  Max: ${metrics.memoryInfo.maxMemoryMB}MB")
            appendLine("  Native Heap: ${metrics.memoryInfo.nativeHeapMB}MB")
            appendLine()
            
            appendLine("Recent Operations (last 10):")
            metrics.recentOperations.takeLast(10).forEach { op ->
                appendLine("  ${op.name} (${op.category}): ${op.duration}ms")
            }
            appendLine()
            
            appendLine("Performance Warnings:")
            metrics.warnings.forEach { warning ->
                appendLine("  ${warning.operation}: ${warning.warning}")
            }
        }
    }
    
    private suspend fun collectMetrics() {
        val memoryInfo = getCurrentMemoryUsage()
        val currentMetrics = _performanceMetrics.value
        
        _performanceMetrics.value = currentMetrics.copy(
            memoryInfo = memoryInfo,
            lastUpdateTime = System.currentTimeMillis(),
            activeOperationCount = activeOperations.size
        )
        
        // Check for memory pressure
        if (memoryInfo.usedMemoryMB > memoryInfo.maxMemoryMB * 0.85) {
            logPerformanceWarning(
                "memory_pressure",
                "High memory usage detected",
                mapOf(
                    "usedMB" to memoryInfo.usedMemoryMB,
                    "maxMB" to memoryInfo.maxMemoryMB,
                    "percentage" to (memoryInfo.usedMemoryMB.toDouble() / memoryInfo.maxMemoryMB * 100)
                )
            )
        }
    }
    
    private fun addCompletedOperation(operation: CompletedOperation) {
        val currentMetrics = _performanceMetrics.value
        val newOperations = (currentMetrics.recentOperations + operation).takeLast(200)
        
        _performanceMetrics.value = currentMetrics.copy(
            recentOperations = newOperations,
            totalOperations = currentMetrics.totalOperations + 1
        )
        
        // Check for slow operations
        if (operation.duration > 2000) {
            logPerformanceWarning(
                operation.name,
                "Slow operation detected",
                mapOf(
                    "duration" to operation.duration,
                    "category" to operation.category,
                    "memoryDelta" to operation.memoryDelta
                )
            )
        }
    }
    
    data class PerformanceMetrics(
        val memoryInfo: MemoryInfo = MemoryInfo(),
        val recentFrameTimes: List<Double> = emptyList(),
        val averageFrameTime: Double = 0.0,
        val maxFrameTime: Double = 0.0,
        val jankyFrameCount: Int = 0,
        val recentOperations: List<CompletedOperation> = emptyList(),
        val totalOperations: Long = 0,
        val activeOperationCount: Int = 0,
        val warnings: List<PerformanceWarning> = emptyList(),
        val lastUpdateTime: Long = System.currentTimeMillis()
    )
    
    data class MemoryInfo(
        val usedMemoryMB: Long = 0,
        val totalMemoryMB: Long = 0,
        val maxMemoryMB: Long = 0,
        val nativeHeapMB: Long = 0,
        val nativeAllocatedMB: Long = 0,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class OperationTracker(
        val id: String,
        val name: String,
        val category: String,
        val startTime: Long,
        val startMemory: MemoryInfo
    )
    
    data class CompletedOperation(
        val name: String,
        val category: String,
        val duration: Long,
        val memoryDelta: Long,
        val timestamp: Long
    )
    
    data class PerformanceWarning(
        val operation: String,
        val warning: String,
        val timestamp: Long,
        val metrics: Map<String, Any>
    )
    
    data class PerformanceSummary(
        val averageFrameTime: Double,
        val jankyFramePercentage: Double,
        val averageOperationTime: Double,
        val slowOperationCount: Int,
        val memoryUsagePercentage: Double,
        val warningCount: Int,
        val isHealthy: Boolean
    )
}