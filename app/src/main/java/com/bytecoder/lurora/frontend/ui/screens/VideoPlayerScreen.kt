package com.bytecoder.lurora.frontend.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.AspectRatio
import kotlin.math.cos
import kotlin.math.sin

/**
 * Video player screen with VLC-like features and controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val isSubtitleBottomSheetVisible by viewModel.isSubtitleBottomSheetVisible.collectAsStateWithLifecycle()
    val isMoreOptionsBottomSheetVisible by viewModel.isMoreOptionsBottomSheetVisible.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.aspectRatio.collectAsStateWithLifecycle()
    
    // Bottom sheet states
    val subtitleBottomSheetState = rememberModalBottomSheetState()
    val moreOptionsBottomSheetState = rememberModalBottomSheetState()
    
    // Handle hardware back button
    BackHandler {
        if (isFullscreen) {
            viewModel.toggleFullscreen()
        } else {
            onBack()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface with gesture detection
        VideoSurface(
            viewModel = viewModel,
            brightness = brightness,
            modifier = Modifier.fillMaxSize()
        )
        
        // Video player controls overlay
        if (isControlsVisible && currentMediaItem != null) {
            VideoPlayerControls(
                viewModel = viewModel,
                playbackState = playbackState,
                isFullscreen = isFullscreen,
                onBack = onBack,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Error display
        val error by viewModel.error.collectAsStateWithLifecycle()
        error?.let { errorMessage ->
            EnhancedErrorDialog(
                errorMessage = errorMessage,
                onDismiss = { viewModel.clearError() },
                onRetry = { viewModel.retryPlayback() },
                onOpenWith = { viewModel.openWithAlternativeApp() },
                onSkipToNext = { viewModel.skipToNextOnError() },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
    
    // Subtitle/Audio Track Bottom Sheet
    if (isSubtitleBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideSubtitleBottomSheet() },
            sheetState = subtitleBottomSheetState
        ) {
            SubtitleAudioTrackBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.hideSubtitleBottomSheet() }
            )
        }
    }
    
    // More Options Bottom Sheet  
    if (isMoreOptionsBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideMoreOptionsBottomSheet() },
            sheetState = moreOptionsBottomSheetState
        ) {
            MoreOptionsBottomSheet(
                viewModel = viewModel,
                aspectRatio = aspectRatio,
                onDismiss = { viewModel.hideMoreOptionsBottomSheet() }
            )
        }
    }
}

/**
 * Video surface with ExoPlayer integration and gesture handling
 */
@Composable
private fun VideoSurface(
    viewModel: VideoPlayerViewModel,
    brightness: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                player = viewModel.exoPlayer
            }
        },
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.toggleControls()
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width.toFloat()
                        if (offset.x < screenWidth * 0.4f) {
                            // Double tap on left side - rewind 10 seconds
                            viewModel.handleDoubleTapLeft()
                        } else if (offset.x > screenWidth * 0.6f) {
                            // Double tap on right side - forward 10 seconds
                            viewModel.handleDoubleTapRight()
                        } else {
                            // Double tap in center - toggle playback
                            viewModel.togglePlayback()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()
                    
                    // Horizontal gesture for seeking
                    if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                        viewModel.handleSeekGesture(dragAmount.x)
                    }
                    // Vertical gesture on left side for brightness
                    else if (dragAmount.x < screenWidth * 0.3f) {
                        viewModel.handleBrightnessGesture(dragAmount.y)
                    }
                    // Vertical gesture on right side for volume
                    else if (dragAmount.x > screenWidth * 0.7f) {
                        viewModel.handleVolumeGesture(dragAmount.y)
                    }
                }
            }
    )
}

/**
 * Video player controls overlay
 */
