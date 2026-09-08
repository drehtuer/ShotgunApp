package de.drehtuer.shotgun

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The app, started the way the launcher starts it.
 *
 * Every other test builds a screen with its arguments handed to it. This one
 * covers the wiring nothing else does: the real application object, the real
 * settings store and the real database behind the view model, the theme chosen
 * from a stored preference, dim mode applied to the window, and the navigation
 * graph hanging off all of it. It is a smoke test on purpose - if this fails,
 * the app does not start.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `the app starts on the home screen`() {
        compose.onNodeWithText("PICK A MODE.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("STARTING PLAYER").assertIsDisplayed()
    }

    @Test
    fun `the navigation graph is live from the first frame`() {
        compose.onNodeWithText("SETTINGS").performClick()

        compose.onNodeWithText("Follows the system theme unless you pick one").assertIsDisplayed()
    }

    /**
     * A rotation cannot happen - the app is portrait only - but a recreate can,
     * on a theme change or a low-memory return, and the settings the activity
     * reads are asynchronous. Coming back to a blank screen is the failure this
     * rules out.
     */
    @Test
    fun `the app comes back after being recreated`() {
        compose.activityRule.scenario.recreate()

        compose.onNodeWithText("PICK A MODE.", substring = true).assertIsDisplayed()
    }
}
