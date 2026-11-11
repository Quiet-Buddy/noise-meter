package com.quietbuddy.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * IEC 61672 A-weighting as a cascade of three biquads (Direct Form II transposed),
 * derived with the plain bilinear transform from the analog poles
 * (20.6 Hz x2, 107.7 Hz, 737.9 Hz, 12194 Hz x2; four zeros at 0), normalised to 0 dB at 1 kHz.
 *
 * Accuracy note: the bilinear transform warps frequency, so the response error grows towards
 * Nyquist. The 12194 Hz poles sit close to Nyquist at 44.1 kHz (22.05 kHz), where the error
 * above ~8 kHz can reach several dB (the digital response falls faster than the analog one).
 * At 48 kHz it is somewhat smaller. Below 4 kHz, where speech and household noise live, the
 * error is well under 0.5 dB.
 */
class AWeightingFilter(sampleRate: Int) {
    private val sections: Array<Biquad>

    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        val fs = sampleRate.toDouble()
        val c = 2.0 * fs
        fun w(f: Double) = 2.0 * PI * f
        val w1 = w(20.598997)
        val w2 = w(107.65265)
        val w3 = w(737.86223)
        val w4 = w(12194.217)

        // s^2 / (s + w1)^2
        val s1 = design(c, 1.0, 0.0, 0.0, 2 * w1, w1 * w1)
        // s^2 / ((s + w2)(s + w3))
        val s2 = design(c, 1.0, 0.0, 0.0, w2 + w3, w2 * w3)
        // 1 / (s + w4)^2  (the analog gain constant is absorbed by the 1 kHz normalisation)
        val s3 = design(c, 0.0, 0.0, 1.0, 2 * w4, w4 * w4)

        // Normalise total gain to 0 dB at 1 kHz.
        val gain = magnitude(listOf(s1, s2, s3), 1000.0, fs)
        val g = 1.0 / gain
        s1.b0 *= g; s1.b1 *= g; s1.b2 *= g
        sections = arrayOf(s1, s2, s3)
    }

    /** Reads 16-bit PCM, writes A-weighted samples (input scaled by 1/32768) into [out]. */
    fun process(input: ShortArray, count: Int, out: DoubleArray) {
        for (i in 0 until count) {
            var x = input[i] / 32768.0
            for (s in sections) x = s.step(x)
            out[i] = x
        }
    }

    fun reset() {
        for (s in sections) s.reset()
    }

    private class Biquad(var b0: Double, var b1: Double, var b2: Double, val a1: Double, val a2: Double) {
        private var z1 = 0.0
        private var z2 = 0.0

        fun step(x: Double): Double {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }

        fun reset() {
            z1 = 0.0
            z2 = 0.0
        }
    }

    private companion object {
        /** Bilinear transform of (n2 s^2 + n1 s + n0) / (s^2 + a1 s + a0) with s = c (1 - z^-1)/(1 + z^-1). */
        fun design(c: Double, n2: Double, n1: Double, n0: Double, a1: Double, a0: Double): Biquad {
            val c2 = c * c
            val b0 = n2 * c2 + n1 * c + n0
            val b1 = -2.0 * n2 * c2 + 2.0 * n0
            val b2 = n2 * c2 - n1 * c + n0
            val d0 = c2 + a1 * c + a0
            val d1 = -2.0 * c2 + 2.0 * a0
            val d2 = c2 - a1 * c + a0
            return Biquad(b0 / d0, b1 / d0, b2 / d0, d1 / d0, d2 / d0)
        }

        fun magnitude(list: List<Biquad>, f: Double, fs: Double): Double {
            val w = 2.0 * PI * f / fs
            val c1 = cos(w); val s1 = sin(w)
            val c2 = cos(2 * w); val s2 = sin(2 * w)
            var mag = 1.0
            for (q in list) {
                // H(e^jw) = (b0 + b1 e^-jw + b2 e^-j2w) / (1 + a1 e^-jw + a2 e^-j2w)
                val nr = q.b0 + q.b1 * c1 + q.b2 * c2
                val ni = -(q.b1 * s1 + q.b2 * s2)
                val dr = 1.0 + q.a1 * c1 + q.a2 * c2
                val di = -(q.a1 * s1 + q.a2 * s2)
                mag *= sqrt(nr * nr + ni * ni) / sqrt(dr * dr + di * di)
            }
            return mag
        }
    }
}
