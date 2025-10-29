package com.bytecoder.lurora.frontend.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecoder.lurora.backend.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Comprehensive ViewModel for About screen with complete app information
 */
@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _aboutData = MutableStateFlow(AboutData())
    val aboutData: StateFlow<AboutData> = _aboutData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAboutData()
    }

    private fun loadAboutData() {
        viewModelScope.launch {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val installTime = packageInfo.firstInstallTime
                val updateTime = packageInfo.lastUpdateTime
                
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val lastUpdateDate = dateFormat.format(Date(updateTime))

                val appInfo = AppInfo(
                    appName = "vPlay",
                    appVersion = packageInfo.versionName ?: "1.0.0",
                    buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    },
                    lastUpdateDate = lastUpdateDate,
                    packageName = context.packageName
                )

                val libraries = getThirdPartyLibraries()
                val changelog = getChangelogEntries()
                val technicalInfo = getTechnicalInfo()

                _aboutData.value = AboutData(
                    appInfo = appInfo,
                    libraries = libraries,
                    changelog = changelog,
                    technicalInfo = technicalInfo
                )
            } catch (e: Exception) {
                // Use default data if unable to fetch package info
                _aboutData.value = AboutData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getThirdPartyLibraries(): List<LibraryCredit> {
        return listOf(
            LibraryCredit(
                name = "Jetpack Compose",
                description = "Modern UI toolkit for Android",
                license = "Apache 2.0",
                url = "https://developer.android.com/jetpack/compose"
            ),
            LibraryCredit(
                name = "Hilt",
                description = "Dependency injection for Android",
                license = "Apache 2.0",
                url = "https://dagger.dev/hilt/"
            ),
            LibraryCredit(
                name = "Room",
                description = "Local database abstraction layer",
                license = "Apache 2.0",
                url = "https://developer.android.com/training/data-storage/room"
            ),
            LibraryCredit(
                name = "OkHttp",
                description = "HTTP client for networking",
                license = "Apache 2.0",
                url = "https://square.github.io/okhttp/"
            ),
            LibraryCredit(
                name = "Coil",
                description = "Image loading library",
                license = "Apache 2.0",
                url = "https://coil-kt.github.io/coil/"
            ),
            LibraryCredit(
                name = "ExoPlayer",
                description = "Media player for Android",
                license = "Apache 2.0",
                url = "https://exoplayer.dev/"
            ),
            LibraryCredit(
                name = "Material3",
                description = "Material Design components",
                license = "Apache 2.0",
                url = "https://m3.material.io/"
            )
        )
    }

    private fun getChangelogEntries(): List<ChangelogEntry> {
        return listOf(
            ChangelogEntry(
                version = "1.0.0",
                date = "October 2025",
                changes = listOf(
                    "🎉 Initial release of vPlay",
                    "🎵 Music and video playback support",
                    "📱 Modern Material 3 design",
                    "🔍 Advanced file explorer",
                    "💬 Comprehensive feedback system",
                    "🛠️ Permission management",
                    "📊 Download manager"
                ),
                type = ChangelogType.MAJOR
            ),
            ChangelogEntry(
                version = "0.9.0",
                date = "September 2025",
                changes = listOf(
                    "🎨 Updated UI with Material 3",
                    "🔧 Performance improvements",
                    "🐛 Bug fixes and stability"
                ),
                type = ChangelogType.MINOR
            ),
            ChangelogEntry(
                version = "0.8.0",
                date = "August 2025",
                changes = listOf(
                    "📁 Enhanced file management",
                    "🎮 Improved media controls",
                    "🔒 Better permission handling"
                ),
                type = ChangelogType.MINOR
            )
        )
    }

    private fun getTechnicalInfo(): TechnicalInfo {
        return TechnicalInfo(
            minAndroidVersion = "Android 7.0 (API 24)",
            targetAndroidVersion = "Android 14 (API 34)",
            appSizeMB = "25",
            keyPermissions = listOf(
                "Read External Storage",
                "Write External Storage", 
                "Internet Access",
                "Network State Access",
                "Wake Lock",
                "Foreground Service"
            )
        )
    }

    /**
     * Open external links safely
     */
    fun openUrl(context: Context, url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - could show a toast or log
        }
    }

    /**
     * Send email to developer
     */
    fun sendEmail(context: Context, email: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:$email")
                putExtra(android.content.Intent.EXTRA_SUBJECT, "vPlay App Feedback")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - fallback to copying email or showing message
        }
    }
}