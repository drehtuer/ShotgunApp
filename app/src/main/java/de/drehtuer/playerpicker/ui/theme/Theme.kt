package de.drehtuer.playerpicker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import de.drehtuer.playerpicker.ui.util.findActivity

/** Which palette to use. Mirrors the `themePref` setting in the design. */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

private val LocalPPColors = staticCompositionLocalOf { ppDarkColors() }
private val LocalPPTypography = staticCompositionLocalOf { ppTypography() }
private val LocalPPDimens = staticCompositionLocalOf { PPDimens() }

/** Accessors for the design tokens: `PPTheme.colors.accent`, and so on. */
object PPTheme {
    val colors: PPColors
        @Composable @ReadOnlyComposable get() = LocalPPColors.current
    val typography: PPTypography
        @Composable @ReadOnlyComposable get() = LocalPPTypography.current
    val dimens: PPDimens
        @Composable @ReadOnlyComposable get() = LocalPPDimens.current
}

/** Zero radius everywhere - Modernist does not round anything. */
private val PPShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun PlayerPickerTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colors = if (dark) ppDarkColors() else ppLightColors()
    val typography = ppTypography()

    // The app's palette can be overridden independently of the system setting,
    // so the system bar icons have to follow the app - not uiMode - or they end
    // up dark on a dark ground.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    // Material3 is not used for chrome, but anything that falls back to it
    // (ripples, text selection handles) should still land on the palette.
    val material = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentInk,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            outline = colors.line,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentInk,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            outline = colors.line,
        )
    }

    CompositionLocalProvider(
        LocalPPColors provides colors,
        LocalPPTypography provides typography,
        LocalPPDimens provides PPDimens(),
    ) {
        MaterialTheme(
            colorScheme = material,
            shapes = PPShapes,
            content = content,
        )
    }
}