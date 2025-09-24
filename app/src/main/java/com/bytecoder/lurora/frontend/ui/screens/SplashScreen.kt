package com.bytecoder.lurora.frontend.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bytecoder.lurora.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var startFadeOut by remember { mutableStateOf(false) }
    
    // Logo scale animation
    val scaleAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "logo_scale"
    )
    
    // Logo rotation animation
    val rotationAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 360f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = LinearEasing
        ),
        label = "logo_rotation"
    )
    
    // Logo alpha animation
    val alphaAnimation = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "logo_alpha"
    )
    
    // Screen fade out animation
    val screenAlpha = animateFloatAsState(
        targetValue = if (startFadeOut) 0f else 1f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "screen_fade"
    )
    
    // Start animations when composable is first composed
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Wait for logo animation to complete
        startFadeOut = true
        delay(500) // Wait for fade out to complete
        onSplashFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated Logo
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Lurora Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(scaleAnimation.value)
                .alpha(alphaAnimation.value)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreen(
            onSplashFinished = { }
        )
    }
}