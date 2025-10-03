package com.bytecoder.lurora.frontend.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.frontend.ui.screens.VideoPlayerScreen
import com.bytecoder.lurora.frontend.ui.theme.LuroraTheme
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_MEDIA_ITEM_BUNDLE = "extra_media_item_bundle"
        
        fun startActivity(context: Context, mediaItem: MediaItem) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            val bundle = Bundle().apply {
                putString("id", mediaItem.id)
                putParcelable("uri", mediaItem.uri)
                putString("title", mediaItem.title)
                putString("artist", mediaItem.artist)
                putString("album", mediaItem.album)
                putString("genre", mediaItem.genre)
                putLong("duration", mediaItem.duration)
                putString("mediaType", mediaItem.mediaType.name)
                putParcelable("albumArtUri", mediaItem.albumArtUri)
                putParcelable("subtitleUri", mediaItem.subtitleUri)
                putString("mimeType", mediaItem.mimeType)
                putLong("size", mediaItem.size)
                putLong("dateAdded", mediaItem.dateAdded)
                putInt("playCount", mediaItem.playCount)
                putBoolean("isFavorite", mediaItem.isFavorite)
                putLong("lastPosition", mediaItem.lastPosition)
            }
            intent.putExtra(EXTRA_MEDIA_ITEM_BUNDLE, bundle)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Get the media item from intent
        val bundle = intent.getBundleExtra(EXTRA_MEDIA_ITEM_BUNDLE)
        val mediaItem = bundle?.let { b ->
            try {
                MediaItem(
                    id = b.getString("id") ?: "",
                    uri = b.getParcelable("uri") ?: Uri.EMPTY,
                    title = b.getString("title") ?: "",
                    artist = b.getString("artist"),
                    album = b.getString("album"),
                    genre = b.getString("genre"),
                    duration = b.getLong("duration", 0L),
                    mediaType = MediaType.valueOf(b.getString("mediaType") ?: "VIDEO"),
                    albumArtUri = b.getParcelable("albumArtUri"),
                    subtitleUri = b.getParcelable("subtitleUri"),
                    mimeType = b.getString("mimeType"),
                    size = b.getLong("size", 0L),
                    dateAdded = b.getLong("dateAdded", System.currentTimeMillis()),
                    playCount = b.getInt("playCount", 0),
                    isFavorite = b.getBoolean("isFavorite", false),
                    lastPosition = b.getLong("lastPosition", 0L),
                    metadata = emptyMap() // Can't easily serialize map, assume empty for now
                )
            } catch (e: Exception) {
                null
            }
        }
        
        setContent {
            LuroraTheme {
                val view = LocalView.current
                val viewModel: VideoPlayerViewModel = hiltViewModel()
                val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
                
                // Control status bar visibility based on controls visibility
                LaunchedEffect(isControlsVisible) {
                    val window = (view.context as ComponentActivity).window
                    val windowInsetsController = WindowCompat.getInsetsController(window, view)
                    
                    if (isControlsVisible) {
                        // Show system bars when controls are visible
                        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                        windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                        windowInsetsController.systemBarsBehavior = 
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        // Hide system bars when controls are hidden for immersive experience
                        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                        windowInsetsController.systemBarsBehavior = 
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
                
                // Restore system bars when leaving the screen
                DisposableEffect(Unit) {
                    onDispose {
                        val window = (view.context as ComponentActivity).window
                        val windowInsetsController = WindowCompat.getInsetsController(window, view)
                        // Restore system bars when activity is destroyed
                        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                        windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                    }
                }
                
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            // Only apply system bars padding when controls are visible
                            if (isControlsVisible) {
                                Modifier.systemBarsPadding()
                            } else {
                                Modifier
                            }
                        ),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Play the media item when activity starts
                    LaunchedEffect(mediaItem) {
                        if (mediaItem != null) {
                            viewModel.playVideo(mediaItem)
                        }
                    }
                    
                    VideoPlayerScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}