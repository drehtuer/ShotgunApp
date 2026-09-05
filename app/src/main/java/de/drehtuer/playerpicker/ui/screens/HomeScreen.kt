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
 * Scaffold for the home screen. The finished screen carries the kicker and
 * headline, three mode cards, the team stepper and the footer link to the last
 * result; for now it navigates so the shell can be walked end to end.
 */
@Composable
fun HomeScreen(
    onStartDraw: (DrawMode) -> Unit,
    onOpenSettings: () -> Unit,
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
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            ) {
                Text(
                    text = "PLAYER PICKER",
                    style = PPTheme.typography.kicker,
                    color = PPTheme.colors.accent,
                )
                Text(
                    text = "PICK A MODE.\nHANDS ON GLASS.",
                    style = PPTheme.typography.display,
                    color = PPTheme.colors.ink,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                text = "SETTINGS",
                style = PPTheme.typography.micro,
                color = PPTheme.colors.dim,
                modifier = Modifier
                    .clickable(onClick = onOpenSettings)
                    .padding(16.dp),
            )
        }
        Rule()

        Box(Modifier.weight(1f)) {
            NotBuiltYet("Mode cards and the team stepper land here.")
        }

        Rule()
        Row(Modifier.fillMaxWidth()) {
            ModeLink("STARTER", Modifier.weight(1f)) { onStartDraw(DrawMode.STARTER) }
            ModeLink("ORDER", Modifier.weight(1f)) { onStartDraw(DrawMode.ORDER) }
            ModeLink("TEAMS", Modifier.weight(1f)) { onStartDraw(DrawMode.TEAMS) }
        }
        Rule()
        Text(
            text = "LAST RESULT & FAIRNESS HEATMAP →",
            style = PPTheme.typography.micro,
            color = PPTheme.colors.dim,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenResult)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/** Temporary navigation affordance standing in for a finished mode card. */
@Composable
private fun ModeLink(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        style = PPTheme.typography.micro,
        color = PPTheme.colors.ink,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    )
}