@Composable
private fun VideoPlayerControls(
    viewModel: VideoPlayerViewModel,
    playbackState: PlaybackState,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top controls - only apply status bar padding when not in fullscreen
        VideoPlayerTopBar(
            viewModel = viewModel,
            isFullscreen = isFullscreen,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(
                    if (isFullscreen) {
                        Modifier
                    } else {
                        Modifier.statusBarsPadding()
                    }
                )
        )
        
        // Only show loading indicator in center when buffering
        if (!playbackState.isPlaying && playbackState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Bottom controls - only apply navigation bar padding when not in fullscreen
        VideoPlayerBottomBar(
            viewModel = viewModel,
            playbackState = playbackState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (isFullscreen) {
                        Modifier
                    } else {
                        Modifier.navigationBarsPadding()
                    }
                )
        )
    }
}

/**
 * Top bar with video information and settings
 */
@Composable
private fun VideoPlayerTopBar(
    viewModel: VideoPlayerViewModel,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { 
            if (isFullscreen) viewModel.toggleFullscreen()
            else onBack()
        }) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
        Text(
            text = currentMediaItem?.title ?: "Video Player",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        // Queue button - moved to rightmost position
        var showQueue by remember { mutableStateOf(false) }
        
        Box {
            IconButton(onClick = { showQueue = true }) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Show Queue",
                    tint = Color.White
                )
            }
            
            // Queue view modal - position below status bar
            if (showQueue) {
                VideoQueueView(
                    viewModel = viewModel,
                    onDismiss = { showQueue = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .then(
                            if (isFullscreen) {
                                Modifier
                            } else {
                                Modifier.statusBarsPadding()
                            }
                        )
                )
            }
        }
    }
}

/**
 * Bottom bar with playback controls and progress
 */
