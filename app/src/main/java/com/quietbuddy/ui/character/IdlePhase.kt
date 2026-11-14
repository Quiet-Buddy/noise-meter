package com.quietbuddy.ui.character

import kotlin.math.PI

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
