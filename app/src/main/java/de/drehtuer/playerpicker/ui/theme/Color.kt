package de.drehtuer.playerpicker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Player Picker palette, transcribed verbatim from the `--pp-*` custom
 * properties in the design export (`Player Picker.dc.html`).
 *
 * These sit on top of the Modernist design system: Modernist supplies the
 * discipline (flat, zero radius, 2px rules, Archivo), while the `--pp-*` tokens
 * are this app's own surface ramp and accent.
 */
@Immutable
data class PPColors(
    /** Page ground. */
    val bg: Color,
    /** Raised panel: mode cards, settings rows, the fairness field. */
    val surface: Color,
    /** Pressed / hovered state of [surface]. */
    val surface2: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text and inactive marks. */
    val dim: Color,
    /** The 2px rules that do all the organising. */
    val line: Color,
    /** The single accent. */
    val accent: Color,
    /** Text placed on top of [accent]. */
    val accentInk: Color,
    /** Accent at low alpha, for fills behind a winning ring. */
    val accentSoft: Color,
    /** Ring fills for team mode, indexed by team. */
    val teamFills: List<PPTeamFill>,
    /** Density ramp for the fairness heatmap, cold to hot. */
    val heatRamp: List<PPHeatStop>,
    /** True when this is the dark palette. Drives status bar icon colour. */
    val isDark: Boolean,
)

/** A team's ring fill and the label colour that stays legible on it. */
@Immutable
data class PPTeamFill(val bg: Color, val ink: Color)

/** One stop of the heatmap gradient: [position] in 0..1 against [color]. */
@Immutable
data class PPHeatStop(val position: Float, val color: Color)

fun ppDarkColors(): PPColors {
    val bg = Color(0xFF201E1D)
    val surface2 = Color(0xFF444141)
    val ink = Color(0xFFF3F2F2)
    val dim = Color(0xFF9B9797)
    val accent = Color(0xFFFF563C)
    return PPColors(
        bg = bg,
        surface = Color(0xFF2D2B2B),
        surface2 = surface2,
        ink = ink,
        dim = dim,
        line = Color(0xFF605D5D),
        accent = accent,
        accentInk = bg,
        accentSoft = accent.copy(alpha = 0.14f),
        teamFills = listOf(
            PPTeamFill(accent, bg),
            PPTeamFill(ink, bg),
            PPTeamFill(dim, bg),
            PPTeamFill(Color(0xFFAE1800), Color(0xFFF3F2F2)),
            PPTeamFill(Color(0xFFFFC4B8), Color(0xFF201E1D)),
            PPTeamFill(surface2, ink),
        ),
        heatRamp = listOf(
            PPHeatStop(0.00f, Color(0xFF2D2B2B)),
            PPHeatStop(0.30f, Color(0xFF4D170E)),
            PPHeatStop(0.58f, Color(0xFFAE1800)),
            PPHeatStop(0.82f, Color(0xFFFF563C)),
            PPHeatStop(1.00f, Color(0xFFFFC4B8)),
        ),
        isDark = true,
    )
}

fun ppLightColors(): PPColors {
    val bg = Color(0xFFF3F2F2)
    val surface2 = Color(0xFFD7D3D3)
    val ink = Color(0xFF201E1D)
    val dim = Color(0xFF7D7979)
    val accent = Color(0xFFEC3013)
    return PPColors(
        bg = bg,
        surface = Color(0xFFEAE9E9),
        surface2 = surface2,
        ink = ink,
        dim = dim,
        line = Color(0xFFBAB6B6),
        accent = accent,
        accentInk = bg,
        accentSoft = accent.copy(alpha = 0.12f),
        teamFills = listOf(
            PPTeamFill(accent, bg),
            PPTeamFill(ink, bg),
            PPTeamFill(dim, bg),
            PPTeamFill(Color(0xFFAE1800), Color(0xFFF3F2F2)),
            PPTeamFill(Color(0xFFFFC4B8), Color(0xFF201E1D)),
            PPTeamFill(surface2, ink),
        ),
        heatRamp = listOf(
            PPHeatStop(0.00f, Color(0xFFEAE9E9)),
            PPHeatStop(0.30f, Color(0xFFFFC4B8)),
            PPHeatStop(0.58f, Color(0xFFFF563C)),
            PPHeatStop(0.82f, Color(0xFFDD2B0F)),
            PPHeatStop(1.00f, Color(0xFF7C1405)),
        ),
        isDark = false,
    )
}
