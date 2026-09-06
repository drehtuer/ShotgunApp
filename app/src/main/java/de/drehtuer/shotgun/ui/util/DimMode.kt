package de.drehtuer.shotgun.ui.util

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
 * could not explain.
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
            attributes.screenBrightness = if (enabled) DIM_LEVEL else BRIGHTNESS_OVERRIDE_NONE
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
 * Low enough to be restful in the dark, high enough that the accent still
 * reads. Not zero: the design dims the room, it does not blank the screen.
 */
private const val DIM_LEVEL = 0.25f

/** Hands brightness back to the system. */
private const val BRIGHTNESS_OVERRIDE_NONE = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
