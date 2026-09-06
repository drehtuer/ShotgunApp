package de.drehtuer.shotgun.ui.screens

/**
 * The design's floor: one team is not a draw, so the stepper stops at two.
 * There is deliberately no ceiling - "the screen is the limit".
 */
const val MIN_TEAMS = 2

/** Keeps a requested team count inside the range the design allows. */
fun clampTeamCount(value: Int): Int = value.coerceAtLeast(MIN_TEAMS)
