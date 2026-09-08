package de.drehtuer.shotgun.ui.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import de.drehtuer.shotgun.ui.navigation.DrawMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * What the app actually asks the vibrator for.
 *
 * These exist because of a bug no amount of reading the design could show: an
 * `UNKNOWN` vibration of three steps or fewer is taken for touch feedback, and
 * a phone with touch feedback switched off drops it before it reaches the
 * vibrator. The starter's single buzz had two steps and never played once; the
 * double buzz survived on four, by accident. So the *step count* of a result
 * effect is a correctness property here, not a matter of taste.
 *
 * How it feels is still the phone's answer to give - see `app/src/androidTest/`.
 */
@RunWith(RobolectricTestRunner::class)
class HapticsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun vibrator(): Vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator

    /** Buzzing time: the even entries. The odd ones are the gaps. */
    private fun buzz(steps: LongArray) = steps.filterIndexed { i, _ -> i % 2 == 0 }.sum()

    /** Silence: the odd entries. */
    private fun gaps(steps: LongArray) = steps.filterIndexed { i, _ -> i % 2 == 1 }.sum()

    @Test
    fun `every result effect has more steps than Android takes for feedback`() {
        DrawMode.entries.forEach { mode ->
            val steps = Haptics.spread(Haptics.resultPattern(mode))
            assertTrue(
                "$mode's result buzz is ${steps.size} steps and would be dropped",
                steps.size > Haptics.HAPTIC_FEEDBACK_MAX_STEPS,
            )
        }
    }

    @Test
    fun `the starter still buzzes for 90 ms, in pieces with no gaps between them`() {
        Haptics.result(context, enabled = true, mode = DrawMode.STARTER)
        val steps = shadowOf(vibrator()).pattern
        assertTrue(steps.size > Haptics.HAPTIC_FEEDBACK_MAX_STEPS)
        assertEquals(90L, buzz(steps))
        // Zero-length gaps: the pieces run together, so it is one buzz.
        assertEquals(0L, gaps(steps))
    }

    @Test
    fun `order still buzzes twice, 90 ms apiece, 60 ms apart`() {
        Haptics.result(context, enabled = true, mode = DrawMode.ORDER)
        val steps = shadowOf(vibrator()).pattern
        assertTrue(steps.size > Haptics.HAPTIC_FEEDBACK_MAX_STEPS)
        assertEquals(180L, buzz(steps))
        assertEquals(60L, gaps(steps))
    }

    @Test
    fun `teams gets the same double buzz as order`() {
        assertArrayEquals(
            Haptics.resultPattern(DrawMode.ORDER),
            Haptics.resultPattern(DrawMode.TEAMS),
        )
    }

    @Test
    fun `a pattern with enough steps already is left alone`() {
        val long = longArrayOf(90, 60, 90, 60)
        assertArrayEquals(long, Haptics.spread(long))
    }

    /**
     * The split must not change what the hand feels - only how many steps the
     * framework counts.
     */
    @Test
    fun `splitting preserves the buzzing and the silence exactly`() {
        listOf(longArrayOf(90), longArrayOf(90, 60, 90), longArrayOf(7)).forEach { pattern ->
            val steps = Haptics.spread(pattern)
            assertEquals(buzz(pattern), buzz(steps))
            assertEquals(gaps(pattern), gaps(steps))
        }
    }

    @Test
    fun `a finger tick is short and stays a one-shot`() {
        Haptics.tick(context, enabled = true)
        val shadow = shadowOf(vibrator())
        assertTrue(shadow.isVibrating)
        assertEquals(Haptics.FINGER_TICK_MS, shadow.milliseconds)
    }

    @Test
    fun `haptics switched off sends nothing at all`() {
        Haptics.tick(context, enabled = false)
        Haptics.result(context, enabled = false, mode = DrawMode.STARTER)
        Haptics.result(context, enabled = false, mode = DrawMode.TEAMS)
        assertFalse(shadowOf(vibrator()).isVibrating)
    }

    @Test
    fun `an empty pattern is not sent`() {
        Haptics.pattern(context, enabled = true, timings = longArrayOf())
        assertFalse(shadowOf(vibrator()).isVibrating)
    }

    /**
     * The gaps must be silent, or `[90, 60, 90]` is one long 240 ms buzz -
     * which is what leaving the amplitudes to `createWaveform(timings, repeat)`
     * would give, since that one starts with a pause instead.
     */
    @Test
    fun `amplitudes buzz on the even entries and rest on the odd ones`() {
        val d = VibrationEffect.DEFAULT_AMPLITUDE
        assertArrayEquals(intArrayOf(d, 0, d), Haptics.amplitudes(3))
        assertArrayEquals(intArrayOf(d, 0), Haptics.amplitudes(2))
    }
}
