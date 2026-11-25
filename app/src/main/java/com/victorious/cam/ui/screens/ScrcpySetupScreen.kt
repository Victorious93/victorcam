package com.victorious.cam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.victorious.cam.R
import com.victorious.cam.scrcpy.ScrcpyCommandOptions
import com.victorious.cam.scrcpy.ScrcpyPrefs
import com.victorious.cam.scrcpy.ScrcpyVideoSource

@Composable
fun ScrcpySetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { ScrcpyPrefs(context).also { it.restoreToGate() } }
    var enabled by remember { mutableStateOf(prefs.isEnabled()) }
    val cameraCmd = remember {
        ScrcpyCommandOptions(videoSource = ScrcpyVideoSource.CAMERA).toCommandLine()
    }
    val screenCmd = remember {
        ScrcpyCommandOptions(videoSource = ScrcpyVideoSource.DISPLAY).toCommandLine()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.scrcpy_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.scrcpy_setup_body), style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(4.dp))
        Text("Enable this optional path (default OFF)", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (enabled) "Enabled — run the PC helper when you want the webcam" else "Off")
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    prefs.setEnabled(it)
                }
            )
        }

        Text("Linux PC helper", fontWeight = FontWeight.SemiBold)
        Text(
            "On a Linux PC with scrcpy 2.x, adb, and v4l2loopback installed, enable USB debugging " +
                "on this phone, then run the helper from a clone of this repo:",
            style = MaterialTheme.typography.bodyMedium
        )
        Mono("scripts/victorcam-scrcpy.sh --camera")
        Mono("scripts/victorcam-scrcpy.sh --screen")
        Text(
            "Equivalent scrcpy command (camera, Android 12+):",
            style = MaterialTheme.typography.bodyMedium
        )
        Mono(cameraCmd)
        Text(
            "Screen-mirror (default scrcpy display source) — useful if you want the phone screen, " +
                "including a VictorCam preview, as the PC webcam:",
            style = MaterialTheme.typography.bodyMedium
        )
        Mono(screenCmd)
        Text(
            "Load the loopback device first if needed: sudo modprobe v4l2loopback devices=1 " +
                "video_nr=10 card_label=VictorCam exclusive_caps=1",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Full steps, limits, and certainty notes: docs/SCRCPY.md",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(R.string.scrcpy_setup_credit),
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun Mono(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth()
    )
}
