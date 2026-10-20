package com.victorious.cam.xposed

import android.media.Image
import android.media.ImageReader
import java.nio.ByteBuffer

/**
 * LSPosed / Xposed entry: Camera2 [ImageReader] frame rewrite from [FeedFrameBuffer].
 *
 * OFF unless [HookGate] allows the package. Hooks [ImageReader.acquireLatestImage]
 * (and acquireNextImage) via reflective XposedHelpers when the API is present.
 */
class VictorCamCameraHook {

    fun handleLoadPackage(lpparam: Any) {
        val packageName = readPackageName(lpparam) ?: return
        if (!HookGate.canHook(packageName)) {
            log("VictorCam: skip hook for $packageName (gate/denylist)")
            return
        }
        val classLoader = readClassLoader(lpparam)
        try {
            hookImageReader(packageName, classLoader)
            log("VictorCam: ImageReader substitution armed for $packageName")
        } catch (t: Throwable) {
            log("VictorCam: hook install failed ($t)")
        }
    }

    private fun hookImageReader(packageName: String, classLoader: ClassLoader?) {
        val helpers = Class.forName("de.robv.android.xposed.XposedHelpers")
        val findAndHook = helpers.methods.first {
            it.name == "findAndHookMethod" && it.parameterTypes.size >= 4
        }
        val callbackClass = Class.forName("de.robv.android.xposed.XC_MethodHook")
        val hook = java.lang.reflect.Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            if (method.name == "afterHookedMethod" && args != null && args.isNotEmpty()) {
                val param = args[0]
                val result = param.javaClass.getField("result").get(param)
                if (result is Image) {
                    substituteImage(packageName, result)
                }
            }
            null
        }
        // acquireLatestImage()
        findAndHook.invoke(
            null,
            "android.media.ImageReader",
            classLoader,
            "acquireLatestImage",
            hook
        )
        findAndHook.invoke(
            null,
            "android.media.ImageReader",
            classLoader,
            "acquireNextImage",
            hook
        )
    }

    /**
     * Rewrites image planes in-place when gate + feed frame allow.
     * Package-visible for unit tests via [substituteImageForTest].
     */
    fun substituteImage(packageName: String, image: Image): Boolean {
        val decision = FrameSubstitutionEngine.shouldSubstitute(
            packageName,
            FeedFrameBuffer.hasFrame()
        )
        if (!decision.substituted) return false
        val frame = FeedFrameBuffer.snapshot() ?: return false
        val rgba = FrameSubstitutionEngine.materializeRgba(frame, image.width, image.height)
        return writeRgbaToImage(image, rgba)
    }

    private fun writeRgbaToImage(image: Image, rgba: ByteArray): Boolean {
        return try {
            val planes = image.planes
            if (planes.isEmpty()) return false
            val plane = planes[0]
            val buf: ByteBuffer = plane.buffer
            buf.rewind()
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = image.width
            val height = image.height
            if (pixelStride == 4 && rowStride >= width * 4) {
                for (y in 0 until height) {
                    buf.position(y * rowStride)
                    buf.put(rgba, y * width * 4, width * 4)
                }
                true
            } else {
                // Best-effort packed write
                val limit = minOf(buf.remaining(), rgba.size)
                buf.put(rgba, 0, limit)
                true
            }
        } catch (t: Throwable) {
            log("VictorCam: plane rewrite failed: $t")
            false
        }
    }

    private fun readPackageName(lpparam: Any): String? = try {
        lpparam.javaClass.getField("packageName").get(lpparam) as? String
    } catch (_: Throwable) {
        try {
            lpparam.javaClass.getDeclaredField("packageName").apply { isAccessible = true }
                .get(lpparam) as? String
        } catch (_: Throwable) {
            null
        }
    }

    private fun readClassLoader(lpparam: Any): ClassLoader? = try {
        lpparam.javaClass.getField("classLoader").get(lpparam) as? ClassLoader
    } catch (_: Throwable) {
        VictorCamCameraHook::class.java.classLoader
    }

    private fun log(msg: String) {
        try {
            val bridge = Class.forName("de.robv.android.xposed.XposedBridge")
            bridge.getMethod("log", String::class.java).invoke(null, msg)
        } catch (_: Throwable) {
            android.util.Log.i(TAG, msg)
        }
    }

    companion object {
        private const val TAG = "VictorCamXposed"

        @JvmStatic
        fun initZygote() {
            HookGate.resetToSafeDefaults()
            FeedFrameBuffer.clear()
        }
    }
}
