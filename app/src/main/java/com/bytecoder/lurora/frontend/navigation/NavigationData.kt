package com.bytecoder.lurora.frontend.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Main bottom navigation tabs
enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    VIDEO("Video", Icons.Default.VideoLibrary, "video"),
    MUSIC("Music", Icons.Default.LibraryMusic, "music"), 
    ONLINE("Online", Icons.Default.Language, "online"),
    MORE("More", Icons.Default.MoreHoriz, "more") // Renamed from OPTIONS to MORE
}

// Sections within each tab
enum class VideoSection(val title: String, val route: String) {
    LIBRARY("Library", "library"),
    PLAYLIST("Playlist", "playlist"),
    FAVORITES("Favorites", "favorites")
}

enum class MusicSection(val title: String, val route: String) {
    LIBRARY("Library", "library"),
    PLAYLIST("Playlist", "playlist"),
    FAVORITES("Favorites", "favorites")
}

enum class OnlineSection(val title: String, val route: String) {
    BROWSE("Browse", "browse"),
    PLAYLIST("Playlist", "playlist"),
    FAVORITES("Favorites", "favorites")
}

// MORE tabs - These are individual tabs within the More section
// Each tab can have its own features, but simplified compared to main tabs
enum class MoreTab(
    val title: String, 
    val route: String,
    val icon: ImageVector,
    val description: String,
    // Tab-specific capabilities
    val hasSearch: Boolean = false,
    val hasSort: Boolean = false,
    val hasFilter: Boolean = false,
    val hasViewOptions: Boolean = false,
    val hasRefresh: Boolean = false,
    val hasExport: Boolean = false,
    val hasSettings: Boolean = false
) {
    HISTORY(
        title = "History", 
        route = "history", 
        icon = Icons.Default.History,
        description = "View your watch and play history",
        hasSearch = true,
        hasSort = true,
        hasFilter = true,
        hasViewOptions = true,
        hasExport = true
    ),
    DOWNLOADS(
        title = "Downloads", 
        route = "downloads", 
        icon = Icons.Default.Download,
        description = "Manage your downloaded files",
        hasSearch = true,
        hasSort = true,
        hasFilter = true,
        hasViewOptions = true,
        hasRefresh = true
    ),
    PERMISSIONS(
        title = "Permissions", 
        route = "permissions", 
        icon = Icons.Default.Security,
        description = "App permissions and access",
        hasRefresh = true,
        hasSettings = true
    ),
    FILE_EXPLORER(
        title = "File Explorer", 
        route = "file_explorer", 
        icon = Icons.Default.Folder,
        description = "Browse and manage files",
        hasSearch = true,
        hasSort = true,
        hasFilter = true,
        hasViewOptions = true,
        hasRefresh = true
    ),
    SETTINGS(
        title = "Settings", 
        route = "settings", 
        icon = Icons.Default.Settings,
        description = "App preferences and configuration",
        hasSearch = true,
        hasExport = true
    ),
    FEEDBACK(
        title = "Feedback", 
        route = "feedback", 
        icon = Icons.Default.Feedback,
        description = "Send feedback and suggestions",
        hasRefresh = true
    ),
    ABOUT(
        title = "About", 
        route = "about", 
        icon = Icons.Default.Info,
        description = "About Lurora and app information"
    ),
    TIPS(
        title = "Tips", 
        route = "tips", 
        icon = Icons.Default.Lightbulb,
        description = "Tips and tricks for using the app",
        hasRefresh = true
    )
}

// Sort options for different contexts
enum class SortOption(val title: String, val icon: ImageVector) {
    NAME_ASC("Name A-Z", Icons.Default.SortByAlpha),
    NAME_DESC("Name Z-A", Icons.Default.SortByAlpha),
    DATE_ADDED_ASC("Date Added ↑", Icons.Default.DateRange),
    DATE_ADDED_DESC("Date Added ↓", Icons.Default.DateRange),
    SIZE_ASC("Size ↑", Icons.Default.Storage),
    SIZE_DESC("Size ↓", Icons.Default.Storage),
    DURATION_ASC("Duration ↑", Icons.Default.AccessTime),
    DURATION_DESC("Duration ↓", Icons.Default.AccessTime),
    ARTIST_ASC("Artist A-Z", Icons.Default.Person),
    ARTIST_DESC("Artist Z-A", Icons.Default.Person),
    ALBUM_ASC("Album A-Z", Icons.Default.Album),
    ALBUM_DESC("Album Z-A", Icons.Default.Album)
}

