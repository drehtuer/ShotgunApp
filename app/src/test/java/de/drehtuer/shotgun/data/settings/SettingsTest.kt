package de.drehtuer.shotgun.data.settings

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import de.drehtuer.shotgun.ui.theme.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stored settings are read defensively. A value written by a newer build, or a
 * corrupted one, must not stop the app launching - these pin that down.
 */
class SettingsTest {

    @Test
    fun `an empty store yields the design's defaults`() {
        val s = preferencesOf().toSettings()
        assertEquals(ThemePreference.SYSTEM, s.themePreference)
        assertEquals(true, s.haptics)
        assertEquals(true, s.dim)
        assertEquals(3, s.countdownSeconds)
        assertEquals(RevealTiming.SUSPENSE, s.revealTiming)
    }

    @Test
    fun `stored values are read back`() {
        val s = preferencesOf(
            stringPreferencesKey("theme_preference") to ThemePreference.DARK.name,
            stringPreferencesKey("reveal_timing") to RevealTiming.INSTANT.name,
            intPreferencesKey("countdown_seconds") to 7,
        ).toSettings()
        assertEquals(ThemePreference.DARK, s.themePreference)
        assertEquals(RevealTiming.INSTANT, s.revealTiming)
        assertEquals(7, s.countdownSeconds)
    }

    /** A theme written by a newer build must fall back, not crash on launch. */
    @Test
    fun `an unknown enum value falls back to the default`() {
        val s = preferencesOf(
            stringPreferencesKey("theme_preference") to "MIDNIGHT",
            stringPreferencesKey("reveal_timing") to "DRAMATIC",
        ).toSettings()
        assertEquals(ThemePreference.SYSTEM, s.themePreference)
        assertEquals(RevealTiming.SUSPENSE, s.revealTiming)
    }

    /** The design gives the countdown stepper a floor of one second. */
    @Test
    fun `countdown is clamped to the floor`() {
        assertEquals(1, preferencesOf(intPreferencesKey("countdown_seconds") to 0).toSettings().countdownSeconds)
        assertEquals(1, preferencesOf(intPreferencesKey("countdown_seconds") to -5).toSettings().countdownSeconds)
    }
}
