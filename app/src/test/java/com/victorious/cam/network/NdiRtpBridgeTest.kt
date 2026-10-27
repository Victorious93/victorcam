package com.victorious.cam.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NdiRtpBridgeTest {
    @Test
    fun classifiesNdiFamily() {
        assertEquals(StreamUrlClassifier.Kind.NDI, StreamUrlClassifier.classify("ndi://Cam1"))
        assertEquals(StreamUrlClassifier.Kind.NDI_RTP, StreamUrlClassifier.classify("ndi+rtp://10.0.0.2:5004"))
        assertEquals(StreamUrlClassifier.Kind.NDI_RTP, StreamUrlClassifier.classify("rtp://10.0.0.2:5004"))
        assertTrue(StreamUrlClassifier.isPlayable(StreamUrlClassifier.Kind.NDI_RTP))
        assertFalse(StreamUrlClassifier.isPlayable(StreamUrlClassifier.Kind.NDI))
    }

    @Test
    fun resolvesRtpDirect() {
        val r = NdiRtpBridge.resolve("ndi+rtp://192.168.1.5:5004")
        assertNotNull(r)
        assertEquals("rtp://192.168.1.5:5004", r!!.playableUrl)
        assertFalse(r.viaGateway)
        assertTrue(r.certainty.contains("Medium"))
    }

    @Test
    fun resolvesViaGateway() {
        val gw = mapOf("Camera1" to "rtsp://192.168.1.20/cam")
        val r = NdiRtpBridge.resolve("ndi://Camera1", gw)
        assertNotNull(r)
        assertTrue(r!!.viaGateway)
        assertEquals("rtsp://192.168.1.20/cam", r.playableUrl)
    }

    @Test
    fun missingGatewayReturnsNull() {
        assertNull(NdiRtpBridge.resolve("ndi://Unknown"))
    }

    @Test
    fun capabilitySummaryMentionsProprietary() {
        assertTrue(NdiCapability.summary().contains("not bundled"))
    }
}
