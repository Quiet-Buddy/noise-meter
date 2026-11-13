package com.quietbuddy.audio

import com.quietbuddy.core.Character
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
 * Per-character alert sounds, synthesized in code (no audio assets, no network).
 *
 * Each [Character] owns a sound set (a recipe list per loud mood); calm moods are silent. The
 * `when (character)` selecting the set is exhaustive, so adding a character fails to compile until
 * it has sounds. Each loud mood has several variants so repeated reminders don't get boring.
 * Buddy's set is cartoonish:
 * - Worried: a "shhh", a questioning "hmm?", bubbly "bloop-bloop".
 * - Sad: sad trombone, falling slide whistle, "uh-oh".
 * - Ears covered: "ahooga" horn, siren whoops, "boing-boing".
 * The kitten's set is made of meows and cat noises (harmonic tones whose pitch rises then falls):
 * - Worried: a questioning "mrrp?" trill, a small rising "mew?", soft "prrt-prrt" chirps.
 * - Sad: a long falling "mewww", a whimpering "mew... mew", a sad descending meow.
 * - Too loud (startled, fur puffed up): a hiss, a vibrato yowl "mrrraaow", a startled double "mreow!".
 * The duck's set is made of quacks (buzzy odd-harmonic tones with a quick pitch drop, often in pairs):
 * - Worried: a questioning "quack?", a small "qua-ack?" with a rising end, soft "quack-quack".
 * - Sad: a long drooping "quaaaack", a whimpering "quack... quack", a quack that sags step by step.
 * - Too loud (flustered): three sharp "quack! quack! quack!", a raucous rasping "quaaaack", a startled double "QUACK! QUACK!".
 *
 * Timbres are rich in harmonics in the 1–4 kHz range, where phone speakers and ears are most
 * sensitive, and are gently saturated so they sound loud and cut through a noisy room.
 * Pure Kotlin so it can be unit-tested off-device.
 */
object AlertSounds {
    const val RATE = 44_100

    fun variantCount(character: Character, mood: Mood): Int = soundSet(character)[mood]?.size ?: 0

    /** 16-bit mono PCM at [RATE] for [character]'s [mood] [variant] (wrapped), or an empty array for calm moods. */
    fun render(character: Character, mood: Mood, variant: Int): ShortArray {
        val list = soundSet(character)[mood] ?: return ShortArray(0)
        val r = list[((variant % list.size) + list.size) % list.size]
        val buf = Buf(r.seconds)
        r.draw(buf)
        return buf.finish(r.gain, r.drive)
    }

    // No `else`: a new Character must get a sound set.
    private fun soundSet(character: Character): Map<Mood, List<Recipe>> = when (character) {
        Character.BUDDY -> buddySounds
        Character.KITTEN -> kittenSounds
        Character.DUCK -> duckSounds
        Character.ROBOT -> duckSounds // TODO(robot): placeholder until Astro's own sounds exist
    }

    private class Recipe(val seconds: Double, val gain: Double, val drive: Double, val draw: Buf.() -> Unit)

