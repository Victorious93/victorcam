package com.victorious.cam.fleettheme

import org.junit.Assert.*
import org.junit.Test

class FleetThemeTest {
    private val pin = "ab".repeat(32)
    private fun row(schema: String? = "1") = mapOf(
        "schemaVersion" to schema, "mode" to "NEON_FLEET",
        "background" to "#FF0B0C12", "primary" to "#00E5FF", "tile" to "#CC1A1D2B",
        "danger" to "bogus", "futureColumn" to "x"
    )

    @Test fun parsesPaletteHex() {
        val p = FleetThemeCodec.decodeRow(row())!!
        assertEquals(0xFF0B0C12L, p["background"])
        assertEquals(0xFF00E5FFL, p["primary"])
        assertEquals(0xCC1A1D2BL, p["tile"])
        assertNull(p["danger"])
        assertEquals("NEON_FLEET", p.mode)
    }
    @Test fun hexEdgeCases() {
        assertNull(FleetThemeCodec.parseHex("00E5FF"))
        assertNull(FleetThemeCodec.parseHex("#12345"))
        assertNull(FleetThemeCodec.parseHex(null))
        assertEquals(0xFFFFFFFFL, FleetThemeCodec.parseHex(" #ffffff "))
    }
    @Test fun rejectsSchemaNot1() {
        assertNull(FleetThemeCodec.decodeRow(row("2")))
        assertNull(FleetThemeCodec.decodeRow(row("0")))
        assertNull(FleetThemeCodec.decodeRow(row(null)))
        assertNull(FleetThemeCodec.decodeRow(row("abc")))
    }
    @Test fun noValidColorsMeansDefaults() {
        assertNull(FleetThemeCodec.decodeRow(mapOf("schemaVersion" to "1", "primary" to "nope")))
    }
    @Test fun securityExceptionFallsBack() {
        assertNull(FleetThemeLoader.load({ true }) { throw SecurityException("not pinned") })
    }
    @Test fun otherExceptionFallsBack() {
        assertNull(FleetThemeLoader.load({ throw IllegalStateException() }) { row() })
    }
    @Test fun untrustedNeverQueries() {
        var queried = false
        assertNull(FleetThemeLoader.load({ false }) { queried = true; row() })
        assertFalse(queried)
    }
    @Test fun trustedLoads() {
        assertNotNull(FleetThemeLoader.load({ true }) { row() })
    }
    @Test fun pinAcceptsMatchingPackageAndCert() {
        val v = ProviderPinVerifier(pinnedSha256 = setOf(pin)) { listOf(pin.uppercase().chunked(2).joinToString(":")) }
        assertTrue(v.isTrusted("com.victorious.launch"))
    }
    @Test fun pinRejectsWrongPackage() {
        val v = ProviderPinVerifier(pinnedSha256 = setOf(pin)) { listOf(pin) }
        assertFalse(v.isTrusted("com.evil.launch"))
        assertFalse(v.isTrusted(null))
    }
    @Test fun pinRejectsWrongCert() {
        val v = ProviderPinVerifier(pinnedSha256 = setOf(pin)) { listOf("cd".repeat(32)) }
        assertFalse(v.isTrusted("com.victorious.launch"))
    }
    @Test fun pinRejectsMissingPackageInfo() {
        assertFalse(ProviderPinVerifier(pinnedSha256 = setOf(pin)) { null }.isTrusted("com.victorious.launch"))
        assertFalse(ProviderPinVerifier(pinnedSha256 = setOf(pin)) { throw RuntimeException() }.isTrusted("com.victorious.launch"))
    }
    @Test fun placeholderPinFailsClosed() {
        val v = ProviderPinVerifier { listOf(pin) }
        assertFalse(v.isTrusted("com.victorious.launch"))
    }
}
