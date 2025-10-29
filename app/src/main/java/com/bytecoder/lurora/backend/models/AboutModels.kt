package com.bytecoder.lurora.backend.models

import androidx.compose.runtime.Immutable

/**
 * Data models for About page information
 */

/**
 * App information data
 */
@Immutable
data class AppInfo(
    val appName: String = "vPlay",
    val appVersion: String = "1.0.0",
    val buildNumber: String = "100",
    val lastUpdateDate: String = "October 2025",
    val packageName: String = "com.bytecoder.lurora"
)

/**
 * Developer information
 */
@Immutable
data class DeveloperInfo(
    val name: String = "ByteCoder",
    val website: String = "https://bytecoder.dev",
    val email: String = "support@bytecoder.dev",
    val portfolioUrl: String = "https://github.com/bytecoder"
)

/**
 * Legal information links
 */
@Immutable
data class LegalInfo(
    val privacyPolicyUrl: String = "https://bytecoder.dev/privacy",
    val termsOfServiceUrl: String = "https://bytecoder.dev/terms",
    val openSourceLicensesUrl: String = "https://bytecoder.dev/licenses"
)

/**
 * Technical details about the app
 */
@Immutable
data class TechnicalInfo(
    val minAndroidVersion: String = "Android 7.0 (API 24)",
    val targetAndroidVersion: String = "Android 14 (API 34)",
    val appSizeMB: String = "25",
    val keyPermissions: List<String> = listOf(
        "Storage Access",
        "Media Playback",
        "Network Access",
        "Notification Access"
    )
)

/**
 * Third-party library information
 */
@Immutable
data class LibraryCredit(
    val name: String,
    val description: String,
    val license: String,
    val url: String
)

/**
 * Changelog entry
 */
@Immutable
data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>,
    val type: ChangelogType = ChangelogType.MINOR
)

enum class ChangelogType {
    MAJOR, MINOR, PATCH, HOTFIX
}

/**
 * Complete about page data
 */
@Immutable
data class AboutData(
    val appInfo: AppInfo = AppInfo(),
    val developerInfo: DeveloperInfo = DeveloperInfo(),
    val legalInfo: LegalInfo = LegalInfo(),
    val technicalInfo: TechnicalInfo = TechnicalInfo(),
    val libraries: List<LibraryCredit> = emptyList(),
    val changelog: List<ChangelogEntry> = emptyList()
)