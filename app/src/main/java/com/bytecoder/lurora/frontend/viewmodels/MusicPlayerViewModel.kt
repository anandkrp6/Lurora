package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.backend.player.LuroraMediaEngine
import com.bytecoder.lurora.backend.services.LuroraMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for music player with Spotify-like features
 */
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val mediaEngine: LuroraMediaEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    // Media engine state flows
    val playbackState = mediaEngine.playbackState
    val currentMediaItem = mediaEngine.currentMediaItem
    val mediaQueue = mediaEngine.mediaQueue
    val error = mediaEngine.error
    
    // Music player specific state
    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()
    
    private val _showLyrics = MutableStateFlow(false)
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()
    
    private val _showEnhancedEqualizer = MutableStateFlow(false)
    val showEnhancedEqualizer: StateFlow<Boolean> = _showEnhancedEqualizer.asStateFlow()
    
    private val _currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricLine>> = _currentLyrics.asStateFlow()
    
    private val _audioEffects = MutableStateFlow<List<AudioEffect>>(getDefaultAudioEffects())
    val audioEffects: StateFlow<List<AudioEffect>> = _audioEffects.asStateFlow()
    
    private val _equalizerBands = MutableStateFlow<List<EqualizerBand>>(getDefaultEqualizerBands())
    val equalizerBands: StateFlow<List<EqualizerBand>> = _equalizerBands.asStateFlow()
    
    private val _bassBoost = MutableStateFlow(AudioEffect.BassBoost(enabled = false, strength = 0f))
    val bassBoost: StateFlow<AudioEffect.BassBoost> = _bassBoost.asStateFlow()
    
    private val _virtualizer = MutableStateFlow(AudioEffect.Virtualizer(enabled = false, strength = 0f))
    val virtualizer: StateFlow<AudioEffect.Virtualizer> = _virtualizer.asStateFlow()
    
    private val _visualizerData = MutableStateFlow<VisualizerData?>(null)
    val visualizerData: StateFlow<VisualizerData?> = _visualizerData.asStateFlow()
    
    private val _isVisualizerEnabled = MutableStateFlow(false)
    val isVisualizerEnabled: StateFlow<Boolean> = _isVisualizerEnabled.asStateFlow()
    
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()
    
    private val _recentlyPlayed = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentlyPlayed: StateFlow<List<MediaItem>> = _recentlyPlayed.asStateFlow()
    
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()
    
    private var sleepTimerJob: Job? = null
    private var visualizerJob: Job? = null
    
    /**
     * Player Controls
     */
    fun playMediaItem(mediaItem: MediaItem) {
        mediaEngine.playMediaItem(mediaItem)
        startBackgroundService()
        addToRecentlyPlayed(mediaItem)
    }
    
    fun playQueue(queue: MediaQueue, startIndex: Int = 0) {
        mediaEngine.playQueue(queue, startIndex)
        startBackgroundService()
        queue.items.getOrNull(startIndex)?.let { addToRecentlyPlayed(it) }
    }
    
    /**
     * Toggle between play and pause
     */
    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            mediaEngine.pause()
        } else {
            mediaEngine.play()
        }
    }
    
    private fun startBackgroundService() {
        // Start the media service for background playback and lock screen controls
        LuroraMediaService.startService(context)
    }
    
    fun togglePlayback() {
        if (mediaEngine.isPlaying()) {
            mediaEngine.pause()
        } else {
            mediaEngine.play()
        }
    }
    
    fun seekTo(position: Long) = mediaEngine.seekTo(position)
    fun seekToNext() = mediaEngine.seekToNext()
    fun seekToPrevious() = mediaEngine.seekToPrevious()
    
    fun setRepeatMode(mode: RepeatMode) = mediaEngine.setRepeatMode(mode)
    fun setShuffleMode(enabled: Boolean) = mediaEngine.setShuffleMode(enabled)
    fun setPlaybackSpeed(speed: Float) = mediaEngine.setPlaybackSpeed(speed)
    
    /**
     * Player UI Controls
     */
    fun expandPlayer() {
        _isExpanded.value = true
    }
    
    fun collapsePlayer() {
        _isExpanded.value = false
    }
    
    /**
     * Lyrics Management
     */
    private fun loadLyrics() {
        val currentItem = currentMediaItem.value ?: return
        viewModelScope.launch {
            // Try to load lyrics from metadata or external source
            val lyrics = loadLyricsFromMetadata(currentItem) ?: loadLyricsFromFile(currentItem)
            _currentLyrics.value = lyrics
        }
    }
    
    private suspend fun loadLyricsFromMetadata(mediaItem: MediaItem): List<LyricLine>? {
        // Check if lyrics are embedded in media metadata
        return mediaItem.metadata["lyrics"]?.let { lyricsText ->
            parseLyrics(lyricsText)
        }
    }
    
    private suspend fun loadLyricsFromFile(mediaItem: MediaItem): List<LyricLine> {
        // Try to find .lrc file with same name as audio file
        // This is a simplified implementation
        return emptyList()
    }
    
    private fun parseLyrics(lyricsText: String): List<LyricLine> {
        // Parse LRC format: [mm:ss.xx]Lyric text
        return lyricsText.lines().mapNotNull { line ->
            val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)".toRegex()
            val matchResult = regex.find(line)
            matchResult?.let { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val centiseconds = match.groupValues[3].toLong()
                val text = match.groupValues[4].trim()
                
                val timeMs = (minutes * 60 + seconds) * 1000 + centiseconds * 10
                LyricLine(text, timeMs)
            }
        }
    }
    
    fun getCurrentLyricLine(): LyricLine? {
        val currentPos = playbackState.value.currentPosition
        return _currentLyrics.value.findLast { it.startTime <= currentPos }
    }
    
    /**
     * Audio Effects
     */
    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
        if (_showEnhancedEqualizer.value) {
            _showEnhancedEqualizer.value = false
        }
    }
    
    fun toggleEnhancedEqualizer() {
        _showEnhancedEqualizer.value = !_showEnhancedEqualizer.value
        if (_showLyrics.value) {
            _showLyrics.value = false
        }
    }
    
    /**
     * Audio effects
     */
    fun setBassBoost(bassBoost: AudioEffect.BassBoost) {
        _bassBoost.value = bassBoost
        // In real implementation, would apply to audio engine
    }
    
    fun setVirtualizer(virtualizer: AudioEffect.Virtualizer) {
        _virtualizer.value = virtualizer
        // In real implementation, would apply to audio engine
    }
    
    /**
     * Stop playback and clear queue
     */
    fun stop() {
        mediaEngine.stop()
        // Note: In a real implementation, we'd need to clear the queue properly
        // For now, just stop the media engine
    }
    
    /**
     * Equalizer
     */
    fun setEqualizerBand(bandIndex: Int, gain: Float) {
        val current = _equalizerBands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = current[bandIndex].copy(gain = gain)
            _equalizerBands.value = current
        }
    }
    
    fun setEqualizerPreset(presetName: String) {
        val preset = getEqualizerPreset(presetName)
        _equalizerBands.value = preset
    }
    
    fun resetEqualizer() {
        _equalizerBands.value = getDefaultEqualizerBands()
    }
    
    /**
     * Visualizer
     */
    fun setVisualizerEnabled(enabled: Boolean) {
        _isVisualizerEnabled.value = enabled
        if (enabled) {
            startVisualizer()
        } else {
            stopVisualizer()
        }
    }
    
    private fun startVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = viewModelScope.launch {
            while (_isVisualizerEnabled.value) {
                // Generate mock visualizer data
                // In real implementation, this would come from audio analysis
                val frequencies = FloatArray(64) { index ->
                    (kotlin.math.sin(System.currentTimeMillis() / 1000.0 + index * 0.1) * 0.5 + 0.5).toFloat()
                }
                val waveform = ByteArray(128) { index ->
                    (kotlin.math.sin(System.currentTimeMillis() / 500.0 + index * 0.05) * 127).toInt().toByte()
                }
                _visualizerData.value = VisualizerData(frequencies, waveform)
                delay(50) // Update at ~20 FPS
            }
        }
    }
    
    private fun stopVisualizer() {
        visualizerJob?.cancel()
        _visualizerData.value = null
    }
    
    /**
     * Sleep Timer
     */
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        if (minutes > 0) {
            startSleepTimer(minutes)
        } else {
            cancelSleepTimer()
        }
    }
    
    private fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            // Fade out and pause
            fadeOutAndPause()
            _sleepTimerMinutes.value = 0
        }
    }
    
    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = 0
    }
    
    private suspend fun fadeOutAndPause() {
        // Gradually reduce volume then pause
        for (volume in 100 downTo 0 step 5) {
            mediaEngine.setVolume(volume / 100f)
            delay(100)
        }
        mediaEngine.pause()
        mediaEngine.setVolume(1f) // Reset volume for next play
    }
    
    /**
     * Favorites Management
     */
    fun toggleFavorite(mediaItem: MediaItem) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(mediaItem.id)) {
            current.remove(mediaItem.id)
        } else {
            current.add(mediaItem.id)
        }
        _favorites.value = current
    }
    
    fun isFavorite(mediaItem: MediaItem): Boolean {
        return _favorites.value.contains(mediaItem.id)
    }
    
    /**
     * Playlist Management
     */
    fun createPlaylist(name: String, description: String = ""): Playlist {
        val playlist = Playlist(
            id = generatePlaylistId(),
            name = name,
            description = description
        )
        val current = _playlists.value.toMutableList()
        current.add(playlist)
        _playlists.value = current
        return playlist
    }
    
    fun addToPlaylist(playlistId: String, mediaItem: MediaItem) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = current[index]
            val updatedItems = playlist.mediaItems + mediaItem
            current[index] = playlist.copy(
                mediaItems = updatedItems,
                totalDuration = updatedItems.sumOf { it.duration },
                dateModified = System.currentTimeMillis()
            )
            _playlists.value = current
        }
    }
    
    fun removeFromPlaylist(playlistId: String, mediaItemId: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = current[index]
            val updatedItems = playlist.mediaItems.filter { it.id != mediaItemId }
            current[index] = playlist.copy(
                mediaItems = updatedItems,
                totalDuration = updatedItems.sumOf { it.duration },
                dateModified = System.currentTimeMillis()
            )
            _playlists.value = current
        }
    }
    
    fun deletePlaylist(playlistId: String) {
        val current = _playlists.value.toMutableList()
        current.removeIf { it.id == playlistId }
        _playlists.value = current
    }
    
    /**
     * Search
     */
    fun searchMusic(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            // In real implementation, this would search the music library
            // For now, return empty results
            _searchResults.value = emptyList()
        }
    }
    
    /**
     * Recently Played
     */
    private fun addToRecentlyPlayed(mediaItem: MediaItem) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeIf { it.id == mediaItem.id } // Remove if already exists
        current.add(0, mediaItem) // Add to beginning
        if (current.size > 50) { // Keep only last 50
            current.removeAt(current.size - 1)
        }
        _recentlyPlayed.value = current
    }
    
    /**
     * Helper Functions
     */
    private fun generatePlaylistId(): String {
        return "playlist_${System.currentTimeMillis()}"
    }
    
    private fun getDefaultAudioEffects(): List<AudioEffect> {
        return listOf(
            AudioEffect.BassBoost(),
            AudioEffect.Virtualizer(),
            AudioEffect.Reverb(),
            AudioEffect.Echo()
        )
    }
    
    private fun getDefaultEqualizerBands(): List<EqualizerBand> {
        val frequencies = listOf(60f, 230f, 910f, 3600f, 14000f, 60000f, 230000f, 910000f)
        return frequencies.mapIndexed { index, frequency ->
            EqualizerBand(frequency = frequency, gain = 0f, bandIndex = index)
        }
    }
    
    private fun getEqualizerPreset(presetName: String): List<EqualizerBand> {
        val bands = getDefaultEqualizerBands().toMutableList()
        
        when (presetName.lowercase()) {
            "pop" -> {
                bands[0] = bands[0].copy(gain = -1.5f)  // 60Hz
                bands[1] = bands[1].copy(gain = 4.5f)   // 230Hz
                bands[2] = bands[2].copy(gain = 7.2f)   // 910Hz
                bands[3] = bands[3].copy(gain = 8.1f)   // 3.6kHz
                bands[4] = bands[4].copy(gain = 5.4f)   // 14kHz
                bands[5] = bands[5].copy(gain = 1.8f)   // 60kHz
                bands[6] = bands[6].copy(gain = -1.1f)  // 230kHz
                bands[7] = bands[7].copy(gain = -1.9f)  // 910kHz
            }
            "rock" -> {
                bands[0] = bands[0].copy(gain = 8.0f)   // 60Hz
                bands[1] = bands[1].copy(gain = 4.9f)   // 230Hz
                bands[2] = bands[2].copy(gain = -5.6f)  // 910Hz
                bands[3] = bands[3].copy(gain = -7.7f)  // 3.6kHz
                bands[4] = bands[4].copy(gain = -3.2f)  // 14kHz
                bands[5] = bands[5].copy(gain = 4.0f)   // 60kHz
                bands[6] = bands[6].copy(gain = 8.8f)   // 230kHz
                bands[7] = bands[7].copy(gain = 11.2f)  // 910kHz
            }
            "jazz" -> {
                bands[0] = bands[0].copy(gain = 4.0f)   // 60Hz
                bands[1] = bands[1].copy(gain = 3.0f)   // 230Hz
                bands[2] = bands[2].copy(gain = 1.4f)   // 910Hz
                bands[3] = bands[3].copy(gain = 2.3f)   // 3.6kHz
                bands[4] = bands[4].copy(gain = -2.1f)  // 14kHz
                bands[5] = bands[5].copy(gain = -2.1f)  // 60kHz
                bands[6] = bands[6].copy(gain = 0.0f)   // 230kHz
                bands[7] = bands[7].copy(gain = 0.0f)   // 910kHz
            }
            "classical" -> {
                bands[0] = bands[0].copy(gain = 0.0f)   // 60Hz
                bands[1] = bands[1].copy(gain = 0.0f)   // 230Hz
                bands[2] = bands[2].copy(gain = 0.0f)   // 910Hz
                bands[3] = bands[3].copy(gain = 0.0f)   // 3.6kHz
                bands[4] = bands[4].copy(gain = -7.2f)  // 14kHz
                bands[5] = bands[5].copy(gain = -7.2f)  // 60kHz
                bands[6] = bands[6].copy(gain = -7.2f)  // 230kHz
                bands[7] = bands[7].copy(gain = -9.6f)  // 910kHz
            }
            "electronic" -> {
                bands[0] = bands[0].copy(gain = 4.8f)   // 60Hz
                bands[1] = bands[1].copy(gain = 3.6f)   // 230Hz
                bands[2] = bands[2].copy(gain = 1.0f)   // 910Hz
                bands[3] = bands[3].copy(gain = 0.0f)   // 3.6kHz
                bands[4] = bands[4].copy(gain = -5.2f)  // 14kHz
                bands[5] = bands[5].copy(gain = 4.0f)   // 60kHz
                bands[6] = bands[6].copy(gain = 7.2f)   // 230kHz
                bands[7] = bands[7].copy(gain = 9.6f)   // 910kHz
            }
        }
        
        return bands
    }
    
    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        visualizerJob?.cancel()
    }
}