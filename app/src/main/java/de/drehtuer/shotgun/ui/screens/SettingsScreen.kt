package de.drehtuer.shotgun.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drehtuer.shotgun.ui.components.NotBuiltYet
import de.drehtuer.shotgun.ui.components.Rule
import de.drehtuer.shotgun.ui.components.ScreenHeader
import de.drehtuer.shotgun.ui.theme.PPTheme
import de.drehtuer.shotgun.ui.theme.ThemePreference

/**
 * Scaffold for settings. Appearance is wired up already - it is the one setting
 * that exercises the theme, so it is worth having on device from the start.
 * Haptics, dim mode, countdown length and reveal timing follow, together with
 * persistence.
 */
@Composable
fun SettingsScreen(
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PPTheme.colors.bg),
    ) {
        ScreenHeader(
            title = "SETTINGS",
            actionLabel = "DONE",
            onAction = onDone,
            actionInAccent = true,
        )

        Column {
            Text(
                text = "APPEARANCE",
                style = PPTheme.typography.settingTitle,
                color = PPTheme.colors.ink,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            Text(
                text = "Follows the system theme unless you pick one",
                style = PPTheme.typography.bodySmall,
                color = PPTheme.colors.dim,
                modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp,
                ),
            )
            Rule()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                ThemeOption("System", ThemePreference.SYSTEM, themePreference, Modifier.weight(1f), onThemePreferenceChange)
                VerticalRule()
                ThemeOption("Light", ThemePreference.LIGHT, themePreference, Modifier.weight(1f), onThemePreferenceChange)
                VerticalRule()
                ThemeOption("Dark", ThemePreference.DARK, themePreference, Modifier.weight(1f), onThemePreferenceChange)
            }
            Rule()
        }

        NotBuiltYet("Haptics, dim mode, countdown and reveal timing land here.")
    }
}

/** A selected option fills with the accent; an unselected one stays bare. */
@Composable
private fun ThemeOption(
    label: String,
    value: ThemePreference,
    selected: ThemePreference,
    modifier: Modifier = Modifier,
    onPick: (ThemePreference) -> Unit,
) {
    val isSelected = value == selected
    Text(
        text = label,
        style = PPTheme.typography.settingTitle,
        color = if (isSelected) PPTheme.colors.accentInk else PPTheme.colors.dim,
        modifier = modifier
            .background(if (isSelected) PPTheme.colors.accent else PPTheme.colors.bg)
            .clickable { onPick(value) }
            .padding(start = 14.dp, top = 16.dp, bottom = 16.dp),
    )
}

/** Sized by the row it sits in, so it always spans the full option height. */
@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .width(PPTheme.dimens.rule)
            .fillMaxHeight()
            .background(PPTheme.colors.line),
    )
}