// View options
enum class ViewOption(val title: String, val icon: ImageVector) {
    LIST("List View", Icons.Default.ViewList),
    GRID("Grid View", Icons.Default.ViewModule),
    COMPACT("Compact View", Icons.Default.ViewCompact)
}

// Filter options
enum class FilterOption(val title: String, val icon: ImageVector) {
    ALL("All", Icons.Default.SelectAll),
    RECENT("Recent", Icons.Default.Schedule),
    FAVORITES("Favorites", Icons.Default.Favorite),
    DOWNLOADED("Downloaded", Icons.Default.Download)
}

// Navigation state data class
data class NavigationState(
    val currentTab: MainTab = MainTab.VIDEO,
    val currentVideoSection: VideoSection = VideoSection.LIBRARY,
    val currentMusicSection: MusicSection = MusicSection.LIBRARY,
    val currentOnlineSection: OnlineSection = OnlineSection.BROWSE,
    val currentMoreTab: MoreTab = MoreTab.HISTORY, // Changed to reflect tab nature
    
    // Navigation Stack for advanced navigation
    val navigationStack: List<NavigationScreen> = listOf(NavigationScreen.MainTab(com.bytecoder.lurora.frontend.navigation.MainTab.VIDEO)),
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    
    val videoSortOption: SortOption = SortOption.NAME_ASC,
    val musicSortOption: SortOption = SortOption.NAME_ASC,
    val onlineSortOption: SortOption = SortOption.NAME_ASC,
    val videoViewOption: ViewOption = ViewOption.GRID,
    val musicViewOption: ViewOption = ViewOption.LIST,
    val onlineViewOption: ViewOption = ViewOption.GRID,
    val videoFilterOption: FilterOption = FilterOption.ALL,
    val musicFilterOption: FilterOption = FilterOption.ALL,
    val onlineFilterOption: FilterOption = FilterOption.ALL
)

// Navigation Screen types for stack management
sealed class NavigationScreen(open val route: String, open val title: String) {
    data class MainTab(val tab: com.bytecoder.lurora.frontend.navigation.MainTab) : NavigationScreen(tab.route, tab.title)
    data class MoreTabPage(val moreTab: MoreTab, val parentTab: com.bytecoder.lurora.frontend.navigation.MainTab = com.bytecoder.lurora.frontend.navigation.MainTab.MORE) : NavigationScreen(
        "${parentTab.route}/${moreTab.route}", 
        moreTab.title
    )
    data class DeepPage(override val route: String, override val title: String, val parent: NavigationScreen) : NavigationScreen(route, title)
}

// Breadcrumb item for navigation trails
data class BreadcrumbItem(
    val title: String,
    val screen: NavigationScreen,
    val isClickable: Boolean = true
)

// Deep link patterns
object DeepLinkPatterns {
    const val BASE_SCHEME = "lurora"
    const val SETTINGS_AUDIO = "$BASE_SCHEME://settings/audio"
    const val SETTINGS_VIDEO = "$BASE_SCHEME://settings/video"
    const val DOWNLOADS = "$BASE_SCHEME://downloads"
    const val PERMISSIONS = "$BASE_SCHEME://permissions"
    const val ABOUT = "$BASE_SCHEME://about"
    const val HISTORY = "$BASE_SCHEME://history"
    const val FILE_EXPLORER = "$BASE_SCHEME://file-explorer"
    const val FEEDBACK = "$BASE_SCHEME://feedback"
    const val TIPS = "$BASE_SCHEME://tips"
    
    fun parseDeepLink(url: String): NavigationScreen? {
        if (!url.startsWith(BASE_SCHEME)) return null
        
        return when {
            url == DOWNLOADS -> NavigationScreen.MoreTabPage(MoreTab.DOWNLOADS)
            url == PERMISSIONS -> NavigationScreen.MoreTabPage(MoreTab.PERMISSIONS)
            url == ABOUT -> NavigationScreen.MoreTabPage(MoreTab.ABOUT)
            url == HISTORY -> NavigationScreen.MoreTabPage(MoreTab.HISTORY)
            url == FILE_EXPLORER -> NavigationScreen.MoreTabPage(MoreTab.FILE_EXPLORER)
            url == FEEDBACK -> NavigationScreen.MoreTabPage(MoreTab.FEEDBACK)
            url == TIPS -> NavigationScreen.MoreTabPage(MoreTab.TIPS)
            url.startsWith("$BASE_SCHEME://settings") -> NavigationScreen.MoreTabPage(MoreTab.SETTINGS)
            else -> null
        }
    }
}

