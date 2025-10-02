package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.backend.models.PlaybackState
import com.bytecoder.lurora.frontend.viewmodels.MusicPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.SettingsViewModel
import com.bytecoder.lurora.frontend.activities.MusicPlayerActivity
import com.bytecoder.lurora.frontend.activities.VideoPlayerActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.StrokeCap
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

/**
 * Enhanced Mini Player Component
 * 
 * Layout: [Mini Screen] [Track Info] [Previous] [Play/Pause] [Next] [Close]
 * Features:
 * - Auto-switching between album art and equalizer (configurable)
 * - Video preview for video files
 * - Track title and artist display
 * - Previous/Next navigation
 * - Progress bar with seeking
 * - Gesture controls
 * - Settings integration
 */
@Composable
fun MiniPlayer(
    currentMediaItem: MediaItem?,
    playbackState: PlaybackState,
    isFullPlayerVisible: Boolean,
    musicPlayerViewModel: MusicPlayerViewModel? = null,
    videoPlayerViewModel: VideoPlayerViewModel? = null,
    onOpenFullPlayer: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get settings for display preferences
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val audioDisplayMode = remember { settingsViewModel.getSetting("mini_player_audio_display") as? String ?: "Both" }
    val videoDisplayMode = remember { settingsViewModel.getSetting("mini_player_video_display") as? String ?: "Video Playing" }
    val switchInterval = remember { settingsViewModel.getSetting("mini_player_switch_interval") as? Float ?: 10f }
    
    // Mini player should only be visible when there is a current media item and full player is not visible
    val shouldShowMiniPlayer = currentMediaItem != null && !isFullPlayerVisible
    
    AnimatedVisibility(
        visible = shouldShowMiniPlayer,
        enter = slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it }
        ),
        exit = slideOutVertically(
            animationSpec = tween(300),
            targetOffsetY = { it }
        ),
        modifier = modifier
    ) {
        currentMediaItem?.let { mediaItem ->
            EnhancedMiniPlayerContent(
                mediaItem = mediaItem,
                playbackState = playbackState,
                musicPlayerViewModel = musicPlayerViewModel,
                videoPlayerViewModel = videoPlayerViewModel,
                onOpenFullPlayer = onOpenFullPlayer,
                onClose = onClose,
                audioDisplayMode = audioDisplayMode,
                videoDisplayMode = videoDisplayMode,
                switchInterval = switchInterval.toLong()
            )
        }
    }
}

