package de.drehtuer.shotgun.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import de.drehtuer.shotgun.data.DrawPoint
import de.drehtuer.shotgun.data.DrawRecord
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class ResultScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(winners: List<DrawPoint>, latest: DrawRecord?) {
        rule.setContent {
            ShotgunTheme {
                ResultScreen(
                    winners = winners,
                    latest = latest,
                    totalDraws = winners.size,
                    onClose = {},
                )
            }
        }
    }

    private fun randomWinners(n: Int): List<DrawPoint> {
        val rng = Random(1)
        return List(n) { DrawPoint(rng.nextFloat(), rng.nextFloat(), won = true, assignment = null) }
    }

    @Test
    fun withNoHistoryItSaysSoRatherThanShowingAnEmptyField() {
        show(emptyList(), null)
        rule.onNodeWithText("NO DRAWS YET").assertIsDisplayed()
        rule.onNodeWithText("0 WINNERS PLOTTED").assertIsDisplayed()
    }

    @Test
    fun theFieldRendersOnceThereIsHistory() {
        show(randomWinners(50), null)
        rule.onNodeWithTag(TAG_FAIRNESS_FIELD).assertIsDisplayed()
        rule.onNodeWithText("50 WINNERS PLOTTED").assertIsDisplayed()
    }

    @Test
    fun theLastDrawIsSummarisedAboveTheField() {
        val latest = DrawRecord(
            mode = DrawMode.TEAMS,
            teamCount = 3,
            timestamp = 1,
            points = List(6) { DrawPoint(0.5f, 0.5f, won = false, assignment = it % 3) },
        )
        show(randomWinners(20), latest)
        rule.onNodeWithText("3 teams").assertIsDisplayed()
        rule.onNodeWithText("3 TEAMS · 6 PLAYERS").assertIsDisplayed()
    }

    /** A large history must not stall the screen; the field is downsampled. */
    @Test
    fun aLargeHistoryStillRenders() {
        show(randomWinners(320), null)
        rule.onNodeWithTag(TAG_FAIRNESS_FIELD).assertIsDisplayed()
        rule.onNodeWithText("320 WINNERS PLOTTED").assertIsDisplayed()
    }
}
