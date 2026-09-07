package de.drehtuer.shotgun.draw

import de.drehtuer.shotgun.ui.navigation.DrawMode

/**
 * Which palette role a ring takes. Kept as a role rather than a colour so this
 * file stays free of Android types and can be unit-tested; the screen maps a
 * role onto `PPTheme.colors`.
 */
enum class RingRole {
    /** Down, nothing decided: a bare ink ring. */
    IDLE,

    /** The draw has run but this finger's turn has not come up yet. */
    PENDING,

    /** Starter mode, and this is the one. */
    WINNER,

    /** Starter mode, and this is not. */
    LOSER,

    /** Order mode: carries a rank. */
    RANKED,

    /** Teams mode: carries a team letter. */
    TEAM,
}

/**
 * Everything the look of one ring depends on, worked out without drawing it.
 *
 * The rules here are real decisions - who is dimmed, what is emphasised, which
 * letter a team gets past Z - and they were previously buried in a composable
 * where nothing could reach them. [teamIndex] is the index into the palette's
 * team fills; [labelSp] is a size in sp, applied by the caller.
 */
data class RingSpec(
    val role: RingRole,
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val filled: Boolean = false,
    val label: String? = null,
    val labelSp: Float = 40f,
    val sub: String? = null,
    val teamIndex: Int? = null,
) {
    /**
     * Whether this ring is showing an answer rather than waiting for one. The
     * ring thickens when it is: the colour is the answer in teams mode, and a
     * 4dp band around a fingertip is not enough of it to read at a glance.
     */
    val answered: Boolean
        get() = role != RingRole.IDLE && role != RingRole.PENDING
}

private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

/**
 * The look of the ring belonging to [fingerId].
 *
 * [revealed] is whether this finger's own assignment is on screen yet, which
 * during a staged reveal is not the same as the draw having happened.
 */
fun ringSpec(
    fingerId: Long,
    outcome: DrawOutcome?,
    phase: DrawPhase,
    revealed: Boolean,
): RingSpec {
    val assignment = outcome?.assignment?.get(fingerId)

    // Still waiting its turn while the reveal walks down the order.
    if (phase == DrawPhase.REVEALING && !(revealed && assignment != null)) {
        return RingSpec(role = RingRole.PENDING, alpha = 0.85f)
    }
    if (!revealed || outcome == null || assignment == null) {
        return RingSpec(role = RingRole.IDLE)
    }

    return when (outcome.mode) {
        DrawMode.STARTER ->
            if (fingerId == outcome.winnerId) {
                RingSpec(
                    role = RingRole.WINNER,
                    scale = 1.14f,
                    filled = true,
                    label = "WON",
                    labelSp = 28f,
                )
            } else {
                // Dimmed hard rather than hidden: you can still see where
                // everyone was, which is the point of not clearing the field.
                RingSpec(role = RingRole.LOSER, alpha = 0.2f, scale = 0.88f)
            }

        DrawMode.ORDER -> {
            val total = outcome.fingers.size
            // Rank 1 is full strength and the last is faint, so the order reads
            // as a gradient rather than as a set of equal numbers.
            val t = if (total > 1) (assignment - 1f) / (total - 1f) else 0f
            val first = assignment == 1
            RingSpec(
                role = RingRole.RANKED,
                alpha = 1f - t * 0.7f,
                scale = if (first) 1.12f else 1f,
                filled = first,
                label = assignment.toString(),
                labelSp = if (first) 46f else 34f,
            )
        }

        DrawMode.TEAMS -> RingSpec(
            role = RingRole.TEAM,
            filled = true,
            label = teamLabel(assignment),
            sub = "TEAM",
            teamIndex = assignment,
        )
    }
}

/**
 * A team's letter.
 *
 * The letters wrap: with more than 26 teams, team 26 is labelled "A" again.
 * That is ambiguous, and it is the behaviour this has always had - the design
 * puts no ceiling on the team count ("the screen is the limit"), so it is
 * reachable in principle and worth knowing about. The numeric fallback only
 * catches an index the modulo cannot produce.
 */
fun teamLabel(assignment: Int): String =
    LETTERS.getOrNull(assignment % LETTERS.length)?.toString() ?: "${assignment + 1}"

/**
 * How far from the finger the rank label sits, along y.
 *
 * Beside the ring while the finger is down, and sliding to the ring's centre as
 * [slide] goes 0 to 1 once it lifts. [above] is false only when the finger is
 * too near the top of the screen to fit the label over it.
 */
fun labelOffsetY(
    radiusPx: Float,
    scale: Float,
    labelHeightPx: Float,
    gapPx: Float,
    above: Boolean,
    slide: Float,
): Float {
    val beside = if (above) {
        -radiusPx * scale - (labelHeightPx + gapPx)
    } else {
        radiusPx * scale + gapPx
    }
    val inside = -labelHeightPx / 2f
    return beside + (inside - beside) * slide
}

/** Whether there is room to put the label above the ring rather than below. */
fun labelFitsAbove(fingerY: Float, radiusPx: Float, labelHeightPx: Float, gapPx: Float): Boolean =
    fingerY > radiusPx + labelHeightPx + gapPx
