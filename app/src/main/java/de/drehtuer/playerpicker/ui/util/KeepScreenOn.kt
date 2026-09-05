package de.drehtuer.playerpicker.ui.util

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Holds the screen on for as long as this composable is in the composition, and
 * releases it on the way out.
 *
 * The draw surface needs this because of how Android's idle timer works: it is
 * reset by touch *events*, and a finger held still on the glass produces none.
 * Players hold position for the whole countdown, so without this the screen can
 * dim - or lock outright - in the middle of a draw.
 *
 * Scoped to the screen that needs it rather than set on the activity, so the
 * flag cannot leak into the rest of the app.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
