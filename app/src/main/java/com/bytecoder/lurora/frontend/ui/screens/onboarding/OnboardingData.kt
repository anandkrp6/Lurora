package com.bytecoder.lurora.frontend.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.bytecoder.lurora.R

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    @DrawableRes val imageRes: Int? = null,
    val backgroundColor: androidx.compose.ui.graphics.Color? = null
)

object OnboardingData {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Lurora",
            description = "Your ultimate media companion for music and videos. Experience entertainment like never before.",
            icon = Icons.Default.MusicNote
        ),
        OnboardingPage(
            title = "Access Your Media",
            description = "We need permission to access your media files to provide you with the best experience. Your privacy is our priority.",
            icon = Icons.Default.Folder
        ),
        OnboardingPage(
            title = "Amazing Features",
            description = "Enjoy high-quality video & audio playback, create playlists, and experience our modern Material 3 interface.",
            icon = Icons.Default.Star
        ),
        OnboardingPage(
            title = "Superior Audio",
            description = "Experience high-quality audio playback with background support, notifications, and seamless Bluetooth connectivity.",
            icon = Icons.Default.Headphones
        ),
        OnboardingPage(
            title = "Make It Yours",
            description = "Personalize your experience with theme selection and custom preferences. Lurora adapts to your style.",
            icon = Icons.Default.Palette
        ),
        OnboardingPage(
            title = "Ready to Start",
            description = "You're all set! Let's begin your amazing media journey with Lurora.",
            icon = Icons.Default.PlayArrow
        )
    )
}