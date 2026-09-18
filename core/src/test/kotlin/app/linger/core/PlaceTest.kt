package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Seam 1: turning a bare wall-clock reading into an instant.
 *
 * Every time in the sample confirmations is stated without a zone, so the Place
 * is the only route to one. DATA-MODEL.md invariant 5 forbids storing an
 * instant without it.
 *
 * Expected instants are worked out by hand from the offsets, not recomputed the
 * way the code computes them, so these tests can disagree with the code.
 */
class PlaceTest {

    @Test
    fun `a bare local time becomes an instant in the Place's zone`() {
        // docs/samples/expedia-arajet-ewr-mde.eml states only
        //   "EWR   11:59pm Wed, Dec 23"
        // New York is UTC-5 in December, so that is 04:59Z on the 24th.
        val newark = Place("EWR", "Newark Liberty International", TimeZone.of("America/New_York"))

        val departure = newark.instantAt(LocalDateTime(2026, 12, 23, 23, 59))

        assertEquals(Instant.parse("2026-12-24T04:59:00Z"), departure)
    }

    @Test
    fun `the same wall clock in two zones is two different instants`() {
        // 05:25 in Santo Domingo, UTC-4 all year, against 05:25 in Newark,
        // UTC-5 in December. An hour apart, and the stated times look identical.
        val santoDomingo = Place("SDQ", "Las Americas International", TimeZone.of("America/Santo_Domingo"))
        val newark = Place("EWR", "Newark Liberty International", TimeZone.of("America/New_York"))
        val clock = LocalDateTime(2026, 12, 24, 5, 25)

        assertEquals(Instant.parse("2026-12-24T09:25:00Z"), santoDomingo.instantAt(clock))
        assertEquals(Instant.parse("2026-12-24T10:25:00Z"), newark.instantAt(clock))
    }
}
