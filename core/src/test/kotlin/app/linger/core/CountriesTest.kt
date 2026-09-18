package app.linger.core

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Seam 1: the fallback when there is no airport code.
 *
 * The Sixt pickup in docs/samples/sixt-car-medellin.eml is a street address,
 * "Carrera 43b No 1a sur-184, Medellin". ROADMAP.md's chain runs: the vendor's
 * calendar file, then the airport code when the branch sits at an airport, then
 * the country, then flag it. This is the country step.
 */
class CountriesTest {

    @Test
    fun `a country with one zone answers`() {
        // Colombia is UTC-5 with no daylight saving and a single zone, so the
        // Medellin branch resolves without geocoding a street address.
        assertEquals(TimeZone.of("America/Bogota"), Countries.soleZoneOf("CO"))
    }

    @Test
    fun `a country with several zones does not answer`() {
        // Picking one would be right for part of the country and wrong for the
        // rest. That is the guess the roadmap forbids, so the step declines and
        // the Booking is flagged.
        assertNull(Countries.soleZoneOf("US"))
    }

    @Test
    fun `a country we know nothing about does not answer`() {
        assertNull(Countries.soleZoneOf("JP"))
    }
}
