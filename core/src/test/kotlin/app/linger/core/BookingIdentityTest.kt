package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
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
}
