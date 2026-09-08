package de.drehtuer.shotgun.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.drehtuer.shotgun.data.DrawPoint
import de.drehtuer.shotgun.data.DrawRecord
import de.drehtuer.shotgun.ui.navigation.DrawMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The database as the app actually opens it: on disk, through the singleton,
 * with the schema created from the entities rather than handed to Room.
 *
 * `RoomDrawHistoryTest` runs in memory, which skips the one thing that can fail
 * on a real device and nowhere else - opening a file-backed database and
 * building the schema in it. There is deliberately no destructive migration
 * fallback, so an open that goes wrong takes the history with it.
 */
@RunWith(RobolectricTestRunner::class)
class ShotgunDatabaseTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `every caller gets the same database`() {
        assertSame(ShotgunDatabase.get(context), ShotgunDatabase.get(context))
    }

    @Test
    fun `a draw survives a round trip through the database on disk`() = runTest {
        val history = RoomDrawHistory(ShotgunDatabase.get(context).drawDao())
        history.clear()

        history.record(
            DrawRecord(
                mode = DrawMode.ORDER,
                teamCount = null,
                points = listOf(
                    DrawPoint(x = 0.4f, y = 0.6f, won = true, assignment = 1),
                    DrawPoint(x = 0.6f, y = 0.4f, won = false, assignment = 2),
                ),
                timestamp = 42L,
            )
        )

        val latest = history.latest().first()!!
        assertEquals(DrawMode.ORDER, latest.mode)
        assertEquals(2, latest.points.size)
        assertEquals(1, history.winners().first().size)
        assertEquals(1, history.count().first())
    }
}
