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
 * ### Why a result buzz is cut into steps
 *
 * The app hands `Vibrator.vibrate` an effect and no `VibrationAttributes`, so
 * the usage is `UNKNOWN` - and Android then *guesses*: an unknown vibration of
 * **three steps or fewer** is re-classified as `USAGE_TOUCH`, which a phone
 * with *Touch feedback* switched off drops before it reaches the vibrator.
 *
 * That is what silenced the starter. Measured on the phone, with
 * `cmd vibrator_manager` and `dumpsys vibrator_manager`:
 *
 * | Effect | Steps | Duration | Outcome |
 * | --- | --- | --- | --- |
 * | `[0, 400]` | 2 | 400 ms | ignored, re-classified `TOUCH` |
 * | `[0, 30, 0, 30]` | 4 | 60 ms | played, stayed `UNKNOWN` |
 *
 * It is the step count, not the length. The old double buzz survived on four
 * steps by accident; the starter's single buzz had two and never played once.
 * So [spread] cuts a short pattern into more steps than the heuristic accepts.
 * The cuts are zero-length, so the buzz is contiguous and unchanged - what the
 * hand feels is still `docs/design.md`'s `[90]`.
 *
 * The finger tick is deliberately left alone. It really *is* touch feedback, so
 * a phone told not to give touch feedback is right to drop it.
 */
object Haptics {

    /** A keyboard-style tick as a finger lands. */
    const val FINGER_TICK_MS = 12L

    /** One firm buzz: the starter has been picked. */
    val WINNER: LongArray = longArrayOf(90)

    /** A heavier double buzz: an order or teams result. */
    val RESULT: LongArray = longArrayOf(90, 60, 90)

    /**
     * Android takes an `UNKNOWN` vibration of this many steps or fewer for
     * haptic feedback. The number is the framework's and is not public API, so
     * it is pinned by a device measurement rather than by a document.
     */
    const val HAPTIC_FEEDBACK_MAX_STEPS = 3

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
        val steps = spread(timings)
        vibrator(context)?.vibrate(
            VibrationEffect.createWaveform(steps, amplitudes(steps.size), -1)
        )
    }

    /**
     * Cuts the first buzz into enough pieces that the effect is not mistaken
     * for haptic feedback - see the note above.
     *
     * The pieces are separated by **zero-length** gaps, so the vibrator plays
     * them back to back and the buzz is exactly as long as it was. Splitting
     * always adds pairs of entries, so the on/off alternation survives it. A
     * pattern that already has enough steps is handed back untouched.
     */
    internal fun spread(timings: LongArray): LongArray {
        if (timings.size > HAPTIC_FEEDBACK_MAX_STEPS) return timings
        val pieces = HAPTIC_FEEDBACK_MAX_STEPS + 2 - timings.size
        val buzz = timings.first()
        val out = ArrayList<Long>(timings.size + (pieces - 1) * 2)
        repeat(pieces) { piece ->
            if (piece > 0) out += 0L
            // The remainder goes to the earliest pieces, so the total is exact.
            out += buzz / pieces + if (piece < buzz % pieces) 1L else 0L
        }
        timings.drop(1).forEach { out += it }
        return out.toLongArray()
    }

    /**
     * Amplitudes for a pattern that **starts on**: the even entries buzz, the
     * odd ones are the gaps. `createWaveform(timings, repeat)` assumes the
     * opposite - it starts with a pause - which is why they are spelled out
     * rather than left to it.
     */
    internal fun amplitudes(size: Int): IntArray =
        IntArray(size) { if (it % 2 == 0) VibrationEffect.DEFAULT_AMPLITUDE else 0 }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
