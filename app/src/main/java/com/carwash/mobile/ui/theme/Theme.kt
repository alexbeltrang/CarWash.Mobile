package com.carwash.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1256A3),
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF4F5F7A),
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF0C1B32),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474F),
    outline = Color(0xFF737780),
    background = Color(0xFFF9F9FF),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191B20),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF002E6A),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFBAC7E0),
    onSecondary = Color(0xFF243149),
    secondaryContainer = Color(0xFF3A4861),
    onSecondaryContainer = Color(0xFFD9E3F8),
    surfaceVariant = Color(0xFF43474F),
    onSurfaceVariant = Color(0xFFC3C6D0),
    outline = Color(0xFF8D919B),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
)

@Composable
fun CarWashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}