package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Seam 2: a Booking projects its Segments onto the timeline.
 *
 * ADR-0001 and DATA-MODEL.md invariant 1: one multi-leg flight is one Booking
 * and four events. Bookings never draw themselves; the timeline is the only
 * thing the UI renders.
 */
class BookingTimelineTest {

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
        )

    @Test
    fun `a four-leg booking projects four events, earliest first`() {
        // docs/samples/expedia-arajet-ewr-mde.eml is a single purchase under
        // confirmation AFGZ2M covering the whole return journey: Newark to
        // Medellin via Santo Domingo, then back via Punta Cana eleven days
        // later. One Booking, four legs.
        //
        // Deliberately built out of order, so the assertion tests ordering
        // rather than the order they were handed over in.
        val booking = Booking(
            segments = listOf(
                leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "PUJ", LocalDateTime(2027, 1, 3, 17, 9)),
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
                leg("PUJ", LocalDateTime(2027, 1, 3, 20, 10), "EWR", LocalDateTime(2027, 1, 3, 23, 30)),
                leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20)),
            ),
        )

        val timeline = booking.timeline()

        assertEquals(4, timeline.size)
        assertEquals(
            listOf("EWR", "SDQ", "MDE", "PUJ"),
            timeline.map { it.place.code },
        )
        // The first event starts when the traveller leaves Newark: 23:59 local,
        // UTC-5 in December, so 04:59Z on the 24th.
        assertEquals(Instant.parse("2026-12-24T04:59:00Z"), timeline.first().startsAt)
    }
}
