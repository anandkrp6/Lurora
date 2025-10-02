package com.bytecoder.lurora.backend.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.bytecoder.lurora.backend.analytics.AnalyticsManager
import com.bytecoder.lurora.backend.analytics.CrashReporter
import com.bytecoder.lurora.backend.data.database.dao.AnalyticsDao
import com.bytecoder.lurora.backend.data.database.dao.DownloadQueueDao
import com.bytecoder.lurora.backend.database.LuroraDatabase
import com.bytecoder.lurora.backend.download.AdvancedDownloadManager
import com.bytecoder.lurora.backend.performance.PerformanceMonitor
import com.bytecoder.lurora.backend.security.SecurityAuditLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension for DataStore
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Hilt module for analytics and monitoring dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideAnalyticsDao(
        database: LuroraDatabase
    ): AnalyticsDao {
        return database.analyticsDao()
    }
    
    @Provides
    @Singleton
    fun provideDownloadQueueDao(
        database: LuroraDatabase
    ): DownloadQueueDao {
        return database.downloadQueueDao()
    }
    
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.settingsDataStore
    }
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context
    ): PerformanceMonitor {
        return PerformanceMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsManager(
        @ApplicationContext context: Context,
        analyticsDao: AnalyticsDao,
        performanceMonitor: PerformanceMonitor,
        securityAuditLogger: SecurityAuditLogger
    ): AnalyticsManager {
        return AnalyticsManager(context, analyticsDao, performanceMonitor, securityAuditLogger)
    }
    
    @Provides
    @Singleton
    fun provideCrashReporter(
        @ApplicationContext context: Context,
        analyticsManager: AnalyticsManager,
        securityAuditLogger: SecurityAuditLogger
    ): CrashReporter {
        return CrashReporter(context, analyticsManager, securityAuditLogger)
    }
    
    @Provides
    @Singleton
    fun provideAdvancedDownloadManager(
        @ApplicationContext context: Context,
        downloadQueueDao: DownloadQueueDao,
        securityAuditLogger: SecurityAuditLogger,
        performanceMonitor: PerformanceMonitor,
        workManager: WorkManager
    ): AdvancedDownloadManager {
        return AdvancedDownloadManager(
            context, 
            downloadQueueDao, 
            securityAuditLogger, 
            performanceMonitor, 
            workManager
        )
    }
}