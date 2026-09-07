package de.drehtuer.shotgun.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The result screen claims the draw is fair - that no corner of the glass wins
 * more often. These check the field actually supports that claim.
 */
class HeatFieldTest {

    private fun at(d: FloatArray, w: Int, x: Int, y: Int) = d[y * w + x]

    @Test
    fun `no history means a flat, empty field`() {
        val d = HeatField.density(emptyList(), 40, 40)
        assertTrue(d.all { it == 0f })
    }

    @Test
    fun `the peak is normalised to one`() {
        val d = HeatField.density(listOf(0.5f to 0.5f), 60, 60)
        assertEquals(1f, d.max(), 1e-4f)
    }

    @Test
    fun `density is highest where the point is and fades with distance`() {
        val w = 80
        val d = HeatField.density(listOf(0.5f to 0.5f), w, 80)
        val centre = at(d, w, 40, 40)
        val near = at(d, w, 45, 40)
        val far = at(d, w, 70, 40)
        assertTrue(centre > near)
        assertTrue(near > far)
    }

    /**
     * The reason edge mirroring exists. A win in the corner must register as
     * strongly as one in the middle, or the field would show the corners as
     * cold and imply the draw favours the centre.
     */
    @Test
    fun `a corner win is as hot at its own location as a centre win`() {
        val w = 80
        val corner = HeatField.density(listOf(0f to 0f), w, 80)
        val centre = HeatField.density(listOf(0.5f to 0.5f), w, 80)
        assertEquals(at(centre, w, 40, 40), at(corner, w, 0, 0), 1e-3f)
    }

    @Test
    fun `an evenly spread history produces an even field`() {
        val w = 60
        val points = buildList {
            for (i in 0 until 6) for (j in 0 until 6) {
                add((i + 0.5f) / 6f to (j + 0.5f) / 6f)
            }
        }
        val d = HeatField.density(points, w, 60)
        // Sampled at each point's own location, the field should barely vary.
        val samples = points.map { (x, y) -> at(d, w, (x * w).toInt(), (y * 60).toInt()) }
        assertTrue(
            "spread too wide: ${samples.min()}..${samples.max()}",
            samples.max() - samples.min() < 0.15f,
        )
    }

    @Test
    fun `every value stays inside the unit range`() {
        val d = HeatField.density(List(50) { (it % 7) / 7f to (it % 11) / 11f }, 50, 90)
        assertTrue(d.all { it in 0f..1f })
    }

    // ---- ramp ---------------------------------------------------------------

    private val ramp = listOf(
        HeatStop(0f, 0xFF000000.toInt()),
        HeatStop(0.5f, 0xFF808080.toInt()),
        HeatStop(1f, 0xFFFFFFFF.toInt()),
    )

    @Test
    fun `ramp ends map to their stops`() {
        assertEquals(0xFF000000.toInt(), HeatField.sample(ramp, 0f))
        assertEquals(0xFFFFFFFF.toInt(), HeatField.sample(ramp, 1f))
    }

    @Test
    fun `ramp interpolates between stops`() {
        val mid = HeatField.sample(ramp, 0.25f)
        val r = (mid shr 16) and 0xFF
        assertTrue("expected mid grey, got $r", r in 0x3A..0x46)
    }

    @Test
    fun `values outside the range are clamped, never wrapped`() {
        assertEquals(HeatField.sample(ramp, 0f), HeatField.sample(ramp, -5f))
        assertEquals(HeatField.sample(ramp, 1f), HeatField.sample(ramp, 5f))
    }

    @Test
    fun `colorise produces one opaque pixel per cell`() {
        val pixels = HeatField.colorise(FloatArray(12) { it / 12f }, ramp)
        assertEquals(12, pixels.size)
        assertTrue(pixels.all { (it ushr 24) == 0xFF })
    }

    // ---- degenerate input ---------------------------------------------------

    /**
     * The field is built from whatever the panel size and the history happen to
     * be, so it has to survive both being empty. A crash here would take out the
     * result screen on the very first launch, before there is anything to show.
     */

    @Test
    fun `a zero-width or zero-height field is empty, not a crash`() {
        assertEquals(0, HeatField.density(listOf(0.5f to 0.5f), 0, 40).size)
        assertEquals(0, HeatField.density(listOf(0.5f to 0.5f), 40, 0).size)
        assertEquals(0, HeatField.density(listOf(0.5f to 0.5f), -5, 40).size)
    }

    /** Every contribution landing off the grid leaves nothing to normalise by. */
    @Test
    fun `points entirely outside the grid leave a flat field`() {
        val d = HeatField.density(listOf(9f to 9f), 20, 20)
        assertTrue(d.all { it == 0f })
    }

    // ---- the ramp -----------------------------------------------------------

    @Test
    fun `a ramp needs at least one stop`() {
        val thrown = runCatching { HeatField.colorise(FloatArray(4), emptyList()) }
        assertTrue(thrown.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `a single-stop ramp is that colour everywhere`() {
        val stops = listOf(HeatStop(0f, 0xFF102030.toInt()))
        val px = HeatField.colorise(floatArrayOf(0f, 0.5f, 1f), stops)
        assertTrue(px.all { it == 0xFF102030.toInt() })
    }

    @Test
    fun `values are clamped to the ends of the ramp`() {
        val stops = listOf(
            HeatStop(0.25f, 0xFF000000.toInt()),
            HeatStop(0.75f, 0xFFFFFFFF.toInt()),
        )
        // Below the first stop and above the last: the ends hold, and nothing
        // wraps around to the wrong colour.
        assertEquals(0xFF000000.toInt(), HeatField.sample(stops, -1f))
        assertEquals(0xFFFFFFFF.toInt(), HeatField.sample(stops, 2f))
    }

    /** Two stops at the same position have no span to interpolate across. */
    @Test
    fun `a zero-width span does not divide by zero`() {
        val stops = listOf(
            HeatStop(0.5f, 0xFF010101.toInt()),
            HeatStop(0.5f, 0xFF020202.toInt()),
        )
        assertEquals(0xFF010101.toInt(), HeatField.sample(stops, 0.5f))
    }
}
