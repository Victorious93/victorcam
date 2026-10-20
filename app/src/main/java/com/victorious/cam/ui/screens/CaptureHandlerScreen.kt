package com.victorious.cam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.victorious.cam.R
import com.victorious.cam.capture.CaptureOutputRouter

@Composable
fun CaptureHandlerScreen(
    plan: CaptureOutputRouter.Plan,
    statusMessage: String,
    errorMessage: String?,
    onPickMedia: () -> Unit,
    onUseDemoFeed: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.capture_banner),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F6FEB))
                .padding(12.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (plan.isVideo) {
                    stringResource(R.string.capture_mode_video)
                } else {
                    stringResource(R.string.capture_mode_image)
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (plan.mode) {
                    CaptureOutputRouter.Mode.WRITE_TO_EXTRA_OUTPUT ->
                        stringResource(R.string.capture_route_extra_output, plan.extraOutput.toString())
                    CaptureOutputRouter.Mode.RETURN_IN_RESULT ->
                        stringResource(R.string.capture_route_result)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPickMedia, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.capture_pick))
            }
            OutlinedButton(onClick = onUseDemoFeed, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.capture_use_feed))
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.capture_cancel))
            }
        }
    }
}
