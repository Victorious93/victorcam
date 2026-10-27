package com.victorious.cam

import android.app.Application
import com.victorious.cam.fleettheme.FleetThemeClient

class VictorCamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try { FleetThemeClient.get(this).start() } catch (_: Exception) { /* defaults */ }
    }
}
