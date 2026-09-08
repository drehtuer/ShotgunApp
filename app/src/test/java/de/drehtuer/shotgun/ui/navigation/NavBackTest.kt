package de.drehtuer.shotgun.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import de.drehtuer.shotgun.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Back, which is where this app's worst bug lived.
 *
 * Popping more often than the stack is deep empties the graph, and a graph with
 * no destination renders **nothing**: the app stays alive and resumed showing a
 * blank window that only a restart clears. Two pops racing each other is enough
 * - a button tapped twice, or a tap arriving alongside the back gesture.
 *
 * So every test here ends by asserting a screen is still on display. "Did not
 * crash" is not the property; "is not blank" is.
 */
@RunWith(RobolectricTestRunner::class)
class NavBackTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var exits = 0
    private lateinit var controller: NavHostController

    private fun host() {
        compose.setContent {
            val nav = rememberNavController()
            SideEffect { controller = nav }
            ShotgunTheme(ThemePreference.DARK) {
                ShotgunNavHost(
                    navController = nav,
                    onThemePreferenceChange = {},
                    onHapticsChange = {},
                    onDimChange = {},
                    onCountdownStep = {},
                    onRevealTimingChange = {},
                    teamCount = 3,
                    onTeamCountChange = {},
                    settings = Settings(),
                    onDrawComplete = { _, _, _ -> },
                    winners = emptyList(),
                    latestDraw = null,
                    drawCount = 0,
                    onExit = { exits++ },
                )
            }
        }
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun assertOnHome() =
        compose.onNodeWithText("One finger wins the draw").assertIsDisplayed()

    @Test
    fun `back on the home screen closes the app rather than emptying the graph`() {
        host()

        pressBack()

        assertEquals(1, exits)
        assertOnHome()
    }

    @Test
    fun `back from settings returns to the modes without closing the app`() {
        host()
        compose.onNodeWithText("SETTINGS").performClick()

        pressBack()

        assertEquals(0, exits)
        assertOnHome()
    }

    /**
     * The blank screen, reproduced as a test: one pop more than the stack is
     * deep. The second back must ask to close the app instead of popping the
     * last destination away.
     */
    @Test
    fun `back twice from settings pops once and then offers to close`() {
        host()
        compose.onNodeWithText("SETTINGS").performClick()

        pressBack()
        pressBack()

        assertEquals(1, exits)
        assertOnHome()
    }

    @Test
    fun `back from the draw surface returns to the modes`() {
        host()
        compose.onNodeWithText("One finger wins the draw").performClick()
        compose.onNodeWithText("STARTING PLAYER").assertIsDisplayed()

        pressBack()

        assertEquals(0, exits)
        assertOnHome()
    }

    @Test
    fun `back from the result screen returns to the modes`() {
        host()
        compose.onNodeWithText("LAST RESULT & FAIRNESS HEATMAP →").performClick()
        compose.onNodeWithText("FAIRNESS").assertIsDisplayed()

        pressBack()

        assertEquals(0, exits)
        assertOnHome()
    }

    /** Every mode has to reach its own surface: the route carries the mode. */
    @Test
    fun `each mode card opens its own draw surface`() {
        host()

        compose.onNodeWithText("Every finger gets a number").performClick()
        compose.onNodeWithText("PLAYER ORDER").assertIsDisplayed()
        pressBack()

        compose.onNodeWithText("Uneven sizes allowed").performClick()
        compose.onNodeWithText("3 TEAMS").assertIsDisplayed()
        pressBack()

        compose.onNodeWithText("One finger wins the draw").performClick()
        compose.onNodeWithText("STARTING PLAYER").assertIsDisplayed()
    }

    /**
     * A double tap on a mode card must open one draw surface, not two - the
     * second navigation is refused while the first is still settling, and a
     * stack two deep would take two backs to leave.
     */
    /**
     * The guard itself, rather than through a screen: a pop is refused when
     * there is nothing behind the current destination, and allowed when there
     * is. Everything above depends on this returning false exactly once per
     * empty stack - a `popBackStack()` in its place would happily pop the last
     * entry and leave the window blank.
     */
    @Test
    fun `popSafely refuses to pop the last destination and says so`() {
        host()

        var popped = true
        compose.runOnUiThread { popped = controller.popSafely() }
        compose.waitForIdle()
        assertEquals(false, popped)
        assertOnHome()

        compose.onNodeWithText("SETTINGS").performClick()
        compose.runOnUiThread { popped = controller.popSafely() }
        compose.waitForIdle()
        assertEquals(true, popped)
        assertOnHome()
    }
}
