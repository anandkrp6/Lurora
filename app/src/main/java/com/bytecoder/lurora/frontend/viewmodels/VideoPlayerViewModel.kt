package com.bytecoder.lurora.frontend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.backend.player.LuroraMediaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for video player with VLC-like features
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val mediaEngine: LuroraMediaEngine
) : ViewModel() {
    
    // Expose exoPlayer for UI components that need direct access
    internal val exoPlayer get() = mediaEngine.exoPlayer
    
    // Media engine state flows
    val playbackState = mediaEngine.playbackState
    val currentMediaItem = mediaEngine.currentMediaItem
    val mediaQueue = mediaEngine.mediaQueue
    val error = mediaEngine.error
    
    // Video player specific state
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()
    
    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()
    
    private val _brightness = MutableStateFlow(0.5f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()
    
    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()
    
    private val _selectedVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val selectedVideoTrack: StateFlow<VideoTrack?> = _selectedVideoTrack.asStateFlow()
    
    private val _selectedAudioTrack = MutableStateFlow<AudioTrack?>(null)
    val selectedAudioTrack: StateFlow<AudioTrack?> = _selectedAudioTrack.asStateFlow()
    
    private val _selectedSubtitleTrack = MutableStateFlow<SubtitleTrack?>(null)
    val selectedSubtitleTrack: StateFlow<SubtitleTrack?> = _selectedSubtitleTrack.asStateFlow()
    
    private val _availableVideoTracks = MutableStateFlow<List<VideoTrack>>(emptyList())
    val availableVideoTracks: StateFlow<List<VideoTrack>> = _availableVideoTracks.asStateFlow()
    
    private val _availableAudioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val availableAudioTracks: StateFlow<List<AudioTrack>> = _availableAudioTracks.asStateFlow()
    
    private val _availableSubtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitleTracks: StateFlow<List<SubtitleTrack>> = _availableSubtitleTracks.asStateFlow()
    
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()
    
    private val _abLoopStart = MutableStateFlow<Long?>(null)
    val abLoopStart: StateFlow<Long?> = _abLoopStart.asStateFlow()
    
    private val _abLoopEnd = MutableStateFlow<Long?>(null)
    val abLoopEnd: StateFlow<Long?> = _abLoopEnd.asStateFlow()
    
    private var controlsHideJob: Job? = null
    private var abLoopJob: Job? = null
    
    /**
     * Basic playback controls
     */
    fun playVideo(mediaItem: MediaItem) {
        mediaEngine.playMediaItem(mediaItem)
        loadVideoTracks()
        loadChapters()
    }
    
    fun playVideoQueue(queue: MediaQueue, startIndex: Int = 0) {
        mediaEngine.playQueue(queue, startIndex)
        loadVideoTracks()
        loadChapters()
    }
    
    fun togglePlayback() {
        if (mediaEngine.isPlaying()) {
            mediaEngine.pause()
        } else {
            mediaEngine.play()
        }
        showControlsTemporarily()
    }
    
    fun seekTo(position: Long) {
        mediaEngine.seekTo(position)
        showControlsTemporarily()
    }
    
    fun seekToNext() {
        mediaEngine.seekToNext()
        loadVideoTracks()
        loadChapters()
        showControlsTemporarily()
    }
    
    fun seekToPrevious() {
        mediaEngine.seekToPrevious()
        loadVideoTracks()
        loadChapters()
        showControlsTemporarily()
    }
    
    fun setPlaybackSpeed(speed: Float) {
        mediaEngine.setPlaybackSpeed(speed)
        showControlsTemporarily()
    }
    
    /**
     * UI controls
     */
    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
        showControlsTemporarily()
    }
    
    fun toggleControls() {
        _isControlsVisible.value = !_isControlsVisible.value
        if (_isControlsVisible.value) {
            autoHideControls()
        }
    }
    
    fun showControlsTemporarily() {
        _isControlsVisible.value = true
        autoHideControls()
    }
    
    private fun autoHideControls() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(3000) // Hide controls after 3 seconds
            if (mediaEngine.isPlaying()) {
                _isControlsVisible.value = false
            }
        }
    }
    
    /**
     * Video adjustments
     */
    fun setBrightness(brightness: Float) {
        _brightness.value = brightness.coerceIn(0f, 1f)
    }
    
    fun adjustBrightness(delta: Float) {
        val newBrightness = (_brightness.value + delta).coerceIn(0f, 1f)
        setBrightness(newBrightness)
    }
    
    /**
     * Track selection
     */
    fun selectVideoTrack(track: VideoTrack) {
        _selectedVideoTrack.value = track
        // In real implementation, would switch video track in ExoPlayer
        showControlsTemporarily()
    }
    
    fun selectAudioTrack(track: AudioTrack) {
        _selectedAudioTrack.value = track
        // In real implementation, would switch audio track in ExoPlayer
        showControlsTemporarily()
    }
    
    fun selectSubtitleTrack(track: SubtitleTrack?) {
        _selectedSubtitleTrack.value = track
        // In real implementation, would enable/disable subtitles in ExoPlayer
        showControlsTemporarily()
    }
    
    private fun loadVideoTracks() {
        val currentItem = currentMediaItem.value ?: return
        
        // Mock video tracks - in real implementation would get from ExoPlayer
        val videoTracks = listOf(
            VideoTrack(id = "video_1", title = "720p"),
            VideoTrack(id = "video_2", title = "1080p", isSelected = true)
        )
        _availableVideoTracks.value = videoTracks
        _selectedVideoTrack.value = videoTracks.find { it.isSelected }
        
        // Mock audio tracks
        val audioTracks = listOf(
            AudioTrack(id = "audio_1", title = "English", language = "en", isSelected = true),
            AudioTrack(id = "audio_2", title = "Spanish", language = "es")
        )
        _availableAudioTracks.value = audioTracks
        _selectedAudioTrack.value = audioTracks.find { it.isSelected }
        
        // Mock subtitle tracks
        val subtitleTracks = listOf(
            SubtitleTrack(id = "subtitle_1", title = "English", language = "en"),
            SubtitleTrack(id = "subtitle_2", title = "Spanish", language = "es")
        )
        _availableSubtitleTracks.value = subtitleTracks
    }
    
    private fun loadChapters() {
        val currentItem = currentMediaItem.value ?: return
        
        // Mock chapters - in real implementation would get from video metadata
        val chapters = listOf(
            Chapter(
                id = "chapter_1",
                title = "Introduction",
                startTime = 0L,
                endTime = 120000L // 2 minutes
            ),
            Chapter(
                id = "chapter_2", 
                title = "Main Content",
                startTime = 120000L,
                endTime = 600000L // 10 minutes
            )
        )
        _chapters.value = chapters
    }
    
    /**
     * Chapter navigation
     */
    fun seekToChapter(chapter: Chapter) {
        seekTo(chapter.startTime)
        showControlsTemporarily()
    }
    
    fun seekToNextChapter() {
        val currentPos = playbackState.value.currentPosition
        val nextChapter = _chapters.value.find { it.startTime > currentPos }
        nextChapter?.let { seekToChapter(it) }
    }
    
    fun seekToPreviousChapter() {
        val currentPos = playbackState.value.currentPosition
        val previousChapter = _chapters.value.findLast { it.startTime < currentPos - 5000 }
        previousChapter?.let { seekToChapter(it) }
    }
    
    /**
     * A-B Loop functionality
     */
    fun setABLoopStart() {
        _abLoopStart.value = playbackState.value.currentPosition
        showControlsTemporarily()
    }
    
    fun setABLoopEnd() {
        val startPos = _abLoopStart.value ?: return
        val currentPos = playbackState.value.currentPosition
        
        if (currentPos > startPos) {
            _abLoopEnd.value = currentPos
            startABLoop()
        }
        showControlsTemporarily()
    }
    
    fun clearABLoop() {
        _abLoopStart.value = null
        _abLoopEnd.value = null
        abLoopJob?.cancel()
        showControlsTemporarily()
    }
    
    private fun startABLoop() {
        val startPos = _abLoopStart.value ?: return
        val endPos = _abLoopEnd.value ?: return
        
        abLoopJob?.cancel()
        abLoopJob = viewModelScope.launch {
            while (_abLoopStart.value != null && _abLoopEnd.value != null) {
                val currentPos = playbackState.value.currentPosition
                if (currentPos >= endPos) {
                    seekTo(startPos)
                }
                delay(500) // Check every 500ms
            }
        }
    }
    
    /**
     * Gesture handling
     */
    fun handleSeekGesture(deltaX: Float) {
        val currentPos = playbackState.value.currentPosition
        val duration = playbackState.value.duration
        val seekAmount = (deltaX / 10f * 1000).toLong() // 1 pixel = 100ms
        val newPosition = (currentPos + seekAmount).coerceIn(0L, duration)
        seekTo(newPosition)
    }
    
    fun handleVolumeGesture(deltaY: Float) {
        // Volume adjustment would be handled by system volume
        // This is a placeholder for volume gesture recognition
        showControlsTemporarily()
    }
    
    fun handleBrightnessGesture(deltaY: Float) {
        val delta = -deltaY / 1000f // Invert Y axis, normalize
        adjustBrightness(delta)
    }
    
    /**
     * Playback statistics
     */
    fun getPlaybackInfo(): Map<String, String> {
        val state = playbackState.value
        val currentItem = currentMediaItem.value
        
        return mapOf(
            "Position" to formatTime(state.currentPosition),
            "Duration" to formatTime(state.duration),
            "Speed" to "${state.playbackSpeed}x",
            "Video Track" to (_selectedVideoTrack.value?.title ?: "Unknown"),
            "Audio Track" to (_selectedAudioTrack.value?.title ?: "Unknown"),
            "Subtitle Track" to (_selectedSubtitleTrack.value?.title ?: "None"),
            "File" to (currentItem?.title ?: "Unknown")
        )
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        controlsHideJob?.cancel()
        abLoopJob?.cancel()
    }
}