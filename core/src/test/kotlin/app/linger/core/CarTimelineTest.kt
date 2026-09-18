package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 2, the car half: one Segment, two TimelineEvents.
 *
 * A rental is one leg, not two. The traveller holds the car continuously from
 * collection to return, so there is one span of time with a Place at each end.
 * It draws two events because collection and return are two separate things the
 * traveller has to turn up and do, four days apart, which is a different
 * question from how many Segments there are.
 */
class CarTimelineTest {

    private fun branch(name: String) = Place(
        code = name,
        name = name,
        zone = assertNotNull(Countries.soleZoneOf("CO")),
    )

    @Test
    fun `a rental returned to its own branch projects two events at that branch`() {
        // docs/samples/sixt-car-medellin.eml, reservation 9739400602:
        //   pick-up  Thu 24 Dec 2026, 12:00
        //   return   Mon 28 Dec 2026, 12:00
        // both at Medellin City El Poblado. Colombia is UTC-5 all year, so the
        // two events land at 17:00Z.
        val poblado = branch("Sixt Medellin City El Poblado")
        val booking = Booking(
            segments = listOf(
                Segment.held(
                    from = poblado,
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                ),
            ),
        )

        val timeline = booking.timeline()

        assertEquals(2, timeline.size)
        assertEquals(listOf(poblado, poblado), timeline.map { it.place })
        assertEquals(Instant.parse("2026-12-24T17:00:00Z"), timeline.first().startsAt)
        assertEquals(Instant.parse("2026-12-28T17:00:00Z"), timeline.last().startsAt)
    }

    @Test
    fun `each event is the moment it asks for, not the four days in between`() {
        // The traveller is at the counter for the collection and at the counter
        // for the return. They are not at the branch for the ninety-six hours
        // between, and an event spanning those hours would fill the timeline
        // with a block that is wrong about where they are, and would swallow
        // every Gap the trip actually has.
        val poblado = branch("Sixt Medellin City El Poblado")
        val booking = Booking(
            segments = listOf(
                Segment.held(
                    from = poblado,
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                ),
            ),
        )

        val timeline = booking.timeline()

        assertEquals(timeline.first().startsAt, timeline.first().endsAt)
        assertEquals(timeline.last().startsAt, timeline.last().endsAt)
    }

    @Test
    fun `a flight still projects one event, because arriving asks nothing of you`() {
        // The rule is not "cars split and flights do not". It is whether the end
        // of the Segment is something the traveller has to turn up and do.
        // Landing is not: the plane arrives whether or not they participate.
        val booking = Booking(
            segments = listOf(
                Segment.between(
                    from = assertNotNull(Airports.find("EWR")),
                    departingAt = LocalDateTime(2026, 12, 23, 23, 59),
                    to = assertNotNull(Airports.find("SDQ")),
                    arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
                ),
            ),
        )

        assertEquals(1, booking.timeline().size)
    }

    @Test
    fun `a one-way rental returns at the drop-off, not where it was collected`() {
        // No sample does this: both cars in docs/samples/ come back to the
        // branch they left. The case is written from the product rule, because
        // getting it wrong sends the traveller 1,100 km to the wrong counter,
        // and the timeline is the only thing the UI renders.
        val poblado = branch("Sixt Medellin City El Poblado")
        val cartagena = branch("Sixt Cartagena Airport")
        val rental = Segment.held(
            from = poblado,
            collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
            to = cartagena,
            returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
        )
        val booking = Booking(segments = listOf(rental))

        val timeline = booking.timeline()

        assertEquals(listOf(poblado, cartagena), timeline.map { it.place })
        // And the Booking says so out loud, so the return can be shown
        // differently from an ordinary one.
        assertTrue(booking.hasOneWayHandback)
    }

    @Test
    fun `a rental back to its own branch is not a one-way handback`() {
        val poblado = branch("Sixt Medellin City El Poblado")
        val booking = Booking(
            segments = listOf(
                Segment.held(
                    from = poblado,
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                ),
            ),
        )

        assertFalse(booking.hasOneWayHandback)
    }

    @Test
    fun `a flight is never a one-way handback, however far apart its ends are`() {
        // from and to differ on every flight leg ever booked. The caveat is
        // about an appointment owed somewhere new, not about going somewhere.
        val booking = Booking(
            segments = listOf(
                Segment.between(
                    from = assertNotNull(Airports.find("EWR")),
                    departingAt = LocalDateTime(2026, 12, 23, 23, 59),
                    to = assertNotNull(Airports.find("SDQ")),
                    arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
                ),
            ),
        )

        assertFalse(booking.hasOneWayHandback)
    }
}
