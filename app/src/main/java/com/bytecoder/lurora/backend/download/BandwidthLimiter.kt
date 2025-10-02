package com.bytecoder.lurora.backend.download

import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Bandwidth limiter for controlling download speeds
 */
class BandwidthLimiter {
    
    private val globalLimitKbps = AtomicInteger(0) // 0 means no limit
    private val downloadSpeeds = ConcurrentHashMap<String, DownloadSpeedTracker>()
    private val lastGlobalCheck = AtomicLong(System.currentTimeMillis())
    private val globalBytesThisSecond = AtomicLong(0L)
    
    /**
     * Set global bandwidth limit in KB/s
     */
    fun setGlobalLimit(limitKbps: Int) {
        globalLimitKbps.set(limitKbps)
    }
    
    /**
     * Get current global limit
     */
    fun getGlobalLimit(): Int {
        return globalLimitKbps.get()
    }
    
    /**
     * Wait for bandwidth availability before downloading chunk
     */
    suspend fun waitForBandwidth(downloadId: String, chunkSize: Int) {
        val limitKbps = globalLimitKbps.get()
        if (limitKbps <= 0) return // No limit
        
        val currentTime = System.currentTimeMillis()
        val tracker = downloadSpeeds.getOrPut(downloadId) { DownloadSpeedTracker() }
        
        // Reset counters every second
        if (currentTime - lastGlobalCheck.get() >= 1000) {
            lastGlobalCheck.set(currentTime)
            globalBytesThisSecond.set(0L)
            tracker.reset(currentTime)
        }
        
        val globalLimitBytes = limitKbps * 1024L
        val currentGlobalBytes = globalBytesThisSecond.get()
        
        // Check if we would exceed global limit
        if (currentGlobalBytes + chunkSize > globalLimitBytes) {
            val timeToWait = 1000 - (currentTime - lastGlobalCheck.get())
            if (timeToWait > 0) {
                delay(timeToWait)
                // Reset after waiting
                lastGlobalCheck.set(System.currentTimeMillis())
                globalBytesThisSecond.set(0L)
                tracker.reset(System.currentTimeMillis())
            }
        }
        
        // Update counters
        globalBytesThisSecond.addAndGet(chunkSize.toLong())
        tracker.addBytes(chunkSize.toLong())
    }
    
    /**
     * Get current download speed for a specific download
     */
    fun getCurrentSpeed(downloadId: String): Long {
        return downloadSpeeds[downloadId]?.getCurrentSpeed() ?: 0L
    }
    
    /**
     * Remove tracking for completed download
     */
    fun removeDownload(downloadId: String) {
        downloadSpeeds.remove(downloadId)
    }
    
    /**
     * Get bandwidth usage statistics
     */
    fun getBandwidthStats(): BandwidthStats {
        val totalSpeed = downloadSpeeds.values.sumOf { it.getCurrentSpeed() }
        val activeDownloads = downloadSpeeds.size
        val globalLimit = globalLimitKbps.get()
        
        return BandwidthStats(
            currentSpeedKbps = totalSpeed / 1024,
            globalLimitKbps = globalLimit,
            utilizationPercentage = if (globalLimit > 0) {
                ((totalSpeed / 1024.0) / globalLimit * 100).toFloat()
            } else 0f,
            activeDownloads = activeDownloads
        )
    }
    
    private class DownloadSpeedTracker {
        private var bytesThisSecond = AtomicLong(0L)
        private var lastSecondStart = AtomicLong(System.currentTimeMillis())
        private var currentSpeed = AtomicLong(0L)
        
        fun addBytes(bytes: Long) {
            bytesThisSecond.addAndGet(bytes)
        }
        
        fun getCurrentSpeed(): Long {
            return currentSpeed.get()
        }
        
        fun reset(currentTime: Long) {
            currentSpeed.set(bytesThisSecond.get())
            bytesThisSecond.set(0L)
            lastSecondStart.set(currentTime)
        }
    }
    
    data class BandwidthStats(
        val currentSpeedKbps: Long,
        val globalLimitKbps: Int,
        val utilizationPercentage: Float,
        val activeDownloads: Int
    )
}