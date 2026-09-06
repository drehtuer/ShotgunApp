package de.drehtuer.shotgun.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.drehtuer.shotgun.data.local.RoomDrawHistory
import de.drehtuer.shotgun.data.local.ShotgunDatabase
import de.drehtuer.shotgun.ui.navigation.DrawMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real DAO. The queries are SQL, so nothing in a JVM test can
 * prove they are right - only running them against SQLite can.
 */
class RoomDrawHistoryTest {

    private lateinit var db: ShotgunDatabase
    private lateinit var history: RoomDrawHistory

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShotgunDatabase::class.java,
        ).build()
        history = RoomDrawHistory(db.drawDao())
    }

    @After
    fun tearDown() = db.close()

    private fun draw(winnerAt: Float, at: Long = 1L) = DrawRecord(
        mode = DrawMode.STARTER,
        teamCount = null,
        timestamp = at,
        points = listOf(
            DrawPoint(x = winnerAt, y = winnerAt, won = true, assignment = null),
            DrawPoint(x = 0.9f, y = 0.9f, won = false, assignment = null),
        ),
    )

    @Test
    fun aRecordedDrawComesBackWithItsWinner() = runBlocking {
        history.record(draw(winnerAt = 0.25f))

        val winners = history.winners().first()
        assertEquals(1, winners.size)
        assertEquals(0.25f, winners.first().x, 1e-5f)
        assertTrue(winners.first().won)
        assertEquals(1, history.count().first())
    }

    @Test
    fun onlyWinnersArePlottedOnTheField() = runBlocking {
        repeat(4) { history.record(draw(winnerAt = 0.5f, at = it.toLong())) }
        // Two points per draw, but only one of them won.
        assertEquals(4, history.winners().first().size)
    }

    @Test
    fun theFieldIsCappedByTheRequestedLimit() = runBlocking {
        repeat(10) { history.record(draw(winnerAt = 0.5f, at = it.toLong())) }
        assertEquals(3, history.winners(limit = 3).first().size)
    }

    @Test
    fun theLatestDrawIsTheMostRecentOne() = runBlocking {
        history.record(draw(winnerAt = 0.1f, at = 100))
        history.record(draw(winnerAt = 0.8f, at = 200))

        val latest = history.latest().first()
        assertEquals(200L, latest?.timestamp)
        assertEquals(DrawMode.STARTER, latest?.mode)
        assertEquals(2, latest?.points?.size)
    }

    @Test
    fun clearingRemovesEverything() = runBlocking {
        history.record(draw(winnerAt = 0.4f))
        history.clear()
        assertEquals(0, history.count().first())
        assertEquals(0, history.winners().first().size)
    }

    /**
     * Points hang off a draw by foreign key with cascade delete, so pruning
     * draws must not leave orphaned points behind.
     */
    @Test
    fun pruningRemovesTheOldestDrawsAndTheirPoints() = runBlocking {
        val keep = DrawHistory.MAX_RETAINED_DRAWS
        // One more than the cap, so exactly one draw should be pruned.
        repeat(keep + 1) { history.record(draw(winnerAt = 0.5f, at = it.toLong())) }
        assertEquals(keep, history.count().first())
        assertEquals(keep, history.winners(limit = keep + 10).first().size)
    }
}