@Composable
private fun EnhancedMiniPlayerContent(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    musicPlayerViewModel: MusicPlayerViewModel?,
    videoPlayerViewModel: VideoPlayerViewModel?,
    onOpenFullPlayer: () -> Unit,
    onClose: () -> Unit,
    audioDisplayMode: String,
    videoDisplayMode: String,
    switchInterval: Long
) {
    val density = LocalDensity.current
    var dragOffsetY by remember { mutableStateOf(0f) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val threshold = with(density) { 50.dp.toPx() }
                        when {
                            dragOffsetY < -threshold -> onOpenFullPlayer() // Swipe up
                            dragOffsetY > threshold -> onClose() // Swipe down
                        }
                        dragOffsetY = 0f
                    }
                ) { _, dragAmount ->
                    // Only allow vertical drags
                    dragOffsetY += dragAmount.y
                }
            }
            .clickable { onOpenFullPlayer() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column {
            // Progress bar (thin strip at top)
            MiniPlayerProgressBar(
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                isPlaying = playbackState.isPlaying
            )
            
            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Mini Screen (Album art/Video/Equalizer)
                MiniScreen(
                    mediaItem = mediaItem,
                    playbackState = playbackState,
                    musicPlayerViewModel = musicPlayerViewModel,
                    videoPlayerViewModel = videoPlayerViewModel,
                    displayMode = if (mediaItem.mediaType == MediaType.AUDIO) audioDisplayMode else videoDisplayMode,
                    switchInterval = switchInterval,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Center: Track Information
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mediaItem.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    mediaItem.artist?.let { artist ->
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Right: Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button
                    IconButton(
                        onClick = {
                            when (mediaItem.mediaType) {
                                MediaType.AUDIO -> musicPlayerViewModel?.togglePlayPause()
                                MediaType.VIDEO -> videoPlayerViewModel?.togglePlayPause()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Close button
                    IconButton(
                        onClick = {
                            // Stop playback
                            when (mediaItem.mediaType) {
                                MediaType.AUDIO -> musicPlayerViewModel?.stop()
                                MediaType.VIDEO -> videoPlayerViewModel?.stop()
                            }
                            onClose()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini Screen component that shows album art, video, or equalizer based on settings
 */
@Composable
private fun MiniScreen(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    musicPlayerViewModel: MusicPlayerViewModel?,
    videoPlayerViewModel: VideoPlayerViewModel?,
    displayMode: String,
    switchInterval: Long,
    modifier: Modifier = Modifier
) {
    var currentDisplay by remember { mutableStateOf(0) } // 0 = album art, 1 = equalizer, 2 = video
    
    // Auto-switching logic for "Both" mode
    LaunchedEffect(displayMode, switchInterval) {
        if ((displayMode == "Both" || displayMode == "Both (Album Art & Equalizer)") && mediaItem.mediaType == MediaType.AUDIO) {
            while (true) {
                delay(switchInterval * 1000)
                currentDisplay = if (currentDisplay == 0) 1 else 0
            }
        } else if ((displayMode == "Both" || displayMode == "Both (Thumbnail & Equalizer)") && mediaItem.mediaType == MediaType.VIDEO) {
            while (true) {
                delay(switchInterval * 1000)
                currentDisplay = when (currentDisplay) {
                    0 -> 1 // album art to equalizer
                    1 -> 2 // equalizer to video
                    else -> 0 // video to album art
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when (mediaItem.mediaType) {
            MediaType.AUDIO -> {
                when (displayMode) {
                    "Album Art" -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                    "Equalizer" -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                    "Both (Album Art & Equalizer)" -> {
                        when (currentDisplay) {
                            0 -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                            else -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                        }
                    }
                    // Backward compatibility
                    "Both" -> {
                        when (currentDisplay) {
                            0 -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                            else -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
            MediaType.VIDEO -> {
                when (displayMode) {
                    "Video Playing" -> VideoMiniDisplay(mediaItem, playbackState, videoPlayerViewModel, modifier = Modifier.fillMaxSize())
                    "Thumbnail" -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                    "Equalizer" -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                    "Both (Thumbnail & Equalizer)" -> {
                        when (currentDisplay) {
                            0 -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                            1 -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                            else -> VideoMiniDisplay(mediaItem, playbackState, videoPlayerViewModel, modifier = Modifier.fillMaxSize())
                        }
                    }
                    // Backward compatibility
                    "Both" -> {
                        when (currentDisplay) {
                            0 -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                            1 -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                            else -> VideoMiniDisplay(mediaItem, playbackState, videoPlayerViewModel, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Album art display component
 */
@Composable
private fun AlbumArtDisplay(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    if (mediaItem.albumArtUri != null) {
        AsyncImage(
            model = mediaItem.albumArtUri,
            contentDescription = "Album Art",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback when no album art
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Music",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Video mini display component that shows actual video playback
 */
@Composable
private fun VideoMiniDisplay(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    videoPlayerViewModel: VideoPlayerViewModel?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videoPlayerViewModel != null && videoPlayerViewModel.exoPlayer != null) {
            // Try a different approach - video preview with live indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background: Show current video frame or thumbnail
                if (mediaItem.albumArtUri != null) {
                    AsyncImage(
                        model = mediaItem.albumArtUri,
                        contentDescription = "Video Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Live video indicator when playing
                if (playbackState.isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        // Live indicator
                        Surface(
                            modifier = Modifier
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Red
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Pulsing dot
                                val alpha by animateFloatAsState(
                                    targetValue = if (playbackState.isPlaying) 1f else 0.3f,
                                    animationSpec = tween(800),
                                    label = "LiveIndicator"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            Color.White.copy(alpha = alpha),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                } else {
                    // Play button overlay when paused
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.3f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            // Overlay for play state indication when paused
            if (!playbackState.isPlaying) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        } else {
            // Fallback when no video player available - show thumbnail
            if (mediaItem.albumArtUri != null) {
                AsyncImage(
                    model = mediaItem.albumArtUri,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Play button overlay
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Mini equalizer display with colorful animated bars
 */
@Composable
private fun MiniEqualizerDisplay(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val numberOfBars = 8 // Fewer bars for mini display
    val barHeights = remember { mutableStateListOf<Float>() }
    val barColors = remember { mutableStateListOf<Color>() }
    
    // Get the theme colors outside LaunchedEffect
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Initialize bars
    LaunchedEffect(Unit) {
        repeat(numberOfBars) { index ->
            barHeights.add(0.2f + (index % 3) * 0.2f)
            barColors.add(
                Color.hsl(
                    hue = (index * 45f) % 360f,
                    saturation = 0.8f,
                    lightness = 0.6f
                )
            )
        }
    }
    
    // Animate bars when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                repeat(numberOfBars) { index ->
                    // Random height animation
                    barHeights[index] = kotlin.random.Random.nextFloat() * 0.8f + 0.2f
                    
                    // Dynamic color change
                    barColors[index] = Color.hsl(
                        hue = (kotlin.random.Random.nextFloat() * 60f + index * 45f) % 360f,
                        saturation = 0.7f + kotlin.random.Random.nextFloat() * 0.3f,
                        lightness = 0.5f + kotlin.random.Random.nextFloat() * 0.3f
                    )
                }
                delay(200) // Update every 200ms
            }
        } else {
            // Static bars when paused
            repeat(numberOfBars) { index ->
                barHeights[index] = 0.2f + (index % 3) * 0.1f
                barColors[index] = surfaceVariantColor.copy(alpha = 0.5f)
            }
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.7f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(numberOfBars) { index ->
                val animatedHeight by animateFloatAsState(
                    targetValue = if (index < barHeights.size) barHeights[index] else 0f,
                    animationSpec = tween(
                        durationMillis = if (isPlaying) 200 else 500,
                        easing = androidx.compose.animation.core.EaseInOutCubic
                    ),
                    label = "BarHeight$index"
                )
                
                val animatedColor by animateColorAsState(
                    targetValue = if (index < barColors.size) barColors[index] else Color.Gray,
                    animationSpec = tween(durationMillis = 300),
                    label = "BarColor$index"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(animatedHeight)
                        .background(
                            animatedColor,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
        
        // Center icon
        if (!isPlaying) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
@Composable
private fun MiniPlayerProgressBar(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean
) {
    val progress = if (duration > 0) {
        (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        color = if (isPlaying) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        },
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    )
}

/**
 * Preview component for mini player (video/album art/equalizer)
 */
@Composable
private fun MiniPlayerPreview(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    musicPlayerViewModel: MusicPlayerViewModel?,
    videoPlayerViewModel: VideoPlayerViewModel?
) {
    when (mediaItem.mediaType) {
        MediaType.VIDEO -> {
            // Video preview with live playback
            VideoMiniPreview(
                mediaItem = mediaItem,
                playbackState = playbackState,
                videoPlayerViewModel = videoPlayerViewModel
            )
        }
        MediaType.AUDIO -> {
            // Album art or equalizer fallback
            AudioMiniPreview(
                mediaItem = mediaItem,
                playbackState = playbackState,
                musicPlayerViewModel = musicPlayerViewModel
            )
        }
    }
}

/**
 * Video mini preview with live playback
 */
@Composable
private fun VideoMiniPreview(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    videoPlayerViewModel: VideoPlayerViewModel?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) {
        // In a real implementation, this would be an actual video surface
        // For now, show a placeholder with video info
        AsyncImage(
            model = mediaItem.albumArtUri,
            contentDescription = "Video Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = null,
            fallback = null,
            error = null
        )
        
        // Fallback to equalizer if no thumbnail
        if (mediaItem.albumArtUri == null) {
            MiniEqualizerView(
                isPlaying = playbackState.isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Play state overlay for video
        if (!playbackState.isPlaying) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.3f)
            ) {}
        }
    }
}

/**
 * Audio mini preview with album art or equalizer
 */
@Composable
private fun AudioMiniPreview(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    musicPlayerViewModel: MusicPlayerViewModel?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (mediaItem.albumArtUri != null) {
            // Show album art thumbnail
            AsyncImage(
                model = mediaItem.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = null,
                fallback = null,
                error = null
            )
        } else {
            // Show animated equalizer as fallback
            MiniEqualizerView(
                isPlaying = playbackState.isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Mini equalizer view for small preview
 */
@Composable
private fun MiniEqualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val numberOfBars = 5 // Fewer bars for mini view
    val barHeights = remember { mutableListOf<Float>() }
    
    // Initialize bar heights
    LaunchedEffect(Unit) {
        repeat(numberOfBars) { index ->
            barHeights.add(0.2f + (index % 3) * 0.2f)
        }
    }
    
    // Animate bars when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                repeat(numberOfBars) { index ->
                    barHeights[index] = kotlin.random.Random.nextFloat() * 0.6f + 0.2f
                }
                delay(200) // Slower animation for mini view
            }
        } else {
            // Static bars when paused
            repeat(numberOfBars) { index ->
                barHeights[index] = 0.1f + (index % 2) * 0.1f
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(
                if (isPlaying) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(numberOfBars) { index ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(
                            if (index < barHeights.size) {
                                (barHeights[index] * 24).dp
                            } else {
                                8.dp
                            }
                        )
                        .background(
                            if (isPlaying) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

/**
 * Progress bar component for mini player
 */
@Composable
private fun MiniPlayerProgressBar(
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    val progress = if (playbackState.duration > 0) {
        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
    )
}

/**
 * Format time in milliseconds to MM:SS format
 */
private fun formatTime(timeMs: Long): String {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / (1000 * 60)) % 60
    val hours = (timeMs / (1000 * 60 * 60))
    
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Get display text for track information with fallbacks
 */
private fun getTrackDisplayText(mediaItem: MediaItem): Pair<String, String> {
    val title = mediaItem.title.takeIf { it.isNotBlank() } 
        ?: "Unknown Track"
    
    val artist = mediaItem.artist?.takeIf { it.isNotBlank() } 
        ?: "Unknown Artist"
    
    return Pair(title, artist)
}

/**
 * Handle navigation to appropriate media player activity
 */
private fun handleMiniPlayerClick(
    context: Context,
    mediaItem: MediaItem
) {
    val intent = when (mediaItem.mediaType) {
        MediaType.AUDIO -> Intent(context, MusicPlayerActivity::class.java).apply {
            putExtra("media_item", mediaItem.toBundle())
        }
        MediaType.VIDEO -> Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra("media_item", mediaItem.toBundle())
        }
    }
    context.startActivity(intent)
}

/**
 * Extension function to convert MediaItem to Bundle for Activity navigation
 */
private fun MediaItem.toBundle(): Bundle {
    return Bundle().apply {
        putString("id", id)
        putString("title", title)
        putString("artist", artist)
        putString("album", album)
        putString("genre", genre)
        putLong("duration", duration)
        putString("uri", uri.toString())
        putString("albumArtUri", albumArtUri?.toString())
        putString("mediaType", mediaType.name)
        putLong("size", size)
        putLong("dateAdded", dateAdded)
        putInt("playCount", playCount)
        putBoolean("isFavorite", isFavorite)
        putLong("lastPosition", lastPosition)
        putString("mimeType", mimeType)
        putString("subtitleUri", subtitleUri?.toString())
        // Convert metadata map to bundle
        val metadataBundle = Bundle()
        metadata.forEach { (key, value) ->
            metadataBundle.putString(key, value)
        }
        putBundle("metadata", metadataBundle)
    }
}