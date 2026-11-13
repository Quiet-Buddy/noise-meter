package com.quietbuddy.ui.home

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

// ---- Body motion ----

/** A pat within this long of the previous one keeps the streak going. */
private const val STREAK_GAP_MS = 600L
/** Squash velocity (fraction of height per second) one pat adds; about a 10% squash from rest. */
private const val SQUASH_IMPULSE = 4.5f
/** Each pat in a streak adds this much to the impulse, up to [STREAK_BOOST_CAP] pats. */
private const val STREAK_BOOST = 0.1f
private const val STREAK_BOOST_CAP = 6
private const val SQUASH_MAX_VELOCITY = 9f
/** Displayed squash never goes past these, however fast the taps come: 20% squash, 14% stretch. */
private const val SQUASH_LIMIT = 0.2f
private const val STRETCH_LIMIT = 0.14f
/** How much wider the body gets per unit of squash. */
private const val WIDEN = 0.8f
/** Lean velocity (degrees per second) for a pat at the very edge; about a 5 degree lean. */
private const val LEAN_IMPULSE = 150f
private const val LEAN_MAX_VELOCITY = 300f
private const val LEAN_LIMIT = 9f
/** A pat in a loud room: a small sway, about 2.5 degrees. */
private const val WOBBLE_IMPULSE = 75f
/** Hop heights as a fraction of the character's height: every fifth pat of a streak, and every tenth. */
private const val SMALL_HOP = 0.08f
private const val BIG_HOP = 0.16f
/** Upward velocity per unit of hop height that [HopSpring] needs to peak at that height. */
private const val HOP_VELOCITY_PER_HEIGHT = 21.4f
/** The squash on landing a hop. */
private const val LAND_IMPULSE = 5f

/** Jelly: bouncy and quick, so stacked pats wobble rather than queue. */
private val SquashSpring: SpringSpec<Float> = spring(dampingRatio = 0.4f, stiffness = 700f, visibilityThreshold = 0.001f)
private val LeanSpring: SpringSpec<Float> = spring(dampingRatio = 0.4f, stiffness = 300f, visibilityThreshold = 0.05f)
/** Launched upward with velocity and pulled back to the floor; the undershoot is hidden as the landing. */
private val HopSpring: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 150f, visibilityThreshold = 0.0005f)
private val SpinSpring: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 200f, visibilityThreshold = 0.5f)

/**
 * The character's spring-driven reaction to pats. Every pat adds velocity to whatever is already moving, so fast
 * tapping stacks into a bigger, wobblier jelly instead of restarting one fixed bounce. A streak of quick pats hops
 * on every fifth and does a big spinning hop on every tenth. Apply it with [tapMotion].
 */
@Stable
class BuddyTapMotion internal constructor(private val scope: CoroutineScope) {
    internal val squash = Animatable(0f)
    internal val lean = Animatable(0f)
    internal val hop = Animatable(0f)
    internal val spin = Animatable(0f)
    private var streak = 0
    private var lastPatMs = 0L

    /**
     * A rewarded pat at [x], from -1 (left edge) to 1 (right edge). The body squashes and leans away from the finger.
     * Returns true on a big milestone (every tenth pat of a streak), for a bigger celebration.
     */
    fun pat(x: Float): Boolean {
        val now = SystemClock.uptimeMillis()
        streak = if (now - lastPatMs <= STREAK_GAP_MS) streak + 1 else 1
        lastPatMs = now
        val boost = 1f + STREAK_BOOST * (streak - 1).coerceAtMost(STREAK_BOOST_CAP)
        kick(squash, SQUASH_IMPULSE * boost, SQUASH_MAX_VELOCITY, SquashSpring)
        kick(lean, -x * LEAN_IMPULSE * boost, LEAN_MAX_VELOCITY, LeanSpring)
        val big = streak % 10 == 0
        when {
            big -> jump(BIG_HOP, spin = true)
            streak % 10 == 5 -> jump(SMALL_HOP, spin = false)
        }
        return big
    }

    /** An unrewarded pat (loud room): just a gentle sway away from the finger. */
    fun wobble(x: Float) = kick(lean, if (x < 0f) WOBBLE_IMPULSE else -WOBBLE_IMPULSE, LEAN_MAX_VELOCITY, LeanSpring)

