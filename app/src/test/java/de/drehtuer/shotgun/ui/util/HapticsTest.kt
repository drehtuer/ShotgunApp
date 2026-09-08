package de.drehtuer.shotgun.ui.util

import android.content.Context
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
 * These exist because of a bug the design could not show: the starter's single
 * buzz was built as a one-step waveform and went unfelt on the phone, while the
 * tick (a one-shot) and the double buzz (a real waveform) both worked. The
 * *shape* of the effect is therefore worth pinning, not just the timings.
 *
 * How it feels is still the phone's answer to give - see `app/src/androidTest/`.
 */
@RunWith(RobolectricTestRunner::class)
class HapticsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun vibrator(): Vibrator =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator

    @Test
    fun `the starter result is a single buzz, sent as a one-shot`() {
        Haptics.result(context, enabled = true, mode = DrawMode.STARTER)
        val shadow = shadowOf(vibrator())
        assertTrue("nothing was sent to the vibrator", shadow.isVibrating)
        // A one-shot reports its duration; a waveform reports timings instead
        // and leaves this at zero. That is the distinction this test is for.
        assertEquals(90L, shadow.milliseconds)
    }

    @Test
    fun `order gets the double buzz as a waveform`() {
        Haptics.result(context, enabled = true, mode = DrawMode.ORDER)
        assertArrayEquals(longArrayOf(90, 60, 90), shadowOf(vibrator()).pattern)
    }

    @Test
    fun `teams gets the same double buzz as order`() {
        assertArrayEquals(
            Haptics.resultPattern(DrawMode.ORDER),
            Haptics.resultPattern(DrawMode.TEAMS),
        )
    }

    @Test
    fun `a finger tick is short and does not repeat`() {
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
     * The gaps must be silent, or a `[90, 60, 90]` double buzz is one long
     * 240 ms buzz - which is what leaving the amplitudes to the framework's
     * `createWaveform(timings, repeat)` would have given, since that one starts
     * with a pause instead.
     */
    @Test
    fun `amplitudes buzz on the even entries and rest on the odd ones`() {
        assertArrayEquals(intArrayOf(255, 0, 255), Haptics.amplitudes(3, 255))
        assertArrayEquals(intArrayOf(255), Haptics.amplitudes(1, 255))
    }
}
