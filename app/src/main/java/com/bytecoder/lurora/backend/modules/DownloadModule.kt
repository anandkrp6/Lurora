package com.bytecoder.lurora.backend.modules

import android.content.Context
import androidx.work.WorkManager
import com.bytecoder.lurora.backend.database.LuroraDatabase
import com.bytecoder.lurora.backend.services.DownloadManager
import com.bytecoder.lurora.backend.services.DownloadManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        database: LuroraDatabase,
        workManager: WorkManager
    ): DownloadManager {
        return DownloadManagerImpl(context, database, workManager)
    }
}