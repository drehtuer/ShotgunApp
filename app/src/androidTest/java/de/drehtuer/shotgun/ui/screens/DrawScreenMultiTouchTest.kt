package de.drehtuer.shotgun.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import org.junit.Rule
import org.junit.Test

/**
 * The only way to exercise multi-touch without hardware.
 *
 * The emulator cannot inject genuine multi-pointer gestures - it exposes one
 * input device per contact - so these drive the composable through Compose's
 * own pointer injection instead. It is not the same as real fingers, but it
 * covers the tracking that unit tests cannot reach: several pointers alive at
 * once, each with its own id.
 */
class DrawScreenMultiTouchTest {

    /**
     * Long enough that no draw fires while a tracking test runs. Before the
     * timers were fixed these tests passed with a 1s countdown purely because
     * the countdown never completed - the bug was holding the tests up.
     */
    private val NO_DRAW_MILLIS = 60_000

    private var frozen = false

    @get:Rule
    val rule = createComposeRule()

    /**
     * With the clock frozen, nothing recomposes until frames are pumped by
     * hand. A few is enough to publish a pointer change without letting a
     * 60-second countdown anywhere near its deadline.
     */
    private fun settle() {
        // Generous: once two fingers are down the countdown's own frame loop
        // consumes frames too, so a handful are needed before the pointer
        // change has actually been drawn.
        if (frozen) repeat(12) { rule.mainClock.advanceTimeByFrame() }
    }

    /**
     * @param liveClock let frames run, so the countdown can actually complete.
     *   The tracking tests freeze it instead: an armed countdown drives a frame
     *   loop, which holds Compose permanently busy, and any assertion that
     *   waits for idle would time out rather than fail.
     */
    private fun setContent(
        mode: DrawMode = DrawMode.STARTER,
        teamCount: Int = 3,
        countdownMillis: Int = NO_DRAW_MILLIS,
        liveClock: Boolean = false,
    ) {
        rule.mainClock.autoAdvance = liveClock
        frozen = !liveClock
        rule.setContent {
            ShotgunTheme {
                DrawScreen(
                    mode = mode,
                    teamCount = teamCount,
                    settings = Settings(
                        countdownMillis = countdownMillis,
                        revealTiming = RevealTiming.INSTANT,
                        haptics = false,
                    ),
                    onBack = {},
                    onOpenResult = {},
                    onDrawComplete = { _, _, _ -> },
                )
            }
        }
    }

    @Test
    fun everyPointerGetsItsOwnRing() {
        setContent()
        // Landed one at a time, as fingers actually arrive. Injecting three in
        // a single gesture silently delivered only two.
        rule.onRoot().performTouchInput { down(1, Offset(200f, 600f)) }
        settle()
        rule.onRoot().performTouchInput { down(2, Offset(600f, 900f)) }
        settle()
        rule.onRoot().performTouchInput { down(3, Offset(400f, 1400f)) }
        settle()
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(3)
    }

    /** MODES has to work, or a shown result cannot be dismissed at all. */
    @Test
    fun theBackLinkIsClickableWhenNoFingersAreDown() {
        var backed = false
        rule.setContent {
            ShotgunTheme {
                DrawScreen(
                    mode = DrawMode.STARTER,
                    teamCount = 3,
                    settings = Settings(countdownMillis = 1_000, haptics = false),
                    onBack = { backed = true },
                    onOpenResult = {},
                    onDrawComplete = { _, _, _ -> },
                )
            }
        }
        settle()
        rule.onNodeWithText("← MODES").performClick()
        rule.runOnIdle { assert(backed) { "MODES did not fire" } }
    }

    @Test
    fun liftingOnePointerRemovesOnlyItsRing() {
        setContent()
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        settle()
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(2)
        rule.onRoot().performTouchInput { up(2) }
        settle()
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(1)
    }

    /** The rule the hint text teaches: moving is repositioning, not joining. */
    @Test
    fun movingAPointerDoesNotAddAPlayer() {
        setContent()
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        rule.onRoot().performTouchInput {
            moveTo(1, Offset(300f, 700f))
            moveTo(2, Offset(700f, 1000f))
        }
        settle()
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(2)
    }

    @Test
    fun twoPointersArmTheCountdownAndItReveals() {
        setContent(countdownMillis = 1_000, liveClock = true)
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TAG_REVEAL_BAR).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TAG_REVEAL_BAR).assertExists()
    }

    /**
     * Regression: a hand resting on glass jitters, and the countdown and reveal
     * timers were keyed on a counter that every move bumped - so they were
     * cancelled and restarted forever and nothing resolved until the fingers
     * came off. Moving throughout must not stop the draw from landing.
     */
    @Test
    fun theDrawStillLandsWhileFingersAreMoving() {
        setContent(countdownMillis = 1_000, liveClock = true)
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        // Keep both pointers alive and twitching, as real fingers do.
        repeat(25) { step ->
            rule.onRoot().performTouchInput {
                moveTo(1, Offset(200f + (step % 3), 600f + (step % 2)))
                moveTo(2, Offset(600f - (step % 3), 900f - (step % 2)))
            }
            Thread.sleep(60)
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TAG_REVEAL_BAR).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TAG_REVEAL_BAR).assertExists()
    }

    /** A single finger must never draw - there would be nobody to beat. */
    @Test
    fun onePointerNeverDraws() {
        setContent(countdownMillis = 1_000, liveClock = true)
        rule.onRoot().performTouchInput { down(1, Offset(200f, 600f)) }
        Thread.sleep(2_000)
        rule.onAllNodesWithTag(TAG_REVEAL_BAR).assertCountEquals(0)
    }

    @Test
    fun teamsRefusesWhenThereAreFewerFingersThanTeams() {
        setContent(mode = DrawMode.TEAMS, teamCount = 5, countdownMillis = 1_000, liveClock = true)
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TAG_REFUSAL).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TAG_REFUSAL).assertExists()
    }
}
