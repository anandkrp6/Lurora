package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.MediaType
import com.bytecoder.lurora.backend.utils.ThumbnailExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A component that loads media thumbnails asynchronously with proper fallbacks
 */
@Composable
fun MediaThumbnailImage(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIconSize: Dp = 24.dp,
    showLoading: Boolean = true
) {
    var thumbnailUri by remember(mediaItem.id) { mutableStateOf(mediaItem.albumArtUri) }
    var isLoading by remember(mediaItem.id) { mutableStateOf(false) }
    val context = LocalContext.current

    // If no thumbnail URI exists, try to extract one
    LaunchedEffect(mediaItem.id) {
        if (thumbnailUri == null && mediaItem.metadata?.get("file_path") != null) {
            isLoading = true
            try {
                val filePath = mediaItem.metadata["file_path"] as String
                val extractor = ThumbnailExtractor(context)
                
                thumbnailUri = withContext(Dispatchers.IO) {
                    when (mediaItem.mediaType) {
                        MediaType.VIDEO -> extractor.getVideoThumbnail(filePath, mediaItem.id)
                        MediaType.AUDIO -> extractor.getAlbumArt(filePath, mediaItem.id)
                    }
                }
            } catch (e: Exception) {
                // Failed to extract thumbnail, will show fallback icon
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbnailUri != null -> {
                AsyncImage(
                    model = thumbnailUri,
                    contentDescription = when (mediaItem.mediaType) {
                        MediaType.VIDEO -> "Video thumbnail"
                        MediaType.AUDIO -> "Album art"
                    },
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale
                )
            }
            isLoading && showLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(fallbackIconSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                // Show fallback icon
                Icon(
                    imageVector = when (mediaItem.mediaType) {
                        MediaType.VIDEO -> Icons.Default.PlayArrow
                        MediaType.AUDIO -> Icons.Default.MusicNote
                    },
                    contentDescription = when (mediaItem.mediaType) {
                        MediaType.VIDEO -> "Video"
                        MediaType.AUDIO -> "Music"
                    },
                    modifier = Modifier.size(fallbackIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}