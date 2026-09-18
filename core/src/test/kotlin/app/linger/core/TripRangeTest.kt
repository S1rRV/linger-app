package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Seam 4: how long a Trip runs.
 *
 * First Segment start to last Segment end, per DATA-MODEL.md. Not arrival at
 * the first Destination: the countdown a traveller wants is "leave home in six
 * days", and that clock starts at the front door.
 */
class TripRangeTest {

    private val newYork = Home(assertNotNull(Airports.find("EWR")))

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
        )

    @Test
    fun `the trip runs from leaving home to getting back`() {
        // docs/samples/expedia-arajet-ewr-mde.eml plus the Alamo car, which is
        // the pair that matters: the car is returned at noon in Cartagena on 3
        // January and the flight lands at Newark at 23:30 the same day, so the
        // last Segment to start is not the last one to end.
        val arajet = Booking(
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
                leg("PUJ", LocalDateTime(2027, 1, 3, 20, 10), "EWR", LocalDateTime(2027, 1, 3, 23, 30)),
            ),
        )
        val alamo = Booking(
            segments = listOf(
                Segment.held(
                    from = Place(
                        code = "Alamo Cartagena Airport",
                        name = "Alamo Cartagena Airport",
                        zone = assertNotNull(Countries.soleZoneOf("CO")),
                    ),
                    collectedAt = LocalDateTime(2026, 12, 29, 12, 0),
                    returnedAt = LocalDateTime(2027, 1, 3, 12, 0),
                ),
            ),
        )

        val trip = Trips.group(listOf(arajet, alamo), home = newYork).single()

        // 23:59 in Newark in December is UTC-5, so 04:59Z on the 24th.
        assertEquals(Instant.parse("2026-12-24T04:59:00Z"), trip.range.start)
        // 23:30 in Newark is 04:30Z the next morning. The car came back at
        // 17:00Z the same day, which is earlier, so the flight wins.
        assertEquals(Instant.parse("2027-01-04T04:30:00Z"), trip.range.endInclusive)
    }
}
