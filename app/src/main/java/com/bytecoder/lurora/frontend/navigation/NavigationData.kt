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
    MUSIC("Music", Icons.Default.MusicNote, "music"), 
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

enum class MoreSection(val title: String, val route: String) {
    HISTORY("History", "history"),
    DOWNLOADS("Downloads", "downloads"),
    PERMISSIONS("Permissions", "permissions"),
    FILE_EXPLORER("File Explorer", "file_explorer"),
    SETTINGS("Settings", "settings"),
    FEEDBACK("Feedback", "feedback"),
    ABOUT("About", "about"),
    TIPS("Tips", "tips")
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
    val currentMoreSection: MoreSection = MoreSection.HISTORY, // Renamed from currentOptionsSection
    
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
    data class MorePage(val section: MoreSection, val parentTab: com.bytecoder.lurora.frontend.navigation.MainTab = com.bytecoder.lurora.frontend.navigation.MainTab.MORE) : NavigationScreen(
        "${parentTab.route}/${section.route}", 
        section.title
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
            url == DOWNLOADS -> NavigationScreen.MorePage(MoreSection.DOWNLOADS)
            url == PERMISSIONS -> NavigationScreen.MorePage(MoreSection.PERMISSIONS)
            url == ABOUT -> NavigationScreen.MorePage(MoreSection.ABOUT)
            url == HISTORY -> NavigationScreen.MorePage(MoreSection.HISTORY)
            url == FILE_EXPLORER -> NavigationScreen.MorePage(MoreSection.FILE_EXPLORER)
            url == FEEDBACK -> NavigationScreen.MorePage(MoreSection.FEEDBACK)
            url == TIPS -> NavigationScreen.MorePage(MoreSection.TIPS)
            url.startsWith("$BASE_SCHEME://settings") -> NavigationScreen.MorePage(MoreSection.SETTINGS)
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
            MainTab.MORE -> MoreSection.values().map { it.title } // Updated from OPTIONS to MORE
        }
    }
    
    fun getDefaultSection(tab: MainTab): String {
        return when (tab) {
            MainTab.VIDEO -> VideoSection.LIBRARY.title
            MainTab.MUSIC -> MusicSection.LIBRARY.title
            MainTab.ONLINE -> OnlineSection.BROWSE.title
            MainTab.MORE -> MoreSection.HISTORY.title // Updated from OPTIONS to MORE
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
            is NavigationScreen.MorePage -> currentState.copy(
                currentTab = com.bytecoder.lurora.frontend.navigation.MainTab.MORE,
                currentMoreSection = targetScreen.section,
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