package com.victorious.cam.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.victorious.cam.R
import com.victorious.cam.xposed.HookDenylist
import com.victorious.cam.xposed.HookGate

@Composable
fun AdvancedHookOptInScreen(onBack: () -> Unit) {
    var master by remember { mutableStateOf(HookGate.masterEnabled) }
    var packageInput by remember { mutableStateOf("") }
    var opted by remember { mutableStateOf(HookGate.optedInPackages) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.hook_optin_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Text(stringResource(R.string.hook_optin_warnings), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("Master enable (default OFF)", fontWeight = FontWeight.SemiBold)
        Switch(
            checked = master,
            onCheckedChange = {
                master = it
                HookGate.masterEnabled = it
                if (!it) {
                    HookGate.optedInPackages = emptySet()
                    opted = emptySet()
                }
            }
        )
        OutlinedTextField(
            value = packageInput,
            onValueChange = { packageInput = it },
            label = { Text("Per-app package to allow") },
            modifier = Modifier.fillMaxWidth(),
            enabled = master
        )
        Button(
            enabled = master,
            onClick = {
                val pkg = packageInput.trim()
                if (pkg.isEmpty()) {
                    error = "Enter a package name"
                    return@Button
                }
                if (HookDenylist.isDenied(pkg)) {
                    error = "Denied: ${HookDenylist.denyReason(pkg)} — banking/KYC/gov ID cannot be hooked"
                    return@Button
                }
                opted = opted + pkg
                HookGate.optedInPackages = opted
                packageInput = ""
                error = null
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add per-app opt-in") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Opted-in packages: ${opted.joinToString().ifBlank { "(none)" }}")
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
