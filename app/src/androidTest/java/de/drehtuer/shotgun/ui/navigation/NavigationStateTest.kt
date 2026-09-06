package de.drehtuer.shotgun.ui.navigation

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drehtuer.shotgun.MainActivity
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The graph must always be showing something.
 *
 * A blank window - the app alive and resumed but drawing nothing - is what
 * happens when the back stack is emptied, and it is unrecoverable without a
 * restart. These hammer the transitions looking for a state with no
 * destination.
 */
@RunWith(AndroidJUnit4::class)
class NavigationStateTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private lateinit var nav: NavHostController

    private fun start() {
        rule.activity.setContent {
            ShotgunTheme {
                nav = rememberNavController()
                ShotgunNavHost(
                    onThemePreferenceChange = {},
                    onHapticsChange = {},
                    onDimChange = {},
                    onCountdownStep = {},
                    onRevealTimingChange = {},
                    teamCount = 2,
                    onTeamCountChange = {},
                    settings = Settings(),
                    onDrawComplete = { _, _, _ -> },
                    winners = emptyList(),
                    latestDraw = null,
                    drawCount = 0,
                    onExit = {},
                    navController = nav,
                )
            }
        }
        rule.waitForIdle()
    }

    private fun assertDefined(step: String) {
        rule.waitForIdle()
        rule.runOnUiThread {
            assertNotNull("no destination after $step", nav.currentDestination)
        }
    }

    @Test
    fun everyDestinationIsReachableAndLeavable() {
        start()
        for (route in listOf(
            Destination.Draw.routeFor(DrawMode.STARTER),
            Destination.Draw.routeFor(DrawMode.ORDER),
            Destination.Draw.routeFor(DrawMode.TEAMS),
            Destination.Settings.route,
            Destination.Result.route,
        )) {
            rule.runOnUiThread { nav.navigate(route) }
            assertDefined("navigate to $route")
            rule.runOnUiThread { nav.popSafely() }
            assertDefined("pop from $route")
        }
    }

    /** Navigating and popping in the same frame is the reported reproduction. */
    @Test
    fun navigatingAndPoppingImmediatelyStillLeavesADestination() {
        start()
        repeat(20) {
            rule.runOnUiThread {
                nav.navigate(Destination.Draw.routeFor(DrawMode.STARTER))
                nav.popSafely()
            }
            assertDefined("navigate+pop round $it")
        }
    }

    /**
     * The reported bug: popping harder than the stack is deep emptied the
     * graph, and the app drew an empty window until it was restarted. Every
     * exit path in the app goes through popSafely for this reason.
     */
    @Test
    fun overPoppingCannotEmptyTheGraph() {
        start()
        rule.runOnUiThread { nav.navigate(Destination.Settings.route) }
        assertDefined("navigate to settings")
        repeat(6) { rule.runOnUiThread { nav.popSafely() } }
        assertDefined("six guarded pops from a two-deep stack")
    }

    /** An unguarded pop is exactly what broke it - keep the guard honest. */
    @Test
    fun theGuardRefusesToPopTheLastScreen() {
        start()
        rule.runOnUiThread {
            assert(!nav.popSafely()) { "popped the only screen" }
        }
        assertDefined("guarded pop on the home screen")
    }

    @Test
    fun rapidlyRepeatedNavigationLeavesADestination() {
        start()
        repeat(10) {
            rule.runOnUiThread {
                nav.navigate(Destination.Draw.routeFor(DrawMode.TEAMS))
                nav.navigate(Destination.Settings.route)
                nav.navigate(Destination.Result.route)
            }
            assertDefined("triple navigate round $it")
        }
    }
}
