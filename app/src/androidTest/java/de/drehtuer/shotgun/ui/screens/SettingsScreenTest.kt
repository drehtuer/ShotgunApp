package de.drehtuer.shotgun.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import de.drehtuer.shotgun.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var settings = Settings()
    private var lastCountdown: Int? = null
    private var lastTiming: RevealTiming? = null
    private var lastTheme: ThemePreference? = null
    private var lastHaptics: Boolean? = null
    private var lastDim: Boolean? = null

    private fun show(initial: Settings = Settings()) {
        settings = initial
        rule.setContent {
            ShotgunTheme {
                SettingsScreen(
                    settings = settings,
                    onThemePreferenceChange = { lastTheme = it },
                    onHapticsChange = { lastHaptics = it },
                    onDimChange = { lastDim = it },
                    onCountdownStep = { lastCountdown = it },
                    onRevealTimingChange = { lastTiming = it },
                    onDone = {},
                )
            }
        }
    }

    /** The screen scrolls, so the lower sections must be scrolled into view. */
    @Test
    fun everySettingFromTheDesignIsPresent() {
        show()
        listOf("HAPTICS", "DIM MODE", "APPEARANCE", "COUNTDOWN", "REVEAL")
            .forEach { rule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    /** SOUND was removed from the design; it must not reappear. */
    @Test
    fun soundIsNotOffered() {
        show()
        rule.onAllNodesWithTextSafely("SOUND").let { count ->
            assertEquals(0, count)
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextSafely(
        text: String,
    ): Int = onAllNodes(
        androidx.compose.ui.test.hasText(text)
    ).fetchSemanticsNodes().size

    @Test
    fun togglingHapticsReportsTheNewValue() {
        show(Settings(haptics = true))
        rule.onNodeWithTag(TAG_HAPTICS_TOGGLE).performClick()
        assertEquals(false, lastHaptics)
    }

    @Test
    fun togglingDimReportsTheNewValue() {
        show(Settings(dim = false))
        rule.onNodeWithTag(TAG_DIM_TOGGLE).performClick()
        assertEquals(true, lastDim)
    }

    @Test
    fun pickingAThemeReportsIt() {
        show()
        rule.onNodeWithText("Dark").performClick()
        assertEquals(ThemePreference.DARK, lastTheme)
    }

    @Test
    fun pickingRevealTimingReportsIt() {
        show()
        rule.onNodeWithText("Instant").performScrollTo().performClick()
        assertEquals(RevealTiming.INSTANT, lastTiming)
    }

    @Test
    fun theCountdownStepperReportsADeltaNotAnAbsoluteValue() {
        show(Settings(countdownMillis = 3_000))
        rule.onNodeWithContentDescriptionText("longer countdown").performScrollTo().performClick()
        assertEquals(+1, lastCountdown)
        rule.onNodeWithContentDescriptionText("shorter countdown").performScrollTo().performClick()
        assertEquals(-1, lastCountdown)
    }

    /** One second is the floor, so the control must not offer to go lower. */
    @Test
    fun theCountdownStepperCannotGoBelowItsFloor() {
        show(Settings(countdownMillis = Settings.MIN_COUNTDOWN_MILLIS))
        lastCountdown = null
        rule.onNodeWithContentDescriptionText("shorter countdown").performScrollTo().performClick()
        assertEquals(null, lastCountdown)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule
        .onNodeWithContentDescriptionText(text: String) =
        onNode(androidx.compose.ui.test.hasContentDescription(text))
}
