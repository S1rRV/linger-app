package app.linger.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 4: where the traveller lives, which is what bounds a Trip.
 *
 * Home is a set of Places rather than one. New York is one place to the person
 * who lives there and three airports to an airline, and a Home of only Newark
 * would decide that flying back into JFK never brought them home, which glues
 * two Trips into one. See ADR-0005.
 */
class HomeTest {

    private fun airport(code: String) = assertNotNull(Airports.find(code))

    @Test
    fun `any of the home airports counts as being back`() {
        val newYork = Home(airport("EWR"), airport("JFK"), airport("LGA"))

        // The sample trip leaves from and returns to Newark, so this is the
        // case that has to work. The other two are the ones that would
        // otherwise silently fail.
        assertTrue(airport("EWR") in newYork)
        assertTrue(airport("JFK") in newYork)
        assertTrue(airport("LGA") in newYork)
    }

    @Test
    fun `somewhere you are visiting is not home`() {
        val newYork = Home(airport("EWR"), airport("JFK"), airport("LGA"))

        assertFalse(airport("MDE") in newYork)
    }
}
