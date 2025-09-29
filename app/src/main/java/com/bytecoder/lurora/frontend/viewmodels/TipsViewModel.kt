package com.bytecoder.lurora.frontend.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Tips screen with tutorial progress tracking and step navigation
 */
@HiltViewModel
class TipsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val context: Context = application
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("lurora_tips", Context.MODE_PRIVATE)
    
    // UI State
    private val _uiState = MutableStateFlow(TipsUiState())
    val uiState: StateFlow<TipsUiState> = _uiState.asStateFlow()
    
    init {
        loadTipsData()
        loadProgress()
    }
    
    /**
     * Load all tips and tutorials data
     */
    private fun loadTipsData() {
        viewModelScope.launch {
            val tipCategories = getTipCategories()
            val tutorials = getTutorials()
            
            val totalTips = tipCategories.sumOf { it.tips.size }
            val totalTutorials = tutorials.size
            
            _uiState.value = _uiState.value.copy(
                tipCategories = tipCategories,
                tutorials = tutorials,
                totalTips = totalTips,
                totalTutorials = totalTutorials,
                isLoading = false
            )
        }
    }
    
    /**
     * Load user progress from SharedPreferences
     */
    private fun loadProgress() {
        viewModelScope.launch {
            val completedTips = sharedPrefs.getStringSet("completed_tips", emptySet()) ?: emptySet()
            val completedTutorials = sharedPrefs.getStringSet("completed_tutorials", emptySet()) ?: emptySet()
            val achievements = sharedPrefs.getStringSet("achievements", emptySet())?.toList() ?: emptyList()
            
            val totalItems = _uiState.value.totalTips + _uiState.value.totalTutorials
            val completedItems = completedTips.size + completedTutorials.size
            val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f
            
            _uiState.value = _uiState.value.copy(
                completedTips = completedTips,
                completedTutorials = completedTutorials,
                achievements = achievements,
                overallProgress = progress
            )
        }
    }
    
    /**
     * Mark a tip as completed
     */
    fun markTipCompleted(tipId: String) {
        viewModelScope.launch {
            val currentCompleted = _uiState.value.completedTips.toMutableSet()
            currentCompleted.add(tipId)
            
            // Save to SharedPreferences
            sharedPrefs.edit()
                .putStringSet("completed_tips", currentCompleted)
                .apply()
            
            // Check for new achievements
            checkAchievements(currentCompleted, _uiState.value.completedTutorials)
            
            loadProgress()
        }
    }
    
    /**
     * Start a tutorial
     */
    fun startTutorial(tutorialId: String) {
        viewModelScope.launch {
            val tutorial = _uiState.value.tutorials.find { it.id == tutorialId }
            tutorial?.let {
                _uiState.value = _uiState.value.copy(
                    currentTutorial = it,
                    tutorialInProgress = true
                )
            }
        }
    }
    
    /**
     * Complete a tutorial
     */
    fun completeTutorial(tutorialId: String) {
        viewModelScope.launch {
            val currentCompleted = _uiState.value.completedTutorials.toMutableSet()
            currentCompleted.add(tutorialId)
            
            // Save to SharedPreferences
            sharedPrefs.edit()
                .putStringSet("completed_tutorials", currentCompleted)
                .apply()
            
            // Check for new achievements
            checkAchievements(_uiState.value.completedTips, currentCompleted)
            
            _uiState.value = _uiState.value.copy(
                currentTutorial = null,
                tutorialInProgress = false
            )
            
            loadProgress()
        }
    }
    
    /**
     * Reset all progress
     */
    fun resetProgress() {
        viewModelScope.launch {
            sharedPrefs.edit()
                .remove("completed_tips")
                .remove("completed_tutorials")
                .remove("achievements")
                .apply()
            
            _uiState.value = _uiState.value.copy(
                completedTips = emptySet(),
                completedTutorials = emptySet(),
                achievements = emptyList(),
                overallProgress = 0f,
                currentTutorial = null,
                tutorialInProgress = false
            )
        }
    }
    
    /**
     * Check for new achievements
     */
    private fun checkAchievements(completedTips: Set<String>, completedTutorials: Set<String>) {
        val currentAchievements = _uiState.value.achievements.toMutableList()
        
        // First tip achievement
        if (completedTips.size == 1 && !currentAchievements.contains("First Steps")) {
            currentAchievements.add("First Steps - Completed your first tip!")
        }
        
        // First tutorial achievement
        if (completedTutorials.size == 1 && !currentAchievements.contains("Learning Path")) {
            currentAchievements.add("Learning Path - Completed your first tutorial!")
        }
        
        // Completion milestones
        val totalTips = _uiState.value.totalTips
        if (completedTips.size >= totalTips / 2 && !currentAchievements.contains("Tip Master")) {
            currentAchievements.add("Tip Master - Completed half of all tips!")
        }
        
        if (completedTips.size == totalTips && !currentAchievements.contains("Tip Champion")) {
            currentAchievements.add("Tip Champion - Completed all tips!")
        }
        
        if (completedTutorials.size == _uiState.value.totalTutorials && !currentAchievements.contains("Tutorial Expert")) {
            currentAchievements.add("Tutorial Expert - Completed all tutorials!")
        }
        
        // Perfect score achievement
        if (completedTips.size == totalTips && completedTutorials.size == _uiState.value.totalTutorials) {
            if (!currentAchievements.contains("Lurora Master")) {
                currentAchievements.add("Lurora Master - Achieved 100% completion!")
            }
        }
        
        // Save achievements
        sharedPrefs.edit()
            .putStringSet("achievements", currentAchievements.toSet())
            .apply()
    }
    
    /**
     * Get tip categories data
     */
    private fun getTipCategories(): List<TipCategory> {
        return listOf(
            TipCategory(
                id = "playback",
                title = "Playback Controls",
                icon = Icons.Default.PlayArrow,
                tips = listOf(
                    TipItem(
                        id = "gesture_controls",
                        title = "Gesture Controls",
                        description = "Swipe up/down to adjust volume, left/right to seek",
                        difficulty = "Easy"
                    ),
                    TipItem(
                        id = "speed_control",
                        title = "Playback Speed",
                        description = "Long press the play button to adjust playback speed",
                        difficulty = "Easy"
                    ),
                    TipItem(
                        id = "repeat_modes",
                        title = "Repeat Modes",
                        description = "Cycle through repeat off, one, and all modes",
                        difficulty = "Easy"
                    )
                )
            ),
            TipCategory(
                id = "interface",
                title = "Interface & Navigation",
                icon = Icons.Default.TouchApp,
                tips = listOf(
                    TipItem(
                        id = "quick_search",
                        title = "Quick Search",
                        description = "Use the search bar to quickly find any media file",
                        difficulty = "Easy"
                    ),
                    TipItem(
                        id = "tabs_navigation",
                        title = "Tab Navigation",
                        description = "Swipe between tabs or use the bottom navigation",
                        difficulty = "Easy"
                    ),
                    TipItem(
                        id = "dark_mode",
                        title = "Dark Mode Toggle",
                        description = "Toggle dark mode from settings or quick settings",
                        difficulty = "Easy"
                    )
                )
            ),
            TipCategory(
                id = "advanced",
                title = "Advanced Features",
                icon = Icons.Default.Settings,
                tips = listOf(
                    TipItem(
                        id = "mini_player",
                        title = "Mini Player",
                        description = "Enable picture-in-picture mode for background playback",
                        difficulty = "Medium"
                    ),
                    TipItem(
                        id = "equalizer",
                        title = "Audio Equalizer",
                        description = "Customize audio settings with the built-in equalizer",
                        difficulty = "Medium"
                    ),
                    TipItem(
                        id = "custom_presets",
                        title = "Custom Presets",
                        description = "Create and save your own equalizer presets",
                        difficulty = "Hard"
                    )
                )
            ),
            TipCategory(
                id = "organization",
                title = "File Organization",
                icon = Icons.Default.FolderOpen,
                tips = listOf(
                    TipItem(
                        id = "playlists",
                        title = "Create Playlists",
                        description = "Organize your media into custom playlists",
                        difficulty = "Easy"
                    ),
                    TipItem(
                        id = "favorites",
                        title = "Mark Favorites",
                        description = "Star your favorite media for quick access",
                        difficulty = "Easy"
                    ),
                    TipItem(
                        id = "file_management",
                        title = "File Management",
                        description = "Use the built-in file manager to organize media",
                        difficulty = "Medium"
                    )
                )
            )
        )
    }
    
    /**
     * Get tutorials data
     */
    private fun getTutorials(): List<TutorialStep> {
        return listOf(
            TutorialStep(
                id = "getting_started",
                title = "Getting Started with Lurora",
                description = "Learn the basics of navigating and using Lurora",
                estimatedTime = 5,
                difficulty = "Beginner",
                prerequisites = emptyList()
            ),
            TutorialStep(
                id = "media_playback",
                title = "Mastering Media Playback",
                description = "Discover all playback controls and gestures",
                estimatedTime = 10,
                difficulty = "Beginner",
                prerequisites = listOf("getting_started")
            ),
            TutorialStep(
                id = "file_management",
                title = "File Management & Organization",
                description = "Learn to organize and manage your media library",
                estimatedTime = 15,
                difficulty = "Intermediate",
                prerequisites = listOf("getting_started")
            ),
            TutorialStep(
                id = "advanced_features",
                title = "Advanced Features & Customization",
                description = "Explore equalizer, mini-player, and advanced settings",
                estimatedTime = 20,
                difficulty = "Advanced",
                prerequisites = listOf("media_playback", "file_management")
            ),
            TutorialStep(
                id = "tips_and_tricks",
                title = "Pro Tips & Hidden Features",
                description = "Discover power-user features and shortcuts",
                estimatedTime = 12,
                difficulty = "Advanced",
                prerequisites = listOf("advanced_features")
            )
        )
    }
}

/**
 * UI state for the tips screen
 */
data class TipsUiState(
    val isLoading: Boolean = true,
    val tipCategories: List<TipCategory> = emptyList(),
    val tutorials: List<TutorialStep> = emptyList(),
    val completedTips: Set<String> = emptySet(),
    val completedTutorials: Set<String> = emptySet(),
    val totalTips: Int = 0,
    val totalTutorials: Int = 0,
    val overallProgress: Float = 0f,
    val achievements: List<String> = emptyList(),
    val currentTutorial: TutorialStep? = null,
    val tutorialInProgress: Boolean = false
)

/**
 * Category of tips
 */
data class TipCategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val tips: List<TipItem>
)

/**
 * Individual tip item
 */
data class TipItem(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String
)

/**
 * Tutorial step with navigation and prerequisites
 */
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val estimatedTime: Int, // in minutes
    val difficulty: String,
    val prerequisites: List<String>
)