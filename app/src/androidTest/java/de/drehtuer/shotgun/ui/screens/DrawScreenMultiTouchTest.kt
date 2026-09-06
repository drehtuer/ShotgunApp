package de.drehtuer.shotgun.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
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

    @get:Rule
    val rule = createComposeRule()

    private fun setContent(
        mode: DrawMode = DrawMode.STARTER,
        teamCount: Int = 3,
        countdownSeconds: Int = 1,
    ) {
        rule.setContent {
            ShotgunTheme {
                DrawScreen(
                    mode = mode,
                    teamCount = teamCount,
                    settings = Settings(
                        countdownSeconds = countdownSeconds,
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
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
            down(3, Offset(400f, 1400f))
        }
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(3)
    }

    @Test
    fun liftingOnePointerRemovesOnlyItsRing() {
        setContent()
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(2)
        rule.onRoot().performTouchInput { up(2) }
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
        rule.onAllNodesWithTag(TAG_FINGER_RING).assertCountEquals(2)
    }

    @Test
    fun twoPointersArmTheCountdownAndItReveals() {
        setContent(countdownSeconds = 1)
        rule.onRoot().performTouchInput {
            down(1, Offset(200f, 600f))
            down(2, Offset(600f, 900f))
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(TAG_REVEAL_BAR).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(TAG_REVEAL_BAR).assertExists()
    }

    /** A single finger must never draw - there would be nobody to beat. */
    @Test
    fun onePointerNeverDraws() {
        setContent(countdownSeconds = 1)
        rule.onRoot().performTouchInput { down(1, Offset(200f, 600f)) }
        Thread.sleep(2_000)
        rule.onAllNodesWithTag(TAG_REVEAL_BAR).assertCountEquals(0)
    }

    @Test
    fun teamsRefusesWhenThereAreFewerFingersThanTeams() {
        setContent(mode = DrawMode.TEAMS, teamCount = 5, countdownSeconds = 1)
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
