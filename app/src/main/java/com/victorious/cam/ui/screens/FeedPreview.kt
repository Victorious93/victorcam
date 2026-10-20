package com.victorious.cam.ui.screens

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import com.victorious.cam.pipeline.FramePipeline
import kotlinx.coroutines.delay

/**
 * Compose preview of the shared [FramePipeline]: TextureView for live video surface
 * plus periodic bitmap overlay for still / composited frames.
 */
@Composable
fun FeedPreview(
    pipeline: FramePipeline,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    var frame by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pipeline) {
        while (true) {
            frame = pipeline.latestFrame()
            delay(66L) // ~15 fps UI refresh
        }
    }

    DisposableEffect(pipeline) {
        onDispose { /* pipeline owned by screen/VM */ }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    // Surface from TextureView can be used by future direct ExoPlayer attach;
                    // bitmap path covers stills / overlays for now.
                    tv.alpha = 0.01f
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        val bmp = frame
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Feed preview",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = label.ifBlank { "No frame" },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
