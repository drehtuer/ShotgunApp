package de.drehtuer.shotgun.data.local

import de.drehtuer.shotgun.data.DrawHistory
import de.drehtuer.shotgun.data.DrawPoint
import de.drehtuer.shotgun.data.DrawRecord
import de.drehtuer.shotgun.ui.navigation.DrawMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** [DrawHistory] backed by Room. */
class RoomDrawHistory(private val dao: DrawDao) : DrawHistory {

    override suspend fun record(draw: DrawRecord) {
        dao.insertComplete(
            draw = DrawEntity(
                mode = draw.mode.name,
                teamCount = draw.teamCount,
                timestamp = draw.timestamp,
            ),
            points = { drawId ->
                draw.points.map {
                    DrawPointEntity(
                        drawId = drawId,
                        x = it.x,
                        y = it.y,
                        won = it.won,
                        assignment = it.assignment,
                    )
                }
            },
            keep = DrawHistory.MAX_RETAINED_DRAWS,
        )
    }

    override fun winners(limit: Int): Flow<List<DrawPoint>> =
        dao.winners(limit).map { rows -> rows.map(DrawPointEntity::toDomain) }

    override fun latest(): Flow<DrawRecord?> =
        dao.latestDraw().map { entity ->
            entity?.let {
                DrawRecord(
                    mode = it.mode.toDrawMode(),
                    teamCount = it.teamCount,
                    points = dao.pointsFor(it.id).map(DrawPointEntity::toDomain),
                    timestamp = it.timestamp,
                )
            }
        }

    override fun count(): Flow<Int> = dao.count()

    override suspend fun clear() = dao.clear()

    /** Convenience for callers that want one snapshot rather than a stream. */
    suspend fun winnersNow(limit: Int = DrawHistory.DEFAULT_FIELD_SAMPLES): List<DrawPoint> =
        winners(limit).first()
}

private fun DrawPointEntity.toDomain() = DrawPoint(x = x, y = y, won = won, assignment = assignment)

/**
 * Modes are stored by name. An unknown one means a row written by a newer build
 * that has since been rolled back; falling back beats crashing on read.
 */
private fun String.toDrawMode(): DrawMode =
    runCatching { DrawMode.valueOf(this) }.getOrDefault(DrawMode.STARTER)