    private val buddySounds: Map<Mood, List<Recipe>> = mapOf(
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

    private val kittenSounds: Map<Mood, List<Recipe>> = mapOf(
        Mood.WORRIED to listOf(
            // "Mrrp?": a rolled trill that lifts at the end.
            Recipe(0.7, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.5, { t -> glide(520.0, 880.0, t / 0.5) }, harmonics = 8, attack = 0.04, release = 0.1,
                    vibratoHz = 28.0, vibratoDepth = 0.08)
            },
            // A small rising "mew?".
            Recipe(0.6, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.5, { t -> meow(t / 0.5, 560.0, 900.0, 800.0, peakAt = 0.7) }, harmonics = 8, attack = 0.06,
                    release = 0.12)
            },
            // "Prrt-prrt": two soft chirps.
            Recipe(0.6, gain = 0.75, drive = 1.5) {
                for (i in 0..1) {
                    tone(i * 0.27, 0.2, { t -> glide(600.0 + 120.0 * i, 900.0 + 150.0 * i, t / 0.2) }, harmonics = 7,
                        attack = 0.02, release = 0.06, vibratoHz = 30.0, vibratoDepth = 0.05)
                }
            },
        ),
        Mood.SAD to listOf(
            // A long falling "mewwww".
            Recipe(1.5, gain = 0.9, drive = 2.0) {
                tone(0.0, 1.4, { t -> meow(t / 1.4, 600.0, 850.0, 380.0, peakAt = 0.2) }, harmonics = 9, attack = 0.08,
                    release = 0.25, vibratoHz = 6.0, vibratoDepth = 0.02)
            },
            // A whimpering "mew... mew".
            Recipe(1.4, gain = 0.9, drive = 2.0) {
                for (i in 0..1) {
                    tone(i * 0.65, 0.55, { t -> meow(t / 0.55, 520.0 - 40.0 * i, 760.0 - 60.0 * i, 430.0 - 40.0 * i) },
                        harmonics = 8, attack = 0.07, release = 0.15, vibratoHz = 7.0, vibratoDepth = 0.025)
                }
            },
            // A sad meow that droops step by step.
            Recipe(1.2, gain = 0.9, drive = 2.0) {
                tone(0.0, 1.1, { t -> glide(780.0, 400.0, t / 1.0) * (1 + 0.05 * sin(2 * PI * 3 * t)) }, harmonics = 9,
                    attack = 0.1, release = 0.25, vibratoHz = 5.5, vibratoDepth = 0.02)
            },
        ),
        Mood.EARS_COVERED to listOf(
            // "Fssssst!": a hiss, with a sharp attack.
            Recipe(0.9, gain = 1.0, drive = 3.0) {
                noise(0.0, 0.85, lowHz = 1_500.0, highHz = 5_000.0, attack = 0.01, release = 0.4)
            },
            // "Mrrraaow": a wavering yowl.
            Recipe(1.4, gain = 1.0, drive = 3.0) {
                tone(0.0, 1.3, { t -> meow(t / 1.3, 500.0, 950.0, 450.0, peakAt = 0.4) }, harmonics = 12, attack = 0.05,
                    release = 0.2, vibratoHz = 8.0, vibratoDepth = 0.05)
                noise(0.0, 1.0, lowHz = 1_800.0, highHz = 4_000.0, attack = 0.1, release = 0.4)
            },
            // A startled double "mreow! mreow!".
            Recipe(1.1, gain = 1.0, drive = 3.0) {
                for (i in 0..1) {
                    tone(i * 0.5, 0.45, { t -> meow(t / 0.45, 600.0, 1_000.0 - 60.0 * i, 500.0, peakAt = 0.25) },
                        harmonics = 10, attack = 0.015, release = 0.1, vibratoHz = 9.0, vibratoDepth = 0.03)
                }
            },
        ),
    )

    private val duckSounds: Map<Mood, List<Recipe>> = mapOf(
        Mood.WORRIED to listOf(
            // "Quack?": a single soft quack that lifts at the start and droops a little.
            Recipe(0.7, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.4, { t -> meow(t / 0.4, 420.0, 560.0, 380.0, peakAt = 0.3) }, harmonics = 12, oddOnly = true,
                    attack = 0.03, release = 0.1, vibratoHz = 40.0, vibratoDepth = 0.03)
            },
            // "Qua-ack?": a small quack, then a questioning rise.
            Recipe(0.65, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.2, { t -> glide(480.0, 340.0, t / 0.2) }, harmonics = 12, oddOnly = true, attack = 0.02,
                    release = 0.06, vibratoHz = 45.0, vibratoDepth = 0.03)
                tone(0.25, 0.35, { t -> glide(360.0, 560.0, t / 0.3) }, harmonics = 10, oddOnly = true, attack = 0.03,
                    release = 0.1, vibratoHz = 40.0, vibratoDepth = 0.03)
            },
            // "Quack-quack": two soft little quacks.
            Recipe(0.6, gain = 0.75, drive = 1.5) {
                for (i in 0..1) {
                    tone(i * 0.27, 0.2, { t -> glide(520.0 + 60.0 * i, 360.0 + 40.0 * i, t / 0.2) }, harmonics = 11,
                        oddOnly = true, attack = 0.02, release = 0.06, vibratoHz = 45.0, vibratoDepth = 0.03)
                }
            },
        ),
        Mood.SAD to listOf(
            // A long, drooping "quaaaack".
            Recipe(1.5, gain = 0.9, drive = 2.0) {
                tone(0.0, 1.4, { t -> meow(t / 1.4, 440.0, 520.0, 220.0, peakAt = 0.15) }, harmonics = 14, oddOnly = true,
                    attack = 0.06, release = 0.25, vibratoHz = 35.0, vibratoDepth = 0.04)
            },
            // A whimpering "quack... quack".
            Recipe(1.4, gain = 0.9, drive = 2.0) {
                for (i in 0..1) {
                    tone(i * 0.65, 0.55, { t -> meow(t / 0.55, 450.0 - 40.0 * i, 520.0 - 50.0 * i, 250.0 - 30.0 * i, peakAt = 0.2) },
                        harmonics = 12, oddOnly = true, attack = 0.05, release = 0.15, vibratoHz = 38.0, vibratoDepth = 0.035)
                }
            },
            // A sad quack that sags step by step.
            Recipe(1.2, gain = 0.9, drive = 2.0) {
                tone(0.0, 1.1, { t -> glide(520.0, 230.0, t / 1.0) * (1 + 0.05 * sin(2 * PI * 3 * t)) }, harmonics = 13,
                    oddOnly = true, attack = 0.08, release = 0.25, vibratoHz = 36.0, vibratoDepth = 0.04)
            },
        ),
        Mood.EARS_COVERED to listOf(
            // "Quack! Quack! Quack!": three sharp, insistent quacks.
            Recipe(0.9, gain = 1.0, drive = 3.0) {
                for (i in 0..2) {
                    tone(i * 0.3, 0.22, { t -> glide(700.0, 380.0, t / 0.2) }, harmonics = 14, oddOnly = true,
                        attack = 0.01, release = 0.06, vibratoHz = 50.0, vibratoDepth = 0.03)
                }
            },
            // A raucous "quaaaack" with a hoarse rasp.
            Recipe(1.4, gain = 1.0, drive = 3.0) {
                tone(0.0, 1.3, { t -> meow(t / 1.3, 550.0, 750.0, 300.0, peakAt = 0.2) }, harmonics = 16, oddOnly = true,
                    attack = 0.03, release = 0.2, vibratoHz = 45.0, vibratoDepth = 0.05)
                noise(0.0, 1.0, lowHz = 1_500.0, highHz = 4_000.0, attack = 0.05, release = 0.4)
            },
            // A startled double "QUACK! QUACK!".
            Recipe(1.1, gain = 1.0, drive = 3.0) {
                for (i in 0..1) {
                    tone(i * 0.5, 0.45, { t -> meow(t / 0.45, 600.0, 800.0 - 60.0 * i, 330.0, peakAt = 0.15) },
                        harmonics = 14, oddOnly = true, attack = 0.012, release = 0.1, vibratoHz = 48.0, vibratoDepth = 0.04)
                }
            },
        ),
    )

    /** A meow's pitch contour as [x] goes 0..1: [from] Hz up to [peak] at [peakAt], then down to [to]. */
    private fun meow(x: Double, from: Double, peak: Double, to: Double, peakAt: Double = 0.35): Double =
        if (x < peakAt) glide(from, peak, x / peakAt) else glide(peak, to, (x - peakAt) / (1 - peakAt))

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
