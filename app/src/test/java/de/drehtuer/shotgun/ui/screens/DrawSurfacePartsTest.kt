package de.drehtuer.shotgun.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.drehtuer.shotgun.draw.DrawOutcome
import de.drehtuer.shotgun.draw.DrawPhase
import de.drehtuer.shotgun.draw.Finger
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
 * The draw surface's pieces, rendered one at a time.
 *
 * Driving the whole surface needs fingers, and **Compose's pointer injection
 * does not reach it under Robolectric** - see `build-environment.md`. So the
 * states the surface reaches only mid-draw are covered by rendering each piece
 * with the state it would have had: a ring for each role the draw can give a
 * finger, the refusal bar, the hint at nought fingers and at one.
 *
 * What this does *not* cover is the surface driving itself into those states.
 * That is `app/src/androidTest/`, on the phone.
 */
@RunWith(RobolectricTestRunner::class)
class DrawSurfacePartsTest {

    @get:Rule
    val compose = createComposeRule()

    // Robolectric's default screen is small - 320x470dp - so a ring placed at
    // phone-sized coordinates lands off it and reads as "not displayed". These
    // also sit low enough that a label drawn *beside* the ring, which is where
    // it goes while the finger is still down, has room above it.
    private val fingers = listOf(Finger(1L, 80f, 200f), Finger(2L, 160f, 300f))

    private fun outcome(mode: DrawMode, teamCount: Int? = null) = DrawOutcome(
        mode = mode,
        teamCount = teamCount,
        assignment = if (mode == DrawMode.TEAMS) mapOf(1L to 0, 2L to 1) else mapOf(1L to 1, 2L to 2),
        order = listOf(1L, 2L),
        winnerId = 1L,
        fingers = fingers,
    )

    /**
     * The surface is a full-screen field and its rings place themselves by
     * absolute offset, so the host has to fill the screen too - a Box that
     * wraps its content clips every ring straight back out of view.
     */
    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) { Box(Modifier.fillMaxSize()) { content() } }
        }
    }

    // ---- the hint -----------------------------------------------------------

    @Test
    fun `with nobody on the glass the hint asks for fingers`() {
        show { Hint(fingerCount = 0) }

        compose.onNodeWithText("EVERYONE, ONE FINGER DOWN.").assertIsDisplayed()
    }

    /** One finger is not a draw: there would be nobody to beat. */
    @Test
    fun `with one finger down the hint asks for one more`() {
        show { Hint(fingerCount = 1) }

        compose.onNodeWithText("ONE MORE FINGER.").assertIsDisplayed()
        compose.onNodeWithText("At least two players are needed to draw.").assertIsDisplayed()
    }

    // ---- refusal and the way out --------------------------------------------

    @Test
    fun `a refusal says it could not draw, and why`() {
        show { Refusal(message = "3 fingers can't fill 5 teams") }

        compose.onNodeWithText("CAN'T DRAW").assertIsDisplayed()
        compose.onNodeWithText("3 fingers can't fill 5 teams").assertIsDisplayed()
    }

    @Test
    fun `the reveal bar opens the result`() {
        var opened = false
        show { RevealBar(onOpenResult = { opened = true }) }

        compose.onNodeWithText("DETAILS").performClick()
        compose.runOnIdle { assertTrue("DETAILS did not fire", opened) }
    }

    @Test
    fun `the edge glow renders at any strength`() {
        show { EdgeGlow(alpha = 0.5f) }

        compose.waitForIdle()
    }

    // ---- a ring for every role a draw can give a finger ----------------------

    @Test
    fun `an idle ring carries no answer yet`() {
        show {
            FingerRing(
                finger = fingers.first(),
                outcome = null,
                phase = DrawPhase.IDLE,
                revealed = false,
                lifted = false,
            )
        }

        compose.waitForIdle()
    }

    @Test
    fun `an order ring shows its rank`() {
        show {
            FingerRing(
                finger = fingers.first(),
                outcome = outcome(DrawMode.ORDER),
                phase = DrawPhase.REVEALED,
                revealed = true,
                lifted = false,
            )
        }

        compose.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `a teams ring shows its team letter and says what it is`() {
        show {
            FingerRing(
                finger = fingers.first(),
                outcome = outcome(DrawMode.TEAMS, teamCount = 2),
                phase = DrawPhase.REVEALED,
                revealed = true,
                lifted = false,
            )
        }

        compose.onNodeWithText("A").assertIsDisplayed()
        compose.onNodeWithText("TEAM").assertIsDisplayed()
    }

    /**
     * The rule the labels exist under: a label sits beside the ring while the
     * finger is down and slides into it once the finger lifts, because a
     * fingertip covers the ring's centre.
     */
    @Test
    fun `a lifted ring still carries its answer`() {
        show {
            FingerRing(
                finger = fingers.first(),
                outcome = outcome(DrawMode.ORDER),
                phase = DrawPhase.REVEALED,
                revealed = true,
                lifted = true,
            )
        }

        compose.onNodeWithText("1").assertIsDisplayed()
    }

    /** A finger whose turn has not been revealed yet must give nothing away. */
    @Test
    fun `an unrevealed ring in a staged reveal shows no rank`() {
        show {
            FingerRing(
                finger = fingers[1],
                outcome = outcome(DrawMode.ORDER),
                phase = DrawPhase.REVEALING,
                revealed = false,
                lifted = false,
            )
        }

        assertEquals(0, compose.onAllNodesWithText("2").fetchSemanticsNodes().size)
    }

    // ---- the mode label -----------------------------------------------------

    @Test
    fun `each mode names itself on the bare surface`() {
        assertEquals("STARTING PLAYER", DrawMode.STARTER.label(teamCount = 3))
        assertEquals("PLAYER ORDER", DrawMode.ORDER.label(teamCount = 3))
        assertEquals("3 TEAMS", DrawMode.TEAMS.label(teamCount = 3))
    }
}
