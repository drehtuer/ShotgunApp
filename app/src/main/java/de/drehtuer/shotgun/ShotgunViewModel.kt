package de.drehtuer.shotgun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import de.drehtuer.shotgun.data.DrawHistory
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.data.settings.SettingsRepository
import de.drehtuer.shotgun.ui.theme.ThemePreference
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import de.drehtuer.shotgun.ui.screens.clampTeamCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Owns settings for the whole app; the screens read and write through it. */
class ShotgunViewModel(
    private val repository: SettingsRepository,
    val history: DrawHistory,
) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // Until the first read completes the defaults apply, which is what a
        // fresh install would see anyway.
        initialValue = Settings(),
    )

    /**
     * How many teams to draw for. Deliberately not persisted: the design keeps
     * it alongside the draw rather than in settings, so it resets with the app
     * the way the mode does.
     */
    private val _teamCount = MutableStateFlow(DEFAULT_TEAMS)
    val teamCount: StateFlow<Int> = _teamCount.asStateFlow()

    /** Floored at two; the design puts no ceiling on it. */
    fun setTeamCount(value: Int) {
        _teamCount.value = clampTeamCount(value)
    }

    fun setThemePreference(value: ThemePreference) = update { repository.setThemePreference(value) }
    fun setHaptics(value: Boolean) = update { repository.setHaptics(value) }
    fun setDim(value: Boolean) = update { repository.setDim(value) }
    fun setCountdownSeconds(value: Int) = update { repository.setCountdownSeconds(value) }
    fun setRevealTiming(value: RevealTiming) = update { repository.setRevealTiming(value) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        /** The design's starting value. */
        const val DEFAULT_TEAMS = 3

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ShotgunApplication
                ShotgunViewModel(app.settings, app.history)
            }
        }
    }
}

/** Scoped to the activity, so every screen sees the same settings. */
@Composable
fun shotgunViewModel(): ShotgunViewModel = viewModel(factory = ShotgunViewModel.Factory)
