package com.quietbuddy.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Wraps AudioRecord: mono 16-bit PCM, 100 ms frames, blocking read on a dedicated thread.
 * Audio is only passed to [onFrame]; it is never stored or logged.
 */
class AudioCapture(
    private val context: Context,
    private val onFrame: (ShortArray, Int, Long) -> Unit,
    private val onFailure: () -> Unit,
    private val onSilenced: (Boolean) -> Unit,
) {
    @Volatile private var running = false
    private var thread: Thread? = null
    private var record: AudioRecord? = null
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)

    var sampleRate: Int = 0
        private set

    /** Creates the AudioRecord; returns the sample rate, or null on failure. Call before [start]. */
    @SuppressLint("MissingPermission")
    fun prepare(): Int? {
        val unprocessed = audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val source = if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        for (rate in intArrayOf(48_000, 44_100)) {
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val size = maxOf(min, rate / 10 * 2 * 4)
            val r = try {
                AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            } catch (_: Exception) { null }
            if (r != null && r.state == AudioRecord.STATE_INITIALIZED) {
                record = r
                sampleRate = rate
                return rate
            }
            r?.release()
        }
        return null
    }

    fun start() {
        val r = record ?: run { onFailure(); return }
        running = true
        try {
            r.startRecording()
        } catch (_: Exception) {
            running = false; onFailure(); return
        }
        registerSilenceCallback(r)
        thread = Thread({
            val frame = ShortArray(sampleRate / 10)
            while (running) {
                val n = r.read(frame, 0, frame.size)
                if (!running) break
                if (n > 0) {
                    onFrame(frame, n, System.currentTimeMillis())
                } else if (n < 0) {
                    running = false
                    onFailure()
                    break
                }
            }
        }, "qb-audio").also { it.start() }
    }

    private fun registerSilenceCallback(r: AudioRecord) {
        val cb = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                val mine = configs.firstOrNull { it.clientAudioSessionId == r.audioSessionId } ?: return
                onSilenced(mine.isClientSilenced)
            }
        }
        recordingCallback = cb
        audioManager.registerAudioRecordingCallback(cb, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        running = false
        recordingCallback?.let { audioManager.unregisterAudioRecordingCallback(it) }
        recordingCallback = null
        try { record?.stop() } catch (_: Exception) {}
        thread?.let { if (it !== Thread.currentThread()) it.join(500) }
        thread = null
        record?.release()
        record = null
    }
}