@Composable
private fun VideoPlayerBottomBar(
    viewModel: VideoPlayerViewModel,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Time Display - moved above progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(playbackState.currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = formatTime(playbackState.duration),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Material 3 Expressive Progress Bar
        WavyProgressIndicator(
            currentPosition = playbackState.currentPosition,
            duration = playbackState.duration,
            bufferedPosition = playbackState.currentPosition, // Use currentPosition as fallback for buffered
            onSeekTo = { viewModel.seekTo(it) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtitle Toggle
                IconButton(onClick = { viewModel.toggleSubtitleBottomSheet() }) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitle/Audio Tracks",
                        tint = Color.White
                    )
                }
                
                // Fullscreen Toggle
                IconButton(onClick = { viewModel.toggleFullscreen() }) {
                    Icon(
                        imageVector = if (isFullscreen) 
                            Icons.Default.FullscreenExit 
                        else 
                            Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = Color.White
                    )
                }
            }
            
            // Middle buttons - main playback controls
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Track
                IconButton(onClick = { viewModel.seekToPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White
                    )
                }
                
                // Rewind 10s
                IconButton(onClick = { 
                    val newPos = (playbackState.currentPosition - 10000).coerceAtLeast(0)
                    viewModel.seekTo(newPos)
                }) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White
                    )
                }
                
                // Play/Pause (main)
                IconButton(onClick = { viewModel.togglePlayback() }) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }
                
                // Fast Forward 10s
                IconButton(onClick = { 
                    val newPos = (playbackState.currentPosition + 10000).coerceAtMost(playbackState.duration)
                    viewModel.seekTo(newPos)
                }) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Fast Forward 10s",
                        tint = Color.White
                    )
                }
                
                // Next Track
                IconButton(onClick = { viewModel.seekToNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White
                    )
                }
            }
            
            // Right side buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aspect Ratio Toggle
                IconButton(onClick = { 
                    val currentRatio = viewModel.aspectRatio.value
                    val nextRatio = when (currentRatio) {
                        AspectRatio.BEST_FIT -> AspectRatio.FIT_SCREEN
                        AspectRatio.FIT_SCREEN -> AspectRatio.FILL
                        AspectRatio.FILL -> AspectRatio.RATIO_16_9
                        AspectRatio.RATIO_16_9 -> AspectRatio.RATIO_9_16
                        AspectRatio.RATIO_9_16 -> AspectRatio.RATIO_4_3
                        AspectRatio.RATIO_4_3 -> AspectRatio.RATIO_3_4
                        AspectRatio.RATIO_3_4 -> AspectRatio.CENTER
                        AspectRatio.CENTER -> AspectRatio.BEST_FIT
                    }
                    viewModel.setAspectRatio(nextRatio)
                }) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White
                    )
                }
                
                // More Options Menu
                IconButton(onClick = { viewModel.toggleMoreOptionsBottomSheet() }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive (Wavy) Progress Indicator for video seeking
 */
@Composable
private fun WavyProgressIndicator(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
    
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }
    
    // Get colors at Composable level
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Animation for the wavy effect
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDragging) dragProgress else progress,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 300),
        label = "progress_animation"
    )
    
    Canvas(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    },
                    onDragEnd = {
                        isDragging = false
                        val newPosition = (dragProgress * duration).toLong()
                        onSeekTo(newPosition)
                    }
                ) { _, dragAmount ->
                    val newProgress = (dragProgress + dragAmount.x / size.width).coerceIn(0f, 1f)
                    dragProgress = newProgress
                }
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val wavyHeight = 12.dp.toPx()
        val centerY = size.height / 2
        
        // Draw buffered track
        drawWavyLine(
            progress = bufferedProgress,
            centerY = centerY,
            trackHeight = trackHeight,
            wavyHeight = wavyHeight / 2,
            color = Color.White.copy(alpha = 0.3f),
            strokeWidth = trackHeight
        )
        
        // Draw main progress track with wavy effect
        drawWavyLine(
            progress = animatedProgress,
            centerY = centerY,
            trackHeight = trackHeight,
            wavyHeight = if (isDragging) wavyHeight else wavyHeight / 2,
            color = primaryColor,
            strokeWidth = trackHeight
        )
        
        // Draw inactive track
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = androidx.compose.ui.geometry.Offset(animatedProgress * size.width, centerY),
            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        
        // Draw thumb (circular indicator)
        val thumbRadius = 8.dp.toPx()
        val thumbX = animatedProgress * size.width
        
        drawCircle(
            color = primaryColor,
            radius = thumbRadius,
            center = androidx.compose.ui.geometry.Offset(thumbX, centerY)
        )
        
        // Draw inner thumb
        drawCircle(
            color = Color.White,
            radius = thumbRadius * 0.6f,
            center = androidx.compose.ui.geometry.Offset(thumbX, centerY)
        )
    }
}

/**
 * Helper function to draw wavy line for expressive progress indicator
 */
