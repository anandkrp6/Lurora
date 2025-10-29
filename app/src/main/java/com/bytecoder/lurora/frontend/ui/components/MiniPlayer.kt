package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.backend.models.PlaybackState
import com.bytecoder.lurora.frontend.ui.components.MediaThumbnailImage
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
 * - Same display modes as the main player (Album Art, Equalizer, Both)
 * - Uses the same settings as the main music player for consistency
 * - Automatic switching between album art and equalizer in "Both" mode
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
    // Get settings for display preferences - use the same setting as the main player
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val audioDisplayMode = remember { settingsViewModel.getSetting("music_player_display_mode") as? String ?: "Both (Album Art & Equalizer)" }
    val videoDisplayMode = remember { settingsViewModel.getSetting("mini_player_video_display") as? String ?: "Video Playing" }
    
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
                videoDisplayMode = videoDisplayMode
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
    videoDisplayMode: String
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
            // Progress bar (thin strip at top) - now interactive
            InteractiveMiniPlayerProgressBar(
                playbackState = playbackState,
                onSeekTo = { position ->
                    when (mediaItem.mediaType) {
                        MediaType.AUDIO -> musicPlayerViewModel?.seekTo(position)
                        MediaType.VIDEO -> videoPlayerViewModel?.seekTo(position)
                    }
                },
                modifier = Modifier.fillMaxWidth()
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artist name
                        mediaItem.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Time display
                        Text(
                            text = "${formatTime(playbackState.currentPosition)} / ${formatTime(playbackState.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) {
        when (mediaItem.mediaType) {
            MediaType.AUDIO -> {
                when (displayMode) {
                    "Album Art" -> {
                        AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                    }
                    "Equalizer" -> {
                        MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                    }
                    "Both", "Both (Album Art & Equalizer)" -> {
                        // Switching between Album Art and Equalizer - same logic as main player
                        var showAlbumArt by remember { mutableStateOf(true) }
                        
                        // Auto-switch every 5 seconds when playing (same as main player)
                        LaunchedEffect(playbackState.isPlaying) {
                            if (playbackState.isPlaying) {
                                while (playbackState.isPlaying) {
                                    kotlinx.coroutines.delay(5000) // 5 seconds - same as main player
                                    showAlbumArt = !showAlbumArt
                                }
                            }
                        }
                        
                        // Crossfade animation between the two views
                        Crossfade(
                            targetState = showAlbumArt,
                            animationSpec = tween(durationMillis = 800),
                            label = "Mini Display Switch"
                        ) { isShowingAlbumArt ->
                            if (isShowingAlbumArt) {
                                AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                            } else {
                                MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    else -> {
                        // Default to album art for unknown modes
                        AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            MediaType.VIDEO -> {
                when (displayMode) {
                    "Video Playing" -> VideoMiniDisplay(mediaItem, playbackState, videoPlayerViewModel, modifier = Modifier.fillMaxSize())
                    "Thumbnail" -> AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                    "Equalizer" -> MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                    "Both (Thumbnail & Equalizer)" -> {
                        var showThumbnail by remember { mutableStateOf(true) }
                        
                        LaunchedEffect(playbackState.isPlaying) {
                            if (playbackState.isPlaying) {
                                while (playbackState.isPlaying) {
                                    kotlinx.coroutines.delay(5000)
                                    showThumbnail = !showThumbnail
                                }
                            }
                        }
                        
                        Crossfade(
                            targetState = showThumbnail,
                            animationSpec = tween(durationMillis = 800),
                            label = "Mini Video Display Switch"
                        ) { isShowingThumbnail ->
                            if (isShowingThumbnail) {
                                AlbumArtDisplay(mediaItem, modifier = Modifier.fillMaxSize())
                            } else {
                                MiniEqualizerDisplay(playbackState.isPlaying, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    else -> {
                        // Default to video playing for unknown modes
                        VideoMiniDisplay(mediaItem, playbackState, videoPlayerViewModel, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Album art display component with black background and custom fallback
 */
@Composable
private fun AlbumArtDisplay(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    var thumbnailUri by remember(mediaItem.id) { mutableStateOf(mediaItem.albumArtUri) }
    var isLoading by remember(mediaItem.id) { mutableStateOf(false) }
    var showFallback by remember(mediaItem.id) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // If no thumbnail URI exists, try to extract one
    LaunchedEffect(mediaItem.id) {
        if (thumbnailUri == null && mediaItem.metadata?.get("file_path") != null) {
            isLoading = true
            try {
                val filePath = mediaItem.metadata["file_path"] as String
                val extractor = com.bytecoder.lurora.backend.utils.ThumbnailExtractor(context)
                
                val extractedUri = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    when (mediaItem.mediaType) {
                        MediaType.VIDEO -> extractor.getVideoThumbnail(filePath, mediaItem.id)
                        MediaType.AUDIO -> extractor.getAlbumArt(filePath, mediaItem.id)
                    }
                }
                
                if (extractedUri != null) {
                    thumbnailUri = extractedUri
                    showFallback = false
                } else {
                    showFallback = true
                }
            } catch (e: Exception) {
                // Failed to extract thumbnail, show fallback icon
                showFallback = true
            } finally {
                isLoading = false
            }
        } else if (thumbnailUri == null) {
            // No album art and no file path, show fallback immediately
            showFallback = true
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
            showFallback || thumbnailUri == null -> {
                // Show custom rainbow music note fallback
                MultiColorMusicNoteIcon(
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                coil.compose.AsyncImage(
                    model = thumbnailUri,
                    contentDescription = when (mediaItem.mediaType) {
                        MediaType.VIDEO -> "Video thumbnail"
                        MediaType.AUDIO -> "Album art"
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { 
                        // When image fails to load, show fallback
                        showFallback = true
                    }
                )
            }
        }
    }
}

/**
 * Rainbow gradient music note icon for when no album art is available
 */
@Composable
private fun MultiColorMusicNoteIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw a large custom music note filled with rainbow gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val noteSize = size.minDimension * 0.85f // Use 85% of available space
            
            // Calculate the bounds of the music note icon area
            val noteLeft = centerX - noteSize * 0.20f
            val noteTop = centerY - noteSize * 0.25f
            val noteRight = centerX + noteSize * 0.30f
            val noteBottom = centerY + noteSize * 0.25f
            
            // Create rainbow gradient brush that spans just the note area
            val rainbowBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF0000), // Red
                    Color(0xFFFF7F00), // Orange  
                    Color(0xFFFFFF00), // Yellow
                    Color(0xFF00FF00), // Green
                    Color(0xFF0000FF), // Blue
                    Color(0xFF4B0082), // Indigo
                    Color(0xFF9400D3)  // Violet
                ),
                start = Offset(centerX, noteTop),
                end = Offset(centerX, noteBottom) // Vertical gradient from top to bottom
            )
            
            // Draw music note head (oval) - centered
            drawOval(
                brush = rainbowBrush,
                topLeft = Offset(
                    centerX - noteSize * 0.20f,
                    centerY + noteSize * 0.05f
                ),
                size = androidx.compose.ui.geometry.Size(
                    noteSize * 0.3f,
                    noteSize * 0.2f
                )
            )
            
            // Draw music note stem - centered
            drawRect(
                brush = rainbowBrush,
                topLeft = Offset(
                    centerX + noteSize * 0.07f,
                    centerY - noteSize * 0.25f
                ),
                size = androidx.compose.ui.geometry.Size(
                    noteSize * 0.06f,
                    noteSize * 0.45f
                )
            )
            
            // Draw music note flag/beam - centered
            val flagPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(centerX + noteSize * 0.13f, centerY - noteSize * 0.25f)
                quadraticBezierTo(
                    centerX + noteSize * 0.30f, centerY - noteSize * 0.20f,
                    centerX + noteSize * 0.20f, centerY - noteSize * 0.05f
                )
                quadraticBezierTo(
                    centerX + noteSize * 0.27f, centerY - noteSize * 0.10f,
                    centerX + noteSize * 0.13f, centerY - noteSize * 0.15f
                )
                close()
            }
            drawPath(
                path = flagPath,
                brush = rainbowBrush
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
                MediaThumbnailImage(
                    mediaItem = mediaItem,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showLoading = false // Don't show loading in mini player
                )
                
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
            MediaThumbnailImage(
                mediaItem = mediaItem,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallbackIconSize = 24.dp,
                showLoading = false
            )
            
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
/**
 * Interactive progress bar component for mini player with seeking capability
 */
@Composable
private fun InteractiveMiniPlayerProgressBar(
    playbackState: PlaybackState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (playbackState.duration > 0) {
        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    val newPosition = (newProgress * playbackState.duration).toLong()
                    onSeekTo(newPosition)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    },
                    onDragEnd = {
                        isDragging = false
                        val newPosition = (dragProgress * playbackState.duration).toLong()
                        onSeekTo(newPosition)
                    }
                ) { _, dragAmount ->
                    val newProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                    dragProgress = newProgress
                }
            }
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        // Progress track
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(if (isDragging) dragProgress else progress)
                .background(
                    if (playbackState.isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
        )
    }
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
        MediaThumbnailImage(
            mediaItem = mediaItem,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallbackIconSize = 32.dp
        )
        
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
        // Show album art thumbnail with equalizer fallback
        MediaThumbnailImage(
            mediaItem = mediaItem,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallbackIconSize = 32.dp
        )
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