package com.example.hotspotmonitor.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Background,
    primaryContainer = CyanGlow,
    onPrimaryContainer = Cyan,
    secondary = GreenOnline,
    onSecondary = Background,
    secondaryContainer = GreenGlow,
    onSecondaryContainer = GreenOnline,
    tertiary = Amber,
    onTertiary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = RedAlert,
    onError = Background,
)

@Composable
fun HotspotMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
