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
 * Astro the robot's set is made of square-ish beeps and boops (odd harmonics, bright and buzzy):
 * - Worried: a questioning "beep-boop?" rising at the end, a soft "bip? bip?", a "hmm" warble of quick alternating tones.
 * - Sad: a slow falling "boop... boop... booop", a power-down slide with a slowing wobble, a sad three-note falling bleep.
 * - Too loud: a rapid alarm-ish "beep-beep-beep!", a frantic warbling bleep-bloop run, a glitchy stutter of short beeps.
 *
 * Snowy the snowman's set is wind chimes and snow (bell-like decaying tones, puffs of wind, crunchy steps):
 * - Worried: a questioning two-note chime, a "fff?" puff then a rising chime, three soft crunchy steps.
 * - Sad: three falling chimes, a cold wind sighing downwards, two "pff" puffs and a sagging note.
 * - Too loud: a frantic jingle of chimes, heavy stomps in the snow, a blustery gust with bell clangs.
 *
 * Mari Domingi's set is Basque folk (txalaparta plank knocks, pandero jingles, a soft txistu flute, a little bell):
 * - Worried: a hesitant two-note txistu phrase, two uneven plank knocks then a questioning note, a soft pandero shake and a bell.
 * - Sad: a slow falling txistu phrase, a lone bell and a sagging flute note, two slow knocks and a drooping flute.
 * - Too loud: fast urgent txalaparta knocks with jingles, a rolling knock run ending in a bell, a jingle-topped flurry of knocks.
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
        Character.ROBOT -> robotSounds
        Character.SNOWMAN -> snowmanSounds
        Character.MARI_DOMINGI -> snowmanSounds // TODO(mari)
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

    private val snowmanSounds: Map<Mood, List<Recipe>> = mapOf(
        Mood.WORRIED to listOf(
            // A questioning two-note wind chime.
            Recipe(0.9, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.5, { 1_568.0 }, harmonics = 4, decay = 5.0, attack = 0.004, release = 0.1)
                tone(0.25, 0.6, { 1_976.0 }, harmonics = 4, decay = 5.0, attack = 0.004, release = 0.1)
            },
            // A "fff?" puff of wind, then a small rising chime.
            Recipe(0.9, gain = 0.75, drive = 1.5) {
                noise(0.0, 0.45, lowHz = 800.0, highHz = 4_500.0, attack = 0.12, release = 0.25)
                tone(0.5, 0.35, { t -> glide(1_175.0, 1_568.0, t / 0.25) }, harmonics = 4, decay = 4.0, release = 0.1)
            },
            // Three soft crunchy steps in the snow.
            Recipe(0.7, gain = 0.75, drive = 1.5) {
                for (i in 0..2) noise(i * 0.2, 0.12, lowHz = 1_500.0, highHz = 7_000.0, attack = 0.004, release = 0.08)
            },
        ),
        Mood.SAD to listOf(
            // Three falling chimes.
            Recipe(1.7, gain = 0.9, drive = 2.0) {
                val hz = doubleArrayOf(1_319.0, 1_047.0, 784.0)
                hz.forEachIndexed { i, f ->
                    tone(i * 0.45, 0.8, { f }, harmonics = 4, decay = 3.5, attack = 0.005, release = 0.15)
                }
            },
            // A cold wind sighing downwards.
            Recipe(1.4, gain = 0.9, drive = 2.0) {
                noise(0.0, 1.3, lowHz = 300.0, highHz = 2_200.0, attack = 0.3, release = 0.6)
                tone(0.1, 1.2, { t -> glide(700.0, 330.0, t / 1.1) }, harmonics = 3, attack = 0.2, release = 0.4,
                    vibratoHz = 5.0, vibratoDepth = 0.02)
            },
            // "Pff... pff...", then a sagging note.
            Recipe(1.3, gain = 0.9, drive = 2.0) {
                noise(0.0, 0.25, lowHz = 600.0, highHz = 3_500.0, attack = 0.05, release = 0.15)
                noise(0.4, 0.25, lowHz = 600.0, highHz = 3_500.0, attack = 0.05, release = 0.15)
                tone(0.8, 0.5, { t -> glide(880.0, 520.0, t / 0.45) }, harmonics = 4, decay = 2.5, release = 0.15)
            },
        ),
        Mood.EARS_COVERED to listOf(
            // A frantic jingle of chimes.
            Recipe(1.2, gain = 1.0, drive = 3.0) {
                for (i in 0 until 8) {
                    tone(i * 0.12, 0.3, { if (i % 2 == 0) 2_093.0 else 1_760.0 }, harmonics = 5, decay = 6.0,
                        attack = 0.003, release = 0.05)
                }
            },
            // Heavy stomps crunching in the snow.
            Recipe(1.1, gain = 1.0, drive = 3.0) {
                for (i in 0 until 5) {
                    noise(i * 0.2, 0.16, lowHz = 1_200.0, highHz = 7_500.0, attack = 0.003, release = 0.1)
                    tone(i * 0.2, 0.15, { t -> glide(220.0, 110.0, t / 0.12) }, harmonics = 3, attack = 0.004,
                        release = 0.08)
                }
            },
            // A blustery gust with two bell clangs.
            Recipe(1.2, gain = 1.0, drive = 3.0) {
                noise(0.0, 1.1, lowHz = 500.0, highHz = 6_000.0, attack = 0.2, release = 0.4)
                for (i in 0..1) {
                    tone(0.3 + i * 0.45, 0.6, { 1_480.0 }, harmonics = 6, oddOnly = true, decay = 4.0, attack = 0.003,
                        release = 0.1)
                }
            },
        ),
    )

    private val robotSounds: Map<Mood, List<Recipe>> = mapOf(
        Mood.WORRIED to listOf(
            // "Beep-boop?": two notes, the last one rising into a question.
            Recipe(0.8, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.18, { 880.0 }, harmonics = 9, oddOnly = true, attack = 0.008, release = 0.04)
                tone(0.22, 0.18, { 660.0 }, harmonics = 9, oddOnly = true, attack = 0.008, release = 0.04)
                tone(0.45, 0.32, { t -> glide(660.0, 1_100.0, t / 0.25) }, harmonics = 9, oddOnly = true, attack = 0.01,
                    release = 0.08)
            },
            // "Bip? bip?": a soft pair of short blips.
            Recipe(0.65, gain = 0.75, drive = 1.5) {
                for (i in 0..1) {
                    tone(i * 0.3, 0.16, { t -> glide(900.0 + 150.0 * i, 1_200.0 + 200.0 * i, t / 0.16) }, harmonics = 8,
                        oddOnly = true, attack = 0.008, release = 0.05)
                }
            },
            // "Hmm": a little warble of quick alternating tones.
            Recipe(0.7, gain = 0.75, drive = 1.5) {
                for (i in 0 until 6) {
                    tone(i * 0.09, 0.08, { if (i % 2 == 0) 740.0 else 988.0 }, harmonics = 9, oddOnly = true,
                        attack = 0.006, release = 0.02)
                }
                tone(0.58, 0.1, { 1_100.0 }, harmonics = 8, oddOnly = true, attack = 0.008, release = 0.04)
            },
        ),
        Mood.SAD to listOf(
            // "Boop... boop... booop": slow, each lower and longer.
            Recipe(1.7, gain = 0.9, drive = 2.0) {
                val hz = doubleArrayOf(660.0, 560.0, 470.0)
                val len = doubleArrayOf(0.25, 0.3, 0.6)
                val at = doubleArrayOf(0.0, 0.5, 1.05)
                for (i in 0..2) {
                    tone(at[i], len[i], { t -> hz[i] * (1 - 0.04 * t / len[i]) }, harmonics = 9, oddOnly = true,
                        attack = 0.01, release = 0.08)
                }
            },
            // Power-down: a falling slide whose wobble slows.
            Recipe(1.5, gain = 0.9, drive = 2.0) {
                tone(0.0, 1.4, { t ->
                    val x = t / 1.4
                    glide(1_200.0, 180.0, x * x) * (1 + 0.06 * sin(2 * PI * (14.0 * t - 5.0 * t * t)))
                }, harmonics = 9, oddOnly = true, attack = 0.02, release = 0.3)
            },
            // A sad three-note falling bleep.
            Recipe(1.2, gain = 0.9, drive = 2.0) {
                val hz = doubleArrayOf(1_047.0, 831.0, 622.0)
                for (i in 0..2) {
                    tone(i * 0.36, 0.3, { hz[i] }, harmonics = 8, oddOnly = true, attack = 0.01,
                        release = 0.1, vibratoHz = 6.0, vibratoDepth = 0.015)
                }
            },
        ),
        Mood.EARS_COVERED to listOf(
            // "Beep-beep-beep!": a rapid, bright alarm.
            Recipe(0.85, gain = 1.0, drive = 3.0) {
                for (i in 0..2) {
                    tone(i * 0.26, 0.18, { 1_400.0 }, harmonics = 7, oddOnly = true, attack = 0.006, release = 0.03)
                }
            },
            // A frantic warbling bleep-bloop run.
            Recipe(1.2, gain = 1.0, drive = 3.0) {
                for (i in 0 until 8) {
                    val up = i % 2 == 0
                    tone(i * 0.13, 0.12, { t -> if (up) glide(700.0, 1_500.0, t / 0.1) else glide(1_500.0, 600.0, t / 0.1) },
                        harmonics = 8, oddOnly = true, attack = 0.006, release = 0.03)
                }
            },
            // A glitchy stutter of short beeps.
            Recipe(1.1, gain = 1.0, drive = 3.0) {
                val at = doubleArrayOf(0.0, 0.07, 0.14, 0.31, 0.38, 0.55, 0.60, 0.65, 0.8)
                val hz = doubleArrayOf(1_200.0, 1_200.0, 1_800.0, 900.0, 900.0, 1_500.0, 2_000.0, 1_100.0, 1_300.0)
                for (i in at.indices) {
                    tone(at[i], if (i == at.lastIndex) 0.22 else 0.05, { hz[i] }, harmonics = 7, oddOnly = true,
                        attack = 0.004, release = 0.02)
                }
            },
        ),
    )

    private val mariSounds: Map<Mood, List<Recipe>> = mapOf(
        Mood.WORRIED to listOf(
            // A hesitant two-note txistu phrase: a note, a pause, a higher unsure note.
            Recipe(0.9, gain = 0.75, drive = 1.5) {
                tone(0.0, 0.25, { 880.0 }, harmonics = 3, attack = 0.04, release = 0.08, vibratoHz = 5.5, vibratoDepth = 0.01)
                tone(0.4, 0.45, { t -> glide(988.0, 1_047.0, t / 0.4) }, harmonics = 3, attack = 0.05, release = 0.15,
                    vibratoHz = 6.0, vibratoDepth = 0.015)
            },
            // Two uneven txalaparta knocks, then a questioning flute note.
            Recipe(0.9, gain = 0.75, drive = 1.5) {
                knock(0.0, 330.0)
                knock(0.17, 262.0)
                tone(0.5, 0.35, { t -> glide(784.0, 988.0, t / 0.3) }, harmonics = 3, attack = 0.04, release = 0.12,
                    vibratoHz = 6.0, vibratoDepth = 0.012)
            },
            // A soft pandero shake, then a small bell.
            Recipe(0.8, gain = 0.75, drive = 1.5) {
                for (i in 0..2) jingle(i * 0.07, 0.12)
                tone(0.4, 0.4, { 1_568.0 }, harmonics = 4, decay = 5.0, attack = 0.004, release = 0.1)
            },
        ),
        Mood.SAD to listOf(
            // A slow falling txistu phrase.
            Recipe(1.7, gain = 0.9, drive = 2.0) {
                val hz = doubleArrayOf(988.0, 784.0, 659.0)
                val len = doubleArrayOf(0.4, 0.4, 0.7)
                for (i in 0..2) {
                    tone(i * 0.5, len[i], { t -> hz[i] * (1 - 0.04 * t / len[i]) }, harmonics = 3, attack = 0.06,
                        release = 0.15, vibratoHz = 5.5, vibratoDepth = 0.02)
                }
            },
            // A lone bell, then a sagging flute note.
            Recipe(1.4, gain = 0.9, drive = 2.0) {
                tone(0.0, 0.8, { 1_319.0 }, harmonics = 4, decay = 3.5, attack = 0.005, release = 0.15)
                tone(0.55, 0.8, { t -> glide(880.0, 587.0, t / 0.7) }, harmonics = 3, attack = 0.08, release = 0.2,
                    vibratoHz = 5.5, vibratoDepth = 0.02)
            },
            // Two slow tired knocks and a drooping flute.
            Recipe(1.3, gain = 0.9, drive = 2.0) {
                knock(0.0, 247.0)
                knock(0.45, 196.0)
                tone(0.85, 0.45, { t -> glide(740.0, 494.0, t / 0.4) }, harmonics = 3, attack = 0.06, release = 0.15,
                    vibratoHz = 5.5, vibratoDepth = 0.02)
            },
        ),
        Mood.EARS_COVERED to listOf(
            // Fast urgent txalaparta knocks with jingles.
            Recipe(1.2, gain = 1.0, drive = 3.0) {
                for (i in 0 until 8) {
                    knock(i * 0.13, if (i % 2 == 0) 330.0 else 247.0)
                    if (i % 2 == 0) jingle(i * 0.13, 0.1)
                }
            },
            // A rolling run of knocks ending in a bright bell.
            Recipe(1.2, gain = 1.0, drive = 3.0) {
                for (i in 0 until 6) knock(i * 0.1, 220.0 + 25.0 * i)
                for (i in 0..1) tone(0.65 + i * 0.25, 0.45, { 1_760.0 }, harmonics = 5, oddOnly = true, decay = 4.0,
                    attack = 0.003, release = 0.08)
            },
            // A flurry of paired knocks with pandero jingles on top.
            Recipe(1.1, gain = 1.0, drive = 3.0) {
                for (i in 0 until 4) {
                    knock(i * 0.24, 300.0)
                    knock(i * 0.24 + 0.09, 240.0)
                    jingle(i * 0.24 + 0.12, 0.1)
                }
            },
        ),
    )

    /** A txalaparta plank knock: a dry woody thump with a short click, at about [hz]. */
    private fun Buf.knock(at: Double, hz: Double) {
        tone(at, 0.14, { t -> hz * (1 - 0.25 * min(1.0, t / 0.05)) }, harmonics = 4, decay = 22.0, attack = 0.002,
            release = 0.04)
        noise(at, 0.03, lowHz = 1_000.0, highHz = 4_500.0, attack = 0.001, release = 0.02)
    }

    /** A pandero jingle: a short burst of bright metallic noise and a high ring. */
    private fun Buf.jingle(at: Double, dur: Double) {
        noise(at, dur, lowHz = 4_000.0, highHz = 9_000.0, attack = 0.003, release = dur * 0.7)
        tone(at, dur, { 3_136.0 }, harmonics = 2, decay = 20.0, attack = 0.002, release = 0.04)
    }

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
