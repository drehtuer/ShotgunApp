package de.drehtuer.shotgun.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
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

/**
 * Navigates only from a screen that is actually resumed.
 *
 * Two taps in quick succession, or a tap racing the back button, otherwise
 * queue navigations against a destination that is already leaving - which can
 * leave the graph with nothing to show, and the app drawing an empty window
 * that only a restart clears.
 */
/**
 * Pops only when there is something underneath.
 *
 * A bare `popBackStack()` will happily pop the last entry, and a graph with no
 * destination renders nothing at all - the app stays alive and resumed showing
 * an empty window that only a restart clears. Two pops racing each other is
 * enough to do it: a button tapped twice, or a tap arriving with the back
 * gesture.
 *
 * @return true if it popped, false if this was the last screen.
 */
internal fun NavController.popSafely(): Boolean =
    if (previousBackStackEntry != null) popBackStack() else false

private fun NavController.navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        navigate(route, builder)
    }
}

@Composable
fun ShotgunNavHost(
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onDimChange: (Boolean) -> Unit,
    onCountdownStep: (Int) -> Unit,
    onRevealTimingChange: (RevealTiming) -> Unit,
    teamCount: Int,
    onTeamCountChange: (Int) -> Unit,
    settings: Settings,
    onDrawComplete: (DrawOutcome, Float, Float) -> Unit,
    winners: List<DrawPoint>,
    latestDraw: DrawRecord?,
    drawCount: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {

    // All back handling is done here, in one place, rather than left to the
    // NavHost's own handler racing anything else. Back either pops to a real
    // destination or closes the app - it can never pop the graph empty, which
    // is what leaves the window drawn but blank.
    BackHandler {
        // Pop to a real destination, or close the app. Never pop to nothing.
        if (!navController.popSafely()) onExit()
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                teamCount = teamCount,
                onTeamCountChange = onTeamCountChange,
                onStartDraw = { mode -> navController.navigateOnce(Destination.Draw.routeFor(mode)) },
                onOpenSettings = { navController.navigateOnce(Destination.Settings.route) },
                onOpenResult = { navController.navigateOnce(Destination.Result.route) },
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
                onBack = { navController.popSafely() },
                onOpenResult = {
                    // Drop the draw surface on the way to the result, so
                    // closing the result returns to the modes rather than
                    // dumping you back into another round of the same one.
                    navController.navigateOnce(Destination.Result.route) {
                        popUpTo(Destination.Home.route)
                    }
                },
                onDrawComplete = onDrawComplete,
            )
        }
        composable(Destination.Result.route) {
            ResultScreen(
                winners = winners,
                latest = latestDraw,
                totalDraws = drawCount,
                onClose = { navController.popSafely() },
            )
        }
        composable(Destination.Settings.route) {
            SettingsScreen(
                settings = settings,
                onThemePreferenceChange = onThemePreferenceChange,
                onHapticsChange = onHapticsChange,
                onDimChange = onDimChange,
                onCountdownStep = onCountdownStep,
                onRevealTimingChange = onRevealTimingChange,
                onDone = { navController.popSafely() },
            )
        }
    }
}
