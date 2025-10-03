package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.ui.components.MediaThumbnailImage
import com.bytecoder.lurora.frontend.viewmodels.MusicPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.SettingsViewModel

/**
 * Main music player screen with Spotify-like interface
 */
@Composable
fun MusicPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val isExpanded by viewModel.isExpanded.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    
    // Initialize and observe display mode from settings
    LaunchedEffect(Unit) {
        val audioDisplayMode = settingsViewModel.getSetting("music_player_display_mode") as? String ?: "Both (Album Art & Equalizer)"
        viewModel.setDisplayMode(audioDisplayMode)
    }
    
    // Observe settings changes - re-read the setting when the UI recomposes
    val currentSettingValue = remember(settingsViewModel) {
        settingsViewModel.getSetting("music_player_display_mode") as? String ?: "Both (Album Art & Equalizer)"
    }
    
    LaunchedEffect(currentSettingValue) {
        viewModel.setDisplayMode(currentSettingValue)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Mini player (collapsed state)
        if (!isExpanded && currentMediaItem != null) {
            MiniPlayer(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        
        // Full player (expanded state)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            FullScreenMusicPlayer(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onBack = onBack,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Mini player shown at bottom when collapsed
 */
@Composable
private fun MiniPlayer(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    
    currentMediaItem?.let { mediaItem ->
        Card(
            modifier = modifier
                .clickable { viewModel.expandPlayer() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art
                MediaThumbnailImage(
                    mediaItem = mediaItem,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    fallbackIconSize = 24.dp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mediaItem.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    mediaItem.artist?.let { artist ->
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Play/pause button
                IconButton(onClick = { viewModel.togglePlayback() }) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play"
                    )
                }
                
                // Next button
                IconButton(onClick = { viewModel.seekToNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next"
                    )
                }
            }
            
            // Progress indicator
            LinearProgressIndicator(
                progress = if (playbackState.duration > 0) {
                    playbackState.currentPosition.toFloat() / playbackState.duration
                } else 0f,
                modifier = Modifier.fillMaxWidth(),
                trackColor = Color.Transparent
            )
        }
    }
}

/**
 * Full-screen music player
 */
@Composable
private fun FullScreenMusicPlayer(
    viewModel: MusicPlayerViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val showLyrics by viewModel.showLyrics.collectAsStateWithLifecycle()
    val showEnhancedEqualizer by viewModel.showEnhancedEqualizer.collectAsStateWithLifecycle()
    val audioDisplayMode by viewModel.audioDisplayMode.collectAsStateWithLifecycle()
    
    currentMediaItem?.let { mediaItem ->
        Column(
            modifier = modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Top bar
            MusicPlayerTopBar(
                onCollapsePlayer = onBack,
                onShowQueue = { /* Show queue */ },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (showLyrics) {
                // Lyrics view
                LyricsView(
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else if (showEnhancedEqualizer) {
                // Enhanced equalizer view
                EnhancedEqualizerView(
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                // Album art and controls
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display based on audioDisplayMode
                    Box(
                        modifier = Modifier.size(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (audioDisplayMode) {
                            "Album Art" -> {
                                AlbumArtDisplay(
                                    mediaItem = mediaItem,
                                    isPlaying = playbackState.isPlaying,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            "Equalizer" -> {
                                AnimatedEqualizerView(
                                    isPlaying = playbackState.isPlaying,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            "Both", "Both (Album Art & Equalizer)" -> {
                                // Album art as background
                                AlbumArtDisplay(
                                    mediaItem = mediaItem,
                                    isPlaying = playbackState.isPlaying,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Equalizer overlay
                                AnimatedEqualizerView(
                                    isPlaying = playbackState.isPlaying,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Track information
                    TrackInfo(
                        mediaItem = mediaItem,
                        isFavorite = viewModel.isFavorite(mediaItem),
                        onToggleFavorite = { viewModel.toggleFavorite(mediaItem) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress bar
            MusicProgressBar(
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                onSeekTo = { viewModel.seekTo(it) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main controls
            MusicPlayerControls(
                playbackState = playbackState,
                onPlayPause = { viewModel.togglePlayback() },
                onPrevious = { viewModel.seekToPrevious() },
                onNext = { viewModel.seekToNext() },
                onRepeatModeChange = { viewModel.setRepeatMode(it) },
                onShuffleToggle = { viewModel.setShuffleMode(!playbackState.shuffleMode) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Secondary controls
            SecondaryControls(
                showLyrics = showLyrics,
                onToggleLyrics = { viewModel.toggleLyrics() },
                onShowQueue = { /* Show queue */ },
                onShowEffects = { 
                    // Enhanced equalizer with effects
                    viewModel.toggleEnhancedEqualizer()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Top bar for full-screen player
 */
@Composable
private fun MusicPlayerTopBar(
    onCollapsePlayer: () -> Unit,
    onShowQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onCollapsePlayer) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse Player"
            )
        }
        
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        IconButton(onClick = onShowQueue) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = "Show Queue"
            )
        }
    }
}

/**
 * Track information display
 */
@Composable
private fun TrackInfo(
    mediaItem: MediaItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                mediaItem.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                mediaItem.album?.let { album ->
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Favorite button
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Music progress bar with time labels
 */
@Composable
private fun MusicProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    Column(modifier = modifier) {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                val newPosition = (newProgress * duration).toLong()
                onSeekTo(newPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Main playback controls
 */
@Composable
private fun MusicPlayerControls(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: (RepeatMode) -> Unit,
    onShuffleToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Shuffle
        IconButton(onClick = onShuffleToggle) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Toggle Shuffle",
                tint = if (playbackState.shuffleMode) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Previous
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Play/Pause
        Card(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        // Next
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Repeat
        IconButton(onClick = {
            val nextMode = when (playbackState.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            onRepeatModeChange(nextMode)
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
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Secondary controls (lyrics, queue, effects)
 */
@Composable
private fun SecondaryControls(
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    onShowEffects: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onToggleLyrics) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Toggle Lyrics",
                tint = if (showLyrics) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onShowQueue) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = "Show Queue"
            )
        }
        
        IconButton(onClick = onShowEffects) {
            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = "Audio Effects"
            )
        }
        
        IconButton(onClick = { /* Show volume */ }) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Volume"
            )
        }
    }
}

/**
 * Lyrics view with auto-scroll
 */
@Composable
private fun LyricsView(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentLyricLine = viewModel.getCurrentLyricLine()
    
    val listState = rememberLazyListState()
    
    // Auto-scroll to current lyric
    LaunchedEffect(currentLyricLine) {
        currentLyricLine?.let { line ->
            val index = lyrics.indexOf(line)
            if (index != -1) {
                listState.animateScrollToItem(index, scrollOffset = -200)
            }
        }
    }
    
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No lyrics available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(vertical = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lyrics) { lyricLine ->
                val isCurrentLine = lyricLine == currentLyricLine
                
                Text(
                    text = lyricLine.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = if (isCurrentLine) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .graphicsLayer {
                            scaleX = if (isCurrentLine) 1.1f else 1f
                            scaleY = if (isCurrentLine) 1.1f else 1f
                        }
                )
            }
        }
    }
}

/**
 * Format time in MM:SS format
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Animated equalizer view as fallback for album art
 */
@Composable
private fun AnimatedEqualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val numberOfBars = 24
    val barHeights = remember { mutableStateListOf<Float>() }
    val barColors = remember { mutableStateListOf<Color>() }
    val pausedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    // Initialize bars
    LaunchedEffect(Unit) {
        repeat(numberOfBars) { index ->
            barHeights.add(0.1f + (index % 4) * 0.2f)
            barColors.add(
                Color.hsl(
                    hue = (index * 15f) % 360f,
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
                        hue = (kotlin.random.Random.nextFloat() * 60f + index * 15f) % 360f,
                        saturation = 0.7f + kotlin.random.Random.nextFloat() * 0.3f,
                        lightness = 0.5f + kotlin.random.Random.nextFloat() * 0.3f
                    )
                }
                kotlinx.coroutines.delay(150) // Update every 150ms
            }
        } else {
            // Static bars when paused - fade to monochrome
            repeat(numberOfBars) { index ->
                barHeights[index] = 0.1f + (index % 4) * 0.1f
                barColors[index] = pausedColor
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(200.dp)
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(numberOfBars) { index ->
                val animatedHeight by animateFloatAsState(
                    targetValue = if (index < barHeights.size) barHeights[index] else 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = if (isPlaying) 150 else 500,
                        easing = androidx.compose.animation.core.EaseInOutCubic
                    ), label = ""
                )
                
                val animatedColor by animateColorAsState(
                    targetValue = if (index < barColors.size) barColors[index] else Color.Gray,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300
                    ), label = "EqualizerBarColor"
                )
                
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height((120.dp.value * animatedHeight).dp)
                        .background(
                            animatedColor,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
        
        // Center icon
        Icon(
            imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Album art display with rotation animation
 */
@Composable
private fun AlbumArtDisplay(
    mediaItem: MediaItem,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(20000, easing = androidx.compose.animation.core.LinearEasing)
        ), label = "AlbumArtRotation"
    )
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .rotate(rotation)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        MediaThumbnailImage(
            mediaItem = mediaItem,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            fallbackIconSize = 120.dp
        )
    }
}

/**
 * Enhanced equalizer view with frequency response and audio effects
 */
@Composable
private fun EnhancedEqualizerView(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val equalizerBands by viewModel.equalizerBands.collectAsStateWithLifecycle()
    val bassBoost by viewModel.bassBoost.collectAsStateWithLifecycle()
    val virtualizer by viewModel.virtualizer.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Audio Effects",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        
        // Frequency response visualization
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Frequency Response",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Visual frequency response display
                    FrequencyResponseDisplay(
                        equalizerBands = equalizerBands,
                        isPlaying = playbackState.isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
        }
        
        // Equalizer controls
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preset buttons
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf("Flat", "Rock", "Pop", "Jazz", "Classical", "Bass", "Treble")
                        items(presets.size) { index ->
                            val preset = presets[index]
                            FilterChip(
                                onClick = { viewModel.setEqualizerPreset(preset) },
                                label = { Text(preset) },
                                selected = false
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Equalizer bands
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        equalizerBands.forEachIndexed { index, band ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Slider(
                                    value = band.gain,
                                    onValueChange = { gain ->
                                        viewModel.setEqualizerBand(index, gain)
                                    },
                                    valueRange = -15f..15f,
                                    modifier = Modifier
                                        .height(150.dp)
                                        .graphicsLayer { rotationZ = 270f },
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                
                                Text(
                                    text = "${band.frequency}Hz",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Bass boost and virtualizer
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Audio Effects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bass boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Switch(
                            checked = bassBoost.enabled,
                            onCheckedChange = { enabled ->
                                viewModel.setBassBoost(bassBoost.copy(enabled = enabled))
                            }
                        )
                    }
                    
                    if (bassBoost.enabled) {
                        Slider(
                            value = bassBoost.strength,
                            onValueChange = { strength ->
                                viewModel.setBassBoost(bassBoost.copy(strength = strength))
                            },
                            valueRange = 0f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Strength: ${bassBoost.strength.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Virtualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Virtualizer",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Switch(
                            checked = virtualizer.enabled,
                            onCheckedChange = { enabled ->
                                viewModel.setVirtualizer(virtualizer.copy(enabled = enabled))
                            }
                        )
                    }
                    
                    if (virtualizer.enabled) {
                        Slider(
                            value = virtualizer.strength,
                            onValueChange = { strength ->
                                viewModel.setVirtualizer(virtualizer.copy(strength = strength))
                            },
                            valueRange = 0f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Strength: ${virtualizer.strength.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Frequency response display with animated bars
 */
@Composable
private fun FrequencyResponseDisplay(
    equalizerBands: List<EqualizerBand>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / equalizerBands.size
        
        equalizerBands.forEachIndexed { index, band ->
            val barHeight = if (isPlaying) {
                // Simulate frequency response based on gain
                height * (0.5f + band.gain / 30f).coerceIn(0.1f, 0.9f)
            } else {
                height * 0.5f
            }
            
            val left = index * barWidth
            val top = height - barHeight
            
            // Draw frequency response bar
            drawRect(
                color = Color.hsl(
                    hue = (index * 30f) % 360f,
                    saturation = if (isPlaying) 0.8f else 0.3f,
                    lightness = if (isPlaying) 0.6f else 0.7f,
                    alpha = if (isPlaying) 0.8f else 0.5f
                ),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight)
            )
        }
    }
}