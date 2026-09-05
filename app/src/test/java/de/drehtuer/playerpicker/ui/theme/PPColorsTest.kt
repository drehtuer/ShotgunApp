package de.drehtuer.playerpicker.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the invariants the design relies on. These are cheap to break by
 * editing a hex value, and a broken one is only visible on device.
 */
class PPColorsTest {

    private val dark = ppDarkColors()
    private val light = ppLightColors()

    @Test
    fun `palettes report their own mode`() {
        assertTrue(dark.isDark)
        assertTrue(!light.isDark)
    }

    @Test
    fun `ink and ground are never the same colour`() {
        assertNotEquals(dark.bg, dark.ink)
        assertNotEquals(light.bg, light.ink)
    }

    /** The two palettes are inversions: dark ink is the light ground, and back. */
    @Test
    fun `palettes invert each other`() {
        assertEquals(dark.ink, light.bg)
        assertEquals(dark.bg, light.ink)
    }

    /** Text on an accent fill is the ground colour, per the design export. */
    @Test
    fun `accent ink is the ground colour`() {
        assertEquals(dark.bg, dark.accentInk)
        assertEquals(light.bg, light.accentInk)
    }

    @Test
    fun `accentSoft is a translucent accent`() {
        for (colors in listOf(dark, light)) {
            assertEquals(colors.accent.red, colors.accentSoft.red, 0.001f)
            assertEquals(colors.accent.green, colors.accentSoft.green, 0.001f)
            assertEquals(colors.accent.blue, colors.accentSoft.blue, 0.001f)
            assertTrue(colors.accentSoft.alpha < colors.accent.alpha)
        }
    }

    /**
     * Team rings index into this list with `team % size`, so it must be
     * non-empty; six is what the design defines.
     */
    @Test
    fun `there are six team fills and none is invisible`() {
        for (colors in listOf(dark, light)) {
            assertEquals(6, colors.teamFills.size)
            assertTrue(colors.teamFills.none { it.bg == it.ink })
        }
    }

    /** The heatmap interpolates between stops, so they must span 0..1 in order. */
    @Test
    fun `heat ramp runs cold to hot across the full range`() {
        for (colors in listOf(dark, light)) {
            val positions = colors.heatRamp.map { it.position }
            assertEquals(0f, positions.first(), 0.0001f)
            assertEquals(1f, positions.last(), 0.0001f)
            assertEquals(positions, positions.sorted())
        }
    }
}
