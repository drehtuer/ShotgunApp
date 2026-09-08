package de.drehtuer.shotgun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.drehtuer.shotgun.ui.components.NotBuiltYet
import de.drehtuer.shotgun.ui.components.Rule
import de.drehtuer.shotgun.ui.components.ScreenHeader
import de.drehtuer.shotgun.ui.components.ShotgunWordmark
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import de.drehtuer.shotgun.ui.theme.ThemePreference
import org.junit.Assert.assertTrue
import org.junit.Rule as JUnitRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The shared chrome: the rules, the header and the identity mark.
 *
 * **This test lives one package up from what it tests, deliberately.** The app
 * has a `Rule()` composable, and inside `ui.components` that name also has to
 * serve as JUnit's `@Rule` - which does not fail with an error but crashes the
 * Kotlin backend outright ("Exception during IR lowering", an NPE in
 * `JvmAnnotationImplementationTransformer`, naming only the file). The import
 * alias below is the second half of the fix; either alone is enough, and both
 * together mean nobody has to rediscover it.
 */
@RunWith(RobolectricTestRunner::class)
class ComponentRenderTest {

    @get:JUnitRule
    val compose = createComposeRule()

    @Test
    fun `the screen header shows its title and fires its action`() {
        var acted = false
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                ScreenHeader(title = "SETTINGS", actionLabel = "DONE", onAction = { acted = true })
            }
        }

        compose.onNodeWithText("SETTINGS").assertIsDisplayed()
        compose.onNodeWithText("DONE").performClick()
        compose.runOnIdle { assertTrue("DONE did not fire", acted) }
    }

    /** The accent variant is the one used where the action is the point. */
    @Test
    fun `the header action can be set in the accent colour`() {
        compose.setContent {
            ShotgunTheme(ThemePreference.LIGHT) {
                ScreenHeader(
                    title = "RESULT",
                    actionLabel = "CLOSE",
                    onAction = {},
                    actionInAccent = true,
                )
            }
        }

        compose.onNodeWithText("RESULT").assertIsDisplayed()
        compose.onNodeWithText("CLOSE").assertIsDisplayed()
    }

    @Test
    fun `a rule and the wordmark render in the dark palette`() {
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) {
                Column {
                    Rule()
                    ShotgunWordmark()
                }
            }
        }

        compose.onNodeWithText("SHOTGUN!").assertIsDisplayed()
    }

    /** The mark is drawn rather than an asset, so both palettes are exercised. */
    @Test
    fun `a rule and the wordmark render in the light palette`() {
        compose.setContent {
            ShotgunTheme(ThemePreference.LIGHT) {
                Column {
                    Rule()
                    ShotgunWordmark()
                }
            }
        }

        compose.onNodeWithText("SHOTGUN!").assertIsDisplayed()
    }

    @Test
    fun `an unbuilt screen says so, and says what it is waiting on`() {
        compose.setContent {
            ShotgunTheme(ThemePreference.DARK) { NotBuiltYet(note = "Waiting on the colour list") }
        }

        compose.onNodeWithText("Waiting on the colour list").assertIsDisplayed()
    }
}
