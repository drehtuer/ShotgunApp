package de.drehtuer.shotgun.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class TeamCountTest {

    @Test
    fun `two is the floor`() {
        assertEquals(2, clampTeamCount(2))
        assertEquals(2, clampTeamCount(1))
        assertEquals(2, clampTeamCount(0))
        assertEquals(2, clampTeamCount(-7))
    }

    /** "The screen is the limit" - nothing here imposes a ceiling. */
    @Test
    fun `there is no upper bound`() {
        assertEquals(3, clampTeamCount(3))
        assertEquals(99, clampTeamCount(99))
        assertEquals(Int.MAX_VALUE, clampTeamCount(Int.MAX_VALUE))
    }
}
