package de.drehtuer.shotgun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.drehtuer.shotgun.ui.navigation.ShotgunNavHost
import de.drehtuer.shotgun.ui.theme.PPTheme
import de.drehtuer.shotgun.ui.theme.ShotgunTheme
import de.drehtuer.shotgun.ui.util.DimScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = shotgunViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val teamCount by viewModel.teamCount.collectAsStateWithLifecycle()
            val winners by viewModel.winners.collectAsStateWithLifecycle()
            val latestDraw by viewModel.latestDraw.collectAsStateWithLifecycle()
            val drawCount by viewModel.drawCount.collectAsStateWithLifecycle()


            ShotgunTheme(preference = settings.themePreference) {
                // Brightness is an app-wide property, so it is applied here
                // rather than on the screen that happens to toggle it.
                DimScreen(enabled = settings.dim)

                ShotgunNavHost(
                    onThemePreferenceChange = viewModel::setThemePreference,
                    onHapticsChange = viewModel::setHaptics,
                    onDimChange = viewModel::setDim,
                    onCountdownStep = viewModel::stepCountdown,
                    onRevealTimingChange = viewModel::setRevealTiming,
                    teamCount = teamCount,
                    onTeamCountChange = viewModel::setTeamCount,
                    settings = settings,
                    onDrawComplete = viewModel::recordDraw,
                    winners = winners,
                    latestDraw = latestDraw,
                    drawCount = drawCount,
                    onExit = { finish() },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PPTheme.colors.bg)
                        .windowInsetsPadding(WindowInsets.systemBars),
                )
            }
        }
    }
}
