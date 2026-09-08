package de.drehtuer.shotgun.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.drehtuer.shotgun.ui.navigation.DrawMode

/**
 * The two strengths the design asks for, so the phone tells you what happened
 * without your having to look at it.
 *
 * Patterns are written the way `docs/design.md` writes them - **on** durations
 * first, alternating with the gaps between them: `[90]` is one buzz,
 * `[90, 60, 90]` is two.
 *
 * Two things here are deliberate, and both come from the starter buzz going
 * unfelt on the phone:
 *
 * - **A single buzz is a one-shot, not a waveform.** A one-element pattern was
 *   previously sent as `createWaveform(longArrayOf(0, 90), -1)` - a waveform
 *   whose only content is one step, behind a zero-length pause. The tick, which
 *   has always been felt, is a one-shot; the double buzz, also felt, is a real
 *   multi-step waveform. The single-step waveform was the odd one out.
 * - **Result buzzes play at full amplitude where the device allows it.** This
 *   app is used with several hands pressing the phone against a table, which
 *   damps the actuator hard. The tick stays at the default amplitude, so the
 *   result is still the stronger of the two.
 */
object Haptics {

    /** A keyboard-style tick as a finger lands. */
    const val FINGER_TICK_MS = 12L

    /** One firm buzz: the starter has been picked. */
    val WINNER: LongArray = longArrayOf(90)

    /** A heavier double buzz: an order or teams result. */
    val RESULT: LongArray = longArrayOf(90, 60, 90)

    /** Which buzz a mode's result gets. Starter has one answer, so one buzz. */
    fun resultPattern(mode: DrawMode): LongArray =
        if (mode == DrawMode.STARTER) WINNER else RESULT

    fun tick(context: Context, enabled: Boolean) {
        if (!enabled) return
        vibrator(context)?.vibrate(
            VibrationEffect.createOneShot(FINGER_TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** The result buzz for [mode], once the draw has landed. */
    fun result(context: Context, enabled: Boolean, mode: DrawMode) =
        pattern(context, enabled, resultPattern(mode))

    fun pattern(context: Context, enabled: Boolean, timings: LongArray) {
        if (!enabled || timings.isEmpty()) return
        val vibrator = vibrator(context) ?: return
        val strength = strength(vibrator)
        vibrator.vibrate(
            if (timings.size == 1) {
                VibrationEffect.createOneShot(timings[0], strength)
            } else {
                VibrationEffect.createWaveform(timings, amplitudes(timings.size, strength), -1)
            }
        )
    }

    /**
     * Amplitudes for a pattern that **starts on**: the even entries buzz, the
     * odd ones are the gaps. `createWaveform(timings, repeat)` assumes the
     * opposite - it starts with a pause - which is why the amplitudes are
     * spelled out rather than left to it.
     */
    internal fun amplitudes(size: Int, strength: Int): IntArray =
        IntArray(size) { if (it % 2 == 0) strength else 0 }

    /**
     * The strongest amplitude the API accepts. `VibrationEffect` documents the
     * range as 1-255 but publishes no constant for the top of it - only
     * `DEFAULT_AMPLITUDE`, which is whatever the device thinks is average.
     */
    private const val FULL_AMPLITUDE = 255

    /** Full strength where the actuator can be driven; the device default otherwise. */
    private fun strength(vibrator: Vibrator): Int =
        if (vibrator.hasAmplitudeControl()) FULL_AMPLITUDE else VibrationEffect.DEFAULT_AMPLITUDE

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
