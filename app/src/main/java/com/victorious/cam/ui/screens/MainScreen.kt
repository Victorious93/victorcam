package com.victorious.cam.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.victorious.cam.R
import com.victorious.cam.feed.FeedSource
import com.victorious.cam.feed.FeedSourceFactory
import com.victorious.cam.feed.ImageFeedSource
import com.victorious.cam.feed.LoopingClipFeedSource
import com.victorious.cam.feed.ScreenRegionFeedSource
import com.victorious.cam.feed.VideoFileFeedSource
import com.victorious.cam.library.FeedLibraryDatabase
import com.victorious.cam.library.FeedLibraryRepository
import com.victorious.cam.library.FeedPreset
import com.victorious.cam.network.NetworkStreamFeedSource
import com.victorious.cam.overlay.OverlayCompositor
import com.victorious.cam.overlay.OverlayConfig
import com.victorious.cam.overlay.OverlayPosition
import com.victorious.cam.pipeline.FramePipeline
import com.victorious.cam.screen.CropRect
import com.victorious.cam.screen.ScreenCaptureService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(onOpenAdvancedHook: () -> Unit = {}) {
    val context = LocalContext.current
    val pipeline = remember { FramePipeline() }
    val sources = remember {
        FeedSourceFactory.all(context).also { FeedSourceFactory.bindAll(it, pipeline) }
    }
    var selected by remember { mutableStateOf(sources.first()) }
    var status by remember { mutableStateOf(selected.statusLine()) }
    var error by remember { mutableStateOf<String?>(null) }
    var overlay by remember {
        mutableStateOf(OverlayConfig(timestampEnabled = true, textEnabled = false))
    }
    var networkUrl by remember { mutableStateOf("rtsp://") }
    var cropCsv by remember { mutableStateOf("0,0,1280,720") }
    var mediaUri by remember { mutableStateOf("content://demo/sample.mp4") }
    var presetName by remember { mutableStateOf("My preset") }
    val scope = rememberCoroutineScope()
    val compositor = remember { OverlayCompositor() }

    val repo = remember {
        FeedLibraryRepository(FeedLibraryDatabase.get(context).presets())
    }
    val presets by repo.observePresets().collectAsState(initial = emptyList())

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            context.startForegroundService(Intent(context, ScreenCaptureService::class.java))
            val screen = selected as? ScreenRegionFeedSource
            screen?.setProjectionResult(result.resultCode, result.data)
            scope.launch {
                selected.start()
                status = selected.statusLine()
            }
        } else {
            error = "Screen capture permission denied"
        }
    }

    DisposableEffect(Unit) {
        pipeline.start()
        onDispose {
            scope.launch { selected.stop() }
            pipeline.stop()
        }
    }

    // Periodically re-apply overlays onto latest frame for still/composited preview
    DisposableEffect(overlay, selected) {
        val job = scope.launch {
            while (true) {
                val frame = pipeline.latestFrame()
                if (frame != null && overlay.anyEnabled()) {
                    val composed = compositor.compose(frame, overlay)
                    pipeline.pushBitmap(composed)
                    if (!composed.isRecycled && composed !== frame) composed.recycle()
                }
                delay(1000L)
            }
        }
        onDispose { job.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.main_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.pick_source), style = MaterialTheme.typography.titleMedium)

        sources.forEach { source ->
            SourceRow(
                source = source,
                selected = source.id == selected.id,
                onSelect = {
                    scope.launch { selected.stop() }
                    selected = source
                    status = source.statusLine()
                    error = null
                }
            )
        }

        when (selected) {
            is VideoFileFeedSource, is ImageFeedSource, is LoopingClipFeedSource -> {
                OutlinedTextField(
                    value = mediaUri,
                    onValueChange = { mediaUri = it },
                    label = { Text("Media URI") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is NetworkStreamFeedSource -> {
                OutlinedTextField(
                    value = networkUrl,
                    onValueChange = { networkUrl = it },
                    label = { Text("Stream URL (rtsp/http/rtmp/srt)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ScreenRegionFeedSource -> {
                OutlinedTextField(
                    value = cropCsv,
                    onValueChange = { cropCsv = it },
                    label = { Text("Crop left,top,width,height") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text("Overlays", style = MaterialTheme.typography.titleMedium)
        OverlayToggles(overlay) { overlay = it }

        Text(stringResource(R.string.preview), style = MaterialTheme.typography.titleMedium)
        FeedPreview(pipeline = pipeline, label = selected.displayName)
        Text(status, style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        applyConfig(selected, mediaUri, networkUrl, cropCsv)
                        if (selected is ScreenRegionFeedSource &&
                            (selected as ScreenRegionFeedSource).let { true }
                        ) {
                            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            // Always request consent for live capture; start still works with placeholder
                            projectionLauncher.launch(mpm.createScreenCaptureIntent())
                        } else {
                            selected.start()
                                .onSuccess { status = selected.statusLine(); error = null }
                                .onFailure { e -> error = e.message; status = selected.statusLine() }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Start") }
            Button(
                onClick = {
                    scope.launch {
                        selected.stop()
                        status = selected.statusLine()
                        error = null
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Stop") }
        }

        Text("Feed library", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = presetName,
            onValueChange = { presetName = it },
            label = { Text("Preset name") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        repo.save(
                            FeedPreset(
                                name = presetName.ifBlank { "Preset" },
                                sourceKind = selected.id.uppercase().let {
                                    // map id to enum name
                                    when (selected) {
                                        is VideoFileFeedSource -> "VIDEO_FILE"
                                        is ImageFeedSource -> "IMAGE"
                                        is LoopingClipFeedSource -> "LOOPING_CLIP"
                                        is ScreenRegionFeedSource -> "SCREEN_REGION"
                                        is NetworkStreamFeedSource -> "NETWORK_STREAM"
                                        else -> "VIDEO_FILE"
                                    }
                                },
                                sourceUri = when (selected) {
                                    is NetworkStreamFeedSource -> networkUrl
                                    is ScreenRegionFeedSource -> null
                                    else -> mediaUri
                                },
                                cropCsv = cropCsv,
                                textEnabled = overlay.textEnabled,
                                text = overlay.text,
                                textPosition = overlay.textPosition.name,
                                textOpacity = overlay.textOpacity,
                                logoEnabled = overlay.logoEnabled,
                                logoUri = overlay.logoUri,
                                logoPosition = overlay.logoPosition.name,
                                logoOpacity = overlay.logoOpacity,
                                timestampEnabled = overlay.timestampEnabled,
                                timestampPosition = overlay.timestampPosition.name,
                                timestampOpacity = overlay.timestampOpacity
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save preset") }
        }
        presets.forEach { p ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(p.name, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    scope.launch {
                        when (val s = sources.find { it.id == p.kindOrDefault().name.lowercase() }
                            ?: sources.find {
                                it.id == when (p.kindOrDefault()) {
                                    com.victorious.cam.feed.FeedSourceKind.VIDEO_FILE -> "video_file"
                                    com.victorious.cam.feed.FeedSourceKind.IMAGE -> "image"
                                    com.victorious.cam.feed.FeedSourceKind.LOOPING_CLIP -> "looping_clip"
                                    com.victorious.cam.feed.FeedSourceKind.SCREEN_REGION -> "screen_region"
                                    com.victorious.cam.feed.FeedSourceKind.NETWORK_STREAM -> "network_stream"
                                }
                            }) {
                            null -> error = "Preset source missing"
                            else -> {
                                selected = s
                                p.sourceUri?.let { mediaUri = it; networkUrl = it }
                                p.cropCsv?.let { cropCsv = it }
                                overlay = OverlayConfig(
                                    textEnabled = p.textEnabled,
                                    text = p.text,
                                    textPosition = runCatching { OverlayPosition.valueOf(p.textPosition) }.getOrDefault(OverlayPosition.TOP_LEFT),
                                    textOpacity = p.textOpacity,
                                    logoEnabled = p.logoEnabled,
                                    logoUri = p.logoUri,
                                    logoPosition = runCatching { OverlayPosition.valueOf(p.logoPosition) }.getOrDefault(OverlayPosition.BOTTOM_RIGHT),
                                    logoOpacity = p.logoOpacity,
                                    timestampEnabled = p.timestampEnabled,
                                    timestampPosition = runCatching { OverlayPosition.valueOf(p.timestampPosition) }.getOrDefault(OverlayPosition.BOTTOM_LEFT),
                                    timestampOpacity = p.timestampOpacity
                                )
                                status = "Loaded preset ${p.name}"
                            }
                        }
                    }
                }) { Text("Load") }
                OutlinedButton(onClick = { scope.launch { repo.rename(p.id, p.name + " *") } }) { Text("Rename") }
                OutlinedButton(onClick = { scope.launch { repo.delete(p.id) } }) { Text("Delete") }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenAdvancedHook, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.hook_optin_open))
        }
    }
}

@Composable
private fun OverlayToggles(config: OverlayConfig, onChange: (OverlayConfig) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = config.textEnabled, onCheckedChange = { onChange(config.copy(textEnabled = it)) })
        Text("Text")
        Checkbox(checked = config.timestampEnabled, onCheckedChange = { onChange(config.copy(timestampEnabled = it)) })
        Text("Timestamp")
        Checkbox(checked = config.logoEnabled, onCheckedChange = { onChange(config.copy(logoEnabled = it)) })
        Text("Logo")
    }
    if (config.textEnabled) {
        OutlinedTextField(
            value = config.text,
            onValueChange = { onChange(config.copy(text = it)) },
            label = { Text("Overlay text") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SourceRow(source: FeedSource, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.padding(start = 8.dp)) {
            Text(source.displayName, style = MaterialTheme.typography.titleSmall)
            Text(source.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun applyConfig(
    source: FeedSource,
    mediaUri: String,
    networkUrl: String,
    cropCsv: String
) {
    when (source) {
        is VideoFileFeedSource -> source.setUri(mediaUri)
        is ImageFeedSource -> source.setUri(mediaUri)
        is LoopingClipFeedSource -> {
            source.setUri(mediaUri)
            source.setLoopCount(Int.MAX_VALUE)
        }
        is NetworkStreamFeedSource -> source.setUrl(networkUrl)
        is ScreenRegionFeedSource -> {
            val crop = CropRect.parse(cropCsv) ?: CropRect(0, 0, 1280, 720)
            source.setRegion(crop.left, crop.top, crop.width, crop.height)
        }
    }
}
