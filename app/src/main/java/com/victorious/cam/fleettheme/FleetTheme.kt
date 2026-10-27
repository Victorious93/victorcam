package com.victorious.cam.fleettheme

/**
 * FLEET_THEME schemaVersion 1 (VictorLaunch docs/specs/FLEET_THEME.md).
 * VictorCam is a READ-ONLY consumer. Nothing in this package writes the provider.
 */
object FleetThemeContract {
    const val PROVIDER_PACKAGE = "com.victorious.launch"
    const val AUTHORITY = "com.victorious.launch.theme"
    const val URI = "content://$AUTHORITY/current"
    const val ACTION_CHANGED = "com.victorious.launch.action.FLEET_THEME_CHANGED"
    const val SUPPORTED_SCHEMA = 1

    /*
     * VictorLaunch publisher pins (fleet-manifest docs/specs/FLEET_THEME.md @ 93b87740,
     * "VictorLaunch publisher pins" + "Cert rotation"). Accept CURRENT or NEXT; REVOKED always
     * rejected; blank/TBD slots never authorize.
     */
    /** current (debug) publisher cert. */
    val PIN_CURRENT: Set<String> = setOf(
        "B9:9A:AF:E2:C1:41:8B:5D:64:AA:0D:4B:AC:61:FC:F7:A7:BA:81:44:35:E0:EF:89:E8:AA:55:BE:56:4E:4A:69"
    )
    /** next (rotation overlap): none published yet. */
    val PIN_NEXT: Set<String> = emptySet()
    /** RELEASE: TBD, no release key exists yet. Do not invent a SHA-256. Add here when published. */
    val PIN_RELEASE: Set<String> = emptySet()
    /** REVOKED: old committed VictorLaunch debug keystore. */
    val PIN_REVOKED: Set<String> = setOf(
        "1B:06:A8:9E:7F:F5:BA:21:C9:A4:F8:F1:F0:4F:95:E5:62:78:A1:22:18:AD:37:33:28:CE:E7:83:1D:4F:BA:CA"
    )

    val PALETTE_KEYS = listOf(
        "background", "surface", "primary", "secondary", "onBackground",
        "muted", "danger", "stale", "tile"
    )
}

/** Colors as ARGB ints; null = key absent/invalid, keep app default for that slot. */
data class FleetPalette(val colors: Map<String, Long>, val mode: String?) {
    operator fun get(key: String): Long? = colors[key]
}

object FleetThemeCodec {
    /** Parses #RRGGBB or #AARRGGBB into 0xAARRGGBB, else null. */
    fun parseHex(raw: String?): Long? {
        val s = raw?.trim() ?: return null
        if (!s.startsWith("#")) return null
        val hex = s.substring(1)
        if (hex.length != 6 && hex.length != 8) return null
        if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        val v = hex.toLong(16)
        return if (hex.length == 6) 0xFF000000L or v else v
    }

    /**
     * Decodes one provider row (column name -> string value). Unknown columns ignored.
     * Returns null (=> use defaults) if schemaVersion != 1. Palette keys are applied per key:
     * absent/blank/invalid keys are simply omitted so only that role keeps the app default.
     */
    fun decodeRow(row: Map<String, String?>): FleetPalette? {
        val schema = row["schemaVersion"]?.trim()?.toIntOrNull() ?: return null
        if (schema != FleetThemeContract.SUPPORTED_SCHEMA) return null
        val colors = buildMap {
            for (k in FleetThemeContract.PALETTE_KEYS) parseHex(row[k])?.let { put(k, it) }
        }
        return FleetPalette(colors, row["mode"])
    }
}

/**
 * Package + signing-cert pin check with rotation (accept current or next, reject revoked).
 * Cert lookup is injected so it can be unit tested.
 */
class ProviderPinVerifier(
    private val expectedPackage: String = FleetThemeContract.PROVIDER_PACKAGE,
    current: Set<String> = FleetThemeContract.PIN_CURRENT + FleetThemeContract.PIN_RELEASE,
    next: Set<String> = FleetThemeContract.PIN_NEXT,
    revoked: Set<String> = FleetThemeContract.PIN_REVOKED,
    private val certDigests: (String) -> List<String>?,
) {
    private val revoked = revoked.norm()
    private val accepted = (current + next).norm() - this.revoked

    /**
     * @param providerPackage package that actually owns the authority on this device.
     * True only if it is VictorLaunch, no signer is revoked, and a signer matches current/next.
     */
    fun isTrusted(providerPackage: String?): Boolean {
        if (providerPackage != expectedPackage || accepted.isEmpty()) return false
        val digests = try { certDigests(providerPackage) } catch (_: Exception) { null }
        if (digests.isNullOrEmpty()) return false
        val d = digests.map { normalize(it) }
        if (d.any { it in revoked }) return false
        return d.any { it in accepted }
    }

    companion object {
        fun normalize(h: String) = h.replace(":", "").trim().lowercase()
        private fun Set<String>.norm() = map { normalize(it) }.filter { it.length == 64 && it.all { c -> c in "0123456789abcdef" } }.toSet()
    }
}
