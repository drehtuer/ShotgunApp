package de.drehtuer.shotgun.draw

import de.drehtuer.shotgun.ui.navigation.DrawMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a ring looks is a set of real decisions - who is dimmed, what is
 * emphasised, which letter a team gets. They used to live inside a composable
 * where nothing could reach them.
 */
class RingSpecTest {

    private fun outcome(
        mode: DrawMode,
        ids: List<Long>,
        teamCount: Int? = null,
    ): DrawOutcome {
        val assignment = ids.withIndex().associate { (i, id) ->
            id to if (mode == DrawMode.TEAMS) i % (teamCount ?: 2) else i + 1
        }
        return DrawOutcome(
            mode = mode,
            teamCount = teamCount,
            assignment = assignment,
            order = ids,
            winnerId = ids.first(),
            fingers = ids.map { Finger(it, 0f, 0f) },
        )
    }

    // ---- before anything is decided ----------------------------------------

    @Test
    fun `a finger with no draw yet is a bare ring`() {
        val s = ringSpec(1, outcome = null, phase = DrawPhase.IDLE, revealed = false)
        assertEquals(RingRole.IDLE, s.role)
        assertNull(s.label)
        assertEquals(1f, s.alpha, 1e-4f)
    }

    @Test
    fun `a finger still awaiting its turn is pending, not idle`() {
        val o = outcome(DrawMode.ORDER, listOf(1, 2, 3))
        val s = ringSpec(3, o, DrawPhase.REVEALING, revealed = false)
        assertEquals(RingRole.PENDING, s.role)
        assertNull(s.label)
    }

    /** Mid-reveal, a finger already shown keeps its answer rather than pending. */
    @Test
    fun `a finger already revealed keeps its rank mid-reveal`() {
        val o = outcome(DrawMode.ORDER, listOf(1, 2, 3))
        val s = ringSpec(1, o, DrawPhase.REVEALING, revealed = true)
        assertEquals(RingRole.RANKED, s.role)
        assertEquals("1", s.label)
    }

    // ---- starter ------------------------------------------------------------

    @Test
    fun `the starter winner is emphasised and everyone else is dimmed`() {
        val o = outcome(DrawMode.STARTER, listOf(7, 8, 9))

        val won = ringSpec(7, o, DrawPhase.REVEALED, revealed = true)
        assertEquals(RingRole.WINNER, won.role)
        assertEquals("WON", won.label)
        assertTrue(won.filled)
        assertTrue(won.scale > 1f)

        val lost = ringSpec(8, o, DrawPhase.REVEALED, revealed = true)
        assertEquals(RingRole.LOSER, lost.role)
        assertNull(lost.label)
        assertTrue(lost.alpha < 0.5f)
        assertTrue(lost.scale < 1f)
    }

    // ---- order --------------------------------------------------------------

    @Test
    fun `rank one is the largest and fully opaque, the last the faintest`() {
        val o = outcome(DrawMode.ORDER, listOf(1, 2, 3, 4))
        val first = ringSpec(1, o, DrawPhase.REVEALED, revealed = true)
        val last = ringSpec(4, o, DrawPhase.REVEALED, revealed = true)

        assertEquals("1", first.label)
        assertEquals("4", last.label)
        assertEquals(1f, first.alpha, 1e-4f)
        assertTrue(last.alpha < first.alpha)
        assertTrue(first.labelSp > last.labelSp)
        assertTrue(first.filled)
        assertTrue(!last.filled)
    }

    /** The gradient is by rank, so it must not collapse with only two players. */
    @Test
    fun `two players still differ in weight`() {
        val o = outcome(DrawMode.ORDER, listOf(1, 2))
        val a = ringSpec(1, o, DrawPhase.REVEALED, revealed = true)
        val b = ringSpec(2, o, DrawPhase.REVEALED, revealed = true)
        assertNotEquals(a.alpha, b.alpha)
    }

    /** A single-finger outcome would divide by zero if the guard were dropped. */
    @Test
    fun `a one-finger order outcome does not divide by zero`() {
        val o = outcome(DrawMode.ORDER, listOf(1))
        val s = ringSpec(1, o, DrawPhase.REVEALED, revealed = true)
        assertEquals(1f, s.alpha, 1e-4f)
        assertEquals("1", s.label)
    }

