package com.example.parkingtimerapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF091626), // Dark blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A2B44),
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color(0xFF091626),
    secondaryContainer = Color(0xFF091626),
    onSecondaryContainer = Color.White,
    background = Color(0xFF1b1a1f), // Updated to match the dark theme background color from Color.kt
    onBackground = Color.White,
    surface = Color(0xFF1b1a1f),
    onSurface = Color.White,
    error = Color.White,
    errorContainer = Color(0xFFB00020),
    onError = Color.White,
    onErrorContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF091626), // Dark blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF091626),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF091626),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EFF7),
    onSecondaryContainer = Color(0xFF091626),
    background = Color.White,
    onBackground = Color(0xFF091626),
    surface = Color.White,
    onSurface = Color(0xFF091626),
    error = Color.White,
    errorContainer = Color(0xFFB00020),
    onError = Color.White,
    onErrorContainer = Color.White
)

@Composable
fun ParkingTimerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}