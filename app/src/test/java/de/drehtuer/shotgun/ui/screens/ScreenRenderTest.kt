package de.drehtuer.shotgun.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import de.drehtuer.shotgun.data.DrawPoint
import de.drehtuer.shotgun.data.DrawRecord
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import de.drehtuer.shotgun.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The screens, rendered on the JVM.
 *
 * These do not replace `app/src/androidTest/`. Multi-touch, real haptics and
 * how the countdown *feels* still need the phone, and an emulator agreeing with
 * Robolectric would only confirm two simulations at once. What these cover is
 * that each screen composes, lays out, shows the right copy and calls back on a
 * tap - which was previously checked nowhere a CI runner could reach.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenRenderTest {

    @get:Rule
    val compose = createComposeRule()

    // ---- home ---------------------------------------------------------------

    @Test
    fun `home offers all three modes`() {
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                HomeScreen(
                    teamCount = 2,
                    onTeamCountChange = {},
                    onStartDraw = {},
                    onOpenSettings = {},
                    onOpenResult = {},
                )
            }
        }
        // Matched by subtitle: "TEAMS" is both a mode card and the stepper's
        // own label, so the title alone is ambiguous.
        compose.onNodeWithText("One finger wins the draw").assertIsDisplayed()
        compose.onNodeWithText("Every finger gets a number").assertIsDisplayed()
        compose.onNodeWithText("Uneven sizes allowed").assertIsDisplayed()
    }

    @Test
    fun `tapping a mode card starts that draw`() {
        var started: DrawMode? = null
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                HomeScreen(
                    teamCount = 2,
                    onTeamCountChange = {},
                    onStartDraw = { started = it },
                    onOpenSettings = {},
                    onOpenResult = {},
                )
            }
        }
        compose.onNodeWithText("PLAYER ORDER").performClick()
        assertEquals(DrawMode.ORDER, started)
    }

    @Test
    fun `the team stepper reports both directions`() {
        val steps = mutableListOf<Int>()
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                HomeScreen(
                    teamCount = 3,
                    onTeamCountChange = { steps += it },
                    onStartDraw = {},
                    onOpenSettings = {},
                    onOpenResult = {},
                )
            }
        }
        compose.onNodeWithContentDescription("more teams").performClick()
        compose.onNodeWithContentDescription("fewer teams").performClick()
        assertEquals(listOf(4, 2), steps)
    }

    // ---- settings -----------------------------------------------------------

    private fun settingsContent(
        settings: Settings = Settings(),
        onHaptics: (Boolean) -> Unit = {},
        onDim: (Boolean) -> Unit = {},
        onTheme: (ThemePreference) -> Unit = {},
        onTiming: (RevealTiming) -> Unit = {},
        onStep: (Int) -> Unit = {},
    ) {
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                SettingsScreen(
                    settings = settings,
                    onThemePreferenceChange = onTheme,
                    onHapticsChange = onHaptics,
                    onDimChange = onDim,
                    onCountdownStep = onStep,
                    onRevealTimingChange = onTiming,
                    onDone = {},
                )
            }
        }
    }

    @Test
    fun `settings shows the version and every control`() {
        settingsContent()
        compose.onNodeWithText("HAPTICS").assertIsDisplayed()
        compose.onNodeWithText("DIM MODE").assertIsDisplayed()
        // The screen scrolls, so everything past the fold has to be reached
        // rather than merely present - which also proves it is reachable.
        compose.onNodeWithText("APPEARANCE").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("COUNTDOWN").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SHOTGUN!").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `toggling haptics reports the opposite of what it was`() {
        var value: Boolean? = null
        settingsContent(settings = Settings(haptics = true), onHaptics = { value = it })
        compose.onNodeWithTag(TAG_HAPTICS_TOGGLE).performClick()
        assertEquals(false, value)
    }

    @Test
    fun `toggling dim reports the opposite of what it was`() {
        var value: Boolean? = null
        settingsContent(settings = Settings(dim = false), onDim = { value = it })
        compose.onNodeWithTag(TAG_DIM_TOGGLE).performClick()
        assertEquals(true, value)
    }

    @Test
    fun `picking a theme reports it`() {
        var picked: ThemePreference? = null
        settingsContent(onTheme = { picked = it })
        compose.onNodeWithText("Light").performClick()
        assertEquals(ThemePreference.LIGHT, picked)
    }

    @Test
    fun `the countdown is shown in seconds, not milliseconds`() {
        settingsContent(settings = Settings(countdownMillis = 2_500))
        compose.onNodeWithText("2.5s").performScrollTo().assertIsDisplayed()
    }

    // ---- result -------------------------------------------------------------

    @Test
    fun `an empty result screen says there is nothing yet`() {
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                ResultScreen(winners = emptyList(), latest = null, totalDraws = 0, onClose = {})
            }
        }
        compose.onNodeWithText("FAIRNESS").assertIsDisplayed()
        compose.onNodeWithText("NO DRAWS YET").assertIsDisplayed()
    }

    @Test
    fun `closing the result screen calls back`() {
        var closed = false
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                ResultScreen(
                    winners = emptyList(),
                    latest = null,
                    totalDraws = 0,
                    onClose = { closed = true },
                )
            }
        }
        compose.onNodeWithText("CLOSE").performClick()
        assertTrue(closed)
    }

    @Test
    fun `a result with history renders the field and the last draw`() {
        val record = DrawRecord(
            mode = DrawMode.ORDER,
            teamCount = null,
            timestamp = 0L,
            points = listOf(
                DrawPoint(x = 0.25f, y = 0.25f, won = true, assignment = 1),
                DrawPoint(x = 0.75f, y = 0.75f, won = false, assignment = 2),
            ),
        )
        compose.setContent {
            ShotgunTheme(ThemePreference.LIGHT) {
                ResultScreen(
                    winners = listOf(DrawPoint(0.25f, 0.25f, won = true, assignment = 1)),
                    latest = record,
                    totalDraws = 1,
                    onClose = {},
                )
            }
        }
        compose.onNodeWithText("FAIRNESS").assertIsDisplayed()
        compose.onNodeWithText("COLD").assertIsDisplayed()
        compose.onNodeWithText("HOT").assertIsDisplayed()
    }

    /**
     * Each mode labels its own result, and only teams carries a count - a
     * record written in one mode must never be described as another.
     */
    @Test
    fun `a starter result is labelled as a starting player`() {
        resultWith(DrawMode.STARTER, teamCount = null, assignment = null)

        compose.onNodeWithText("Starting player").assertIsDisplayed()
        compose.onNodeWithText("2 PLAYERS").assertIsDisplayed()
    }

    @Test
    fun `a teams result is labelled with how many teams`() {
        resultWith(DrawMode.TEAMS, teamCount = 3, assignment = 0)

        compose.onNodeWithText("3 teams").assertIsDisplayed()
        compose.onNodeWithText("3 TEAMS · 2 PLAYERS").assertIsDisplayed()
    }

    @Test
    fun `an order result counts the players in it`() {
        resultWith(DrawMode.ORDER, teamCount = null, assignment = 1)

        compose.onNodeWithText("Player order").assertIsDisplayed()
        compose.onNodeWithText("2 IN ORDER").assertIsDisplayed()
    }

    private fun resultWith(mode: DrawMode, teamCount: Int?, assignment: Int?) {
        val record = DrawRecord(
            mode = mode,
            teamCount = teamCount,
            timestamp = 0L,
            points = listOf(
                DrawPoint(x = 0.3f, y = 0.3f, won = mode != DrawMode.TEAMS, assignment = assignment),
                DrawPoint(x = 0.7f, y = 0.7f, won = false, assignment = assignment?.plus(1)),
            ),
        )
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                ResultScreen(winners = emptyList(), latest = record, totalDraws = 1, onClose = {})
            }
        }
    }

    /**
     * The source link opens a browser, and nothing guarantees one exists. The
     * guard around it is why a phone with no browser does not take the app down
     * with it, so the tap has to be exercised rather than assumed harmless.
     */
    @Test
    fun `the source link can be tapped without a browser to open`() {
        settingsContent()

        compose.onNodeWithText("SOURCE →").performScrollTo().performClick()
        compose.onNodeWithText("SOURCE →").assertIsDisplayed()
    }
}
