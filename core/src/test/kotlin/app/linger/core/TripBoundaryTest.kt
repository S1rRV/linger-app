package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Seam 4: which Bookings are the same Trip.
 *
 * ADR-0005: a Booking joins an existing Trip unless the traveller was Home in
 * between. Not geography, not a date window, and no threshold anywhere.
 */
class TripBoundaryTest {

    private val newYork = Home(
        assertNotNull(Airports.find("EWR")),
        assertNotNull(Airports.find("JFK")),
        assertNotNull(Airports.find("LGA")),
    )

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
        )

    /** docs/samples/expedia-arajet-ewr-mde.eml, confirmation AFGZ2M, all four legs. */
    private fun arajet() = Booking(
        segments = listOf(
            leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
            leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20)),
            leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "PUJ", LocalDateTime(2027, 1, 3, 17, 9)),
            leg("PUJ", LocalDateTime(2027, 1, 3, 20, 10), "EWR", LocalDateTime(2027, 1, 3, 23, 30)),
        ),
    )

    @Test
    fun `going home ends the trip`() {
        // The Arajet booking lands back at Newark on 3 January at 23:30. A
        // second booking in February is invented, because nothing in
        // docs/samples/ is a second trip: it exists to be told apart from the
        // first. Note what does not tell them apart. Same traveller, same two
        // airports, same account, and no gap rule is consulted. The only thing
        // that separates them is having been home in between.
        val february = Booking(
            segments = listOf(
                leg("EWR", LocalDateTime(2027, 2, 12, 8, 30), "MDE", LocalDateTime(2027, 2, 12, 14, 15)),
            ),
        )

        val trips = Trips.group(listOf(arajet(), february), home = newYork)

        assertEquals(2, trips.size)
        assertEquals(listOf(arajet()), trips.first().bookings)
        assertEquals(listOf(february), trips.last().bookings)
    }
}
