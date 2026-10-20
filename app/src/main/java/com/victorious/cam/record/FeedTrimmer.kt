package com.victorious.cam.record

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Basic start/end trim using MediaExtractor + MediaMuxer (no re-encode).
 * Media3 Transformer can replace this path when available on the device classpath.
 */
class FeedTrimmer(private val context: Context) {

    data class TrimRequest(
        val input: File,
        val output: File,
        val startMs: Long,
        val endMs: Long
    ) {
        init {
            require(startMs >= 0) { "startMs >= 0" }
            require(endMs > startMs) { "endMs > startMs" }
        }

        fun durationMs(): Long = endMs - startMs
    }

    fun trim(request: TrimRequest): Result<File> {
        if (!request.input.exists()) {
            return Result.failure(IllegalArgumentException("missing input"))
        }
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(request.input.absolutePath)
            val muxer = MediaMuxer(request.output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = IntArray(extractor.trackCount) { -1 }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                trackMap[i] = muxer.addTrack(format)
                extractor.selectTrack(i)
            }
            muxer.start()
            val startUs = request.startMs * 1000L
            val endUs = request.endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val buffer = ByteBuffer.allocate(1_048_576)
            val info = android.media.MediaCodec.BufferInfo()
            while (true) {
                val track = extractor.sampleTrackIndex
                if (track < 0) break
                val pts = extractor.sampleTime
                if (pts < 0 || pts > endUs) break
                if (pts >= startUs) {
                    info.offset = 0
                    info.size = extractor.readSampleData(buffer, 0)
                    if (info.size < 0) break
                    info.presentationTimeUs = pts - startUs
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(trackMap[track], buffer, info)
                }
                if (!extractor.advance()) break
            }
            muxer.stop()
            muxer.release()
            extractor.release()
            Result.success(request.output)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /** Pure helper for UI validation / tests. */
    companion object {
        fun clampRange(startMs: Long, endMs: Long, mediaDurationMs: Long): Pair<Long, Long> {
            val end = endMs.coerceAtMost(mediaDurationMs).coerceAtLeast(1)
            val start = startMs.coerceIn(0, end - 1)
            return start to end
        }
    }
}
