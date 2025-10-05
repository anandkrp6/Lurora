package com.bytecoder.lurora.frontend.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
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
    // Create a surface that extends to the bottom edge
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NavigationBarDefaults.containerColor,
        tonalElevation = 8.dp
    ) {
        Column {
            // Actual navigation content with proper padding to avoid device nav bar
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(), // Adds safe padding above device nav bar
                tonalElevation = 0.dp, // Remove elevation since parent Surface handles it
                containerColor = androidx.compose.ui.graphics.Color.Transparent // Make transparent to show parent surface
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
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }
    }
}@Composable
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
    // Create a surface that extends to the bottom edge
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NavigationBarDefaults.containerColor,
        tonalElevation = 8.dp
    ) {
        Column {
            // Mini Player (if music is playing) - positioned above nav bar content
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
            
            // Bottom Navigation Bar with safe padding
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(), // Adds safe padding above device nav bar
                tonalElevation = 0.dp, // Remove elevation since parent Surface handles it
                containerColor = androidx.compose.ui.graphics.Color.Transparent // Make transparent to show parent surface
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
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }
    }
}