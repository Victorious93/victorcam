package com.victorious.cam.fleettheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.victorious.cam.ui.theme.withFleet
import org.junit.Assert.assertEquals
import org.junit.Test

class FleetColorMappingTest {
    private val base = darkColorScheme()

    @Test fun mapsPerSpecTable() {
        val p = FleetThemeCodec.decodeRow(mapOf(
            "schemaVersion" to "1", "background" to "#FF0B0C12", "surface" to "#FF161821",
            "primary" to "#FF00E5FF", "secondary" to "#FFE840FF", "onBackground" to "#FFF5F7FF",
            "muted" to "#FF9AA3B5", "danger" to "#FFFF6B8A", "stale" to "#FFFFC14D", "tile" to "#CC1A1D2B"
        ))
        val s = base.withFleet(p)
        val bg = Color(0xFF0B0C12)
        assertEquals(bg, s.background); assertEquals(bg, s.onPrimary)
        assertEquals(bg, s.onSecondary); assertEquals(bg, s.onError)
        assertEquals(Color(0xFF161821), s.surface)
        assertEquals(Color(0xFF00E5FF), s.primary)
        assertEquals(Color(0xFFE840FF), s.secondary)
        assertEquals(Color(0xFFF5F7FF), s.onBackground); assertEquals(Color(0xFFF5F7FF), s.onSurface)
        assertEquals(Color(0xFF9AA3B5), s.onSurfaceVariant); assertEquals(Color(0xFF9AA3B5), s.outline)
        assertEquals(Color(0xFFFF6B8A), s.error)
        assertEquals(Color(0xCC1A1D2B), s.surfaceVariant)
        assertEquals(base.tertiary, s.tertiary) // unmapped roles keep defaults
    }

    @Test fun badKeyKeepsOnlyThatRoleDefault() {
        val s = base.withFleet(FleetThemeCodec.decodeRow(mapOf(
            "schemaVersion" to "1", "primary" to "garbage", "secondary" to "#FFE840FF")))
        assertEquals(base.primary, s.primary)
        assertEquals(Color(0xFFE840FF), s.secondary)
        assertEquals(base.background, s.background)
    }
}
