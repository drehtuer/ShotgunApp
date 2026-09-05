package de.drehtuer.playerpicker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drehtuer.playerpicker.ui.components.NotBuiltYet
import de.drehtuer.playerpicker.ui.components.Rule
import de.drehtuer.playerpicker.ui.navigation.DrawMode
import de.drehtuer.playerpicker.ui.theme.PPTheme

/**
 * Scaffold for the draw surface - the heart of the app, and the only screen
 * that needs real multi-touch. The finished screen tracks a ring per pointer,
 * arms a countdown once two fingers are down (each further finger extends it),
 * animates the edge glow with countdown progress, then reveals the draw.
 */
@Composable
fun DrawScreen(
    mode: DrawMode,
    onBack: () -> Unit,
    onOpenResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PPTheme.colors.bg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "← MODES",
                style = PPTheme.typography.micro,
                color = PPTheme.colors.dim,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            Text(
                text = mode.label(),
                style = PPTheme.typography.micro,
                color = PPTheme.colors.accent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        Rule()

        Box(Modifier.weight(1f)) {
            NotBuiltYet("Multi-touch rings, the countdown glow and the reveal land here.")
        }

        Rule()
        Text(
            text = "DETAILS",
            style = PPTheme.typography.micro,
            color = PPTheme.colors.accentInk,
            modifier = Modifier
                .fillMaxWidth()
                .background(PPTheme.colors.accent)
                .clickable(onClick = onOpenResult)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

/** The label shown top-right on the draw surface. */
private fun DrawMode.label(): String = when (this) {
    DrawMode.STARTER -> "STARTING PLAYER"
    DrawMode.ORDER -> "PLAYER ORDER"
    DrawMode.TEAMS -> "TEAMS"
}
