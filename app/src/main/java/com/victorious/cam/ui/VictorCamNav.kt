package com.victorious.cam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.victorious.cam.data.DisclosurePrefs
import com.victorious.cam.ui.screens.AdvancedHookOptInScreen
import com.victorious.cam.ui.screens.DisclosureScreen
import com.victorious.cam.ui.screens.MainScreen

object Routes {
    const val DISCLOSURE = "disclosure"
    const val MAIN = "main"
    const val ADVANCED_HOOK = "advanced_hook"
}

@Composable
fun VictorCamNav() {
    val context = LocalContext.current
    val prefs = remember { DisclosurePrefs(context) }
    val start = if (prefs.hasAccepted()) Routes.MAIN else Routes.DISCLOSURE
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.DISCLOSURE) {
            DisclosureScreen(
                onAccept = {
                    prefs.accept()
                    nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.DISCLOSURE) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(onOpenAdvancedHook = { nav.navigate(Routes.ADVANCED_HOOK) })
        }
        composable(Routes.ADVANCED_HOOK) {
            AdvancedHookOptInScreen(onBack = { nav.popBackStack() })
        }
    }
}
