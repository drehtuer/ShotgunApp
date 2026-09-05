package de.drehtuer.playerpicker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modernist has zero corner radius and strong 2px rules; alignment and the
 * strength of the dividers do all the organising. [rule] is therefore the most
 * load-bearing value here.
 */
@Immutable
data class PPDimens(
    /** Every divider and border in the design. */
    val rule: Dp = 2.dp,
    /** Standard horizontal padding for screen content. */
    val screenPadding: Dp = 16.dp,
    /** Square tap target for stepper +/- controls. */
    val control: Dp = 56.dp,
    /** Width of the value cell in a stepper. */
    val stepperValueWidth: Dp = 64.dp,
    /** Default finger ring diameter on the draw surface. */
    val ringDiameter: Dp = 112.dp,
)
