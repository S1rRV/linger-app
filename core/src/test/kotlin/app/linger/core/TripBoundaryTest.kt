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

    @Test
    fun `connecting through your own home airport does not end the trip`() {
        // Invented, because no sample routes through Newark mid-ticket. It is
        // the case ADR-0005 admitted the Home rule would get wrong, and it is
        // worth getting right: half the cheap fares out of New York connect
        // somewhere, and one of those somewheres is home.
        //
        // What saves it is that the touch is inside a single purchase. A
        // traveller sitting in Terminal C for two hours on one ticket has not
        // been home, whatever the airport code says, and this needs no
        // threshold to tell: it is one Booking.
        val throughNewark = Booking(
            segments = listOf(
                leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "EWR", LocalDateTime(2027, 1, 3, 19, 40)),
                leg("EWR", LocalDateTime(2027, 1, 3, 21, 55), "LGW", LocalDateTime(2027, 1, 4, 9, 50)),
            ),
        )

        val trips = Trips.group(listOf(throughNewark), home = newYork)

        assertEquals(1, trips.size)
    }

    @Test
    fun `the whole sample itinerary is one trip`() {
        // Everything in docs/samples/, which is the case ADR-0005 was written
        // to get right. Four bookings, three vendors, two cities 1,100 km
        // apart, twelve days. The old geography rule had no answer for it.
        //
        //   Arajet    EWR -> SDQ -> MDE, 23 Dec, and back 3 Jan via PUJ
        //   Sixt      Medellin El Poblado, 24 to 28 Dec
        //   JetSMART  MDE -> CTG 28 Dec, CTG -> MDE 3 Jan
        //   Alamo     Cartagena airport, 29 Dec to 3 Jan
        //
        // Newark is touched exactly twice, at each end, which is what makes
        // this one trip and not four.
        val poblado = Place(
            code = "Sixt Medellin City El Poblado",
            name = "Sixt Medellin City El Poblado",
            zone = assertNotNull(Countries.soleZoneOf("CO")),
        )
        val cartagenaAirport = Place(
            code = "Alamo Cartagena Airport",
            name = "Alamo Cartagena Airport",
            zone = assertNotNull(Countries.soleZoneOf("CO")),
        )

        val sixt = Booking(
            segments = listOf(
                Segment.held(
                    from = poblado,
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                ),
            ),
        )
        val jetsmart = Booking(
            segments = listOf(
                leg("MDE", LocalDateTime(2026, 12, 28, 5, 0), "CTG", LocalDateTime(2026, 12, 28, 6, 14)),
                leg("CTG", LocalDateTime(2027, 1, 3, 7, 50), "MDE", LocalDateTime(2027, 1, 3, 9, 9)),
            ),
        )
        val alamo = Booking(
            segments = listOf(
                Segment.held(
                    from = cartagenaAirport,
                    collectedAt = LocalDateTime(2026, 12, 29, 12, 0),
                    returnedAt = LocalDateTime(2027, 1, 3, 12, 0),
                ),
            ),
        )

        // Handed over in the order the confirmations arrived, which is not the
        // order they happen in.
        val trips = Trips.group(listOf(jetsmart, arajet(), alamo, sixt), home = newYork)

        assertEquals(1, trips.size)
        assertEquals(4, trips.single().bookings.size)
    }
}
