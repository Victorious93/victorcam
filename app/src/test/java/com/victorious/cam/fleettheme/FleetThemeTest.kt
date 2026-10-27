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
    @Test fun perKeyFallback() {
        val p = FleetThemeCodec.decodeRow(mapOf("schemaVersion" to "1", "primary" to "nope", "surface" to "", "secondary" to "#FFE840FF"))!!
        assertNull(p["primary"]); assertNull(p["surface"]); assertNull(p["background"])
        assertEquals(0xFFE840FFL, p["secondary"])
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
        val v = ProviderPinVerifier(current = setOf(pin)) { listOf(pin.uppercase().chunked(2).joinToString(":")) }
        assertTrue(v.isTrusted("com.victorious.launch"))
    }
    @Test fun pinRejectsWrongPackage() {
        val v = ProviderPinVerifier(current = setOf(pin)) { listOf(pin) }
        assertFalse(v.isTrusted("com.evil.launch"))
        assertFalse(v.isTrusted(null))
    }
    @Test fun pinRejectsWrongCert() {
        val v = ProviderPinVerifier(current = setOf(pin)) { listOf("cd".repeat(32)) }
        assertFalse(v.isTrusted("com.victorious.launch"))
    }
    @Test fun pinRejectsMissingPackageInfo() {
        assertFalse(ProviderPinVerifier(current = setOf(pin)) { null }.isTrusted("com.victorious.launch"))
        assertFalse(ProviderPinVerifier(current = setOf(pin)) { throw RuntimeException() }.isTrusted("com.victorious.launch"))
    }
    private val cur = "B9:9A:AF:E2:C1:41:8B:5D:64:AA:0D:4B:AC:61:FC:F7:A7:BA:81:44:35:E0:EF:89:E8:AA:55:BE:56:4E:4A:69"
    private val rev = "1B:06:A8:9E:7F:F5:BA:21:C9:A4:F8:F1:F0:4F:95:E5:62:78:A1:22:18:AD:37:33:28:CE:E7:83:1D:4F:BA:CA"
    private val L = "com.victorious.launch"
    @Test fun defaultPinsAcceptCurrentDebug() {
        assertTrue(ProviderPinVerifier { listOf(cur) }.isTrusted(L))
        assertTrue(ProviderPinVerifier { listOf(cur.replace(":", "").lowercase()) }.isTrusted(L))
    }
    @Test fun defaultPinsRejectRevoked() {
        assertFalse(ProviderPinVerifier { listOf(rev) }.isTrusted(L))
        assertFalse(ProviderPinVerifier { listOf(cur, rev) }.isTrusted(L))
    }
    @Test fun rotationAcceptsNext() {
        val nxt = "cd".repeat(32)
        assertTrue(ProviderPinVerifier(current = setOf(cur), next = setOf(nxt)) { listOf(nxt) }.isTrusted(L))
        assertTrue(ProviderPinVerifier(current = setOf(cur), next = setOf(nxt)) { listOf(cur) }.isTrusted(L))
    }
    @Test fun revokedWinsEvenIfAlsoListedCurrent() {
        assertFalse(ProviderPinVerifier(current = setOf(rev), revoked = setOf(rev)) { listOf(rev) }.isTrusted(L))
    }
    @Test fun blankOrTbdSlotsNeverAuthorize() {
        assertFalse(ProviderPinVerifier(current = setOf("", "TBD"), next = emptySet()) { listOf("") }.isTrusted(L))
        assertFalse(ProviderPinVerifier(current = setOf(cur)) { emptyList() }.isTrusted(L))
    }
    @Test fun releaseAndNextSlotsEmpty() {
        assertTrue(FleetThemeContract.PIN_RELEASE.isEmpty())
        assertTrue(FleetThemeContract.PIN_NEXT.isEmpty())
    }
}
