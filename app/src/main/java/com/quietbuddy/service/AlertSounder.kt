package com.quietbuddy.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.quietbuddy.audio.AlertSounds
import com.quietbuddy.core.Mood
import kotlin.random.Random

/**
 * Plays Buddy's cartoon alert sounds ([AlertSounds]), rotating through each mood's variants so
 * repeated reminders stay fun.
 *
 * Uses the media stream (USAGE_MEDIA), which follows the media volume and still plays when the phone
 * is on vibrate/silent, unlike notification sounds.
 */
class AlertSounder {
    private val tracks = HashMap<Pair<Mood, Int>, AudioTrack>()
    private val durations = HashMap<Pair<Mood, Int>, Long>()
    // Next variant per mood; start at a random one so each session doesn't open with the same sound.
    private val next = HashMap<Mood, Int>()
    private var current: AudioTrack? = null

    /** Starts the next sound for [mood]; returns its length in ms (0 if nothing played). Non-blocking. */
    fun play(mood: Mood): Long {
        val count = AlertSounds.variantCount(mood)
        if (count == 0) return 0
        val variant = next.getOrPut(mood) { Random.nextInt(count) }
        next[mood] = (variant + 1) % count
        val key = mood to variant
        return runCatching {
            current?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop() }
            val track = tracks.getOrPut(key) { build(key) }
            track.reloadStaticData()
            track.play()
            current = track
            durations[key] ?: 0L
        }.getOrDefault(0L)
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
        current = null
    }

    private fun build(key: Pair<Mood, Int>): AudioTrack {
        val pcm = AlertSounds.render(key.first, key.second)
        durations[key] = pcm.size * 1000L / AlertSounds.RATE
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
                    .setSampleRate(AlertSounds.RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }
}
