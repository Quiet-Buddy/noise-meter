package com.quietbuddy.audio

import com.quietbuddy.core.Mood
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Buddy's cartoon alert sounds, synthesized in code (no audio assets, no network).
 *
 * Each loud mood has several playful variants so repeated reminders don't get boring:
 * - Worried: a "shhh", a questioning "hmm?", bubbly "bloop-bloop".
 * - Sad: sad trombone, falling slide whistle, "uh-oh".
 * - Ears covered: "ahooga" horn, siren whoops, "boing-boing".
 *
 * Timbres are rich in harmonics in the 1–4 kHz range, where phone speakers and ears are most
 * sensitive, and are gently saturated so they sound loud and cut through a noisy room.
 * Pure Kotlin so it can be unit-tested off-device.
 */
object AlertSounds {
    const val RATE = 44_100

    fun variantCount(mood: Mood): Int = recipes[mood]?.size ?: 0

    /** 16-bit mono PCM at [RATE] for [mood]'s [variant] (wrapped), or an empty array for calm moods. */
    fun render(mood: Mood, variant: Int): ShortArray {
        val list = recipes[mood] ?: return ShortArray(0)
        val r = list[((variant % list.size) + list.size) % list.size]
        val buf = Buf(r.seconds)
        r.draw(buf)
        return buf.finish(r.gain, r.drive)
    }

    private class Recipe(val seconds: Double, val gain: Double, val drive: Double, val draw: Buf.() -> Unit)

