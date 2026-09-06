package de.drehtuer.shotgun.data

import de.drehtuer.shotgun.ui.navigation.DrawMode

/**
 * One finger's position and outcome from a completed draw.
 *
 * Positions are stored **normalised to 0..1** against the surface they were
 * captured on, never as raw pixels. This is what lets old records stay
 * comparable after a screen size or device change - the fairness field plots
 * history from every draw ever made, so a record written on one geometry has to
 * still mean the same thing on another.
 */
data class DrawPoint(
    /** Horizontal position, 0 (left edge) to 1 (right edge). */
    val x: Float,
    /** Vertical position, 0 (top edge) to 1 (bottom edge). */
    val y: Float,
    /** Whether this finger won: the starter, or rank 1 in order mode. */
    val won: Boolean,
    /** Rank in order mode, team index in teams mode, null in starter mode. */
    val assignment: Int?,
)

/** A completed draw: every finger that was down, and what it was given. */
data class DrawRecord(
    val mode: DrawMode,
    /** Teams drawn for, or null outside teams mode. */
    val teamCount: Int?,
    val points: List<DrawPoint>,
    /** Wall-clock time of the draw, epoch milliseconds. */
    val timestamp: Long,
)

/**
 * Converts a pixel position on a surface of [width] x [height] into the
 * normalised form that gets stored.
 *
 * Degenerate surfaces (zero or negative) collapse to the centre rather than
 * producing NaN or dividing by zero - a draw is never worth crashing over, and
 * a centre point is honest about carrying no positional information.
 */
fun normalise(x: Float, y: Float, width: Float, height: Float): Pair<Float, Float> {
    val nx = if (width > 0f) (x / width).coerceIn(0f, 1f) else 0.5f
    val ny = if (height > 0f) (y / height).coerceIn(0f, 1f) else 0.5f
    return nx to ny
}
