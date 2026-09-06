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

/** Everything the settings screen owns. Defaults match the design export. */
data class Settings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val haptics: Boolean = true,
    val dim: Boolean = true,
    val countdownSeconds: Int = 3,
    val revealTiming: RevealTiming = RevealTiming.SUSPENSE,
) {
    companion object {
        /** The design's stepper floor; there is no ceiling worth enforcing. */
        const val MIN_COUNTDOWN_SECONDS = 1
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes [Settings]. Backed by DataStore, so writes survive death. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DIM = booleanPreferencesKey("dim")
        val COUNTDOWN = intPreferencesKey("countdown_seconds")
        val TIMING = stringPreferencesKey("reveal_timing")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setThemePreference(value: ThemePreference) = edit { it[Keys.THEME] = value.name }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.HAPTICS] = value }
    suspend fun setDim(value: Boolean) = edit { it[Keys.DIM] = value }
    suspend fun setRevealTiming(value: RevealTiming) = edit { it[Keys.TIMING] = value.name }

    suspend fun setCountdownSeconds(value: Int) = edit {
        it[Keys.COUNTDOWN] = value.coerceAtLeast(Settings.MIN_COUNTDOWN_SECONDS)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
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
        countdownSeconds = (this[intPreferencesKey("countdown_seconds")] ?: defaults.countdownSeconds)
            .coerceAtLeast(Settings.MIN_COUNTDOWN_SECONDS),
        revealTiming = this[stringPreferencesKey("reveal_timing")]
            ?.let { name -> runCatching { RevealTiming.valueOf(name) }.getOrNull() }
            ?: defaults.revealTiming,
    )
}
