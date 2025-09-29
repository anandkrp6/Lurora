package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bytecoder.lurora.backend.models.*
import com.bytecoder.lurora.frontend.navigation.MoreTab

/**
 * Router that displays the appropriate screen based on the current MoreTab selection
 */
@Composable
fun MoreTabScreenRouter(
    currentMoreTab: MoreTab,
    onNavigateBack: () -> Unit = {},
    onPlayMedia: (MediaItem, Long) -> Unit = { _, _ -> },
    onOpenFile: (FileSystemItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (currentMoreTab) {
        MoreTab.HISTORY -> {
            HistoryScreen(
                onNavigateBack = onNavigateBack,
                onPlayMedia = onPlayMedia,
                modifier = modifier
            )
        }
        MoreTab.DOWNLOADS -> {
            DownloadsScreen(
                onNavigateBack = onNavigateBack,
                onOpenFile = { downloadItem -> 
                    // Convert DownloadItem to FileSystemItem for callback compatibility
                    // This is a simplified conversion - in real implementation, 
                    // you'd need proper conversion logic
                },
                modifier = modifier
            )
        }
        MoreTab.PERMISSIONS -> {
            // TODO: Fix PermissionsScreen compilation error
            // Temporary placeholder
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = onNavigateBack) {
                    Text("Back")
                }
            }
        }
        MoreTab.FILE_EXPLORER -> {
            FileExplorerScreen(
                onPlayMedia = { fileItem -> 
                    // Convert FileSystemItem to MediaItem for playback if needed
                    // This is a simplified conversion - in real implementation,
                    // you'd need proper conversion logic
                },
                onOpenFile = onOpenFile,
                modifier = modifier
            )
        }
        MoreTab.SETTINGS -> {
            SettingsScreen(
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
        MoreTab.ABOUT -> {
            AboutScreen(
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
        MoreTab.FEEDBACK -> {
            FeedbackScreen(
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
        MoreTab.TIPS -> {
            TipsScreen(
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
    }
}