    /** Adds [impulse] to the spring's current velocity and lets it settle back to rest from where it is now. */
    private fun kick(a: Animatable<Float, AnimationVector1D>, impulse: Float, max: Float, spec: SpringSpec<Float>) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            a.animateTo(0f, spec, initialVelocity = (a.velocity + impulse).coerceIn(-max, max))
        }
    }

    private fun jump(height: Float, spin: Boolean) {
        if (spin) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                this@BuddyTapMotion.spin.snapTo(this@BuddyTapMotion.spin.value % 360f)
                this@BuddyTapMotion.spin.animateTo(360f, SpinSpring)
                this@BuddyTapMotion.spin.snapTo(0f)
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var landed = false
            hop.animateTo(0f, HopSpring, initialVelocity = hop.velocity - height * HOP_VELOCITY_PER_HEIGHT) {
                if (!landed && value > 0f && velocity > 0f) {
                    landed = true
                    kick(squash, LAND_IMPULSE, SQUASH_MAX_VELOCITY, SquashSpring)
                }
            }
        }
    }
}

@Composable
fun rememberBuddyTapMotion(): BuddyTapMotion {
    val scope = rememberCoroutineScope()
    return remember(scope) { BuddyTapMotion(scope) }
}

/**
 * Applies [motion]: hop and spin about the middle, then squash, stretch and lean about the feet. Reads the springs
 * only in the layer block, so pats never recompose the character.
 */
fun Modifier.tapMotion(motion: BuddyTapMotion): Modifier = this
    .graphicsLayer {
        translationY = motion.hop.value.coerceAtMost(0f) * size.height
        rotationZ = motion.spin.value
    }
    .graphicsLayer {
        val s = motion.squash.value.coerceIn(-STRETCH_LIMIT, SQUASH_LIMIT)
        scaleY = 1f - s
        scaleX = 1f + s * WIDEN
        rotationZ = motion.lean.value.coerceIn(-LEAN_LIMIT, LEAN_LIMIT)
        transformOrigin = TransformOrigin(0.5f, 0.9f)
    }

// ---- Particles ----

/** Live particles beyond this drop the oldest first. */
private const val MAX_PARTICLES = 40
private const val BIG_BURST = 14
/** Seconds to pop in, with overshoot. */
private const val POP_IN_S = 0.16f
/** Upward pull (dp/s²) and drag (1/s): particles slow down, then drift up at about 40 dp/s. */
private const val BUOYANCY = 80f
private const val DRAG = 2f

private enum class Kind { HEART, STAR, DOT }

/** One heart, star or sparkle dot. Position in px, velocity in px/s, angles in degrees, times in seconds. */
private class Particle(
    val kind: Kind,
    val color: Color,
    val size: Float,
    val life: Float,
    val spin: Float,
    val phase: Float,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
) {
    var age = 0f
    var rotation = 0f
}

/**
 * Hearts and sparkles that pop out where the character was patted, float up and fade. [burst] spawns some;
 * [TapBurstLayer] moves and draws them, and only asks for frames while any are alive.
 */
