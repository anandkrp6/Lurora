package com.bytecoder.lurora.frontend.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for About screen with app information and external link handling
 */
@RequiresApi(Build.VERSION_CODES.P)
@HiltViewModel
class AboutViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val context: Context = application
    
    // UI State
    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()
    
    init {
        loadAppInformation()
    }
    
    /**
     * Load all app information
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun loadAppInformation() {
        viewModelScope.launch {
            val appInfo = getAppInfo()
            val teamMembers = getTeamMembers()
            val statistics = getAppStatistics()
            
            _uiState.value = _uiState.value.copy(
                appVersion = appInfo.version,
                appInfo = appInfo,
                teamMembers = teamMembers,
                appStatistics = statistics,
                isLoading = false
            )
        }
    }
    
    /**
     * Get comprehensive app information
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun getAppInfo(): AppInfo {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            
            AppInfo(
                version = packageInfo.versionName ?: "Unknown",
                buildNumber = "${packageInfo.longVersionCode}",
                releaseDate = "December 2024",
                minSdkVersion = applicationInfo.minSdkVersion,
                targetSdkVersion = applicationInfo.targetSdkVersion,
                architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                packageName = context.packageName,
                installedDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(packageInfo.firstInstallTime)),
                lastUpdated = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(packageInfo.lastUpdateTime))
            )
        } catch (e: Exception) {
            AppInfo(
                version = "1.0.0",
                buildNumber = "1",
                releaseDate = "December 2024",
                minSdkVersion = 21,
                targetSdkVersion = 34,
                architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                packageName = "com.bytecoder.lurora",
                installedDate = "Unknown",
                lastUpdated = "Unknown"
            )
        }
    }
    
    /**
     * Get team member information
     */
    private fun getTeamMembers(): List<TeamMember> {
        return listOf(
            TeamMember(
                name = "Alex Chen",
                role = "Lead Developer",
                bio = "Passionate about creating seamless user experiences",
                profileUrl = "https://github.com/alexchen",
                avatar = null
            ),
            TeamMember(
                name = "Sarah Johnson",
                role = "UI/UX Designer",
                bio = "Crafting beautiful and intuitive interfaces",
                profileUrl = "https://dribbble.com/sarahjohnson",
                avatar = null
            ),
            TeamMember(
                name = "Michael Rodriguez",
                role = "Backend Engineer",
                bio = "Building robust and scalable systems",
                profileUrl = "https://github.com/mrodriguez",
                avatar = null
            ),
            TeamMember(
                name = "Emma Wilson",
                role = "Quality Assurance",
                bio = "Ensuring the highest quality standards",
                profileUrl = "https://linkedin.com/in/emmawilson",
                avatar = null
            )
        )
    }
    
    /**
     * Get app usage statistics
     */
    private fun getAppStatistics(): AppStatistics {
        return AppStatistics(
            totalDownloads = "50K+",
            userRating = "4.8",
            activeUsers = "10K+"
        )
    }
    
    /**
     * Share the app with others
     */
    fun shareApp() {
        viewModelScope.launch {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Check out Lurora!")
                    putExtra(Intent.EXTRA_TEXT, 
                        "I'm using Lurora - the ultimate media player! Download it now: https://play.google.com/store/apps/details?id=${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                val chooserIntent = Intent.createChooser(shareIntent, "Share Lurora")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
                
            } catch (e: Exception) {
                // Handle share error
            }
        }
    }
    
    /**
     * Open team member profile
     */
    fun openMemberProfile(profileUrl: String) {
        openUrl(profileUrl)
    }
    
    /**
     * Rate the app
     */
    fun rateApp() {
        openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
    }
    
    /**
     * Report a bug
     */
    fun reportBug() {
        openUrl("https://github.com/lurora/lurora/issues/new?template=bug_report.md")
    }
    
    /**
     * Request a feature
     */
    fun requestFeature() {
        openUrl("https://github.com/lurora/lurora/issues/new?template=feature_request.md")
    }
    
    /**
     * Join community
     */
    fun joinCommunity() {
        openUrl("https://discord.gg/lurora")
    }
    
    /**
     * Check for app updates
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(checkingUpdates = true)
            
            // Simulate update check
            kotlinx.coroutines.delay(2000)
            
            _uiState.value = _uiState.value.copy(
                checkingUpdates = false,
                updateMessage = "You're using the latest version!"
            )
        }
    }
    
    /**
     * Open privacy policy
     */
    fun openPrivacyPolicy() {
        openUrl("https://lurora.app/privacy")
    }
    
    /**
     * Open terms of service
     */
    fun openTermsOfService() {
        openUrl("https://lurora.app/terms")
    }
    
    /**
     * Open licenses
     */
    fun openLicenses() {
        openUrl("https://lurora.app/licenses")
    }
    
    /**
     * Open source code
     */
    fun openSourceCode() {
        openUrl("https://github.com/lurora/lurora")
    }
    
    /**
     * Open GitHub
     */
    fun openGitHub() {
        openUrl("https://github.com/lurora")
    }
    
    /**
     * Open Twitter
     */
    fun openTwitter() {
        openUrl("https://twitter.com/lurora")
    }
    
    /**
     * Open Discord
     */
    fun openDiscord() {
        openUrl("https://discord.gg/lurora")
    }
    
    /**
     * Open website
     */
    fun openWebsite() {
        openUrl("https://lurora.app")
    }
    
    /**
     * Clear update message
     */
    fun clearUpdateMessage() {
        _uiState.value = _uiState.value.copy(updateMessage = null)
    }
    
    /**
     * Generic method to open URLs
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle URL opening error
            // In production, you might want to show a toast or fallback
        }
    }
}

/**
 * UI state for the About screen
 */
data class AboutUiState(
    val isLoading: Boolean = true,
    val appVersion: String = "1.0.0",
    val appInfo: AppInfo = AppInfo(),
    val teamMembers: List<TeamMember> = emptyList(),
    val appStatistics: AppStatistics = AppStatistics(),
    val checkingUpdates: Boolean = false,
    val updateMessage: String? = null
)

/**
 * Comprehensive app information
 */
data class AppInfo(
    val version: String = "1.0.0",
    val buildNumber: String = "1",
    val releaseDate: String = "December 2024",
    val minSdkVersion: Int = 21,
    val targetSdkVersion: Int = 34,
    val architecture: String = "arm64-v8a",
    val packageName: String = "com.bytecoder.lurora",
    val installedDate: String = "Unknown",
    val lastUpdated: String = "Unknown"
)

/**
 * Team member information
 */
data class TeamMember(
    val name: String,
    val role: String,
    val bio: String,
    val profileUrl: String,
    val avatar: String? = null
)

/**
 * App usage statistics
 */
data class AppStatistics(
    val totalDownloads: String = "0",
    val userRating: String = "5.0",
    val activeUsers: String = "0"
)