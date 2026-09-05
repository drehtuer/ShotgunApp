package de.drehtuer.playerpicker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import de.drehtuer.playerpicker.ui.theme.PPTheme

/** A 2px full-bleed rule - the workhorse of the Modernist layout. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(PPTheme.dimens.rule)
            .background(PPTheme.colors.line),
    )
}

/**
 * The header every non-home screen shares: a heavy title flush left, a
 * wide-tracked action label flush right, and a rule underneath.
 */
@Composable
fun ScreenHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionInAccent: Boolean = false,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = PPTheme.typography.screenTitle,
                color = PPTheme.colors.ink,
                modifier = Modifier.padding(
                    horizontal = PPTheme.dimens.screenPadding,
                    vertical = 14.dp,
                ),
            )
            Text(
                text = actionLabel,
                style = PPTheme.typography.micro,
                color = if (actionInAccent) PPTheme.colors.accent else PPTheme.colors.dim,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(
                        horizontal = PPTheme.dimens.screenPadding,
                        vertical = 18.dp,
                    ),
            )
        }
        Rule()
    }
}

/**
 * Placeholder body for a screen that is scaffolded but not yet built. Present
 * so the theme can be reviewed on a real device before the screens land.
 */
@Composable
fun NotBuiltYet(note: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PPTheme.colors.surface)
            .padding(PPTheme.dimens.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NOT BUILT YET",
                style = PPTheme.typography.microWide,
                color = PPTheme.colors.accent,
            )
            Text(
                text = note,
                style = PPTheme.typography.body,
                color = PPTheme.colors.dim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
