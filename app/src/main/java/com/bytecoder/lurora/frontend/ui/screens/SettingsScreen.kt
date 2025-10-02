package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytecoder.lurora.frontend.viewmodels.SettingsViewModel

/**
 * Settings Screen with expandable categories and Material 3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsCategories = remember { getSettingsCategories() }
    val filteredCategories = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            settingsCategories
        } else {
            settingsCategories.filter { category ->
                category.title.contains(searchQuery, ignoreCase = true) ||
                category.settings.any { it.title.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // Handle UI state
    LaunchedEffect(uiState.showMessage) {
        uiState.showMessage?.let {
            // Show snackbar or toast message
            viewModel.clearMessage()
        }
    }

    // Reset confirmation dialog
    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetDialog() },
            title = { Text("Reset All Settings") },
            text = { 
                Text("This will reset all settings to their default values. This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resetAllSettings() }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissResetDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar with search and menu
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = { showMoreMenu = true }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Settings") },
                            onClick = { 
                                viewModel.exportSettings()
                                showMoreMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Settings") },
                            onClick = { 
                                viewModel.importSettings()
                                showMoreMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset All") },
                            onClick = { 
                                viewModel.showResetDialog()
                                showMoreMenu = false 
                            }
                        )
                    }
                }
            }
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search settings") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        // Settings Categories
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCategories) { category ->
                SettingsCategoryCard(
                    category = category,
                    onSettingChanged = { setting, value -> 
                        viewModel.updateSetting(setting.key, value)
                    }
                )
            }
        }
    }
}

/**
 * Individual Settings Category Card with expandable content
 */
