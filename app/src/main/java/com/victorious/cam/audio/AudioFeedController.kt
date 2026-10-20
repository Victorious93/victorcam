package com.victorious.cam.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Virtual mic / audio feed paired with the video pipeline.
 *
 * **Stock Android limits:** apps cannot register as a system-wide microphone
 * replacement without privileged/role APIs or root. This controller:
 * - Plays audio from the selected video/stream (ExoPlayer audio track)
 * - Or plays a user audio file / generates silence via [AudioTrack]
 * - Syncs with video when sharing the same ExoPlayer from [FeedPlayer]
 *
 * **Root / LSPosed:** injecting into remote [AudioRecord] is out of this class;
 * see README. MediaProjection audio for screen capture is optional via
 * [enableProjectionAudioFlag] (caller wires AudioPlaybackCapture on API 29+).
 */
@UnstableApi
class AudioFeedController(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var silentTrack: AudioTrack? = null
    private val active = AtomicBoolean(false)
    private var mode: Mode = Mode.FROM_VIDEO
    var enableProjectionAudioFlag: Boolean = false
        private set

    enum class Mode { FROM_VIDEO, USER_FILE, SILENT }

    fun mode(): Mode = mode

    fun playFromVideoPlayer(shared: ExoPlayer?) {
        stop()
        mode = Mode.FROM_VIDEO
        if (shared != null) {
            shared.volume = 1f
            player = shared
            active.set(true)
        } else {
            active.set(false)
        }
    }

    fun playUserFile(uri: Uri) {
        stop()
        mode = Mode.USER_FILE
        val exo = ExoPlayer.Builder(appContext).build().also {
            it.setMediaItem(MediaItem.fromUri(uri))
            it.repeatMode = Player.REPEAT_MODE_ONE
            it.prepare()
            it.playWhenReady = true
        }
        player = exo
        active.set(true)
    }

    fun playSilent(sampleRate: Int = 44100) {
        stop()
        mode = Mode.SILENT
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        silentTrack = track
        track.play()
        Thread {
            val zeros = ByteArray(minBuf)
            while (active.get() && mode == Mode.SILENT) {
                track.write(zeros, 0, zeros.size)
            }
        }.also { it.isDaemon = true; it.start() }
        active.set(true)
    }

    fun setProjectionAudioEnabled(enabled: Boolean) {
        enableProjectionAudioFlag = enabled
    }

    fun isActive(): Boolean = active.get()

    fun stop() {
        active.set(false)
        // Do not release shared video player
        if (mode != Mode.FROM_VIDEO) {
            player?.release()
        }
        player = null
        silentTrack?.stop()
        silentTrack?.release()
        silentTrack = null
    }
}

/**
 * Documents / computes whether virtual-mic injection is available.
 */
object VirtualMicCapability {
    enum class Level { PREVIEW_ONLY, CAPTURE_INTENT_AUDIO, SYSTEM_MIC_REPLACE }

    fun stockLevel(): Level = Level.PREVIEW_ONLY

    fun withRootHook(): Level = Level.SYSTEM_MIC_REPLACE

    fun description(level: Level): String = when (level) {
        Level.PREVIEW_ONLY ->
            "Stock: AudioTrack/ExoPlayer playback for preview + attach audio to our capture intents only."
        Level.CAPTURE_INTENT_AUDIO ->
            "Can mux audio into IMAGE/VIDEO capture results we produce."
        Level.SYSTEM_MIC_REPLACE ->
            "Root/LSPosed required to appear as the system microphone to other apps."
    }
}
