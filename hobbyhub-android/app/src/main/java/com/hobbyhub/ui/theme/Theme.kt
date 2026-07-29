package com.hobbyhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    secondary = SecondaryTurquoise,
    tertiary = TertiaryCoral,
    background = ObsidianBg,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = TextPrimary
)

@Composable
fun HobbyHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
