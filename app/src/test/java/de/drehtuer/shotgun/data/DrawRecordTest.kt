package de.drehtuer.shotgun.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Normalisation is the one piece of the history layer that can corrupt data
 * silently: storing raw pixels would skew the fairness field, and the damage is
 * invisible until the surface size changes. These pin the behaviour down.
 */
class DrawRecordTest {

    @Test
    fun `corners map to the corners of the unit square`() {
        assertEquals(0f to 0f, normalise(0f, 0f, 1080f, 2400f))
        assertEquals(1f to 1f, normalise(1080f, 2400f, 1080f, 2400f))
    }

    @Test
    fun `centre maps to the centre`() {
        val (x, y) = normalise(540f, 1200f, 1080f, 2400f)
        assertEquals(0.5f, x, 1e-6f)
        assertEquals(0.5f, y, 1e-6f)
    }

    /** The same finger on two different screens must land on the same point. */
    @Test
    fun `the same relative position is device independent`() {
        val phone = normalise(270f, 600f, 1080f, 2400f)
        val tablet = normalise(400f, 900f, 1600f, 3600f)
        assertEquals(phone.first, tablet.first, 1e-6f)
        assertEquals(phone.second, tablet.second, 1e-6f)
    }

    @Test
    fun `positions outside the surface are clamped, never stored out of range`() {
        val (x, y) = normalise(-50f, 9999f, 1080f, 2400f)
        assertEquals(0f, x, 1e-6f)
        assertEquals(1f, y, 1e-6f)
    }

    /**
     * A zero-sized surface would divide by zero. Collapsing to the centre keeps
     * a draw from crashing and is honest: the point carries no position.
     */
    @Test
    fun `a degenerate surface collapses to the centre instead of NaN`() {
        val (x, y) = normalise(10f, 10f, 0f, 0f)
        assertEquals(0.5f, x, 1e-6f)
        assertEquals(0.5f, y, 1e-6f)
        assertTrue(!x.isNaN() && !y.isNaN())
    }

    @Test
    fun `every normalised value stays inside the unit square`() {
        val surfaces = listOf(1080f to 2400f, 720f to 1280f, 1440f to 3120f)
        for ((w, h) in surfaces) {
            for (px in listOf(-100f, 0f, w / 3f, w, w + 100f)) {
                for (py in listOf(-100f, 0f, h / 2f, h, h + 100f)) {
                    val (x, y) = normalise(px, py, w, h)
                    assertTrue("x=$x out of range", x in 0f..1f)
                    assertTrue("y=$y out of range", y in 0f..1f)
                }
            }
        }
    }
}
