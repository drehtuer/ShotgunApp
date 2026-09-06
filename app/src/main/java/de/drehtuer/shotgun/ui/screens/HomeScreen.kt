package de.drehtuer.shotgun.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.drehtuer.shotgun.ui.components.ModeMotif
import de.drehtuer.shotgun.ui.components.Rule
import de.drehtuer.shotgun.ui.components.ShotgunWordmark
import de.drehtuer.shotgun.ui.components.Stepper
import de.drehtuer.shotgun.ui.components.VerticalRule
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.PPTheme

/**
 * Home. Mode lives here, deliberately, so that the draw surface itself can stay
 * bare - nothing to read once hands are on the glass.
 */
@Composable
fun HomeScreen(
    teamCount: Int,
    onTeamCountChange: (Int) -> Unit,
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
        Header(onOpenSettings)
        Rule()

        Column(Modifier.weight(1f)) {
            ModeCard(
                mode = DrawMode.STARTER,
                title = "STARTING PLAYER",
                subtitle = "One finger wins the draw",
                onClick = { onStartDraw(DrawMode.STARTER) },
                modifier = Modifier.weight(1f),
            )
            Rule()
            ModeCard(
                mode = DrawMode.ORDER,
                title = "PLAYER ORDER",
                subtitle = "Every finger gets a number",
                onClick = { onStartDraw(DrawMode.ORDER) },
                modifier = Modifier.weight(1f),
            )
            Rule()
            // Slightly taller than the others: it carries the stepper as well.
            Column(
                Modifier
                    .weight(1.2f)
                    .background(PPTheme.colors.surface),
            ) {
                ModeCard(
                    mode = DrawMode.TEAMS,
                    title = "TEAMS",
                    subtitle = "Uneven sizes allowed",
                    onClick = { onStartDraw(DrawMode.TEAMS) },
                    modifier = Modifier.weight(1f),
                )
                Rule()
                Stepper(
                    label = "TEAMS",
                    value = teamCount.toString(),
                    // Two is the floor: one team is not a draw.
                    onDecrement = { onTeamCountChange(teamCount - 1) }
                        .takeIf { teamCount > MIN_TEAMS },
                    onIncrement = { onTeamCountChange(teamCount + 1) },
                    decrementLabel = "fewer teams",
                    incrementLabel = "more teams",
                )
            }
        }
        Rule()

        Text(
            text = "LAST RESULT & FAIRNESS HEATMAP →",
            style = PPTheme.typography.micro,
            color = PPTheme.colors.dim,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenResult)
                .padding(horizontal = PPTheme.dimens.screenPadding, vertical = 14.dp),
        )
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        ) {
            ShotgunWordmark()
            Text(
                text = "PICK A MODE.\nHANDS ON GLASS.",
                style = PPTheme.typography.display,
                color = PPTheme.colors.ink,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        VerticalRule()
        VerticalSettingsTab(onOpenSettings)
    }
}

/**
 * The settings tab reads down the right edge, as `writing-mode: vertical-rl`
 * does in the design. Compose has no writing-mode: the label is measured
 * **unbounded** so it keeps its natural width, then rotated for drawing.
 * Measuring it against the tab's 60dp width instead would wrap it mid-word.
 */
@Composable
private fun VerticalSettingsTab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Settings" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SETTINGS",
            style = PPTheme.typography.micro,
            color = PPTheme.colors.dim,
            softWrap = false,
            modifier = Modifier
                .graphicsLayer { rotationZ = 90f }
                .wrapContentSize(unbounded = true),
        )
    }
}

/** A mode card: the motif at the top, the name and its one-line promise below. */
@Composable
private fun ModeCard(
    mode: DrawMode,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PPTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(PPTheme.dimens.screenPadding),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ModeMotif(mode)
        CardTitle(title, subtitle)
    }
}

@Composable
private fun ColumnScope.CardTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = PPTheme.typography.cardTitle, color = PPTheme.colors.ink)
        Text(
            text = subtitle,
            style = PPTheme.typography.body,
            color = PPTheme.colors.dim,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
