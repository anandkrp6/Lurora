package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.backend.models.PlaybackState
import com.bytecoder.lurora.frontend.viewmodels.MusicPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel
import kotlinx.coroutines.delay

/**
 * Mini Player Component
 * 
 * Features:
 * - 30% preview window (video/album art/equalizer)
 * - 40% play/pause controls (Material 3 FAB)
 * - 30% close button
 * - Thin 2dp progress bar at top
 * - Gesture controls (tap/swipe up to open, swipe down to close)
 * - Auto visibility management
 * - Queue integration
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
    // Mini player should only be visible when:
    // 1. There is a current media item
    // 2. Full player is not visible
    // 3. Media is playing or has some state
    val shouldShowMiniPlayer = currentMediaItem != null && 
                              !isFullPlayerVisible && 
                              (playbackState.isPlaying || currentMediaItem != null)
    
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
            MiniPlayerContent(
                mediaItem = mediaItem,
                playbackState = playbackState,
                musicPlayerViewModel = musicPlayerViewModel,
                videoPlayerViewModel = videoPlayerViewModel,
                onOpenFullPlayer = onOpenFullPlayer,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun MiniPlayerContent(
    mediaItem: MediaItem,
    playbackState: PlaybackState,
    musicPlayerViewModel: MusicPlayerViewModel?,
    videoPlayerViewModel: VideoPlayerViewModel?,
    onOpenFullPlayer: () -> Unit,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    var dragOffsetY by remember { mutableStateOf(0f) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
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
            // Progress bar (thin 2dp strip at top)
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
                // Left: Preview window (30%)
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                ) {
                    MiniPlayerPreview(
                        mediaItem = mediaItem,
                        playbackState = playbackState,
                        musicPlayerViewModel = musicPlayerViewModel,
                        videoPlayerViewModel = videoPlayerViewModel
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Center: Play/Pause button (40%)
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            when (mediaItem.mediaType) {
                                MediaType.AUDIO -> musicPlayerViewModel?.togglePlayPause()
                                MediaType.VIDEO -> videoPlayerViewModel?.togglePlayPause()
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Right: Close button (30%)
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Progress bar component for mini player
 */
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