package de.drehtuer.shotgun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drehtuer.shotgun.ui.theme.PPTheme

/**
 * The design's stepper: a wide-tracked label, then minus, value and plus as
 * equal square cells divided by 2px rules.
 *
 * [onDecrement] is null when the value is at its floor, which greys the control
 * rather than letting it look live and do nothing.
 */
@Composable
fun Stepper(
    label: String,
    value: String,
    onDecrement: (() -> Unit)?,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementLabel: String = "decrease $label",
    incrementLabel: String = "increase $label",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = PPTheme.typography.microWide,
            color = PPTheme.colors.dim,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = PPTheme.dimens.screenPadding),
        )
        VerticalRule()
        StepButton("−", onDecrement, decrementLabel)
        VerticalRule()
        Box(
            modifier = Modifier
                .width(PPTheme.dimens.stepperValueWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = PPTheme.typography.stepperValue,
                color = PPTheme.colors.ink,
                textAlign = TextAlign.Center,
            )
        }
        VerticalRule()
        StepButton("+", onIncrement, incrementLabel)
    }
}

@Composable
private fun StepButton(glyph: String, onClick: (() -> Unit)?, description: String) {
    val enabled = onClick != null
    Box(
        modifier = Modifier
            .size(PPTheme.dimens.control)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = if (enabled) PPTheme.colors.ink else PPTheme.colors.line,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A 2px vertical rule that takes its height from the row it sits in. */
@Composable
fun VerticalRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(PPTheme.dimens.rule)
            .fillMaxHeight()
            .background(PPTheme.colors.line),
    )
}
