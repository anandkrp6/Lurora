package com.bytecoder.lurora.backend.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LuroraDatabase {
        return Room.databaseBuilder(
            context,
            LuroraDatabase::class.java,
            "lurora_database"
        )
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration() // Only for development
        .build()
    }

    @Provides
    fun provideMediaDao(database: LuroraDatabase): MediaDao {
        return database.mediaDao()
    }

    @Provides
    fun providePlaylistDao(database: LuroraDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideHistoryDao(database: LuroraDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideSettingsDao(database: LuroraDatabase): UserSettingsDao {
        return database.settingsDao()
    }
}