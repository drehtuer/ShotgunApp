package de.drehtuer.playerpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.drehtuer.playerpicker.ui.navigation.PlayerPickerNavHost
import de.drehtuer.playerpicker.ui.theme.PPTheme
import de.drehtuer.playerpicker.ui.theme.PlayerPickerTheme
import de.drehtuer.playerpicker.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Held here for now so the theme can be switched on device. This
            // moves to persisted settings alongside the rest of the toggles.
            var themePreference by rememberSaveable {
                mutableStateOf(ThemePreference.SYSTEM)
            }

            PlayerPickerTheme(preference = themePreference) {
                PlayerPickerNavHost(
                    themePreference = themePreference,
                    onThemePreferenceChange = { themePreference = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PPTheme.colors.bg)
                        .windowInsetsPadding(WindowInsets.systemBars),
                )
            }
        }
    }
}
