package com.bytecoder.lurora.backend.utils

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accessibility helper functions and utilities
 */
@Singleton
class AccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    /**
     * Check if screen reader is enabled
     */
    fun isScreenReaderEnabled(): Boolean {
        return accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Check if accessibility services are enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        return accessibilityManager.isEnabled
    }

    /**
     * Get recommended touch target size based on accessibility settings
     */
    fun getRecommendedTouchTargetSize(): Dp {
        return if (isAccessibilityEnabled()) 48.dp else 44.dp
    }

    /**
     * Get recommended spacing for accessibility
     */
    fun getRecommendedSpacing(): Dp {
        return if (isAccessibilityEnabled()) 16.dp else 12.dp
    }

    /**
     * Announce message to screen reader
     */
    fun announceForAccessibility(message: String) {
        if (isScreenReaderEnabled()) {
            accessibilityManager.interrupt()
        }
    }
}

/**
 * Composable functions for accessibility support
 */
@Composable
fun rememberAccessibilityState(): AccessibilityState {
    val context = LocalContext.current
    val accessibilityManager = remember { 
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager 
    }
    
    return remember {
        AccessibilityState(
            isScreenReaderEnabled = accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled,
            isAccessibilityEnabled = accessibilityManager.isEnabled
        )
    }
}

/**
 * Accessibility state data class
 */
data class AccessibilityState(
    val isScreenReaderEnabled: Boolean,
    val isAccessibilityEnabled: Boolean
) {
    val recommendedTouchTargetSize: Dp = if (isAccessibilityEnabled) 48.dp else 44.dp
    val recommendedSpacing: Dp = if (isAccessibilityEnabled) 16.dp else 12.dp
}

/**
 * Enhanced semantics for media items
 */
fun Modifier.mediaItemSemantics(
    title: String,
    artist: String? = null,
    duration: String? = null,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    onPlay: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null
): Modifier {
    return this.semantics {
        contentDescription = buildString {
            append(title)
            artist?.let { append(", by $it") }
            duration?.let { append(", duration $it") }
            if (isPlaying) append(", currently playing")
            if (isSelected) append(", selected")
        }
        
        if (onPlay != null) {
            customActions = listOf(
                CustomAccessibilityAction(
                    label = if (isPlaying) "Pause" else "Play",
                    action = { onPlay(); true }
                )
            )
        }
        
        if (onSelect != null) {
            onClick {
                onSelect()
                true
            }
        }
        
        role = Role.Button
        
        if (isSelected) {
            selected = true
        }
    }
}

/**
 * Semantics for navigation elements
 */
fun Modifier.navigationSemantics(
    label: String,
    isSelected: Boolean = false,
    hasNotification: Boolean = false
): Modifier {
    return this.semantics {
        contentDescription = buildString {
            append(label)
            if (isSelected) append(", selected")
            if (hasNotification) append(", has notifications")
        }
        role = Role.Tab
        if (isSelected) selected = true
    }
}

/**
 * Semantics for player controls
 */
fun Modifier.playerControlSemantics(
    action: String,
    state: String? = null,
    isEnabled: Boolean = true
): Modifier {
    return this.semantics {
        contentDescription = buildString {
            append(action)
            state?.let { append(", $it") }
            if (!isEnabled) append(", disabled")
        }
        role = Role.Button
        if (!isEnabled) {
            disabled()
        }
    }
}

/**
 * Semantics for progress indicators
 */
fun Modifier.progressSemantics(
    progress: Float,
    description: String = "Progress"
): Modifier {
    return this.semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(
            current = progress,
            range = 0f..1f
        )
        contentDescription = "$description: ${(progress * 100).toInt()}%"
    }
}

/**
 * Accessible spacers that announce content separation
 */
@Composable
fun AccessibleSpacer(
    height: Dp = 0.dp,
    width: Dp = 0.dp,
    contentDescription: String? = null
) {
    val accessibilityState = rememberAccessibilityState()
    
    Spacer(
        modifier = Modifier
            .height(if (height > 0.dp) height else accessibilityState.recommendedSpacing)
            .width(if (width > 0.dp) width else 0.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { 
                        this.contentDescription = contentDescription
                        invisibleToUser()
                    }
                } else Modifier
            )
    )
}

/**
 * High contrast theme detection
 */
@Composable
fun isHighContrastMode(): Boolean {
    // This would typically check system accessibility settings
    // For now, we'll use a simple implementation
    return false
}

/**
 * Large font scale detection
 */
@Composable
fun isLargeFontScale(): Boolean {
    val density = LocalDensity.current
    return density.fontScale > 1.3f
}

/**
 * Accessible card with proper semantics
 */
fun Modifier.accessibleCard(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = buildString {
            append(title)
            subtitle?.let { append(", $it") }
            description?.let { append(", $it") }
            if (isSelected) append(", selected")
        }
        
        onClick?.let { action ->
            this.onClick {
                action()
                true
            }
        }
        
        role = Role.Button
        if (isSelected) selected = true
    }
}

/**
 * Keyboard navigation helpers
 */
object KeyboardNavigation {
    const val TAB_KEY = "Tab"
    const val ENTER_KEY = "Enter"
    const val SPACE_KEY = "Space"
    const val ARROW_UP_KEY = "ArrowUp"
    const val ARROW_DOWN_KEY = "ArrowDown"
    const val ARROW_LEFT_KEY = "ArrowLeft"
    const val ARROW_RIGHT_KEY = "ArrowRight"
    const val ESCAPE_KEY = "Escape"
}

/**
 * Media key constants for keyboard support
 */
object MediaKeys {
    const val PLAY_PAUSE = "MediaPlayPause"
    const val NEXT_TRACK = "MediaTrackNext"
    const val PREVIOUS_TRACK = "MediaTrackPrevious"
    const val STOP = "MediaStop"
    const val VOLUME_UP = "VolumeUp"
    const val VOLUME_DOWN = "VolumeDown"
    const val MUTE = "VolumeMute"
}