    // ---- teams --------------------------------------------------------------

    @Test
    fun `a team ring carries its letter and the TEAM sub-label`() {
        val o = outcome(DrawMode.TEAMS, listOf(1, 2, 3, 4), teamCount = 2)
        val a = ringSpec(1, o, DrawPhase.REVEALED, revealed = true)
        val b = ringSpec(2, o, DrawPhase.REVEALED, revealed = true)

        assertEquals(RingRole.TEAM, a.role)
        assertEquals("A", a.label)
        assertEquals("B", b.label)
        assertEquals("TEAM", a.sub)
        assertEquals(0, a.teamIndex)
        assertEquals(1, b.teamIndex)
        assertTrue(a.filled)
    }

    @Test
    fun `team letters run A to Z and then wrap`() {
        assertEquals("A", teamLabel(0))
        assertEquals("Z", teamLabel(25))
        // Ambiguous by design-as-built: there is no 27th letter.
        assertEquals("A", teamLabel(26))
    }

    @Test
    fun `a nonsensical team index falls back to a number`() {
        assertEquals("0", teamLabel(-1))
    }

    @Test
    fun `only a ring showing an answer counts as answered`() {
        val o = outcome(DrawMode.ORDER, listOf(1, 2, 3))
        assertTrue(!ringSpec(1, null, DrawPhase.IDLE, revealed = false).answered)
        assertTrue(!ringSpec(3, o, DrawPhase.REVEALING, revealed = false).answered)
        assertTrue(ringSpec(1, o, DrawPhase.REVEALED, revealed = true).answered)
    }

    // ---- label placement ----------------------------------------------------

    @Test
    fun `the label goes above the ring when there is room`() {
        assertTrue(labelFitsAbove(fingerY = 900f, radiusPx = 56f, labelHeightPx = 64f, gapPx = 10f))
    }

    @Test
    fun `the label flips below near the top edge`() {
        assertTrue(!labelFitsAbove(fingerY = 40f, radiusPx = 56f, labelHeightPx = 64f, gapPx = 10f))
    }

    @Test
    fun `a label beside the ring clears it, above and below`() {
        val above = labelOffsetY(
            radiusPx = 56f, scale = 1f, labelHeightPx = 64f, gapPx = 10f,
            above = true, slide = 0f,
        )
        val below = labelOffsetY(
            radiusPx = 56f, scale = 1f, labelHeightPx = 64f, gapPx = 10f,
            above = false, slide = 0f,
        )
        assertEquals(-130f, above, 1e-3f)     // -56 - (64 + 10)
        assertEquals(66f, below, 1e-3f)       //  56 + 10
    }

    /** Lifted, the label sits centred on the ring whichever side it came from. */
    @Test
    fun `a lifted label lands centred on the ring from either side`() {
        val fromAbove = labelOffsetY(
            radiusPx = 56f, scale = 1f, labelHeightPx = 64f, gapPx = 10f,
            above = true, slide = 1f,
        )
        val fromBelow = labelOffsetY(
            radiusPx = 56f, scale = 1f, labelHeightPx = 64f, gapPx = 10f,
            above = false, slide = 1f,
        )
        assertEquals(-32f, fromAbove, 1e-3f)  // -labelHeight / 2
        assertEquals(-32f, fromBelow, 1e-3f)
    }

    @Test
    fun `a half-slid label is between the two positions`() {
        val beside = labelOffsetY(56f, 1f, 64f, 10f, above = true, slide = 0f)
        val inside = labelOffsetY(56f, 1f, 64f, 10f, above = true, slide = 1f)
        val half = labelOffsetY(56f, 1f, 64f, 10f, above = true, slide = 0.5f)
        assertEquals((beside + inside) / 2f, half, 1e-3f)
    }

    /** An enlarged ring pushes its label further out, or it would sit on the rim. */
    @Test
    fun `a scaled-up ring pushes the label further out`() {
        val plain = labelOffsetY(56f, 1f, 64f, 10f, above = true, slide = 0f)
        val big = labelOffsetY(56f, 1.14f, 64f, 10f, above = true, slide = 0f)
        assertTrue(big < plain)
    }
}
