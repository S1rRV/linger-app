package app.linger.core

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Seam 1: the bundled airport table.
 *
 * PlaceTest covers the conversion arithmetic. This covers the data: that a code
 * maps to the right zone, and that an unfamiliar code is a gap rather than a
 * crash or a guess.
 */
class AirportsTest {

    @Test
    fun `a known code carries the right zone`() {
        val newark = assertNotNull(Airports.find("EWR"))

        assertEquals(TimeZone.of("America/New_York"), newark.zone)
    }

    @Test
    fun `lookup is forgiving about how a code is written`() {
        // Parsers will hand over whatever the email contained.
        assertEquals(Airports.find("EWR"), Airports.find(" ewr "))
    }

    @Test
    fun `an unrecognised code is a gap, not a crash and not a guess`() {
        // ROADMAP.md: flagged rather than guessed. The table will always be
        // incomplete, so this path is the common one, not the exception.
        assertNull(Airports.find("ZZZ"))
    }
}
