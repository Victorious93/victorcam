package com.victorious.cam.fleettheme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

/** Live fleet palette; null => app default theme. */
object FleetThemeState {
    private val _palette = MutableStateFlow<FleetPalette?>(null)
    val palette: StateFlow<FleetPalette?> = _palette
    fun set(p: FleetPalette?) { _palette.value = p }
}

/** Pure decision logic for one refresh, unit-testable without Android. */
object FleetThemeLoader {
    fun load(trusted: () -> Boolean, query: () -> Map<String, String?>?): FleetPalette? = try {
        if (!trusted()) null else query()?.let { FleetThemeCodec.decodeRow(it) }
    } catch (_: SecurityException) {
        null // not pinned by VictorLaunch -> our defaults, never invent colors
    } catch (_: Exception) {
        null
    }
}

/**
 * READ-ONLY reader of VictorLaunch's fleet theme. Only calls ContentResolver.query();
 * never insert/update/delete/call.
 */
class FleetThemeClient(context: Context) {
    private val app = context.applicationContext
    private val uri: Uri = Uri.parse(FleetThemeContract.URI)
    private val main = Handler(Looper.getMainLooper())
    private val verifier = ProviderPinVerifier(certDigests = ::certSha256)

    private val observer = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    fun start() {
        refresh()
        try { app.contentResolver.registerContentObserver(uri, false, observer) }
        catch (e: Exception) { Log.w(TAG, "observer register failed", e) }
        // Receiver is also declared exported in the manifest (FleetThemeChangedReceiver);
        // runtime registration covers the in-process live path.
        ContextCompat.registerReceiver(
            app, runtimeReceiver, IntentFilter(FleetThemeContract.ACTION_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private val runtimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refresh()
    }

    fun refresh() {
        Thread {
            val p = FleetThemeLoader.load(
                trusted = { verifier.isTrusted(providerPackage()) },
                query = ::queryRow
            )
            FleetThemeState.set(p)
        }.start()
    }

    private fun providerPackage(): String? =
        app.packageManager.resolveContentProvider(FleetThemeContract.AUTHORITY, 0)?.packageName

    private fun queryRow(): Map<String, String?>? =
        app.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (!c.moveToFirst()) null
            else (0 until c.columnCount).associate { idx ->
                c.getColumnName(idx) to when (c.getType(idx)) {
                    Cursor.FIELD_TYPE_NULL -> null
                    else -> c.getString(idx)
                }
            }
        }

    @Suppress("DEPRECATION")
    private fun certSha256(pkg: String): List<String>? {
        val pm = app.packageManager
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return null
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else {
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
        } ?: return null
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.map { s -> md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }
    }

    companion object {
        private const val TAG = "FleetThemeClient"
        @Volatile private var instance: FleetThemeClient? = null
        fun get(context: Context) = instance ?: synchronized(this) {
            instance ?: FleetThemeClient(context).also { instance = it }
        }
    }
}

/** Manifest-declared exported receiver: VictorLaunch sends explicitly to our package. */
class FleetThemeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FleetThemeContract.ACTION_CHANGED) return
        // Extras are a wake-up only; never trusted. Re-query the pinned provider.
        FleetThemeClient.get(context).refresh()
    }
}