@Composable
fun SettingsCategoryCard(
    category: SettingsCategory,
    onSettingChanged: (SettingItem, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (category.subtitle.isNotEmpty()) {
                        Text(
                            text = category.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    category.settings.forEach { setting ->
                        SettingItem(
                            setting = setting,
                            onValueChanged = { value -> onSettingChanged(setting, value) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Setting Item
 */
@Composable
fun SettingItem(
    setting: SettingItem,
    onValueChanged: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    when (setting.type) {
        SettingType.SWITCH -> {
            SwitchSettingItem(
                setting = setting,
                isChecked = setting.value as Boolean,
                onCheckedChange = onValueChanged,
                modifier = modifier
            )
        }
        SettingType.SLIDER -> {
            SliderSettingItem(
                setting = setting,
                value = setting.value as Float,
                onValueChange = onValueChanged,
                modifier = modifier
            )
        }
        SettingType.DROPDOWN -> {
            DropdownSettingItem(
                setting = setting,
                selectedValue = setting.value as String,
                onValueSelected = onValueChanged,
                modifier = modifier
            )
        }
        SettingType.TEXT_INPUT -> {
            TextInputSettingItem(
                setting = setting,
                value = setting.value as String,
                onValueChange = onValueChanged,
                modifier = modifier
            )
        }
        SettingType.ACTION -> {
            ActionSettingItem(
                setting = setting,
                onAction = { onValueChanged(Unit) },
                modifier = modifier
            )
        }
    }
}

@Composable
fun SwitchSettingItem(
    setting: SettingItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (setting.subtitle.isNotEmpty()) {
                Text(
                    text = setting.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderSettingItem(
    setting: SettingItem,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (setting.subtitle.isNotEmpty()) {
                    Text(
                        text = setting.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingItem(
    setting: SettingItem,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = setting.options ?: emptyList()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = setting.title,
            style = MaterialTheme.typography.bodyLarge
        )
        if (setting.subtitle.isNotEmpty()) {
            Text(
                text = setting.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TextInputSettingItem(
    setting: SettingItem,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = setting.title,
            style = MaterialTheme.typography.bodyLarge
        )
        if (setting.subtitle.isNotEmpty()) {
            Text(
                text = setting.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun ActionSettingItem(
    setting: SettingItem,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAction() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (setting.subtitle.isNotEmpty()) {
                Text(
                    text = setting.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Data classes for settings structure
data class SettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val settings: List<SettingItem>
)

data class SettingItem(
    val key: String,
    val title: String,
    val subtitle: String = "",
    val type: SettingType,
    val value: Any,
    val options: List<String>? = null
)

enum class SettingType {
    SWITCH,
    SLIDER, 
    DROPDOWN,
    TEXT_INPUT,
    ACTION
}

/**
 * Generate the complete settings categories structure
 */
private fun getSettingsCategories(): List<SettingsCategory> {
    return listOf(
        // General Settings
        SettingsCategory(
            id = "general",
            title = "General",
            subtitle = "App behavior and preferences",
            icon = Icons.Default.Settings,
            settings = listOf(
                SettingItem("dark_mode", "Dark Mode", "Enable dark theme", SettingType.SWITCH, true),
                SettingItem("auto_theme", "Follow System Theme", "Match system appearance", SettingType.SWITCH, false),
                SettingItem("language", "Language", "App language", SettingType.DROPDOWN, "English", 
                    listOf("English", "Spanish", "French", "German", "Chinese")),
                SettingItem("startup_screen", "Startup Screen", "Default screen on app launch", SettingType.DROPDOWN, "Home",
                    listOf("Home", "Downloads", "File Explorer", "History")),
                SettingItem("notifications", "Notifications", "Enable app notifications", SettingType.SWITCH, true),
                SettingItem("vibration", "Vibration Feedback", "Haptic feedback", SettingType.SWITCH, true)
            )
        ),
        
        // Video Player Settings
        SettingsCategory(
            id = "video_player",
            title = "Video Player",
            subtitle = "Video playback settings",
            icon = Icons.Default.VideoLibrary,
            settings = listOf(
                SettingItem("auto_rotate", "Auto Rotate", "Rotate screen for video", SettingType.SWITCH, true),
                SettingItem("default_quality", "Default Quality", "Preferred video quality", SettingType.DROPDOWN, "Auto",
                    listOf("Auto", "144p", "240p", "360p", "480p", "720p", "1080p")),
                SettingItem("playback_speed", "Default Speed", "Playback speed", SettingType.SLIDER, 1.0f),
                SettingItem("brightness", "Brightness", "Video brightness", SettingType.SLIDER, 0.8f),
                SettingItem("volume", "Volume", "Video volume", SettingType.SLIDER, 0.7f),
                SettingItem("subtitles", "Auto Subtitles", "Enable subtitles when available", SettingType.SWITCH, false),
                SettingItem("fullscreen_gestures", "Fullscreen Gestures", "Swipe gestures in fullscreen", SettingType.SWITCH, true)
            )
        ),
        
        // Audio Player Settings  
        SettingsCategory(
            id = "audio_player",
            title = "Audio Player",
            subtitle = "Audio playback settings",
            icon = Icons.Default.AudioFile,
            settings = listOf(
                SettingItem("background_audio", "Background Playback", "Continue audio when app is minimized", SettingType.SWITCH, true),
                SettingItem("audio_quality", "Audio Quality", "Audio stream quality", SettingType.DROPDOWN, "High",
                    listOf("Low", "Medium", "High", "Lossless")),
                SettingItem("equalizer", "Equalizer", "Enable audio equalizer", SettingType.SWITCH, false),
                SettingItem("crossfade", "Crossfade", "Smooth transition between tracks", SettingType.SWITCH, false),
                SettingItem("gapless", "Gapless Playback", "No gaps between tracks", SettingType.SWITCH, true)
            )
        ),
        
        // Mini Player Settings
        SettingsCategory(
            id = "mini_player",
            title = "Mini Player",
            subtitle = "Picture-in-picture settings",
            icon = Icons.Default.PictureInPicture,
            settings = listOf(
                SettingItem("mini_player_enabled", "Enable Mini Player", "Picture-in-picture mode", SettingType.SWITCH, true),
                SettingItem("mini_player_size", "Mini Player Size", "Size of mini player", SettingType.SLIDER, 0.6f),
                SettingItem("mini_player_position", "Default Position", "Mini player position", SettingType.DROPDOWN, "Bottom Right",
                    listOf("Top Left", "Top Right", "Bottom Left", "Bottom Right")),
                SettingItem("auto_mini_player", "Auto Mini Player", "Auto-enable when minimizing", SettingType.SWITCH, true),
                SettingItem("mini_player_audio_display", "Audio Display Mode", "What to show for audio files", SettingType.DROPDOWN, "Both (Album Art & Equalizer)",
                    listOf("Album Art", "Equalizer", "Both (Album Art & Equalizer)")),
                SettingItem("mini_player_video_display", "Video Display Mode", "What to show for video files", SettingType.DROPDOWN, "Video Playing",
                    listOf("Video Playing", "Thumbnail", "Equalizer", "Both (Thumbnail & Equalizer)")),
                SettingItem("mini_player_switch_interval", "Switch Interval", "Auto-switch interval in seconds", SettingType.SLIDER, 10f)
            )
        ),
        
        // Online Settings
        SettingsCategory(
            id = "online",
            title = "Online",
            subtitle = "Network and streaming settings",
            icon = Icons.Default.CloudDownload,
            settings = listOf(
                SettingItem("wifi_only", "WiFi Only", "Use WiFi for streaming", SettingType.SWITCH, false),
                SettingItem("data_saver", "Data Saver", "Reduce data usage", SettingType.SWITCH, false),
                SettingItem("preload_videos", "Preload Videos", "Buffer next videos", SettingType.SWITCH, true),
                SettingItem("cache_size", "Cache Size", "Video cache limit", SettingType.DROPDOWN, "1GB",
                    listOf("500MB", "1GB", "2GB", "5GB", "10GB", "Unlimited")),
                SettingItem("proxy_url", "Proxy URL", "Custom proxy server", SettingType.TEXT_INPUT, ""),
                SettingItem("timeout", "Connection Timeout", "Network timeout in seconds", SettingType.SLIDER, 0.3f)
            )
        ),
        
        // Downloads Settings
        SettingsCategory(
            id = "downloads",
            title = "Downloads",
            subtitle = "Download behavior and storage",
            icon = Icons.Default.Download,
            settings = listOf(
                SettingItem("download_location", "Download Location", "Choose download folder", SettingType.ACTION, Unit),
                SettingItem("download_quality", "Download Quality", "Default download quality", SettingType.DROPDOWN, "720p",
                    listOf("360p", "480p", "720p", "1080p", "Best Available")),
                SettingItem("parallel_downloads", "Parallel Downloads", "Number of simultaneous downloads", SettingType.SLIDER, 0.3f),
                SettingItem("auto_retry", "Auto Retry", "Retry failed downloads", SettingType.SWITCH, true),
                SettingItem("wifi_only_download", "WiFi Only Downloads", "Download only on WiFi", SettingType.SWITCH, true),
                SettingItem("auto_delete", "Auto Delete", "Delete completed downloads after", SettingType.DROPDOWN, "Never",
                    listOf("Never", "1 Day", "3 Days", "1 Week", "1 Month"))
            )
        ),
        
        // History Settings
        SettingsCategory(
            id = "history",
            title = "History",
            subtitle = "Viewing history and privacy",
            icon = Icons.Default.History,
            settings = listOf(
                SettingItem("save_history", "Save History", "Keep viewing history", SettingType.SWITCH, true),
                SettingItem("history_limit", "History Limit", "Maximum history entries", SettingType.DROPDOWN, "1000",
                    listOf("100", "500", "1000", "5000", "Unlimited")),
                SettingItem("auto_clear_history", "Auto Clear History", "Automatically clear old history", SettingType.DROPDOWN, "Never",
                    listOf("Never", "1 Week", "1 Month", "3 Months", "1 Year")),
                SettingItem("incognito_mode", "Incognito Mode", "Don't save history", SettingType.SWITCH, false),
                SettingItem("clear_history", "Clear History", "Delete all viewing history", SettingType.ACTION, Unit)
            )
        ),
        
        // File Manager Settings
        SettingsCategory(
            id = "file_manager",
            title = "File Manager", 
            subtitle = "File browser and organization",
            icon = Icons.Default.Folder,
            settings = listOf(
                SettingItem("show_hidden", "Show Hidden Files", "Display hidden files and folders", SettingType.SWITCH, false),
                SettingItem("default_view", "Default View", "File list view style", SettingType.DROPDOWN, "List",
                    listOf("List", "Grid", "Detailed List")),
                SettingItem("sort_by", "Sort By", "Default file sorting", SettingType.DROPDOWN, "Name",
                    listOf("Name", "Date", "Size", "Type")),
                SettingItem("show_thumbnails", "Show Thumbnails", "Display media thumbnails", SettingType.SWITCH, true),
                SettingItem("remember_location", "Remember Location", "Remember last opened folder", SettingType.SWITCH, true)
            )
        ),
        
        // Privacy & Security
        SettingsCategory(
            id = "privacy_security",
            title = "Privacy & Security",
            subtitle = "Privacy and security options",
            icon = Icons.Default.Security,
            settings = listOf(
                SettingItem("app_lock", "App Lock", "Require authentication to open app", SettingType.SWITCH, false),
                SettingItem("fingerprint", "Fingerprint Lock", "Use fingerprint authentication", SettingType.SWITCH, false),
                SettingItem("auto_lock", "Auto Lock", "Lock app when minimized", SettingType.SWITCH, false),
                SettingItem("hide_recent", "Hide from Recent Apps", "Hide app from recent apps", SettingType.SWITCH, false),
                SettingItem("clear_data", "Clear App Data", "Reset all app data", SettingType.ACTION, Unit),
                SettingItem("privacy_policy", "Privacy Policy", "View privacy policy", SettingType.ACTION, Unit)
            )
        ),
        
        // Advanced Settings
        SettingsCategory(
            id = "advanced",
            title = "Advanced",
            subtitle = "Developer and advanced options",
            icon = Icons.Default.Code,
            settings = listOf(
                SettingItem("debug_mode", "Debug Mode", "Enable debug logging", SettingType.SWITCH, false),
                SettingItem("hardware_acceleration", "Hardware Acceleration", "Use GPU for decoding", SettingType.SWITCH, true),
                SettingItem("experimental_features", "Experimental Features", "Enable beta features", SettingType.SWITCH, false),
                SettingItem("export_logs", "Export Logs", "Export debug logs", SettingType.ACTION, Unit),
                SettingItem("reset_settings", "Reset Settings", "Reset all settings to default", SettingType.ACTION, Unit),
                SettingItem("app_version", "App Version", "View app information", SettingType.ACTION, Unit)
            )
        )
    )
}