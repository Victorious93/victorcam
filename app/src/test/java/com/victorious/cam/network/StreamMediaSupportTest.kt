package com.victorious.cam.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamMediaSupportTest {
    @Test
    fun rtmpAndSrtAreFullySupportedKinds() {
        assertTrue(StreamMediaSourceFactory.supportsFully(StreamUrlClassifier.Kind.RTMP))
        assertTrue(StreamMediaSourceFactory.supportsFully(StreamUrlClassifier.Kind.SRT))
        assertTrue(StreamMediaSourceFactory.supportsFully(StreamUrlClassifier.Kind.RTSP))
        assertFalse(StreamMediaSourceFactory.supportsFully(StreamUrlClassifier.Kind.NDI))
        assertTrue(StreamMediaSourceFactory.supportsFully(StreamUrlClassifier.Kind.NDI_RTP))
    }

    @Test
    fun srtEndpointParsing() {
        assertEquals("10.0.0.5" to 9000, SrtDataSource.parseEndpoint("srt://10.0.0.5:9000"))
        assertEquals("live.example" to 9000, SrtDataSource.parseEndpoint("srt://live.example"))
        assertNull(SrtDataSource.parseEndpoint("rtmp://x/app"))
    }

    @Test
    fun classifierStillMapsSchemes() {
        assertEquals(StreamUrlClassifier.Kind.RTMP, StreamUrlClassifier.classify("rtmps://a/b"))
        assertEquals(StreamUrlClassifier.Kind.SRT, StreamUrlClassifier.classify("srt://a:9000"))
        assertNotNull(StreamUrlClassifier.classify("https://x/a.m3u8"))
    }
}
