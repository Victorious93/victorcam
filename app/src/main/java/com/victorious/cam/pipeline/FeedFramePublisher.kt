package com.victorious.cam.pipeline

import android.graphics.Bitmap
import com.victorious.cam.xposed.FeedFrame
import com.victorious.cam.xposed.SharedFrameTransport
import java.nio.ByteBuffer

/**
 * Publishes [FramePipeline] bitmaps into the cross-process [SharedFrameTransport]
 * (and legacy in-process buffer) for Camera2 rewrite.
 */
object FeedFramePublisher {
    fun publishBitmap(bitmap: Bitmap): Boolean {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
        val buf = ByteBuffer.allocate(copy.byteCount)
        copy.copyPixelsToBuffer(buf)
        val ok = SharedFrameTransport.publish(
            FeedFrame(
                width = copy.width,
                height = copy.height,
                format = FeedFrame.Format.RGBA_8888,
                bytes = buf.array()
            )
        )
        if (!copy.isRecycled) copy.recycle()
        return ok
    }

    fun publishFromPipeline(pipeline: FramePipeline): Boolean {
        val frame = pipeline.latestFrame() ?: return false
        val ok = publishBitmap(frame)
        if (!frame.isRecycled) frame.recycle()
        return ok
    }

    fun clear() = SharedFrameTransport.reset()
}
