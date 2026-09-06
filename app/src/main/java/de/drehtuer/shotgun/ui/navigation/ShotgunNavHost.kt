package de.drehtuer.shotgun.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.drehtuer.shotgun.ui.screens.DrawScreen
import de.drehtuer.shotgun.ui.screens.HomeScreen
import de.drehtuer.shotgun.ui.screens.ResultScreen
import de.drehtuer.shotgun.ui.screens.SettingsScreen
import de.drehtuer.shotgun.data.DrawPoint
import de.drehtuer.shotgun.data.DrawRecord
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.draw.DrawOutcome
import de.drehtuer.shotgun.ui.theme.ThemePreference

@Composable
fun ShotgunNavHost(
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onDimChange: (Boolean) -> Unit,
    onCountdownChange: (Int) -> Unit,
    onRevealTimingChange: (RevealTiming) -> Unit,
    teamCount: Int,
    onTeamCountChange: (Int) -> Unit,
    settings: Settings,
    onDrawComplete: (DrawOutcome, Float, Float) -> Unit,
    winners: List<DrawPoint>,
    latestDraw: DrawRecord?,
    drawCount: Int,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                teamCount = teamCount,
                onTeamCountChange = onTeamCountChange,
                onStartDraw = { mode -> navController.navigate(Destination.Draw.routeFor(mode)) },
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
                onOpenResult = { navController.navigate(Destination.Result.route) },
            )
        }
        composable(
            route = Destination.Draw.route,
            arguments = listOf(navArgument(Destination.Draw.ARG_MODE) { type = NavType.StringType }),
        ) { entry ->
            val mode = entry.arguments
                ?.getString(Destination.Draw.ARG_MODE)
                ?.let(DrawMode::valueOf)
                ?: DrawMode.STARTER
            DrawScreen(
                mode = mode,
                teamCount = teamCount,
                settings = settings,
                onBack = { navController.popBackStack() },
                onOpenResult = { navController.navigate(Destination.Result.route) },
                onDrawComplete = onDrawComplete,
            )
        }
        composable(Destination.Result.route) {
            ResultScreen(
                winners = winners,
                latest = latestDraw,
                totalDraws = drawCount,
                onClose = { navController.popBackStack() },
            )
        }
        composable(Destination.Settings.route) {
            SettingsScreen(
                settings = settings,
                onThemePreferenceChange = onThemePreferenceChange,
                onHapticsChange = onHapticsChange,
                onDimChange = onDimChange,
                onCountdownChange = onCountdownChange,
                onRevealTimingChange = onRevealTimingChange,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
