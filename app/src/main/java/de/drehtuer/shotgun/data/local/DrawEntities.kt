package de.drehtuer.shotgun.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One completed draw. Points hang off this by [DrawPointEntity.drawId]. */
@Entity(tableName = "draws", indices = [Index("timestamp")])
data class DrawEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [de.drehtuer.shotgun.ui.navigation.DrawMode] name, stored as text so a
     *  new mode does not silently renumber the existing rows. */
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "team_count") val teamCount: Int?,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
)

/**
 * One finger from a draw. `x` and `y` are normalised to 0..1 - see
 * [de.drehtuer.shotgun.data.normalise] for why that matters.
 */
@Entity(
    tableName = "draw_points",
    foreignKeys = [
        ForeignKey(
            entity = DrawEntity::class,
            parentColumns = ["id"],
            childColumns = ["draw_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("draw_id"), Index("won")],
)
data class DrawPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "draw_id") val drawId: Long,
    @ColumnInfo(name = "x") val x: Float,
    @ColumnInfo(name = "y") val y: Float,
    @ColumnInfo(name = "won") val won: Boolean,
    @ColumnInfo(name = "assignment") val assignment: Int?,
)
