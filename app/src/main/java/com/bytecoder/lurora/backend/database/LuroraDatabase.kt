package com.bytecoder.lurora.backend.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bytecoder.lurora.backend.models.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Room database for local storage
 */
@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistMediaCrossRef::class,
        HistoryEntryEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LuroraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): UserSettingsDao
}

/**
 * Type converters for Room
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromMediaType(value: MediaType): String {
        return value.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return MediaType.valueOf(value)
    }

    @TypeConverter
    fun fromHistorySource(value: HistorySource): String {
        return value.name
    }

    @TypeConverter
    fun toHistorySource(value: String): HistorySource {
        return HistorySource.valueOf(value)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return value.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        if (value.isEmpty()) return emptyMap()
        return value.split(",").associate {
            val parts = it.split(":")
            parts[0] to (parts.getOrNull(1) ?: "")
        }
    }
}

/**
 * Database entities
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val duration: Long,
    val mediaType: MediaType,
    val albumArtUri: String?,
    val subtitleUri: String?,
    val mimeType: String?,
    val size: Long,
    val dateAdded: Date,
    val playCount: Int,
    val isFavorite: Boolean,
    val lastPosition: Long,
    val metadata: Map<String, String>
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverArtUri: String?,
    val dateCreated: Date,
    val dateModified: Date,
    val isSystemPlaylist: Boolean
)

@Entity(
    tableName = "playlist_media_cross_ref",
    primaryKeys = ["playlistId", "mediaItemId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistMediaCrossRef(
    val playlistId: String,
    val mediaItemId: String,
    val position: Int
)

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey val id: String,
    val mediaItemId: String,
    val lastPlayedAt: Date,
    val playbackProgress: Float,
    val lastPosition: Long,
    val playCount: Int,
    val source: HistorySource,
    val isPrivateSession: Boolean
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
    val type: String // "string", "int", "boolean", "float"
)

/**
 * Data Access Objects
 */
@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType ORDER BY title ASC")
    fun getMediaByType(mediaType: MediaType): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(media: List<MediaItemEntity>)

    @Update
    suspend fun updateMedia(media: MediaItemEntity)

    @Delete
    suspend fun deleteMedia(media: MediaItemEntity)

    @Query("DELETE FROM media_items")
    suspend fun clearAllMedia()

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: String): MediaItemEntity?
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY dateModified DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isSystemPlaylist = 0 ORDER BY name ASC")
    fun getUserPlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithMedia(playlistId: String): PlaylistWithMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMediaToPlaylist(crossRef: PlaylistMediaCrossRef)

    @Delete
    suspend fun removeMediaFromPlaylist(crossRef: PlaylistMediaCrossRef)

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT h.*, m.* FROM history_entries h INNER JOIN media_items m ON h.mediaItemId = m.id ORDER BY h.lastPlayedAt DESC")
    fun getAllHistoryWithMedia(): Flow<List<HistoryWithMedia>>

    @Query("SELECT h.*, m.* FROM history_entries h INNER JOIN media_items m ON h.mediaItemId = m.id WHERE m.mediaType = :mediaType ORDER BY h.lastPlayedAt DESC")
    fun getHistoryByType(mediaType: MediaType): Flow<List<HistoryWithMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntryEntity)

    @Update
    suspend fun updateHistoryEntry(entry: HistoryEntryEntity)

    @Delete
    suspend fun deleteHistoryEntry(entry: HistoryEntryEntity)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()

    @Query("DELETE FROM history_entries WHERE lastPlayedAt < :cutoffDate")
    suspend fun deleteOldEntries(cutoffDate: Date)

    @Query("SELECT * FROM history_entries WHERE mediaItemId = :mediaItemId")
    suspend fun getHistoryForMedia(mediaItemId: String): HistoryEntryEntity?
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings")
    fun getAllSettings(): Flow<List<UserSettingsEntity>>

    @Query("SELECT value FROM user_settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: UserSettingsEntity)

    @Delete
    suspend fun deleteSetting(setting: UserSettingsEntity)

    @Query("DELETE FROM user_settings")
    suspend fun clearAllSettings()
}

/**
 * Relationship data classes
 */
data class PlaylistWithMedia(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistMediaCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "mediaItemId"
        )
    )
    val mediaItems: List<MediaItemEntity>
)

data class HistoryWithMedia(
    @Embedded val historyEntry: HistoryEntryEntity,
    @Relation(
        parentColumn = "mediaItemId",
        entityColumn = "id"
    )
    val mediaItem: MediaItemEntity
)

/**
 * Database migration
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example migration - add new column to existing table
        // database.execSQL("ALTER TABLE media_items ADD COLUMN newColumn TEXT")
    }
}
