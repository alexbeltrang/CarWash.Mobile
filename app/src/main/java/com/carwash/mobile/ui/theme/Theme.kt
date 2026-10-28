package com.carwash.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B54),
    primaryContainer = Color(0xFFB3EFDA),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF765A00),
    secondaryContainer = Color(0xFFF9E09C),
    onSecondaryContainer = Color(0xFF241A00),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    background = Color(0xFFF6FAF8),
    surface = Color(0xFFF6FAF8),
    onSurface = Color(0xFF18201D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF66DBB7),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF00513F),
    onPrimaryContainer = Color(0xFF7FF8D0),
    secondary = Color(0xFFEAC35A),
    onSecondary = Color(0xFF3D2F00),
    secondaryContainer = Color(0xFF584400),
    onSecondaryContainer = Color(0xFFFFDF95),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938C),
    background = Color(0xFF101513),
    surface = Color(0xFF101513),
    onSurface = Color(0xFFDEE4DF),
)

@Composable
fun CarWashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}