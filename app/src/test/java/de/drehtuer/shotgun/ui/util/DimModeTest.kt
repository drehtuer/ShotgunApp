package de.drehtuer.shotgun.ui.util

import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Dim mode: brightness for a dark room.
 *
 * Two wrong answers shipped before this one - a fixed level made an
 * already-dim phone *brighter*, and capping at the current level did nothing at
 * all on a phone at minimum. Both are arithmetic, and both are pinned here.
 *
 * The other half is the window: dimming must touch this app's window only, and
 * must hand the brightness back on the way out, or a phone stays dark after the
 * app closes with nothing to explain it.
 */
@RunWith(RobolectricTestRunner::class)
class DimModeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun systemBrightness(value: Int) {
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }

    @Test
    fun `dimming halves whatever the screen is already at`() {
        systemBrightness(200)

        assertEquals(200f / 255f * 0.5f, dimTarget(context), 0.0001f)
    }

    /** The failure that made dim mode *undim*: a bright screen must still halve. */
    @Test
    fun `a bright screen is dimmed proportionally, not to a fixed level`() {
        systemBrightness(255)
        val bright = dimTarget(context)

        systemBrightness(60)
        val dim = dimTarget(context)

        assertTrue("dimming a dim screen must not raise it: $dim vs $bright", dim < bright)
    }

    @Test
    fun `a screen already at minimum is dimmed further, but never blanked`() {
        systemBrightness(0)

        val target = dimTarget(context)
        assertTrue("dim mode blanked the screen", target > 0f)
        assertEquals(0.004f, target, 0.0001f)
    }

    @Test
    fun `dimming lowers this window's brightness and hands it back on the way out`() {
        val dimming = mutableStateOf(true)
        compose.setContent { if (dimming.value) DimScreen(enabled = true) }
        compose.waitForIdle()

        val dimmed = compose.activity.window.attributes.screenBrightness
        assertTrue("the window was not dimmed", dimmed > 0f && dimmed < 1f)

        dimming.value = false
        compose.waitForIdle()

        assertEquals(
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            compose.activity.window.attributes.screenBrightness,
            0.0001f,
        )
    }

    @Test
    fun `with dim mode off the window keeps the system's brightness`() {
        compose.setContent { DimScreen(enabled = false) }
        compose.waitForIdle()

        assertEquals(
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            compose.activity.window.attributes.screenBrightness,
            0.0001f,
        )
    }

    // ---- finding the window to dim ------------------------------------------

    /**
     * Compose hands out a context that may be wrapped several times, so the
     * cast alone is not safe - and a miss here means dim mode silently does
     * nothing rather than failing.
     */
    @Test
    fun `the activity is found through however many wrappers`() {
        val activity = compose.activity
        val wrapped = ContextWrapper(ContextWrapper(activity))

        assertSame(activity, wrapped.findActivity())
        assertSame(activity, activity.findActivity())
    }

    @Test
    fun `a context with no activity behind it finds none`() {
        assertNull(context.findActivity())
    }
}
