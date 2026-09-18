package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Seam 7: which Places a Trip is about, and which it merely passes through.
 *
 * A layover under eight hours is a Waypoint. Eight hours or more is long enough
 * to leave the airport and see something, so it is a Destination. A Place where
 * a car is collected is a Destination whatever the clock says, because
 * collecting a car is not passing through.
 */
class DestinationTest {

    private val newYork = Home(assertNotNull(Airports.find("EWR")))

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
            operator = "Arajet",
        )

    /** The whole of docs/samples/, as four bookings. */
    private fun sampleTrip(): Trip {
        val colombia = assertNotNull(Countries.soleZoneOf("CO"))
        val arajet = Booking(
            references = setOf(Reference("Expedia", "73545609581279")),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
                leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20)),
                leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "PUJ", LocalDateTime(2027, 1, 3, 17, 9)),
                leg("PUJ", LocalDateTime(2027, 1, 3, 20, 10), "EWR", LocalDateTime(2027, 1, 3, 23, 30)),
            ),
        )
        val jetsmart = Booking(
            references = setOf(Reference("Expedia", "QCLZ7N")),
            segments = listOf(
                leg("MDE", LocalDateTime(2026, 12, 28, 5, 0), "CTG", LocalDateTime(2026, 12, 28, 6, 14)),
                leg("CTG", LocalDateTime(2027, 1, 3, 7, 50), "MDE", LocalDateTime(2027, 1, 3, 9, 9)),
            ),
        )
        val sixt = Booking(
            references = setOf(Reference("Sixt", "9739400602")),
            segments = listOf(
                Segment.held(
                    from = Place("Sixt Medellin", "Sixt Medellin City El Poblado", colombia, city = "Medellin"),
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                    operator = "Sixt",
                ),
            ),
        )
        val alamo = Booking(
            references = setOf(Reference("Booking.com", "721303130")),
            segments = listOf(
                Segment.held(
                    from = Place("Alamo Cartagena", "Alamo Cartagena Airport", colombia, city = "Cartagena"),
                    collectedAt = LocalDateTime(2026, 12, 29, 12, 0),
                    returnedAt = LocalDateTime(2027, 1, 3, 12, 0),
                    operator = "Alamo",
                ),
            ),
        )
        return Trips.group(listOf(arajet, jetsmart, sixt, alamo), home = newYork).single()
    }

    @Test
    fun `the short layovers are waypoints`() {
        // Santo Domingo is 2 h 25 m on the way out and Punta Cana is 3 h 1 m on
        // the way back, both with an onward flight already booked. Neither is
        // somewhere the traveller went.
        //
        // Medellin is reached twice, four days in December and then four hours
        // on 3 January connecting between two separate tickets, and it is not
        // listed here. A city ever stayed in is a Destination, and a later
        // connection through it does not demote it to somewhere passed through.
        val waypoints = sampleTrip().waypoints.map { it.code }

        assertEquals(listOf("SDQ", "PUJ"), waypoints)
    }

    @Test
    fun `the places actually stayed in are destinations`() {
        val destinations = sampleTrip().destinations.map { it.code }

        assertEquals(listOf("MDE", "CTG"), destinations)
    }

    @Test
    fun `home is neither, at either end`() {
        val trip = sampleTrip()

        assertEquals(emptyList(), (trip.destinations + trip.waypoints).filter { it.code == "EWR" })
    }

    @Test
    fun `eight hours is the line`() {
        // Invented, because no sample sits near the boundary. Seven hours and
        // fifty nine minutes is a long wait in a terminal; eight hours is time
        // enough to leave it, which is the whole distinction.
        fun onwardAt(hour: Int, minute: Int): List<String> {
            val booking = Booking(
                references = setOf(Reference("Expedia", "1")),
                segments = listOf(
                    leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
                    leg("SDQ", LocalDateTime(2026, 12, 24, hour, minute), "MDE", LocalDateTime(2026, 12, 24, 23, 0)),
                ),
            )
            return Trips.group(listOf(booking), home = newYork).single().destinations.map { it.code }
        }

        // Landing at 05:25, so 13:24 is seven hours fifty nine and 13:25 is
        // eight hours exactly. Both zones are fixed, so the wall clock and the
        // elapsed time agree here.
        assertEquals(listOf("MDE"), onwardAt(hour = 13, minute = 24))
        assertEquals(listOf("SDQ", "MDE"), onwardAt(hour = 13, minute = 25))
    }

    @Test
    fun `collecting a car makes the place a destination whatever the clock says`() {
        // A traveller who lands, collects a car and drives off within the hour
        // has still arrived somewhere. The layover rule would call this a
        // waypoint on the timing alone.
        val colombia = assertNotNull(Countries.soleZoneOf("CO"))
        val flights = Booking(
            references = setOf(Reference("Expedia", "2")),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
                leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20)),
                leg("MDE", LocalDateTime(2026, 12, 24, 14, 0), "EWR", LocalDateTime(2026, 12, 24, 22, 0)),
            ),
        )
        val car = Booking(
            references = setOf(Reference("Sixt", "3")),
            segments = listOf(
                Segment.held(
                    from = Place("Sixt MDE", "Sixt Medellin Airport", colombia, city = "Medellin"),
                    collectedAt = LocalDateTime(2026, 12, 24, 10, 0),
                    returnedAt = LocalDateTime(2026, 12, 24, 13, 0),
                    operator = "Sixt",
                ),
            ),
        )

        val trip = Trips.group(listOf(flights, car), home = newYork).single()

        assertEquals(listOf("Medellin"), trip.destinations.map { it.city })
    }

    @Test
    fun `the trip names itself after where it went`() {
        // DATA-MODEL.md: built from Destinations in the order they occur, with
        // Waypoints and Home left out. Never from a month or a year, because
        // this trip runs 23 December to 3 January and any such name is wrong at
        // one end.
        assertEquals("Medellin and Cartagena", sampleTrip().name)
    }

    @Test
    fun `three places read as a list`() {
        val colombia = assertNotNull(Countries.soleZoneOf("CO"))
        val flights = Booking(
            references = setOf(Reference("Expedia", "4")),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25)),
                leg("SDQ", LocalDateTime(2026, 12, 26, 7, 50), "MDE", LocalDateTime(2026, 12, 26, 9, 20)),
                leg("MDE", LocalDateTime(2026, 12, 29, 5, 0), "CTG", LocalDateTime(2026, 12, 29, 6, 14)),
                leg("CTG", LocalDateTime(2027, 1, 3, 7, 50), "EWR", LocalDateTime(2027, 1, 3, 15, 0)),
            ),
        )

        val trip = Trips.group(listOf(flights), home = newYork).single()

        assertEquals("Santo Domingo, Medellin and Cartagena", trip.name)
    }

    @Test
    fun `one place is just that place`() {
        val flights = Booking(
            references = setOf(Reference("Expedia", "5")),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "MDE", LocalDateTime(2026, 12, 24, 7, 25)),
                leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "EWR", LocalDateTime(2027, 1, 3, 21, 30)),
            ),
        )

        val trip = Trips.group(listOf(flights), home = newYork).single()

        assertEquals("Medellin", trip.name)
    }

    @Test
    fun `a trip with nowhere named falls back rather than coming out blank`() {
        // A Booking whose only Places are a car branch nobody has given a town
        // to. Better a plain word than an empty title in a list of trips.
        val nameless = Booking(
            references = setOf(Reference("Sixt", "6")),
            segments = listOf(
                Segment.held(
                    from = assertNotNull(Airports.find("MDE")),
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                ),
            ),
        )
        val trip = Trip(bookings = listOf(nameless), home = Home(assertNotNull(Airports.find("MDE"))))

        assertEquals("Trip", trip.name)
    }
}
