package com.victorious.cam

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.victorious.cam.capture.CaptureOutputRouter
import com.victorious.cam.ui.screens.CaptureHandlerScreen
import com.victorious.cam.ui.theme.VictorCamTheme
import java.io.InputStream
import java.io.OutputStream

/**
 * Appears as a camera app for IMAGE_CAPTURE / VIDEO_CAPTURE / STILL_IMAGE_CAMERA.
 * User picks owned media (or a demo feed Uri); result is routed via [CaptureOutputRouter].
 */
class CaptureHandlerActivity : ComponentActivity() {

    private var plan by mutableStateOf(
        CaptureOutputRouter.plan(null, null)
    )
    private var statusMessage by mutableStateOf("Pick user-owned media to return as the capture.")
    private var errorMessage by mutableStateOf<String?>(null)

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            errorMessage = "No media selected"
            return@registerForActivityResult
        }
        deliverCapture(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val action = intent?.action
        @Suppress("DEPRECATION")
        val extraOutput = intent?.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
        plan = CaptureOutputRouter.plan(action, extraOutput)

        setContent {
            VictorCamTheme {
                CaptureHandlerScreen(
                    plan = plan,
                    statusMessage = statusMessage,
                    errorMessage = errorMessage,
                    onPickMedia = {
                        pickMedia.launch(CaptureOutputRouter.acceptedMimeTypes(plan.isVideo))
                    },
                    onUseDemoFeed = {
                        val demo = if (plan.isVideo) {
                            Uri.parse("content://com.victorious.cam.demo/sample.mp4")
                        } else {
                            Uri.parse("content://com.victorious.cam.demo/sample.jpg")
                        }
                        // Demo Uri is not a real stream — synthesize a tiny bitmap / empty video marker
                        deliverDemo(demo)
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    private fun deliverDemo(demoUri: Uri) {
        if (plan.mode == CaptureOutputRouter.Mode.WRITE_TO_EXTRA_OUTPUT) {
            val target = plan.extraOutput
            if (target == null) {
                errorMessage = "EXTRA_OUTPUT missing"
                return
            }
            try {
                contentResolver.openOutputStream(target)?.use { out ->
                    if (plan.isVideo) {
                        // Placeholder bytes — real pipeline would copy a user file
                        out.write("VICTORCAM_DEMO_VIDEO".toByteArray())
                    } else {
                        writePlaceholderJpeg(out)
                    }
                } ?: run {
                    errorMessage = "Could not open EXTRA_OUTPUT"
                    return
                }
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed writing EXTRA_OUTPUT"
            }
            return
        }

        // RETURN_IN_RESULT
        val result = Intent()
        if (plan.isVideo) {
            result.data = demoUri
            result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            val thumb = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            thumb.eraseColor(0xFF00C2A8.toInt())
            result.putExtra("data", thumb)
            result.data = demoUri
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun deliverCapture(source: Uri) {
        if (!CaptureOutputRouter.isAcceptableSource(source, plan.isVideo, contentResolver.getType(source))) {
            errorMessage = "Selected media does not match capture type (${if (plan.isVideo) "video" else "image"})"
            return
        }

        when (plan.mode) {
            CaptureOutputRouter.Mode.WRITE_TO_EXTRA_OUTPUT -> {
                val target = plan.extraOutput
                if (target == null) {
                    errorMessage = "EXTRA_OUTPUT missing"
                    return
                }
                try {
                    copyUri(source, target)
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Copy to EXTRA_OUTPUT failed"
                }
            }
            CaptureOutputRouter.Mode.RETURN_IN_RESULT -> {
                val result = Intent()
                if (plan.isVideo) {
                    result.data = source
                    result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    val thumb = loadThumbnail(source)
                    if (thumb != null) {
                        result.putExtra("data", thumb)
                    }
                    result.data = source
                    result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }
    }

    private fun copyUri(from: Uri, to: Uri) {
        contentResolver.openInputStream(from).use { input ->
            requireNotNull(input) { "Cannot open source" }
            contentResolver.openOutputStream(to).use { output ->
                requireNotNull(output) { "Cannot open EXTRA_OUTPUT" }
                input.copyTo(output)
            }
        }
    }

    private fun loadThumbnail(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { input: InputStream ->
                val full = BitmapFactory.decodeStream(input) ?: return null
                Bitmap.createScaledBitmap(full, 256, 256, true)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writePlaceholderJpeg(out: OutputStream) {
        val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(0xFF00C2A8.toInt())
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
}
