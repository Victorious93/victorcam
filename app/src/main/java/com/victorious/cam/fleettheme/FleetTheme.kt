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

    /**
     * SHA-256 of VictorLaunch's signing certificate (lowercase hex, no colons).
     * TODO(don): fill in the real VictorLaunch release (and debug, if used) cert hash, e.g.
     *   apksigner verify --print-certs victorlaunch.apk | grep SHA-256
     * While this set contains only the placeholder, verification FAILS CLOSED and the app
     * uses its own default theme ("not-pinned fallback").
     */
    val PINNED_CERT_SHA256: Set<String> = setOf(
        "TODO_FILL_IN_VICTORLAUNCH_SIGNING_CERT_SHA256"
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
     * Returns null (=> use defaults) if schemaVersion != 1 or no valid palette color.
     */
    fun decodeRow(row: Map<String, String?>): FleetPalette? {
        val schema = row["schemaVersion"]?.trim()?.toIntOrNull() ?: return null
        if (schema != FleetThemeContract.SUPPORTED_SCHEMA) return null
        val colors = buildMap {
            for (k in FleetThemeContract.PALETTE_KEYS) parseHex(row[k])?.let { put(k, it) }
        }
        if (colors.isEmpty()) return null
        return FleetPalette(colors, row["mode"])
    }
}

/** Package + signing-cert pin check. Cert lookup is injected so it can be unit tested. */
class ProviderPinVerifier(
    private val expectedPackage: String = FleetThemeContract.PROVIDER_PACKAGE,
    private val pinnedSha256: Set<String> = FleetThemeContract.PINNED_CERT_SHA256,
    private val certDigests: (String) -> List<String>?,
) {
    /**
     * @param providerPackage package that actually owns the authority on this device.
     * True only if it is VictorLaunch AND at least one of its signing certs matches a pin.
     */
    fun isTrusted(providerPackage: String?): Boolean {
        if (providerPackage != expectedPackage) return false
        val pins = pinnedSha256.map { normalize(it) }.filter { it.length == 64 }.toSet()
        if (pins.isEmpty()) return false
        val digests = try { certDigests(providerPackage) } catch (_: Exception) { null } ?: return false
        return digests.any { normalize(it) in pins }
    }

    companion object {
        fun normalize(h: String) = h.replace(":", "").trim().lowercase()
    }
}