@Stable
class TapBurstState internal constructor(
    /** Pixels per dp. */
    private val dp: Float,
    private val hearts: List<Color>,
    private val sparkles: List<Color>,
    /** The character's ink, so hearts and stars carry the same outline as the art and read against the fur. */
    private val outline: Color,
) {
    private val particles = ArrayList<Particle>()
    private var lastNanos = 0L
    /** Bumped on every burst to wake the frame loop. */
    internal var spawns by mutableIntStateOf(0)
        private set
    /** Bumped every frame; read only while drawing, so a frame redraws without recomposing. */
    internal var frame by mutableLongStateOf(0L)
        private set
    internal val alive get() = particles.isNotEmpty()

    /** A few hearts and sparkles at [at] (px), shooting upward; [big] is a larger ring of them for a milestone. */
    fun burst(at: Offset, big: Boolean = false) {
        val count = if (big) BIG_BURST else Random.nextInt(3, 6)
        repeat(count) { i ->
            if (particles.size >= MAX_PARTICLES) particles.removeAt(0)
            val kind = Random.nextFloat().let { if (it < 0.5f) Kind.HEART else if (it < 0.8f) Kind.STAR else Kind.DOT }
            // A small burst fans upward; a big one rings all the way round and floats up after.
            val angle = if (big) {
                2f * PI.toFloat() * (i + Random.nextFloat() * 0.6f) / count
            } else {
                -PI.toFloat() / 2f + (Random.nextFloat() - 0.5f) * 1.4f
            }
            val speed = dp * if (big) Random.nextFloat() * 140f + 200f else Random.nextFloat() * 110f + 110f
            particles += Particle(
                kind = kind,
                color = (if (kind == Kind.HEART) hearts else sparkles).random(),
                size = dp * when (kind) {
                    Kind.HEART -> Random.nextFloat() * 10f + 28f
                    Kind.STAR -> Random.nextFloat() * 8f + 22f
                    Kind.DOT -> Random.nextFloat() * 4f + 8f
                },
                life = Random.nextFloat() * 0.3f + if (big) 1.0f else 0.8f,
                spin = if (kind == Kind.STAR) (Random.nextFloat() * 160f + 90f) * (if (Random.nextBoolean()) 1 else -1) else 0f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                x = at.x,
                y = at.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
            )
        }
        spawns++
    }

    internal fun step(nanos: Long) {
        val dt = if (lastNanos == 0L) 0f else ((nanos - lastNanos) / 1e9f).coerceAtMost(0.05f)
        lastNanos = nanos
        val drag = exp(-DRAG * dt)
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age += dt
            if (p.age >= p.life) {
                iterator.remove()
                continue
            }
            p.vx *= drag
            p.vy = p.vy * drag - BUOYANCY * dp * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
        }
        if (particles.isEmpty()) lastNanos = 0L
        frame = nanos
    }

    internal fun DrawScope.drawParticles() {
        for (p in particles) {
            val t = p.age / p.life
            val pop = if (p.age < POP_IN_S) easeOutBack(p.age / POP_IN_S) else 1f
            val scale = p.size * pop * (1f - 0.3f * t)
            if (scale < 0.5f) continue
            val alpha = if (t < 0.55f) 1f else 1f - (t - 0.55f) / 0.45f
            // Hearts sway side to side as they rise; stars spin.
            val sway = if (p.kind == Kind.HEART) sin(p.age * 7f + p.phase) else 0f
            withTransform({
                translate(p.x + sway * 3f * dp, p.y)
                rotate(p.rotation + sway * 12f, pivot = Offset.Zero)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // Stroke width is in the unit shape's space: about 2.5 dp whatever the particle's size.
                val ink = Stroke(width = 2.5f * dp / scale, join = StrokeJoin.Round)
                when (p.kind) {
                    Kind.HEART -> {
                        drawPath(HeartPath, p.color, alpha = alpha)
                        drawPath(HeartPath, outline, alpha = alpha, style = ink)
                    }
                    Kind.STAR -> {
                        drawPath(StarPath, p.color, alpha = alpha)
                        drawPath(StarPath, outline, alpha = alpha, style = ink)
                    }
                    Kind.DOT -> drawCircle(p.color, radius = 0.5f, center = Offset.Zero, alpha = alpha)
                }
            }
        }
    }
}

/** The particle colours come from the current character's palette: its cheeks and tongue, plus warm gold and green. */
@Composable
fun rememberTapBurst(): TapBurstState {
    val palette = LocalPalette.current
    val density = LocalDensity.current.density
    return remember(palette, density) {
        TapBurstState(
            dp = density,
            hearts = listOf(palette.tongue, palette.tongue, palette.cheek),
            sparkles = listOf(QB.Loud, QB.Loud, palette.cheek, QB.Calm),
            outline = palette.outline,
        )
    }
}

/** Draws [state]'s particles over whatever is beneath; give it the same bounds as the thing being patted. */
@Composable
fun TapBurstLayer(state: TapBurstState, modifier: Modifier = Modifier) {
    LaunchedEffect(state) {
        // Each burst wakes the loop; it runs frames until the last particle fades, then sleeps.
        snapshotFlow { state.spawns }.collect {
            while (state.alive) withFrameNanos(state::step)
        }
    }
    Canvas(modifier) {
        state.frame
        with(state) { drawParticles() }
    }
}

private fun easeOutBack(t: Float): Float {
    val c = 2.2f
    val u = t - 1f
    return 1f + (c + 1f) * u * u * u + c * u * u
}

/** A heart about 0.7 wide and tall, centred on the origin. */
private val HeartPath = Path().apply {
    moveTo(0f, 0.3f)
    cubicTo(-0.6f, -0.15f, -0.3f, -0.6f, 0f, -0.25f)
    cubicTo(0.3f, -0.6f, 0.6f, -0.15f, 0f, 0.3f)
    close()
}

/** A five-point star of radius 0.5, centred on the origin. */
private val StarPath = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 0.5f else 0.22f
        val a = -PI / 2 + i * PI / 5
        val x = (cos(a) * r).toFloat()
        val y = (sin(a) * r).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
