package com.quietbuddy.ui.character

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The angle, in radians, of an idle rhythm such as a sway, a float or a wobble.
 *
 * Drawing `sin(time * 2π * hz)` straight from the clock jumps to a different point of the swing whenever `hz`
 * changes, which it does during every mood transition. Advancing the angle by each frame's own speed instead
 * lets the rhythm speed up or slow down without skipping.
 */
class IdlePhase {
    var radians: Float = 0f
        private set

    /** Moves the angle on by [dtSec] at [hz] cycles per second. */
    fun advance(hz: Float, dtSec: Float) {
        radians = (radians + TAU * hz * dtSec) % TAU
    }

    private companion object {
        const val TAU = (2.0 * PI).toFloat()
    }
}

/*
 * Oscillations that run straight off the animation time (a tremble, a flicker, a twinkle) use a Double time that
 * never moves back, so a rebase of the Float clock does not make them jump. The maths is in Double because a
 * Float angle loses its fractions after a few hours of time times a fast rate.
 */

/** `sin(time * rate + offset)` for a [time] in seconds and a [rate] in radians per second. */
fun waveSin(time: Double, rate: Double, offset: Float = 0f): Float = sin(time * rate + offset).toFloat()

/** `cos(time * rate)` for a [time] in seconds and a [rate] in radians per second. */
fun waveCos(time: Double, rate: Double): Float = cos(time * rate).toFloat()

/** How far through its cycle a rhythm of [perSec] cycles per second is at [time], 0 until 1. */
fun waveCycle(time: Double, perSec: Double): Float = ((time * perSec) % 1.0).toFloat()
