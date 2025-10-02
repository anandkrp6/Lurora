package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytecoder.lurora.frontend.navigation.*
import com.bytecoder.lurora.backend.models.MediaItem
import com.bytecoder.lurora.backend.models.PlaybackState
import com.bytecoder.lurora.frontend.viewmodels.MusicPlayerViewModel
import com.bytecoder.lurora.frontend.viewmodels.VideoPlayerViewModel

@Composable
fun LuroraBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = 8.dp
    ) {
        MainTab.values().forEach { tab ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = { 
                    Text(
                        text = tab.title,
                        fontSize = 12.sp,
                        fontWeight = if (currentTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                },
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun LuroraBottomBarWithMiniPlayer(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    currentMediaItem: MediaItem?,
    playbackState: PlaybackState,
    isFullPlayerVisible: Boolean,
    musicPlayerViewModel: MusicPlayerViewModel? = null,
    videoPlayerViewModel: VideoPlayerViewModel? = null,
    onOpenFullPlayer: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Mini Player (if music is playing)
        MiniPlayer(
            currentMediaItem = currentMediaItem,
            playbackState = playbackState,
            isFullPlayerVisible = isFullPlayerVisible,
            musicPlayerViewModel = musicPlayerViewModel,
            videoPlayerViewModel = videoPlayerViewModel,
            onOpenFullPlayer = onOpenFullPlayer,
            onClose = onClose,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Bottom Navigation Bar
        LuroraBottomBar(
            currentTab = currentTab,
            onTabSelected = onTabSelected
        )
    }
}