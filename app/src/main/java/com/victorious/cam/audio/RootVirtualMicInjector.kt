package com.victorious.cam.audio

import com.victorious.cam.xposed.HookDenylist
import com.victorious.cam.xposed.HookGate

/**
 * Root / Magisk path to present VictorCam audio as the **system microphone**
 * to other apps (AudioRecord hooks / Magisk module binder).
 *
 * **Stock limitation unchanged:** without root/Magisk + LSPosed-style hooks,
 * Android will not let a normal app replace the system mic
 * ([VirtualMicCapability.Level.PREVIEW_ONLY]).
 *
 * **Safety (same as camera hooks):**
 * - Master gate OFF by default ([HookGate.masterEnabled])
 * - Per-app opt-in only
 * - [HookDenylist] forever blocks banking / payment / gov-ID / KYC packages
 * - Hard warnings must be acknowledged in the host UI before enabling
 *
 * **Certainty:** High for gating / documentation; Medium for actual AudioRecord
 * rewrite (requires a loaded Magisk/LSPosed module — not auto-installed).
 */
class RootVirtualMicInjector(
    private val gate: HookGateAccess = HookGateAccess.Default,
    private val denylist: (String) -> Boolean = { HookDenylist.isDenied(it) }
) {
    data class Decision(
        val allowed: Boolean,
        val reason: String,
        val level: VirtualMicCapability.Level
    )

    fun evaluate(targetPackage: String): Decision {
        if (!gate.masterEnabled) {
            return Decision(false, "master_gate_off", VirtualMicCapability.stockLevel())
        }
        if (denylist(targetPackage)) {
            return Decision(
                false,
                "denylist:${HookDenylist.denyReason(targetPackage) ?: "matched"}",
                VirtualMicCapability.stockLevel()
            )
        }
        if (!gate.isOptedIn(targetPackage)) {
            return Decision(false, "not_opted_in", VirtualMicCapability.stockLevel())
        }
        if (!gate.rootAvailable) {
            return Decision(
                false,
                "root_or_magisk_unavailable",
                VirtualMicCapability.stockLevel()
            )
        }
        return Decision(
            true,
            "ok_root_mic_inject",
            VirtualMicCapability.withRootHook()
        )
    }

    /**
     * Arms injection for [targetPackage] when [evaluate] allows.
     * Returns false without side effects when gated.
     */
    fun arm(targetPackage: String, pcmProvider: () -> ByteArray): Boolean {
        val d = evaluate(targetPackage)
        if (!d.allowed) return false
        armedPackage = targetPackage
        this.pcmProvider = pcmProvider
        return true
    }

    fun disarm() {
        armedPackage = null
        pcmProvider = null
    }

    fun isArmedFor(packageName: String): Boolean =
        armedPackage == packageName && evaluate(packageName).allowed

    /** Pull next PCM chunk for a hooked AudioRecord (test / module bridge). */
    fun pullPcm(packageName: String, maxBytes: Int): ByteArray? {
        if (!isArmedFor(packageName)) return null
        val all = pcmProvider?.invoke() ?: return null
        return all.copyOf(minOf(maxBytes, all.size))
    }

    @Volatile private var armedPackage: String? = null
    @Volatile private var pcmProvider: (() -> ByteArray)? = null
}

/**
 * Abstraction over [HookGate] + root detection so unit tests stay JVM-safe.
 */
interface HookGateAccess {
    val masterEnabled: Boolean
    val rootAvailable: Boolean
    fun isOptedIn(packageName: String): Boolean

    object Default : HookGateAccess {
        override val masterEnabled: Boolean get() = HookGate.masterEnabled
        override val rootAvailable: Boolean
            get() = RootDetector.isRootLikely()
        override fun isOptedIn(packageName: String): Boolean =
            packageName in HookGate.optedInPackages
    }
}

/**
 * Best-effort root / Magisk presence probe. Never throws; never escalates.
 * Certainty: Medium (heuristic file/property checks only).
 */
object RootDetector {
    fun isRootLikely(
        whichSu: () -> Boolean = {
            listOf("/system/bin/su", "/system/xbin/su", "/sbin/su").any {
                try { java.io.File(it).exists() } catch (_: Exception) { false }
            }
        },
        magiskMarker: () -> Boolean = {
            try {
                java.io.File("/data/adb/magisk").exists() ||
                    java.io.File("/sbin/.magisk").exists()
            } catch (_: Exception) {
                false
            }
        }
    ): Boolean = whichSu() || magiskMarker()
}
