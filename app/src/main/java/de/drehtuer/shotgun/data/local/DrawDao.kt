package de.drehtuer.shotgun.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {

    @Insert
    suspend fun insertDraw(draw: DrawEntity): Long

    @Insert
    suspend fun insertPoints(points: List<DrawPointEntity>)

    /** Winning positions, newest first, for the density field. */
    @Query(
        """
        SELECT p.* FROM draw_points p
        JOIN draws d ON d.id = p.draw_id
        WHERE p.won = 1
        ORDER BY d.timestamp DESC
        LIMIT :limit
        """
    )
    fun winners(limit: Int): Flow<List<DrawPointEntity>>

    @Query("SELECT * FROM draws ORDER BY timestamp DESC LIMIT 1")
    fun latestDraw(): Flow<DrawEntity?>

    @Query("SELECT * FROM draw_points WHERE draw_id = :drawId")
    suspend fun pointsFor(drawId: Long): List<DrawPointEntity>

    @Query("SELECT COUNT(*) FROM draws")
    fun count(): Flow<Int>

    /**
     * Drops the oldest draws beyond [keep]. Points go with them by cascade, so
     * this is the only pruning needed.
     */
    @Query(
        """
        DELETE FROM draws WHERE id NOT IN (
            SELECT id FROM draws ORDER BY timestamp DESC LIMIT :keep
        )
        """
    )
    suspend fun prune(keep: Int)

    @Transaction
    suspend fun insertComplete(draw: DrawEntity, points: (Long) -> List<DrawPointEntity>, keep: Int) {
        val id = insertDraw(draw)
        insertPoints(points(id))
        prune(keep)
    }

    @Query("DELETE FROM draws")
    suspend fun clear()
}
