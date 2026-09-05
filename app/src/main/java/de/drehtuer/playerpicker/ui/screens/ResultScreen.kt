package de.drehtuer.playerpicker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.drehtuer.playerpicker.ui.components.NotBuiltYet
import de.drehtuer.playerpicker.ui.components.ScreenHeader
import de.drehtuer.playerpicker.ui.theme.PPTheme

/**
 * Scaffold for the result screen. The finished screen plots the last draw over
 * a fairness heatmap - a density field of logged winners, rendered on a Canvas
 * against [PPColors.heatRamp] - to show that no corner of the screen wins more.
 */
@Composable
fun ResultScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PPTheme.colors.bg),
    ) {
        ScreenHeader(title = "RESULT", actionLabel = "CLOSE", onAction = onClose)
        NotBuiltYet("The fairness heatmap and the last draw's dots land here.")
    }
}
