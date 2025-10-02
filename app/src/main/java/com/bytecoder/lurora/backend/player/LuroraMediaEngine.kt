package com.bytecoder.lurora.backend.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import com.bytecoder.lurora.backend.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Core media engine handling both audio and video playback
 * Uses ExoPlayer as the underlying media player
 */
@Singleton
class LuroraMediaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()
    
    private val _mediaQueue = MutableStateFlow(MediaQueue())
    val mediaQueue: StateFlow<MediaQueue> = _mediaQueue.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            updatePlaybackState()
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updatePlaybackState()
        }
        
        override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            updateCurrentMediaItem()
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            super.onPlayerError(error)
            _error.value = error.message ?: "Unknown playback error"
        }
        
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            updatePlaybackState()
        }
        
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled)
            updatePlaybackState()
        }
    }
    
    init {
        initializePlayer()
    }
    
    /**
     * Initialize the ExoPlayer instance
     */
    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        if (_exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Lurora Media Player")
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
            
            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            _exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
            
            _exoPlayer?.addListener(playerListener)
        }
    }
    
    /**
     * Play a single media item
     */
    fun playMediaItem(mediaItem: MediaItem) {
        _exoPlayer?.let { player ->
            val exoMediaItem = ExoMediaItem.Builder()
                .setUri(mediaItem.uri)
                .setMediaId(mediaItem.id)
                .build()
            
            player.setMediaItem(exoMediaItem)
            player.prepare()
            player.play()
            
            _currentMediaItem.value = mediaItem
            updateQueue(MediaQueue(listOf(mediaItem), 0))
        }
    }
    
    /**
     * Play a queue of media items
     */
    fun playQueue(queue: MediaQueue, startIndex: Int = 0) {
        _exoPlayer?.let { player ->
            val exoMediaItems = queue.items.map { mediaItem ->
                ExoMediaItem.Builder()
                    .setUri(mediaItem.uri)
                    .setMediaId(mediaItem.id)
                    .build()
            }
            
            player.setMediaItems(exoMediaItems, startIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            
            updateQueue(queue.copy(currentIndex = startIndex))
        }
    }
    
    /**
     * Add media item to queue
     */
    fun addToQueue(mediaItem: MediaItem, index: Int = -1) {
        val currentQueue = _mediaQueue.value
        val newItems = currentQueue.items.toMutableList()
        
        if (index == -1) {
            newItems.add(mediaItem)
        } else {
            newItems.add(index, mediaItem)
        }
        
        val exoMediaItem = ExoMediaItem.Builder()
            .setUri(mediaItem.uri)
            .setMediaId(mediaItem.id)
            .build()
        
        if (index == -1) {
            _exoPlayer?.addMediaItem(exoMediaItem)
        } else {
            _exoPlayer?.addMediaItem(index, exoMediaItem)
        }
        
        updateQueue(currentQueue.copy(items = newItems))
    }
    
    /**
     * Remove media item from queue
     */
    fun removeFromQueue(index: Int) {
        val currentQueue = _mediaQueue.value
        if (index in currentQueue.items.indices) {
            val newItems = currentQueue.items.toMutableList()
            newItems.removeAt(index)
            
            _exoPlayer?.removeMediaItem(index)
            
            val newCurrentIndex = when {
                index < currentQueue.currentIndex -> currentQueue.currentIndex - 1
                index == currentQueue.currentIndex && index == newItems.size -> newItems.size - 1
                else -> currentQueue.currentIndex
            }
            
            updateQueue(currentQueue.copy(items = newItems, currentIndex = newCurrentIndex))
        }
    }
    
    /**
     * Basic playback controls
     */
    fun play() = _exoPlayer?.play()
    fun pause() = _exoPlayer?.pause()
    fun stop() = _exoPlayer?.stop()
    
    fun seekTo(position: Long) = _exoPlayer?.seekTo(position)
    fun seekToNext() = _exoPlayer?.seekToNext()
    fun seekToPrevious() = _exoPlayer?.seekToPrevious()
    
    /**
     * Playback settings
     */
    fun setPlaybackSpeed(speed: Float) {
        _exoPlayer?.setPlaybackSpeed(speed)
        updatePlaybackState()
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        val exoRepeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        _exoPlayer?.repeatMode = exoRepeatMode
    }
    
    fun setShuffleMode(enabled: Boolean) {
        _exoPlayer?.shuffleModeEnabled = enabled
    }
    
    fun setVolume(volume: Float) {
        _exoPlayer?.volume = volume.coerceIn(0f, 1f)
        updatePlaybackState()
    }
    
    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long = _exoPlayer?.currentPosition ?: 0L
    
    /**
     * Get total duration
     */
    fun getDuration(): Long = _exoPlayer?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
    
    /**
     * Check if player is playing
     */
    fun isPlaying(): Boolean = _exoPlayer?.isPlaying == true
    
    /**
     * Update playback state
     */
    private fun updatePlaybackState() {
        _exoPlayer?.let { player ->
            val exoRepeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            
            _playbackState.value = PlaybackState(
                isPlaying = player.isPlaying,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                currentPosition = player.currentPosition,
                duration = if (player.duration != C.TIME_UNSET) player.duration else 0L,
                playbackSpeed = player.playbackParameters.speed,
                repeatMode = exoRepeatMode,
                shuffleMode = player.shuffleModeEnabled,
                currentMediaItemIndex = player.currentMediaItemIndex,
                totalItems = player.mediaItemCount
            )
        }
    }
    
    /**
     * Update current media item
     */
    private fun updateCurrentMediaItem() {
        _exoPlayer?.let { player ->
            val currentIndex = player.currentMediaItemIndex
            val currentQueue = _mediaQueue.value
            
            if (currentIndex in currentQueue.items.indices) {
                _currentMediaItem.value = currentQueue.items[currentIndex]
                updateQueue(currentQueue.copy(currentIndex = currentIndex))
            }
        }
    }
    
    /**
     * Update queue state
     */
    private fun updateQueue(queue: MediaQueue) {
        _mediaQueue.value = queue
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Play media at specific index in queue
     */
    fun playMediaAtIndex(index: Int) {
        _exoPlayer?.let { player ->
            if (index in 0 until player.mediaItemCount) {
                player.seekToDefaultPosition(index)
                player.play()
            }
        }
    }
    
    /**
     * Adjust volume
     */
    fun adjustVolume(delta: Float) {
        _exoPlayer?.let { player ->
            val currentVolume = player.volume
            val newVolume = (currentVolume + delta).coerceIn(0f, 1f)
            player.volume = newVolume
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        _exoPlayer?.removeListener(playerListener)
        _exoPlayer?.release()
        _exoPlayer = null
    }
}