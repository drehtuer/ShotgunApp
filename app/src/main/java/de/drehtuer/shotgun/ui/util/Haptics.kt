package de.drehtuer.shotgun.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The two strengths the design asks for, so the phone tells you what happened
 * without your having to look at it.
 */
object Haptics {

    /** A keyboard-style tick as a finger lands. */
    const val FINGER_TICK_MS = 12L

    /** One firm buzz: the starter has been picked. */
    val WINNER: LongArray = longArrayOf(0, 90)

    /** A heavier double buzz: an order or teams result. */
    val RESULT: LongArray = longArrayOf(0, 90, 60, 90)

    fun tick(context: Context, enabled: Boolean) {
        if (!enabled) return
        vibrator(context)?.vibrate(VibrationEffect.createOneShot(FINGER_TICK_MS, DEFAULT_AMPLITUDE))
    }

    fun pattern(context: Context, enabled: Boolean, timings: LongArray) {
        if (!enabled) return
        vibrator(context)?.vibrate(VibrationEffect.createWaveform(timings, -1))
    }

    private const val DEFAULT_AMPLITUDE = VibrationEffect.DEFAULT_AMPLITUDE

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
