package com.victorious.cam.pipeline

import android.graphics.Bitmap
import com.victorious.cam.xposed.FeedFrame
import com.victorious.cam.xposed.FeedFrameBuffer
import java.nio.ByteBuffer

/**
 * Publishes [FramePipeline] bitmaps into the Xposed [FeedFrameBuffer] for Camera2 rewrite.
 */
object FeedFramePublisher {
    fun publishBitmap(bitmap: Bitmap) {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        val buf = ByteBuffer.allocate(copy.byteCount)
        copy.copyPixelsToBuffer(buf)
        FeedFrameBuffer.publish(
            FeedFrame(
                width = copy.width,
                height = copy.height,
                format = FeedFrame.Format.RGBA_8888,
                bytes = buf.array()
            )
        )
        if (!copy.isRecycled) copy.recycle()
    }

    fun publishFromPipeline(pipeline: FramePipeline) {
        val frame = pipeline.latestFrame() ?: return
        publishBitmap(frame)
        if (!frame.isRecycled) frame.recycle()
    }

    fun clear() = FeedFrameBuffer.clear()
}
