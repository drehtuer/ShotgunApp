package de.drehtuer.playerpicker.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {

    @Test
    fun `draw route carries the mode as its only argument`() {
        assertEquals("draw/{mode}", Destination.Draw.route)
        assertEquals("mode", Destination.Draw.ARG_MODE)
    }

    @Test
    fun `routeFor builds a route for every mode`() {
        assertEquals("draw/STARTER", Destination.Draw.routeFor(DrawMode.STARTER))
        assertEquals("draw/ORDER", Destination.Draw.routeFor(DrawMode.ORDER))
        assertEquals("draw/TEAMS", Destination.Draw.routeFor(DrawMode.TEAMS))
    }

    /**
     * The NavHost reads the argument back with [DrawMode.valueOf], so every
     * route this builds has to survive the round trip.
     */
    @Test
    fun `every built route parses back to the mode it came from`() {
        for (mode in DrawMode.entries) {
            val parsed = DrawMode.valueOf(Destination.Draw.routeFor(mode).substringAfter("draw/"))
            assertEquals(mode, parsed)
        }
    }

    @Test
    fun `static routes are distinct and non-empty`() {
        val routes = listOf(
            Destination.Home.route,
            Destination.Draw.route,
            Destination.Result.route,
            Destination.Settings.route,
        )
        assertEquals(routes.size, routes.toSet().size)
        assertTrue(routes.none { it.isBlank() })
    }
}
