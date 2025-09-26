package com.bytecoder.lurora.frontend.ui.screens

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel

/**
 * Video player screen with VLC-like features and controls
 */
@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val abLoopStart by viewModel.abLoopStart.collectAsStateWithLifecycle()
    val abLoopEnd by viewModel.abLoopEnd.collectAsStateWithLifecycle()
    
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
                abLoopStart = abLoopStart,
                abLoopEnd = abLoopEnd,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Error display
        val error by viewModel.error.collectAsStateWithLifecycle()
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
    abLoopStart: Long?,
    abLoopEnd: Long?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top controls
        VideoPlayerTopBar(
            viewModel = viewModel,
            isFullscreen = isFullscreen,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
        
        // Center play/pause button
        if (!playbackState.isPlaying && playbackState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            IconButton(
                onClick = { viewModel.togglePlayback() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // Bottom controls
        VideoPlayerBottomBar(
            viewModel = viewModel,
            playbackState = playbackState,
            abLoopStart = abLoopStart,
            abLoopEnd = abLoopEnd,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
        
        // A-B Loop indicators
        if (abLoopStart != null || abLoopEnd != null) {
            ABLoopIndicators(
                abLoopStart = abLoopStart,
                abLoopEnd = abLoopEnd,
                duration = playbackState.duration,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Top bar with video information and settings
 */
@Composable
private fun VideoPlayerTopBar(
    viewModel: VideoPlayerViewModel,
    isFullscreen: Boolean,
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
            else {/* Navigate back */}
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
        
        // Settings menu
        var showMenu by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color.White
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Playback Speed") },
                    onClick = { 
                        showMenu = false
                        // Show speed selection dialog
                    }
                )
                DropdownMenuItem(
                    text = { Text("Audio Track") },
                    onClick = { 
                        showMenu = false
                        // Show audio track selection
                    }
                )
                DropdownMenuItem(
                    text = { Text("Subtitles") },
                    onClick = { 
                        showMenu = false
                        // Show subtitle selection
                    }
                )
                DropdownMenuItem(
                    text = { Text("Video Info") },
                    onClick = { 
                        showMenu = false
                        // Show video information
                    }
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
    abLoopStart: Long?,
    abLoopEnd: Long?,
    modifier: Modifier = Modifier
) {
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
        // Progress bar
        VideoProgressBar(
            currentPosition = playbackState.currentPosition,
            duration = playbackState.duration,
            abLoopStart = abLoopStart,
            abLoopEnd = abLoopEnd,
            playbackState = playbackState,
            onSeekTo = { viewModel.seekTo(it) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // A-B Loop controls
            IconButton(onClick = { 
                if (abLoopStart == null) {
                    viewModel.setABLoopStart()
                } else if (abLoopEnd == null) {
                    viewModel.setABLoopEnd()
                } else {
                    viewModel.clearABLoop()
                }
            }) {
                Icon(
                    imageVector = when {
                        abLoopStart == null -> Icons.Default.LooksOne
                        abLoopEnd == null -> Icons.Default.LooksTwo
                        else -> Icons.Default.Clear
                    },
                    contentDescription = when {
                        abLoopStart == null -> "Set A Point"
                        abLoopEnd == null -> "Set B Point"
                        else -> "Clear A-B Loop"
                    },
                    tint = if (abLoopStart != null) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            
            // Previous
            IconButton(onClick = { viewModel.seekToPrevious() }) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Rewind
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
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Fast forward
            IconButton(onClick = { 
                val newPos = (playbackState.currentPosition + 10000).coerceAtMost(playbackState.duration)
                viewModel.seekTo(newPos)
            }) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    tint = Color.White
                )
            }
            
            // Next
            IconButton(onClick = { viewModel.seekToNext() }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Fullscreen toggle
            IconButton(onClick = { viewModel.toggleFullscreen() }) {
                Icon(
                    imageVector = if (viewModel.isFullscreen.collectAsState().value) 
                        Icons.Default.FullscreenExit 
                    else 
                        Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Video progress bar with A-B loop indicators
 */
@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    abLoopStart: Long?,
    abLoopEnd: Long?,
    playbackState: PlaybackState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
            
            // A-B loop indicators
            if (abLoopStart != null && duration > 0) {
                val startProgress = abLoopStart.toFloat() / duration
                Box(
                    modifier = Modifier
                        .fillMaxWidth(startProgress)
                        .height(4.dp)
                        .background(
                            Color.Green.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterStart)
                )
            }
            
            if (abLoopEnd != null && duration > 0) {
                val endProgress = abLoopEnd.toFloat() / duration
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f - endProgress)
                        .height(4.dp)
                        .background(
                            Color.Red.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterEnd)
                )
            }
        }
        
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
            
            Row {
                if (playbackState.playbackSpeed != 1.0f) {
                    Text(
                        text = "${playbackState.playbackSpeed}x",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * A-B Loop indicators in top-right corner
 */
@Composable
private fun ABLoopIndicators(
    abLoopStart: Long?,
    abLoopEnd: Long?,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "A-B Loop",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
            
            if (abLoopStart != null) {
                Text(
                    text = "A: ${formatTime(abLoopStart)}",
                    color = Color.Green,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (abLoopEnd != null) {
                Text(
                    text = "B: ${formatTime(abLoopEnd)}",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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