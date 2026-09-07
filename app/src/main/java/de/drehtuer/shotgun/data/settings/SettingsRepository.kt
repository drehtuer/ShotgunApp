package de.drehtuer.shotgun.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.drehtuer.shotgun.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the draw reveals its result. */
enum class RevealTiming { SUSPENSE, INSTANT }

/** Everything the settings screen owns. */
data class Settings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val haptics: Boolean = true,
    val dim: Boolean = true,
    /** Held in milliseconds so the stepper can move in half seconds. */
    val countdownMillis: Int = DEFAULT_COUNTDOWN_MILLIS,
    val revealTiming: RevealTiming = RevealTiming.INSTANT,
) {
    companion object {
        const val DEFAULT_COUNTDOWN_MILLIS = 3_500

        /** The stepper moves in half seconds; there is no ceiling worth enforcing. */
        const val COUNTDOWN_STEP_MILLIS = 500

        /** One step is also the floor. */
        const val MIN_COUNTDOWN_MILLIS = COUNTDOWN_STEP_MILLIS
    }
}

/** "2s" or "2.5s" - the trailing ".0" is noise on a control this small. */
fun formatCountdown(millis: Int): String =
    if (millis % 1_000 == 0) "${millis / 1_000}s" else "${millis / 1_000}.${(millis % 1_000) / 100}s"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes [Settings]. Backed by DataStore, so writes survive death. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DIM = booleanPreferencesKey("dim")
        val COUNTDOWN = intPreferencesKey("countdown_millis")

        /** Pre-0.5s-step key, read once so an existing setting is not lost. */
        val LEGACY_COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val TIMING = stringPreferencesKey("reveal_timing")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setThemePreference(value: ThemePreference) = edit { it[Keys.THEME] = value.name }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.HAPTICS] = value }
    suspend fun setDim(value: Boolean) = edit { it[Keys.DIM] = value }
    suspend fun setRevealTiming(value: RevealTiming) = edit { it[Keys.TIMING] = value.name }

    suspend fun setCountdownMillis(value: Int) = edit {
        it[Keys.COUNTDOWN] = value.coerceAtLeast(Settings.MIN_COUNTDOWN_MILLIS)
    }

    /**
     * Steps the countdown by [steps] half-seconds, reading and writing inside
     * one transaction.
     *
     * Writing an absolute "current + one step" looks equivalent and is not: the
     * value comes back asynchronously, so two quick taps both read the old
     * number and write the same result - the second tap is silently lost. That
     * is why the countdown appeared to ignore the setting.
     */
    suspend fun stepCountdown(steps: Int) = edit { prefs ->
        prefs[Keys.COUNTDOWN] = steppedCountdown(
            stored = prefs[Keys.COUNTDOWN],
            legacySeconds = prefs[Keys.LEGACY_COUNTDOWN_SECONDS],
            steps = steps,
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}

/**
 * The countdown a step applies to, resolved and stepped.
 *
 * Pulled out of [SettingsRepository.stepCountdown] so it can be tested without
 * a DataStore. The fallback to [legacySeconds] is a migration that runs once
 * per upgrade and never again - exactly the kind of path that is never
 * exercised by hand, so it is pinned here instead.
 */
internal fun steppedCountdown(stored: Int?, legacySeconds: Int?, steps: Int): Int {
    val current = stored
        ?: legacySeconds?.times(1_000)
        ?: Settings.DEFAULT_COUNTDOWN_MILLIS
    return (current + steps * Settings.COUNTDOWN_STEP_MILLIS)
        .coerceAtLeast(Settings.MIN_COUNTDOWN_MILLIS)
}

/**
 * Stored values are read defensively: a preference written by a newer build, or
 * corrupted, falls back to the default rather than crashing the app on launch.
 */
internal fun Preferences.toSettings(): Settings {
    val defaults = Settings()
    return Settings(
        themePreference = this[stringPreferencesKey("theme_preference")]
            ?.let { name -> runCatching { ThemePreference.valueOf(name) }.getOrNull() }
            ?: defaults.themePreference,
        haptics = this[booleanPreferencesKey("haptics")] ?: defaults.haptics,
        dim = this[booleanPreferencesKey("dim")] ?: defaults.dim,
        // Falls back to the pre-0.5s-step key so an existing setting survives.
        countdownMillis = (
            this[intPreferencesKey("countdown_millis")]
                ?: this[intPreferencesKey("countdown_seconds")]?.times(1_000)
                ?: defaults.countdownMillis
            ).coerceAtLeast(Settings.MIN_COUNTDOWN_MILLIS),
        revealTiming = this[stringPreferencesKey("reveal_timing")]
            ?.let { name -> runCatching { RevealTiming.valueOf(name) }.getOrNull() }
            ?: defaults.revealTiming,
    )
}
