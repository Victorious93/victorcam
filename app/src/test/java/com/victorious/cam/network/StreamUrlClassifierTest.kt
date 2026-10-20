package com.victorious.cam.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUrlClassifierTest {
    @Test
    fun classifiesSchemes() {
        assertEquals(StreamUrlClassifier.Kind.RTSP, StreamUrlClassifier.classify("rtsp://cam/stream"))
        assertEquals(StreamUrlClassifier.Kind.RTMP, StreamUrlClassifier.classify("rtmp://live/app"))
        assertEquals(StreamUrlClassifier.Kind.SRT, StreamUrlClassifier.classify("srt://host:9000"))
        assertEquals(StreamUrlClassifier.Kind.HLS, StreamUrlClassifier.classify("https://x/a.m3u8"))
        assertEquals(StreamUrlClassifier.Kind.HTTP, StreamUrlClassifier.classify("https://x/v.mp4"))
        assertEquals(StreamUrlClassifier.Kind.NDI, StreamUrlClassifier.classify("ndi://Source"))
        assertEquals(StreamUrlClassifier.Kind.UNSUPPORTED, StreamUrlClassifier.classify("ftp://x"))
    }

    @Test
    fun playableFlags() {
        assertTrue(StreamUrlClassifier.isPlayable(StreamUrlClassifier.Kind.RTSP))
        assertTrue(StreamUrlClassifier.isPlayable(StreamUrlClassifier.Kind.RTMP))
        assertTrue(StreamUrlClassifier.isPlayable(StreamUrlClassifier.Kind.SRT))
        assertFalse(StreamUrlClassifier.isPlayable(StreamUrlClassifier.Kind.NDI))
    }
}
