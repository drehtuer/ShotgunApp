package de.drehtuer.shotgun.draw

import de.drehtuer.shotgun.ui.navigation.DrawMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DrawEngineTest {

    private fun engine(
        mode: DrawMode = DrawMode.STARTER,
        teams: Int = 3,
        seconds: Int = 3,
        instant: Boolean = false,
        seed: Int = 1,
    ) = DrawEngine(mode, teams, seconds, instant, Random(seed))

    // ---- arming and the countdown ------------------------------------------

    @Test
    fun `one finger does not arm anything`() {
        val e = engine()
        e.onDown(1, 0f, 0f, now = 0)
        assertEquals(DrawPhase.IDLE, e.phase)
        assertNull(e.tick(now = 10_000))
    }

    @Test
    fun `the second finger arms the countdown`() {
        val e = engine(seconds = 3)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 100)
        assertEquals(DrawPhase.COUNTING, e.phase)
        assertNull(e.tick(now = 3_000))              // armed at 100, due at 3100
        assertNotNull(e.tick(now = 3_100))
    }

    /** The rule that keeps a late joiner from costing the group their draw. */
    @Test
    fun `every finger after the second adds exactly one second`() {
        val e = engine(seconds = 3)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)                 // due at 3000
        e.onDown(3, 0f, 0f, now = 500)               // due at 4000
        e.onDown(4, 0f, 0f, now = 600)               // due at 5000
        assertNull(e.tick(now = 4_999))
        assertNotNull(e.tick(now = 5_000))
    }

    @Test
    fun `dropping below two fingers cancels the countdown`() {
        val e = engine()
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        e.onUp(2)
        assertEquals(DrawPhase.IDLE, e.phase)
        assertNull(e.tick(now = 10_000))
    }

    @Test
    fun `progress runs zero to one across the countdown`() {
        val e = engine(seconds = 2)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        assertEquals(0f, e.progress(0), 1e-4f)
        assertEquals(0.5f, e.progress(1_000), 1e-4f)
        assertEquals(1f, e.progress(2_000), 1e-4f)
        assertEquals(1f, e.progress(9_999), 1e-4f)   // never overshoots
    }

    // ---- touch rules --------------------------------------------------------

    /** The rule the hint text teaches: moving is not joining. */
    @Test
    fun `moving a finger repositions it and never adds a player`() {
        val e = engine()
        e.onDown(1, 10f, 10f, now = 0)
        e.onMove(1, 50f, 60f)
        assertEquals(1, e.fingers.size)
        assertEquals(50f, e.fingers.first().x, 1e-4f)
        assertEquals(60f, e.fingers.first().y, 1e-4f)
    }

    @Test
    fun `moving does not buzz - only landing does`() {
        val e = engine()
        assertEquals(DrawEffect.FingerTick, e.onDown(1, 0f, 0f, now = 0))
        e.onMove(1, 5f, 5f)                          // returns nothing at all
        assertEquals(1, e.fingers.size)
    }

    @Test
    fun `the same pointer landing twice is ignored`() {
        val e = engine()
        e.onDown(1, 0f, 0f, now = 0)
        assertNull(e.onDown(1, 5f, 5f, now = 10))
        assertEquals(1, e.fingers.size)
    }

    @Test
    fun `a finger landing after the reveal is ignored`() {
        val e = engine(instant = true)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        e.tick(now = 5_000)
        assertEquals(DrawPhase.REVEALED, e.phase)
        assertNull(e.onDown(3, 0f, 0f, now = 5_100))
        assertEquals(2, e.fingers.size)
    }

    @Test
    fun `the result stays until the last hand leaves`() {
        val e = engine(instant = true)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        e.tick(now = 5_000)
        e.onUp(1)
        assertEquals(DrawPhase.REVEALED, e.phase)
        e.onUp(2)
        assertEquals(DrawPhase.IDLE, e.phase)
        assertNull(e.outcome)
    }

    // ---- drawing ------------------------------------------------------------

    @Test
    fun `starter picks exactly one winner from the fingers present`() {
        val e = engine(mode = DrawMode.STARTER)
        listOf(1L, 2L, 3L, 4L).forEach { e.onDown(it, 0f, 0f, now = 0) }
        val effect = e.tick(now = 100_000) as DrawEffect.Drawn
        assertTrue(effect.outcome.winnerId in listOf(1L, 2L, 3L, 4L))
        assertEquals(4, effect.outcome.assignment.size)
    }

    @Test
    fun `order gives every finger a distinct rank from one upwards`() {
        val e = engine(mode = DrawMode.ORDER)
        listOf(1L, 2L, 3L, 4L, 5L).forEach { e.onDown(it, 0f, 0f, now = 0) }
        val outcome = (e.tick(now = 100_000) as DrawEffect.Drawn).outcome
        assertEquals(listOf(1, 2, 3, 4, 5), outcome.assignment.values.sorted())
        assertEquals(1, outcome.assignment[outcome.winnerId])
    }

    @Test
    fun `teams deals round robin, so sizes differ by at most one`() {
        val e = engine(mode = DrawMode.TEAMS, teams = 3)
        (1L..7L).forEach { e.onDown(it, 0f, 0f, now = 0) }
        val outcome = (e.tick(now = 100_000) as DrawEffect.Drawn).outcome
        val sizes = outcome.assignment.values.groupingBy { it }.eachCount().values
        assertEquals(3, sizes.size)
        assertTrue("uneven by more than one: $sizes", sizes.max() - sizes.min() <= 1)
    }

    @Test
    fun `teams refuses when there are fewer fingers than teams`() {
        val e = engine(mode = DrawMode.TEAMS, teams = 5)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        val effect = e.tick(now = 100_000)
        assertTrue(effect is DrawEffect.Refused)
        assertEquals("2 fingers can't fill 5 teams", (effect as DrawEffect.Refused).message)
        assertEquals(DrawPhase.IDLE, e.phase)
        assertNull(e.outcome)
    }

    @Test
    fun `starter reveals at once even when suspense is asked for`() {
        val e = engine(mode = DrawMode.STARTER, instant = false)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        e.tick(now = 100_000)
        assertEquals(DrawPhase.REVEALED, e.phase)
    }

    @Test
    fun `order holds the result back when suspense is asked for`() {
        val e = engine(mode = DrawMode.ORDER, instant = false)
        e.onDown(1, 0f, 0f, now = 0)
        e.onDown(2, 0f, 0f, now = 0)
        e.tick(now = 100_000)
        assertEquals(DrawPhase.SUSPENSE, e.phase)
        e.reveal()
        assertEquals(DrawPhase.REVEALED, e.phase)
    }

    /**
     * The fairness claim the result screen makes. Every finger must win about
     * as often as any other, whatever order they landed in.
     */
    @Test
    fun `over many draws every position wins about equally often`() {
        val wins = IntArray(5)
        repeat(10_000) { seed ->
            val e = DrawEngine(DrawMode.STARTER, 3, 3, true, Random(seed))
            (0L..4L).forEach { e.onDown(it, 0f, 0f, now = 0) }
            val outcome = (e.tick(now = 100_000) as DrawEffect.Drawn).outcome
            wins[outcome.winnerId.toInt()]++
        }
        val expected = 10_000 / 5
        wins.forEachIndexed { i, count ->
            assertTrue(
                "finger $i won $count times, expected about $expected",
                count > expected * 0.85 && count < expected * 1.15,
            )
        }
    }
}