private fun DrawScope.drawWavyLine(
    progress: Float,
    centerY: Float,
    trackHeight: Float,
    wavyHeight: Float,
    color: Color,
    strokeWidth: Float
) {
    val endX = progress * size.width
    val waveLength = 40.dp.toPx()
    val segments = 100
    
    val path = androidx.compose.ui.graphics.Path()
    var started = false
    
    for (i in 0..segments) {
        val x = (i.toFloat() / segments) * endX
        if (x > endX) break
        
        val waveOffset = sin((x / waveLength) * 2 * kotlin.math.PI) * wavyHeight * progress
        val y = centerY + waveOffset.toFloat()
        
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

/**
 * Video progress bar - keeping original as fallback
 */
@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    playbackState: PlaybackState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    Column(modifier = modifier) {
        // Main progress bar
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                val newPosition = (newProgress * duration).toLong()
                onSeekTo(newPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = formatTime(duration),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Format time in H:MM:SS or MM:SS format
 */
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

/**
 * Video queue view modal
 */
@Composable
private fun VideoQueueView(
    viewModel: VideoPlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaQueue by viewModel.mediaQueue.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    
    Card(
        modifier = modifier
            .width(300.dp)
            .height(400.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue (${mediaQueue.items.size})",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(onClick = { viewModel.toggleShuffleMode() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleMode) 
                                MaterialTheme.colorScheme.primary 
                            else Color.White
                        )
                    }
                    
                    IconButton(onClick = { 
                        val nextMode = when (playbackState.repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        viewModel.setRepeatMode(nextMode)
                    }) {
                        Icon(
                            imageVector = when (playbackState.repeatMode) {
                                RepeatMode.OFF -> Icons.Default.Repeat
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                RepeatMode.ALL -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat Mode",
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) 
                                MaterialTheme.colorScheme.primary 
                            else Color.White
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            
            // Queue items
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(mediaQueue.items.size) { index ->
                    val item = mediaQueue.items[index]
                    val isCurrentItem = item.id == currentMediaItem?.id
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playMediaAtIndex(index) }
                            .background(
                                if (isCurrentItem) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrentItem) 
                                MaterialTheme.colorScheme.primary 
                            else Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(24.dp)
                        )
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.title,
                                color = if (isCurrentItem) Color.White else Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrentItem) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            item.artist?.let { artist ->
                                Text(
                                    text = artist,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        if (item.duration > 0) {
                            Text(
                                text = formatTime(item.duration),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced error dialog with retry and alternative options
 */
@Composable
private fun EnhancedErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenWith: () -> Unit,
    onSkipToNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Playback Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Retry button
                Button(
                    onClick = {
                        onRetry()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
                
                // Open with alternative app
                OutlinedButton(
                    onClick = {
                        onOpenWith()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open With...")
                }
                
                // Skip to next
                OutlinedButton(
                    onClick = {
                        onSkipToNext()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Skip to Next")
                }
                
                // Dismiss
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Subtitle and Audio Track selection bottom sheet
 */
@Composable
private fun SubtitleAudioTrackBottomSheet(
    viewModel: VideoPlayerViewModel,
    onDismiss: () -> Unit
) {
    val availableSubtitleTracks by viewModel.availableSubtitleTracks.collectAsStateWithLifecycle()
    val availableAudioTracks by viewModel.availableAudioTracks.collectAsStateWithLifecycle()
    val selectedSubtitleTrack by viewModel.selectedSubtitleTrack.collectAsStateWithLifecycle()
    val selectedAudioTrack by viewModel.selectedAudioTrack.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Subtitles & Audio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Subtitle tracks section
        Text(
            text = "Subtitles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Subtitle Off option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    viewModel.selectSubtitleTrack(null)
                    onDismiss()
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedSubtitleTrack == null,
                onClick = { 
                    viewModel.selectSubtitleTrack(null)
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Off")
        }
        
        // Available subtitle tracks
        availableSubtitleTracks.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        viewModel.selectSubtitleTrack(track)
                        onDismiss()
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSubtitleTrack?.id == track.id,
                    onClick = { 
                        viewModel.selectSubtitleTrack(track)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(track.title)
                    track.language?.let { language ->
                        Text(
                            text = language,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Audio tracks section
        Text(
            text = "Audio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        availableAudioTracks.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        viewModel.selectAudioTrack(track)
                        onDismiss()
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedAudioTrack?.id == track.id,
                    onClick = { 
                        viewModel.selectAudioTrack(track)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(track.title)
                    track.language?.let { language ->
                        Text(
                            text = "$language • ${track.channels} channels",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * More options bottom sheet with aspect ratio and other settings
 */
@Composable
private fun MoreOptionsBottomSheet(
    viewModel: VideoPlayerViewModel,
    aspectRatio: AspectRatio,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Video Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Aspect Ratio Section
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        AspectRatio.values().forEach { ratio ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        viewModel.setAspectRatio(ratio)
                        onDismiss()
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = aspectRatio == ratio,
                    onClick = { 
                        viewModel.setAspectRatio(ratio)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(ratio.displayName)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}