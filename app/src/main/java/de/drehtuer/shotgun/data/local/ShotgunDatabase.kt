package de.drehtuer.shotgun.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1. Migrations are added here as the schema moves - the schema is
 * exported to `app/schemas/` so a diff is reviewable and a migration can be
 * written against a real before-and-after rather than from memory.
 */
@Database(
    entities = [DrawEntity::class, DrawPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ShotgunDatabase : RoomDatabase() {

    abstract fun drawDao(): DrawDao

    companion object {
        private const val NAME = "shotgun.db"

        @Volatile
        private var instance: ShotgunDatabase? = null

        fun get(context: Context): ShotgunDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShotgunDatabase::class.java,
                    NAME,
                )
                    // No fallbackToDestructiveMigration on purpose: history is
                    // the point of the fairness field, and silently wiping it
                    // on a schema change would make the screen lie.
                    .build()
                    .also { instance = it }
            }
    }
}
