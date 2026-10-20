package com.victorious.cam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF00C2A8)
private val TealDark = Color(0xFF008F7A)
private val BgDark = Color(0xFF0D1117)
private val BgLight = Color(0xFFF6F8FA)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    secondary = TealDark,
    background = BgDark,
    surface = Color(0xFF161B22),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3)
)

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    secondary = Teal,
    background = BgLight,
    surface = Color.White,
    onBackground = Color(0xFF0D1117),
    onSurface = Color(0xFF0D1117)
)

@Composable
fun VictorCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
