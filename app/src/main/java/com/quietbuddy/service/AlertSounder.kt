package com.quietbuddy.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.quietbuddy.core.Mood
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Plays short synthesized alert tones that get more insistent with the noise level.
 *
 * Uses the media stream (USAGE_MEDIA), which follows the media volume and still plays when the phone
 * is on vibrate/silent, unlike notification sounds.
 */
class AlertSounder {
    private data class Note(val hz: Double, val ms: Int, val gapMs: Int = 0)
    private data class Pattern(val notes: List<Note>, val gain: Float)

    private val tracks = HashMap<Mood, AudioTrack>()
    private val durations = HashMap<Mood, Long>()

    /** Starts the tone for [mood]; returns its length in ms (0 if nothing played). Non-blocking. */
    fun play(mood: Mood): Long {
        val p = pattern(mood) ?: return 0
        return runCatching {
            val track = tracks.getOrPut(mood) { build(mood, p) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
            durations[mood] ?: 0L
        }.getOrDefault(0L)
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun pattern(mood: Mood): Pattern? = when (mood) {
        // One soft low note.
        Mood.WORRIED -> Pattern(listOf(Note(E5, 220)), gain = 0.35f)
        // Two descending notes: a clear "uh-oh".
        Mood.SAD -> Pattern(listOf(Note(A5, 180, gapMs = 60), Note(E5, 260)), gain = 0.6f)
        // Three quick high notes, loudest.
        Mood.EARS_COVERED -> Pattern(
            listOf(Note(C6, 140, gapMs = 70), Note(C6, 140, gapMs = 70), Note(C6, 220)),
            gain = 0.9f,
        )
        else -> null
    }

    private fun build(mood: Mood, p: Pattern): AudioTrack {
        val pcm = synth(p)
        durations[mood] = pcm.size * 1000L / RATE
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun synth(p: Pattern): ShortArray {
        val total = p.notes.sumOf { (it.ms + it.gapMs) * RATE / 1000 }
        val out = ShortArray(total)
        var pos = 0
        val fade = RATE * 12 / 1000 // 12 ms attack/release: no clicks
        for (n in p.notes) {
            val len = n.ms * RATE / 1000
            for (i in 0 until len) {
                val env = min(1.0, min(i, len - 1 - i).toDouble() / fade)
                // Fundamental plus a soft octave for a bell-like, more audible tone on small speakers.
                val t = i.toDouble() / RATE
                val s = 0.8 * sin(2 * PI * n.hz * t) + 0.2 * sin(4 * PI * n.hz * t)
                out[pos + i] = (s * env * p.gain * Short.MAX_VALUE).toInt().toShort()
            }
            pos += len + n.gapMs * RATE / 1000
        }
        return out
    }

    private companion object {
        const val RATE = 44_100
        const val E5 = 659.25
        const val A5 = 880.0
        const val C6 = 1046.5
    }
}
