package de.drehtuer.shotgun

import android.app.Application
import de.drehtuer.shotgun.data.DrawHistory
import de.drehtuer.shotgun.data.local.RoomDrawHistory
import de.drehtuer.shotgun.data.local.ShotgunDatabase
import de.drehtuer.shotgun.data.settings.SettingsRepository

/**
 * Holds the two things the app needs for its whole life. Deliberately plain -
 * a dependency injection framework would be more machinery than this app has
 * dependencies.
 */
class ShotgunApplication : Application() {

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val history: DrawHistory by lazy { RoomDrawHistory(ShotgunDatabase.get(this).drawDao()) }
}
