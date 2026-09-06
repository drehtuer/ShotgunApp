package de.drehtuer.shotgun.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.ui.components.Rule
import de.drehtuer.shotgun.ui.components.ScreenHeader
import de.drehtuer.shotgun.ui.components.Stepper
import de.drehtuer.shotgun.ui.components.VerticalRule
import de.drehtuer.shotgun.ui.theme.PPTheme
import de.drehtuer.shotgun.ui.theme.ThemePreference

const val TAG_HAPTICS_TOGGLE = "toggle-haptics"
const val TAG_DIM_TOGGLE = "toggle-dim"

/**
 * Settings. Everything here is persisted, so a choice made once holds - see
 * `SettingsRepository`.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onDimChange: (Boolean) -> Unit,
    onCountdownChange: (Int) -> Unit,
    onRevealTimingChange: (RevealTiming) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PPTheme.colors.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "SETTINGS",
            actionLabel = "DONE",
            onAction = onDone,
            actionInAccent = true,
        )

        Toggle(
            title = "HAPTICS",
            subtitle = "A tick per finger, a stronger buzz on the result",
            checked = settings.haptics,
            onChange = onHapticsChange,
            modifier = Modifier.testTag(TAG_HAPTICS_TOGGLE),
        )
        Toggle(
            title = "DIM MODE",
            subtitle = "Lowers screen brightness, like an alarm clock",
            checked = settings.dim,
            onChange = onDimChange,
            modifier = Modifier.testTag(TAG_DIM_TOGGLE),
        )

        Section(
            title = "APPEARANCE",
            subtitle = "Follows the system theme unless you pick one",
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                ThemePreference.entries.forEachIndexed { index, option ->
                    if (index > 0) VerticalRule()
                    Pill(
                        label = option.label(),
                        selected = settings.themePreference == option,
                        onClick = { onThemePreferenceChange(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Section(
            title = "COUNTDOWN",
            subtitle = "Each new finger adds one second",
        ) {
            Stepper(
                label = "SECONDS",
                value = "${settings.countdownSeconds}s",
                onDecrement = { onCountdownChange(settings.countdownSeconds - 1) }
                    .takeIf { settings.countdownSeconds > Settings.MIN_COUNTDOWN_SECONDS },
                onIncrement = { onCountdownChange(settings.countdownSeconds + 1) },
                decrementLabel = "shorter countdown",
                incrementLabel = "longer countdown",
            )
        }

        Section(
            title = "REVEAL",
            subtitle = "Show order and teams at once, or after a beat",
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                RevealTiming.entries.forEachIndexed { index, option ->
                    if (index > 0) VerticalRule()
                    Pill(
                        label = option.label(),
                        selected = settings.revealTiming == option,
                        onClick = { onRevealTimingChange(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            text = "Mode lives on the home screen so the draw surface stays bare. " +
                "Moving a finger never counts as a new player. Team count has no " +
                "ceiling — the screen is the limit.",
            style = PPTheme.typography.micro.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            ),
            color = PPTheme.colors.dim,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** A titled block with its rules, as every settings group in the design has. */
@Composable
private fun Section(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
            Text(title, style = PPTheme.typography.settingTitle, color = PPTheme.colors.ink)
            Text(
                text = subtitle,
                style = PPTheme.typography.bodySmall,
                color = PPTheme.colors.dim,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Rule()
        content()
        Rule()
    }
}

/**
 * The design's switch: a hard-edged track with a square knob. Nothing here is
 * rounded - Modernist has no radius anywhere.
 */
@Composable
private fun Toggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PPTheme.colors
    val knobOffset by animateDpAsState(if (checked) 28.dp else 2.dp, label = "knob")

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .semantics { contentDescription = "$title, ${if (checked) "on" else "off"}" }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = PPTheme.typography.settingTitle, color = colors.ink)
                Text(
                    text = subtitle,
                    style = PPTheme.typography.bodySmall,
                    color = colors.dim,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier
                    .size(width = 56.dp, height = 30.dp)
                    .background(if (checked) colors.accent else colors.bg)
                    .border(PPTheme.dimens.rule, if (checked) colors.accent else colors.line),
            ) {
                Box(
                    Modifier
                        .offset(x = knobOffset, y = 2.dp)
                        .size(22.dp)
                        .background(if (checked) colors.accentInk else colors.dim),
                )
            }
        }
        Rule()
    }
}

/** A selected option fills with the accent; an unselected one stays bare. */
@Composable
private fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = PPTheme.typography.settingTitle,
        color = if (selected) PPTheme.colors.accentInk else PPTheme.colors.dim,
        modifier = modifier
            .background(if (selected) PPTheme.colors.accent else PPTheme.colors.bg)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 16.dp, bottom = 16.dp),
    )
}

private fun ThemePreference.label(): String = when (this) {
    ThemePreference.SYSTEM -> "System"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
}

private fun RevealTiming.label(): String = when (this) {
    RevealTiming.SUSPENSE -> "Suspense"
    RevealTiming.INSTANT -> "Instant"
}
