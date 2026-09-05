package de.drehtuer.shotgun.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.drehtuer.shotgun.ui.theme.PPTheme

/**
 * Normalised dot geometry of the Shotgun! mark, from `design/Shotgun Logo.dc.html`:
 * centre x, centre y, relative size, and whether this is the claimed dot.
 *
 * Four fingers on the glass, one of them called - the game itself.
 */
private val MARK_DOTS = listOf(
    MarkDot(0.19f, 0.44f, 1.00f, claimed = true),
    MarkDot(0.44f, 0.19f, 0.90f, claimed = false),
    MarkDot(0.79f, 0.33f, 0.84f, claimed = false),
    MarkDot(0.56f, 0.74f, 0.95f, claimed = false),
)

private data class MarkDot(val x: Float, val y: Float, val scale: Float, val claimed: Boolean)

/** The mark on its own, drawn from the same geometry as the launcher icon. */
@Composable
fun ShotgunMark(size: Dp = 26.dp, modifier: Modifier = Modifier) {
    val claimed = PPTheme.colors.accent
    val missed = PPTheme.colors.dim
    Canvas(modifier.size(size)) {
        val side = this.size.minDimension
        val stroke = side * 0.0365f * 2f
        MARK_DOTS.forEach { dot ->
            val radius = side * 0.25f * dot.scale / 2f
            val centre = Offset(dot.x * side, dot.y * side)
            if (dot.claimed) {
                drawCircle(claimed, radius, centre)
            } else {
                // The design draws the ring inside the diameter (border-box),
                // so the path radius is inset by half the stroke.
                drawCircle(missed, radius - stroke / 2f, centre, style = Stroke(stroke))
            }
        }
    }
}

/**
 * The mark plus the wordmark. The exclamation mark is part of the name and is
 * always in accent - the identity spec is explicit that it is never dropped.
 */
@Composable
fun ShotgunWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShotgunMark()
        Text(
            text = buildAnnotatedString {
                append("SHOTGUN")
                withStyle(SpanStyle(color = PPTheme.colors.accent)) { append("!") }
            },
            style = PPTheme.typography.wordmark,
            color = PPTheme.colors.ink,
        )
    }
}
