package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 3: whether two records describe the same Booking.
 *
 * ADR-0003: the same Seller and Reference, or the same journey. Either is
 * enough, and neither is enough on its own.
 */
class BookingIdentityTest {

    private val varun = Traveller(legalName = "Varun Sudhakar Ranipeta", commonName = "Varun")

    private fun branch() = Place(
        code = "Sixt Medellin City El Poblado",
        name = "Sixt Medellin City El Poblado",
        zone = assertNotNull(Countries.soleZoneOf("CO")),
    )

    @Test
    fun `a reference holds a booking together when the dates move under it`() {
        // docs/samples/sixt-car-medellin.eml, reservation 9739400602. Sixt
        // reschedules the pickup from the 24th to the 23rd and emails again.
        //
        // Nothing about the journey still matches: different start date, so a
        // different journey key. Only the reservation number connects them, and
        // this is exactly the duplicate ADR-0003 exists to prevent, since an
        // amendment arriving as a second Booking is the worst outcome.
        val sixt = Reference(issuer = "Sixt", code = "9739400602")
        val asBooked = Booking(
            references = setOf(sixt),
            travellers = setOf(varun),
            segments = listOf(
                Segment.held(
                    from = branch(),
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                    operator = "Sixt",
                ),
            ),
        )
        val asMoved = Booking(
            references = setOf(sixt),
            travellers = setOf(varun),
            segments = listOf(
                Segment.held(
                    from = branch(),
                    collectedAt = LocalDateTime(2026, 12, 23, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                    operator = "Sixt",
                ),
            ),
        )

        assertTrue(Bookings.sameBooking(asBooked, asMoved))
    }

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime, flight: String) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
            operator = "Arajet",
            serviceNumber = flight,
        )

    /** The outbound half of docs/samples/expedia-arajet-ewr-mde.eml. */
    private fun outbound() = listOf(
        leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
        leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20), "DM 321"),
    )

    @Test
    fun `the same flights from two sellers are one booking`() {
        // The case ADR-0003 opens with. Expedia sells the flights and issues
        // 73545609581279; Arajet flies them and emails its own AFGZ2M, which
        // airlines routinely do. No reference is shared, so reference matching
        // alone would file these as two Bookings, and the traveller would see
        // every flight twice and get every reminder twice.
        val fromExpedia = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = outbound(),
        )
        val fromArajet = Booking(
            references = setOf(Reference(issuer = "Arajet", code = "AFGZ2M")),
            travellers = setOf(varun),
            segments = outbound(),
        )

        assertTrue(Bookings.sameBooking(fromExpedia, fromArajet))
    }

    @Test
    fun `two people on the same flight are two bookings`() {
        // The false positive content matching can produce and reference
        // matching cannot. Varun and Arunima book the same flights separately,
        // which is why ADR-0003 puts the traveller in the journey key rather
        // than the journey alone. Merging these would put one person's ticket
        // numbers on the other person's booking.
        val arunima = Traveller(legalName = "Arunima", commonName = "Arunima")
        val his = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = outbound(),
        )
        val hers = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "88812390045511")),
            travellers = setOf(arunima),
            segments = outbound(),
        )

        assertFalse(Bookings.sameBooking(his, hers))
    }

    @Test
    fun `the same route a day later is a different booking`() {
        // The journey key is per Segment and carries the start local date, so
        // a weekly commuter does not collapse into one Booking.
        val thisWeek = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = outbound(),
        )
        val nextDay = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "99900011122233")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 24, 23, 59), "SDQ", LocalDateTime(2026, 12, 25, 5, 25), "DM 621"),
                leg("SDQ", LocalDateTime(2026, 12, 25, 7, 50), "MDE", LocalDateTime(2026, 12, 25, 9, 20), "DM 321"),
            ),
        )

        assertFalse(Bookings.sameBooking(thisWeek, nextDay))
    }

    @Test
    fun `two sellers disagreeing about the arrival still describe one flight`() {
        // Expedia prints 5:25am into Santo Domingo. Suppose Arajet's own email
        // says 5:31am, which is the sort of thing that differs between a seller
        // quoting a schedule and an operator quoting its own.
        //
        // The journey key holds the start and deliberately not the end, so this
        // stays one Booking. Without that, every such disagreement produces a
        // duplicate, which is the whole failure ADR-0003 is about.
        val fromExpedia = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
            ),
        )
        val fromArajet = Booking(
            references = setOf(Reference(issuer = "Arajet", code = "AFGZ2M")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 31), "DM 621"),
            ),
        )

        assertTrue(Bookings.sameBooking(fromExpedia, fromArajet))
    }

    @Test
    fun `two different flights out of one airport on one day are two bookings`() {
        // The key is operator, start place and start local date, which on its
        // own says these are the same journey. They are not: same airline, same
        // airport, same morning, different flights.
        //
        // This is what ADR-0003 keeps the service number for. It is a
        // tiebreaker rather than part of the key, because half of phase 0 has
        // none, but where both records state one it settles the question.
        val toSantoDomingo = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
            ),
        )
        val toPuntaCana = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "55544433322211")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 8, 15), "PUJ", LocalDateTime(2026, 12, 23, 12, 40), "DM 880"),
            ),
        )

        assertFalse(Bookings.sameBooking(toSantoDomingo, toPuntaCana))
    }

    @Test
    fun `a missing service number does not break the match`() {
        // The tiebreaker only speaks when both records state one. Sixt and
        // Alamo never do, and a seller that omits the flight number should not
        // stop a match it would otherwise make.
        val withNumber = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
            ),
        )
        val withoutNumber = Booking(
            references = setOf(Reference(issuer = "Arajet", code = "AFGZ2M")),
            travellers = setOf(varun),
            segments = listOf(
                Segment.between(
                    from = assertNotNull(Airports.find("EWR")),
                    departingAt = LocalDateTime(2026, 12, 23, 23, 59),
                    to = assertNotNull(Airports.find("SDQ")),
                    arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
                    operator = "Arajet",
                ),
            ),
        )

        assertTrue(Bookings.sameBooking(withNumber, withoutNumber))
    }

    @Test
    fun `an operator emailing only the outbound half still matches the whole`() {
        // Airlines routinely confirm one direction at a time while the seller
        // holds the whole purchase. Comparing whole journeys made these two
        // Bookings, so the traveller saw the outbound twice and got every
        // reminder for it twice.
        //
        // One matching Segment is enough. The same person cannot be on the same
        // flight, on the same day, under two genuinely separate bookings.
        val wholeTrip = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = outbound() + listOf(
                leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "PUJ", LocalDateTime(2027, 1, 3, 17, 9), "DM 322"),
                leg("PUJ", LocalDateTime(2027, 1, 3, 20, 10), "EWR", LocalDateTime(2027, 1, 3, 23, 30), "DM 622"),
            ),
        )
        val outboundOnly = Booking(
            references = setOf(Reference(issuer = "Arajet", code = "AFGZ2M")),
            travellers = setOf(varun),
            segments = outbound(),
        )

        assertTrue(Bookings.sameBooking(wholeTrip, outboundOnly))
    }

    @Test
    fun `sharing one leg is enough even when the rest of the trip differs`() {
        // The case that makes the rule worth having and also the one that could
        // go wrong. Two records that agree about one flight on one day for one
        // person are describing that flight, whatever else either of them says.
        val viaPuntaCana = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
                leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "PUJ", LocalDateTime(2026, 12, 24, 8, 40), "DM 900"),
            ),
        )

        assertTrue(Bookings.sameBooking(viaPuntaCana, Booking(
            references = setOf(Reference(issuer = "Arajet", code = "AFGZ2M")),
            travellers = setOf(varun),
            segments = outbound(),
        )))
    }

    @Test
    fun `different people sharing a leg are still different bookings`() {
        // The guard that has to survive the rule loosening. Any-Segment matching
        // widens what counts as a match, so the traveller check is now carrying
        // more weight than it was.
        val arunima = Traveller(legalName = "Arunima", commonName = "Arunima")

        assertFalse(
            Bookings.sameBooking(
                Booking(
                    references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
                    travellers = setOf(varun),
                    segments = outbound(),
                ),
                Booking(
                    references = setOf(Reference(issuer = "Expedia", code = "88812390045511")),
                    travellers = setOf(arunima),
                    segments = outbound(),
                ),
            ),
        )
    }
}
