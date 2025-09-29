package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.navigation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _videoFiles = MutableStateFlow<List<MediaItem>>(emptyList())
    val videoFiles: StateFlow<List<MediaItem>> = _videoFiles.asStateFlow()

    private val _audioFiles = MutableStateFlow<List<MediaItem>>(emptyList())
    val audioFiles: StateFlow<List<MediaItem>> = _audioFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOption = MutableStateFlow(com.bytecoder.lurora.frontend.navigation.SortOption.NAME_ASC)
    val sortOption: StateFlow<com.bytecoder.lurora.frontend.navigation.SortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(com.bytecoder.lurora.frontend.navigation.FilterOption.ALL)
    val filterOption: StateFlow<com.bytecoder.lurora.frontend.navigation.FilterOption> = _filterOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        scanMediaFiles()
    }

    fun scanMediaFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val videos = scanVideoFiles()
                val audios = scanAudioFiles()
                _videoFiles.value = videos
                _audioFiles.value = audios
            } catch (e: Exception) {
                // Handle scanning error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLibrary() {
        scanMediaFiles()
    }

    fun setSortOption(option: com.bytecoder.lurora.frontend.navigation.SortOption) {
        _sortOption.value = option
    }

    fun setFilterOption(option: com.bytecoder.lurora.frontend.navigation.FilterOption) {
        _filterOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredVideos(): StateFlow<List<MediaItem>> {
        return combine(
            _videoFiles,
            _sortOption,
            _filterOption,
            _searchQuery
        ) { videos, sort, filter, search ->
            applyFiltersAndSort(videos, sort, filter, search)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getFilteredAudios(): StateFlow<List<MediaItem>> {
        return combine(
            _audioFiles,
            _sortOption,
            _filterOption,
            _searchQuery
        ) { audios, sort, filter, search ->
            applyFiltersAndSort(audios, sort, filter, search)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private fun applyFiltersAndSort(
        items: List<MediaItem>,
        sort: com.bytecoder.lurora.frontend.navigation.SortOption,
        filter: com.bytecoder.lurora.frontend.navigation.FilterOption,
        search: String
    ): List<MediaItem> {
        var filtered = items

        // Apply search filter
        if (search.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.title.contains(search, ignoreCase = true) ||
                item.artist?.contains(search, ignoreCase = true) == true ||
                item.album?.contains(search, ignoreCase = true) == true
            }
        }

        // Apply file type filter
        filtered = when (filter) {
            com.bytecoder.lurora.frontend.navigation.FilterOption.ALL -> filtered
            com.bytecoder.lurora.frontend.navigation.FilterOption.RECENT -> filtered.sortedByDescending { it.dateAdded }.take(50)
            com.bytecoder.lurora.frontend.navigation.FilterOption.FAVORITES -> filtered.filter { it.isFavorite }
            com.bytecoder.lurora.frontend.navigation.FilterOption.DOWNLOADED -> filtered
        }

        // Apply sorting
        filtered = when (sort) {
            com.bytecoder.lurora.frontend.navigation.SortOption.NAME_ASC -> filtered.sortedBy { it.title }
            com.bytecoder.lurora.frontend.navigation.SortOption.NAME_DESC -> filtered.sortedByDescending { it.title }
            com.bytecoder.lurora.frontend.navigation.SortOption.DATE_ADDED_ASC -> filtered.sortedBy { it.dateAdded }
            com.bytecoder.lurora.frontend.navigation.SortOption.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAdded }
            com.bytecoder.lurora.frontend.navigation.SortOption.SIZE_ASC -> filtered.sortedBy { it.size }
            com.bytecoder.lurora.frontend.navigation.SortOption.SIZE_DESC -> filtered.sortedByDescending { it.size }
            com.bytecoder.lurora.frontend.navigation.SortOption.DURATION_ASC -> filtered.sortedBy { it.duration }
            com.bytecoder.lurora.frontend.navigation.SortOption.DURATION_DESC -> filtered.sortedByDescending { it.duration }
            com.bytecoder.lurora.frontend.navigation.SortOption.ARTIST_ASC -> filtered.sortedBy { it.artist ?: "" }
            com.bytecoder.lurora.frontend.navigation.SortOption.ARTIST_DESC -> filtered.sortedByDescending { it.artist ?: "" }
            com.bytecoder.lurora.frontend.navigation.SortOption.ALBUM_ASC -> filtered.sortedBy { it.album ?: "" }
            com.bytecoder.lurora.frontend.navigation.SortOption.ALBUM_DESC -> filtered.sortedByDescending { it.album ?: "" }
        }

        return filtered
    }

    private fun scanVideoFiles(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM
        )

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(displayNameColumn) ?: ""
                val title = it.getString(titleColumn) ?: displayName
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val data = it.getString(dataColumn) ?: ""
                val mimeType = it.getString(mimeTypeColumn) ?: ""
                val dateAdded = it.getLong(dateAddedColumn) * 1000L // Convert to milliseconds
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)

                // Verify file exists
                if (data.isNotEmpty() && File(data).exists()) {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    mediaItems.add(
                        MediaItem(
                            id = id.toString(),
                            uri = Uri.parse(data),
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            mediaType = MediaType.VIDEO,
                            mimeType = mimeType,
                            size = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        }

        return mediaItems
    }

    private fun scanAudioFiles(): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val genreColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(displayNameColumn) ?: ""
                val title = it.getString(titleColumn) ?: displayName
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                val data = it.getString(dataColumn) ?: ""
                val mimeType = it.getString(mimeTypeColumn) ?: ""
                val dateAdded = it.getLong(dateAddedColumn) * 1000L // Convert to milliseconds
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val genre = it.getString(genreColumn)
                val albumId = it.getLong(albumIdColumn)

                // Verify file exists
                if (data.isNotEmpty() && File(data).exists()) {
                    // Get album art URI
                    val albumArtUri = if (albumId != 0L) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                    } else null

                    mediaItems.add(
                        MediaItem(
                            id = id.toString(),
                            uri = Uri.parse(data),
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            duration = duration,
                            mediaType = MediaType.AUDIO,
                            albumArtUri = albumArtUri,
                            mimeType = mimeType,
                            size = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        }

        return mediaItems
    }
}