// Helper functions
object NavigationHelper {
    fun getTabSections(tab: MainTab): List<String> {
        return when (tab) {
            MainTab.VIDEO -> VideoSection.values().map { it.title }
            MainTab.MUSIC -> MusicSection.values().map { it.title }
            MainTab.ONLINE -> OnlineSection.values().map { it.title }
            MainTab.MORE -> MoreTab.values().map { it.title } // Updated to use MoreTab
        }
    }
    
    fun getDefaultSection(tab: MainTab): String {
        return when (tab) {
            MainTab.VIDEO -> VideoSection.LIBRARY.title
            MainTab.MUSIC -> MusicSection.LIBRARY.title
            MainTab.ONLINE -> OnlineSection.BROWSE.title
            MainTab.MORE -> MoreTab.HISTORY.title // Updated to use MoreTab
        }
    }
    
    // Get available features for a specific MoreTab
    fun getMoreTabFeatures(moreTab: MoreTab): Set<String> {
        return buildSet {
            if (moreTab.hasSearch) add("search")
            if (moreTab.hasSort) add("sort")
            if (moreTab.hasFilter) add("filter")
            if (moreTab.hasViewOptions) add("viewOptions")
            if (moreTab.hasRefresh) add("refresh")
            if (moreTab.hasExport) add("export")
            if (moreTab.hasSettings) add("settings")
        }
    }
    
    fun getSortOptionsForTab(tab: MainTab): List<SortOption> {
        return when (tab) {
            MainTab.VIDEO -> listOf(
                SortOption.NAME_ASC, SortOption.NAME_DESC,
                SortOption.DATE_ADDED_ASC, SortOption.DATE_ADDED_DESC,
                SortOption.SIZE_ASC, SortOption.SIZE_DESC,
                SortOption.DURATION_ASC, SortOption.DURATION_DESC
            )
            MainTab.MUSIC -> listOf(
                SortOption.NAME_ASC, SortOption.NAME_DESC,
                SortOption.ARTIST_ASC, SortOption.ARTIST_DESC,
                SortOption.ALBUM_ASC, SortOption.ALBUM_DESC,
                SortOption.DATE_ADDED_ASC, SortOption.DATE_ADDED_DESC,
                SortOption.DURATION_ASC, SortOption.DURATION_DESC
            )
            MainTab.ONLINE -> listOf(
                SortOption.NAME_ASC, SortOption.NAME_DESC,
                SortOption.DATE_ADDED_ASC, SortOption.DATE_ADDED_DESC
            )
            MainTab.MORE -> emptyList() // Updated from OPTIONS to MORE
        }
    }
    
    // Navigation Stack Management
    fun pushToStack(currentStack: List<NavigationScreen>, newScreen: NavigationScreen): List<NavigationScreen> {
        return currentStack + newScreen
    }
    
    fun popFromStack(currentStack: List<NavigationScreen>): List<NavigationScreen> {
        return if (currentStack.size > 1) currentStack.dropLast(1) else currentStack
    }
    
    fun generateBreadcrumbs(navigationStack: List<NavigationScreen>): List<BreadcrumbItem> {
        return navigationStack.map { screen ->
            BreadcrumbItem(
                title = screen.title,
                screen = screen,
                isClickable = screen != navigationStack.last() // Last item (current) is not clickable
            )
        }
    }
    
    fun navigateToScreen(
        currentState: NavigationState,
        targetScreen: NavigationScreen
    ): NavigationState {
        val newStack = pushToStack(currentState.navigationStack, targetScreen)
        val newBreadcrumbs = generateBreadcrumbs(newStack)
        
        return when (targetScreen) {
            is NavigationScreen.MainTab -> currentState.copy(
                currentTab = targetScreen.tab,
                navigationStack = listOf(targetScreen), // Reset stack for main tabs
                breadcrumbs = emptyList()
            )
            is NavigationScreen.MoreTabPage -> currentState.copy(
                currentTab = com.bytecoder.lurora.frontend.navigation.MainTab.MORE,
                currentMoreTab = targetScreen.moreTab,
                navigationStack = newStack,
                breadcrumbs = newBreadcrumbs
            )
            is NavigationScreen.DeepPage -> currentState.copy(
                navigationStack = newStack,
                breadcrumbs = newBreadcrumbs
            )
        }
    }
}