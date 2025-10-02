package com.bytecoder.lurora.frontend.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    
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
            PermissionsScreen(
                onNavigateBack = onNavigateBack,
                onOpenSettings = { 
                    // Navigate to app-specific settings page
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to general settings if app-specific settings fail
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                modifier = modifier
            )
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