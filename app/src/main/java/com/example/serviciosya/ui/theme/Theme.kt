package com.example.serviciosya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = Color(0xFF5F2100),
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = BlueAccentDark,
    background = DarkBackground,
    onBackground = WarmWhite,
    surface = DarkSurface,
    onSurface = WarmWhite,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = Color(0xFF3B0B00),
    secondary = BlueAccent,
    background = WarmBackground,
    onBackground = Ink,
    surface = WarmSurface,
    onSurface = Ink,
    error = ErrorRed
)

@Composable
fun ServiciosYATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
