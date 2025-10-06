package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp

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
            
            // Material 3 Expressive Progress Indicator
            WavyProgressIndicator(
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                onSeekTo = { viewModel.seekTo(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Full-screen music player
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val showSeekButtons by viewModel.showSeekButtons.collectAsStateWithLifecycle()
    
    // Bottom sheet state for more options
    val bottomSheetState = rememberModalBottomSheetState()
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    
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
                onShowMoreOptions = { showMoreOptionsSheet = true },
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
                    // Visual Display Area - Square with rounded corners covering screen width
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (audioDisplayMode) {
                                "Album Art" -> {
                                    AlbumArtDisplay(
                                        mediaItem = mediaItem,
                                        isPlaying = playbackState.isPlaying,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                    )
                                }
                                "Equalizer" -> {
                                    AnimatedEqualizerView(
                                        isPlaying = playbackState.isPlaying,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "Both", "Both (Album Art & Equalizer)" -> {
                                    // Switching between Album Art and Equalizer
                                    var showAlbumArt by remember { mutableStateOf(true) }
                                    
                                    // Auto-switch every 5 seconds when playing
                                    LaunchedEffect(playbackState.isPlaying) {
                                        if (playbackState.isPlaying) {
                                            while (playbackState.isPlaying) {
                                                kotlinx.coroutines.delay(5000) // 5 seconds
                                                showAlbumArt = !showAlbumArt
                                            }
                                        }
                                    }
                                    
                                    // Crossfade animation between the two views
                                    Crossfade(
                                        targetState = showAlbumArt,
                                        animationSpec = tween(durationMillis = 800),
                                        label = "Visual Display Switch"
                                    ) { isShowingAlbumArt ->
                                        if (isShowingAlbumArt) {
                                            AlbumArtDisplay(
                                                mediaItem = mediaItem,
                                                isPlaying = playbackState.isPlaying,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(24.dp))
                                            )
                                        } else {
                                            AnimatedEqualizerView(
                                                isPlaying = playbackState.isPlaying,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Compact Track Information Bar (above progress bar)
            TrackInfoBar(
                mediaItem = mediaItem,
                isFavorite = viewModel.isFavorite(mediaItem),
                onToggleFavorite = { viewModel.toggleFavorite(mediaItem) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Material 3 Expressive Progress Bar
            WavyProgressIndicator(
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                onSeekTo = { viewModel.seekTo(it) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(playbackState.currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatTime(playbackState.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main controls
            MusicPlayerControls(
                playbackState = playbackState,
                onPlayPause = { viewModel.togglePlayback() },
                onPrevious = { viewModel.seekToPrevious() },
                onNext = { viewModel.seekToNext() },
                onRepeatModeChange = { viewModel.setRepeatMode(it) },
                onShuffleToggle = { viewModel.setShuffleMode(!playbackState.shuffleMode) },
                onSeekBackward = { viewModel.seekTo(maxOf(0, playbackState.currentPosition - 15000)) }, // Seek backward 15 seconds
                onSeekForward = { viewModel.seekTo(minOf(playbackState.duration, playbackState.currentPosition + 15000)) }, // Seek forward 15 seconds
                showSeekButtons = showSeekButtons,
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
        
        // More Options Bottom Sheet
        if (showMoreOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreOptionsSheet = false },
                sheetState = bottomSheetState
            ) {
                MoreOptionsBottomSheet(
                    mediaItem = mediaItem,
                    viewModel = viewModel,
                    onDismiss = { showMoreOptionsSheet = false },
                    onAudioSettings = { /* Navigate to audio settings */ },
                    onSleepTimer = { /* Show sleep timer dialog */ },
                    onShareTrack = { /* Share current track */ },
                    onAddToPlaylist = { /* Show add to playlist dialog */ },
                    onSongInfo = { /* Show song info dialog */ }
                )
            }
        }
    }
}

/**
 * Top bar for full-screen player
 */
@Composable
private fun MusicPlayerTopBar(
    onCollapsePlayer: () -> Unit,
    onShowMoreOptions: () -> Unit,
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
        
        IconButton(onClick = onShowMoreOptions) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options"
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
 * Compact track information bar with album art, song details, and favorite button
 */
@Composable
private fun TrackInfoBar(
    mediaItem: MediaItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album Art (leftmost)
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                mediaItem.albumArtUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Album artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No artwork",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Song details (center, expandable)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Song Title (larger, bold)
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Artist and Album row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Artist Name (left, smaller, italic)
                    mediaItem.artist?.let { artist ->
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Album Name (right, smaller, italic)
                    mediaItem.album?.let { album ->
                        Text(
                            text = album,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Favorite Button (rightmost)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
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
    onRepeatModeChange: (com.bytecoder.lurora.backend.models.RepeatMode) -> Unit,
    onShuffleToggle: () -> Unit,
    onSeekBackward: () -> Unit = {},
    onSeekForward: () -> Unit = {},
    showSeekButtons: Boolean = true,
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
        
        // Backward Seek (15 seconds) - conditional
        if (showSeekButtons) {
            IconButton(onClick = onSeekBackward) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Seek Backward 15s",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        
        // Forward Seek (15 seconds) - conditional
        if (showSeekButtons) {
            IconButton(onClick = onSeekForward) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Seek Forward 15s",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                com.bytecoder.lurora.backend.models.RepeatMode.OFF -> com.bytecoder.lurora.backend.models.RepeatMode.ALL
                com.bytecoder.lurora.backend.models.RepeatMode.ALL -> com.bytecoder.lurora.backend.models.RepeatMode.ONE
                com.bytecoder.lurora.backend.models.RepeatMode.ONE -> com.bytecoder.lurora.backend.models.RepeatMode.OFF
            }
            onRepeatModeChange(nextMode)
        }) {
            Icon(
                imageVector = when (playbackState.repeatMode) {
                    com.bytecoder.lurora.backend.models.RepeatMode.OFF -> Icons.Default.Repeat
                    com.bytecoder.lurora.backend.models.RepeatMode.ONE -> Icons.Default.RepeatOne
                    com.bytecoder.lurora.backend.models.RepeatMode.ALL -> Icons.Default.Repeat
                },
                contentDescription = "Repeat Mode",
                tint = if (playbackState.repeatMode != com.bytecoder.lurora.backend.models.RepeatMode.OFF) 
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
    val numberOfBars = 40 // Increased from 24 to 40 bars
    val barHeights = remember { mutableStateListOf<Float>() }
    val barColors = remember { mutableStateListOf<Color>() }
    val pausedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    // Initialize bars
    LaunchedEffect(Unit) {
        repeat(numberOfBars) { index ->
            // Create bell curve pattern - tallest in middle, smaller towards edges
            val center = numberOfBars / 2f
            val distance = kotlin.math.abs(index - center) / center // Normalized distance from center (0-1)
            val bellCurveHeight = 0.8f * kotlin.math.exp(-3.0 * distance * distance).toFloat() + 0.1f
            
            barHeights.add(bellCurveHeight)
            barColors.add(
                Color.hsl(
                    hue = (index * 9f) % 360f, // More color variation
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
                    // Random height animation with more range
                    barHeights[index] = kotlin.random.Random.nextFloat() * 0.9f + 0.1f
                    
                    // Dynamic color change
                    barColors[index] = Color.hsl(
                        hue = (kotlin.random.Random.nextFloat() * 60f + index * 9f) % 360f,
                        saturation = 0.7f + kotlin.random.Random.nextFloat() * 0.3f,
                        lightness = 0.5f + kotlin.random.Random.nextFloat() * 0.3f
                    )
                }
                kotlinx.coroutines.delay(120) // Slightly faster updates for more bars
            }
        } else {
            // Static bars when paused - fade to monochrome with bell curve pattern
            repeat(numberOfBars) { index ->
                // Create bell curve pattern - tallest in middle, smaller towards edges
                val center = numberOfBars / 2f
                val distance = kotlin.math.abs(index - center) / center // Normalized distance from center (0-1)
                val bellCurveHeight = 0.6f * kotlin.math.exp(-3.0 * distance * distance).toFloat() + 0.1f
                
                barHeights[index] = bellCurveHeight
                barColors[index] = pausedColor
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp) // Match the parent container's rounded corners
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize() // Use full available space
                .padding(16.dp), // Add padding from edges
            horizontalArrangement = Arrangement.spacedBy(3.dp), // Tighter spacing for more bars
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(numberOfBars) { index ->
                val animatedHeight by animateFloatAsState(
                    targetValue = if (index < barHeights.size) barHeights[index] else 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = if (isPlaying) 120 else 500,
                        easing = androidx.compose.animation.core.EaseInOutCubic
                    ), label = ""
                )
                
                val animatedColor by animateColorAsState(
                    targetValue = if (index < barColors.size) barColors[index] else Color.Gray,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300
                    ), label = "EqualizerBarColor"
                )
                
                // Calculate dynamic bar width based on available space
                val maxBarHeight = 250.dp // Increased from 120dp for bigger display area
                
                Box(
                    modifier = Modifier
                        .weight(1f) // Use equal weight for all bars to fill width
                        .height((maxBarHeight.value * animatedHeight).dp)
                        .background(
                            animatedColor,
                            RoundedCornerShape(4.dp) // Slightly larger corner radius
                        )
                )
            }
        }
        
        // Center icon - made smaller relative to the bigger area
        Icon(
            imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp), // Increased from 48dp
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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

/**
 * More Options Bottom Sheet with various player actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsBottomSheet(
    mediaItem: MediaItem,
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit,
    onAudioSettings: () -> Unit,
    onSleepTimer: () -> Unit,
    onShareTrack: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSongInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp)
                )
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "More Options",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Current track info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaThumbnailImage(
                mediaItem = mediaItem,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                fallbackIconSize = 24.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                mediaItem.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Options list
        MoreOptionItem(
            icon = Icons.Default.Settings,
            title = "Audio Settings",
            subtitle = "Equalizer, sound effects",
            onClick = {
                onAudioSettings()
                onDismiss()
            }
        )

        // Seek buttons toggle option
        val showSeekButtons by viewModel.showSeekButtons.collectAsStateWithLifecycle()
        MoreOptionItemWithSwitch(
            icon = if (showSeekButtons) Icons.Default.FastForward else Icons.Default.FastRewind,
            title = "Seek Buttons",
            subtitle = if (showSeekButtons) "Hide forward/backward buttons" else "Show forward/backward buttons",
            isChecked = showSeekButtons,
            onToggle = {
                viewModel.toggleSeekButtons()
            }
        )
        
        MoreOptionItem(
            icon = Icons.Default.Timer,
            title = "Sleep Timer",
            subtitle = "Auto-stop music playback",
            onClick = {
                onSleepTimer()
                onDismiss()
            }
        )
        
        MoreOptionItem(
            icon = Icons.Default.Share,
            title = "Share Track",
            subtitle = "Share this song with others",
            onClick = {
                onShareTrack()
                onDismiss()
            }
        )
        
        MoreOptionItem(
            icon = Icons.Default.PlaylistAdd,
            title = "Add to Playlist",
            subtitle = "Save to your playlists",
            onClick = {
                onAddToPlaylist()
                onDismiss()
            }
        )
        
        MoreOptionItem(
            icon = Icons.Default.Info,
            title = "Song Info",
            subtitle = "View detailed track information",
            onClick = {
                onSongInfo()
                onDismiss()
            }
        )
        
        // Bottom padding for safe area
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Individual option item in the bottom sheet
 */
@Composable
private fun MoreOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual option item with switch in the bottom sheet
 */
@Composable
private fun MoreOptionItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
    }
}

/**
 * Material 3 Expressive (Wavy) Progress Indicator for music seeking
 */
@Composable
private fun WavyProgressIndicator(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long = currentPosition,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
    
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }
    
    // Get colors at Composable level
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    
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
            color = surfaceVariant.copy(alpha = 0.5f),
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
            color = surfaceVariant.copy(alpha = 0.3f),
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
            color = surfaceColor,
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