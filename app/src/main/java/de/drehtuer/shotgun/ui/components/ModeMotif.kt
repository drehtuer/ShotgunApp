package de.drehtuer.shotgun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.PPTheme

/** Letters used for team labels, matching the design. */
private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

/**
 * The four-dot motif on a mode card: a preview of what that mode does to the
 * fingers on the glass.
 *
 * - starter: one claimed, three not
 * - order: every finger numbered, the first claimed
 * - teams: fingers coloured by team, sizes uneven on purpose
 */
@Composable
fun ModeMotif(mode: DrawMode, modifier: Modifier = Modifier) {
    val colors = PPTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (mode) {
            DrawMode.STARTER -> {
                Dot(fill = colors.accent)
                repeat(3) { Dot() }
            }

            DrawMode.ORDER -> {
                Dot(fill = colors.accent, label = "1", labelColor = colors.accentInk)
                for (n in 2..4) Dot(label = "$n", labelColor = colors.dim)
            }

            DrawMode.TEAMS -> {
                // A, B, A, C - the repeat is the point: teams need not be even.
                listOf(0, 1, 0, 2).forEach { team ->
                    val fill = colors.teamFills[team % colors.teamFills.size]
                    Dot(fill = fill.bg, label = LETTERS[team].toString(), labelColor = fill.ink)
                }
            }
        }
    }
}

/** One mark: filled when [fill] is given, otherwise a bare ring. */
@Composable
private fun Dot(
    fill: Color? = null,
    label: String? = null,
    labelColor: Color = PPTheme.colors.dim,
) {
    val outline = PPTheme.colors.line
    Box(
        modifier = Modifier
            .size(26.dp)
            .then(
                if (fill != null) Modifier.background(fill, CircleShape)
                else Modifier.border(PPTheme.dimens.rule, outline, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = if (fill != null) FontWeight.ExtraBold else FontWeight.Bold,
            )
        }
    }
}
