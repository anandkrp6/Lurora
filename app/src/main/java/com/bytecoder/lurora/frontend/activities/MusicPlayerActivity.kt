package com.bytecoder.lurora.frontend.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.frontend.ui.screens.MusicPlayerScreen
import com.bytecoder.lurora.frontend.ui.theme.LuroraTheme
import com.bytecoder.lurora.frontend.viewmodels.MusicPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MusicPlayerActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_MEDIA_ITEM_BUNDLE = "extra_media_item_bundle"
        
        fun startActivity(context: Context, mediaItem: MediaItem) {
            val intent = Intent(context, MusicPlayerActivity::class.java)
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
                    mediaType = MediaType.valueOf(b.getString("mediaType") ?: "AUDIO"),
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
                val isDarkTheme = isSystemInDarkTheme()
                
                // Make system bars transparent
                LaunchedEffect(isDarkTheme) {
                    val window = (view.context as ComponentActivity).window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    
                    val windowInsetsController = WindowCompat.getInsetsController(window, view)
                    // Adapt status bar content to theme: dark content for light theme, light content for dark theme
                    windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }
                
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MusicPlayerViewModel = hiltViewModel()
                    
                    // Play the media item when activity starts
                    LaunchedEffect(mediaItem) {
                        if (mediaItem != null) {
                            viewModel.playMediaItem(mediaItem)
                            viewModel.expandPlayer()
                        }
                    }
                    
                    MusicPlayerScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}