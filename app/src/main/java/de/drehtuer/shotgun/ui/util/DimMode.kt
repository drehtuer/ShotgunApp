package de.drehtuer.shotgun.ui.util

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Brightness for a dark room, the way an alarm clock dims.
 *
 * Applies to this app's window only, via
 * [WindowManager.LayoutParams.screenBrightness], and restores the previous
 * value on the way out. It deliberately does **not** touch the system-wide
 * setting: leaving a phone dimmed after the app closes would be a bug the user
 * could not explain, and could not easily undo.
 *
 * This is a brightness change, not a palette change. The theme is chosen
 * separately under APPEARANCE, and dimming must not quietly darken the colours.
 */
@Composable
fun DimScreen(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val window = view.context.findActivity()?.window
        val attributes = window?.attributes
        val previous = attributes?.screenBrightness

        if (window != null && attributes != null) {
            attributes.screenBrightness = if (enabled) {
                dimTarget(view.context)
            } else {
                BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = attributes
        }

        onDispose {
            if (window != null && previous != null) {
                val restored = window.attributes
                restored.screenBrightness = previous
                window.attributes = restored
            }
        }
    }
}

/**
 * How dim to go: a fraction of whatever the screen is already set to.
 *
 * Two wrong answers were tried first, and both are instructive. A fixed 0.25 is
 * *absolute*, so on a phone already turned well down it made the screen
 * **brighter** - dim mode undimming. Capping at the current level fixed that but
 * made the setting do nothing at all on a phone already at minimum, which is
 * exactly the phone most likely to want it.
 *
 * Halving always darkens, by an amount proportional to where you already were.
 */
internal fun dimTarget(context: Context): Float {
    val current = systemBrightness(context) ?: FALLBACK_LEVEL
    return (current * DIM_FACTOR).coerceAtLeast(DIM_FLOOR)
}

/** System brightness as a 0..1 fraction, or null when it cannot be read. */
private fun systemBrightness(context: Context): Float? = runCatching {
    val value = Settings.System.getInt(
        context.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS,
    )
    (value / MAX_SYSTEM_BRIGHTNESS).coerceIn(0f, 1f)
}.getOrNull()

/** Half as bright as it was. Always a visible change, never an increase. */
private const val DIM_FACTOR = 0.5f

/** Used only when the current brightness cannot be read. */
private const val FALLBACK_LEVEL = 0.10f

/** The design dims the room; it does not blank the screen. */
private const val DIM_FLOOR = 0.004f

/** Settings.System.SCREEN_BRIGHTNESS is 0..255 on effectively every device. */
private const val MAX_SYSTEM_BRIGHTNESS = 255f

/** Hands brightness back to the system. */
private const val BRIGHTNESS_OVERRIDE_NONE = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