    private val recipes: Map<Mood, List<Recipe>> = mapOf(
        Mood.WORRIED to listOf(
            // "Shhhhh": Buddy shushes.
            Recipe(1.0, gain = 0.75, drive = 1.5) {
                noise(0.0, 0.95, lowHz = 2_500.0, highHz = 8_000.0, attack = 0.15, release = 0.4)
            },
            // "Hmm?": a note, then a questioning rise.
            Recipe(0.65, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.2, { 587.3 }, harmonics = 5)
                tone(0.22, 0.4, { t -> glide(587.3, 880.0, t / 0.3) }, harmonics = 5, vibratoHz = 6.0, vibratoDepth = 0.01)
            },
            // "Bloop-bloop": two bubbly chirps.
            Recipe(0.5, gain = 0.75, drive = 1.5) {
                for (i in 0..1) {
                    tone(i * 0.2, 0.16, { t -> 450.0 * 2.0.pow(1.6 * (1 - exp(-t * 25))) }, harmonics = 3, decay = 8.0)
                }
            },
        ),
        Mood.SAD to listOf(
            // Sad trombone: "wah wah wah waaaah".
            Recipe(1.8, gain = 0.9, drive = 2.0) {
                val notes = doubleArrayOf(493.9, 466.2, 440.0)
                notes.forEachIndexed { i, hz ->
                    tone(i * 0.3, 0.27, { t -> hz * (1 - 0.03 * t / 0.27) }, harmonics = 9, attack = 0.03)
                }
                tone(0.9, 0.85, { 415.3 }, harmonics = 9, attack = 0.03, release = 0.25, vibratoHz = 5.5, vibratoDepth = 0.035)
            },
            // Slide whistle falling down.
            Recipe(1.05, gain = 0.9, drive = 2.0) {
                tone(0.0, 1.0, { t -> glide(1_900.0, 480.0, t / 0.95) }, harmonics = 2, release = 0.15,
                    vibratoHz = 7.0, vibratoDepth = 0.012)
            },
            // "Uh-oh".
            Recipe(0.75, gain = 0.9, drive = 2.0) {
                tone(0.0, 0.22, { 740.0 }, harmonics = 7, oddOnly = true)
                tone(0.3, 0.42, { t -> 554.4 * (1 - 0.05 * t / 0.42) }, harmonics = 7, oddOnly = true, release = 0.12)
            },
        ),
        Mood.EARS_COVERED to listOf(
            // "Ahooga! Ahooga!" cartoon horn.
            Recipe(1.3, gain = 1.0, drive = 3.0) {
                for (i in 0..1) {
                    tone(i * 0.65, 0.55, { t -> glide(330.0, 660.0, t / 0.3) }, harmonics = 13, oddOnly = true,
                        attack = 0.02, release = 0.08)
                }
            },
            // Three siren whoops.
            Recipe(1.15, gain = 1.0, drive = 3.0) {
                for (i in 0..2) {
                    tone(i * 0.38, 0.34, { t -> glide(650.0, 1_600.0, t / 0.34) }, harmonics = 6, release = 0.06)
                }
            },
            // "Boing! Boing!"
            Recipe(1.2, gain = 1.0, drive = 3.0) {
                for (i in 0..1) {
                    val base = if (i == 0) 260.0 else 340.0
                    tone(i * 0.55, 0.6, { t ->
                        base * 2.0.pow(1.3 * (1 - exp(-t * 10))) * (1 + 0.18 * exp(-t * 5) * sin(2 * PI * 13 * t))
                    }, harmonics = 8, decay = 4.0)
                }
            },
        ),
    )

    /** Exponential (musical) glide from [from] to [to] Hz as [x] goes 0..1, then holds. */
    private fun glide(from: Double, to: Double, x: Double): Double =
        from * (to / from).pow(x.coerceIn(0.0, 1.0))

    private class Buf(seconds: Double) {
        val data = FloatArray((seconds * RATE).toInt())
        private val rng = Random(7)

        fun tone(
            startS: Double,
            durS: Double,
            freq: (Double) -> Double,
            harmonics: Int,
            oddOnly: Boolean = false,
            attack: Double = 0.01,
            release: Double = 0.05,
            decay: Double = 0.0,
            vibratoHz: Double = 0.0,
            vibratoDepth: Double = 0.0,
        ) {
            val start = (startS * RATE).toInt()
            val len = min((durS * RATE).toInt(), data.size - start)
            var phase = 0.0
            for (i in 0 until len) {
                val t = i.toDouble() / RATE
                val f = freq(t) * (1 + vibratoDepth * sin(2 * PI * vibratoHz * t))
                phase += 2 * PI * f / RATE
                var s = 0.0
                var k = 1
                var n = 0
                while (n < harmonics && k * f < MAX_PARTIAL_HZ) {
                    s += sin(k * phase) / k
                    k += if (oddOnly) 2 else 1
                    n++
                }
                data[start + i] += (s * envelope(t, durS, attack, release, decay)).toFloat()
            }
        }

        /** Band-limited noise (two one-pole filters), e.g. a "shhh". */
        fun noise(startS: Double, durS: Double, lowHz: Double, highHz: Double, attack: Double, release: Double) {
            val start = (startS * RATE).toInt()
            val len = min((durS * RATE).toInt(), data.size - start)
            val aHp = exp(-2 * PI * lowHz / RATE)
            val aLp = exp(-2 * PI * highHz / RATE)
            var lp = 0.0
            var hpIn = 0.0
            var hp = 0.0
            for (i in 0 until len) {
                val x = rng.nextDouble(-1.0, 1.0)
                lp = (1 - aLp) * x + aLp * lp
                hp = aHp * (hp + lp - hpIn)
                hpIn = lp
                val t = i.toDouble() / RATE
                data[start + i] += (hp * envelope(t, durS, attack, release, 0.0)).toFloat()
            }
        }

        private fun envelope(t: Double, dur: Double, attack: Double, release: Double, decay: Double): Double {
            val a = min(1.0, t / attack)
            val r = min(1.0, max(0.0, (dur - t) / release))
            return a * r * exp(-decay * t)
        }

        /** Peak-normalize, soft-saturate for loudness, scale to [gain] and convert to 16-bit. */
        fun finish(gain: Double, drive: Double): ShortArray {
            var peak = 0f
            for (v in data) peak = max(peak, abs(v))
            if (peak == 0f) return ShortArray(data.size)
            val norm = tanh(drive)
            return ShortArray(data.size) { i ->
                val y = tanh(drive * data[i] / peak) / norm
                (y * gain * Short.MAX_VALUE).toInt().coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
    }

    /** Keep partials well below Nyquist and in the range small speakers reproduce. */
    private const val MAX_PARTIAL_HZ = 9_000.0
}
