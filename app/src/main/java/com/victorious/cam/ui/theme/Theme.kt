package com.victorious.cam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.victorious.cam.fleettheme.FleetPalette
import com.victorious.cam.fleettheme.FleetThemeState
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

/**
 * Overlays fleet palette.* per FLEET_THEME v1 "Palette keys -> Material3 roles".
 * Per-key fallback: a missing/invalid key keeps VictorCam's default for its roles only.
 * `stale` has no Material3 role and is intentionally not mapped.
 */
fun ColorScheme.withFleet(p: FleetPalette?): ColorScheme {
    if (p == null) return this
    fun c(k: String, d: Color) = p[k]?.let { Color(it.toInt()) } ?: d
    return copy(
        background = c("background", background),
        onPrimary = c("background", onPrimary),
        onSecondary = c("background", onSecondary),
        onError = c("background", onError),
        surface = c("surface", surface),
        primary = c("primary", primary),
        secondary = c("secondary", secondary),
        onBackground = c("onBackground", onBackground),
        onSurface = c("onBackground", onSurface),
        onSurfaceVariant = c("muted", onSurfaceVariant),
        outline = c("muted", outline),
        error = c("danger", error),
        surfaceVariant = c("tile", surfaceVariant)
    )
}

@Composable
fun VictorCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val fleet by FleetThemeState.palette.collectAsState()
    val base = when (fleet?.mode) {
        "LIGHT" -> LightColors
        null -> if (darkTheme) DarkColors else LightColors
        else -> DarkColors
    }
    MaterialTheme(colorScheme = base.withFleet(fleet), content = content)
}
