package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 3: folding a later record into the one already held.
 *
 * ADR-0003: the first record seen is primary. Later records fill fields that
 * are empty and never overwrite fields that are already set, because an
 * amendment is usually partial and a thinner record must not hollow out a
 * complete one.
 */
class BookingMergeTest {

    private val varun = Traveller(legalName = "Varun Sudhakar Ranipeta", commonName = "Varun")

    private fun branch() = Place(
        code = "Sixt Medellin City El Poblado",
        name = "Sixt Medellin City El Poblado",
        zone = assertNotNull(Countries.soleZoneOf("CO")),
    )

    private fun rental(collected: LocalDateTime, operator: String? = "Sixt") = Segment.held(
        from = branch(),
        collectedAt = collected,
        returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
        operator = operator,
    )

    @Test
    fun `the later record adds its reference without taking the first one away`() {
        // docs/samples/: Expedia issues 73545609581279 for flights Arajet holds
        // as AFGZ2M. Whichever lands second, the traveller wants both, because
        // the airline desk asks for one and the agent asks for the other.
        val fromExpedia = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
            travellers = setOf(varun),
            segments = listOf(rental(LocalDateTime(2026, 12, 24, 12, 0))),
        )
        val fromArajet = fromExpedia.copy(
            references = setOf(Reference(issuer = "Arajet", code = "AFGZ2M")),
        )

        val merged = Bookings.merge(first = fromExpedia, later = fromArajet)

        assertEquals(2, merged.references.size)
        assertTrue(Reference(issuer = "Expedia", code = "73545609581279") in merged.references)
        assertTrue(Reference(issuer = "Arajet", code = "AFGZ2M") in merged.references)
    }

    @Test
    fun `a thinner second record does not hollow out the first`() {
        // Sixt's "your booking has moved" states the new date and may say
        // nothing about the vehicle, the operator or the price. Without the
        // first-record-wins rule, every amendment would strip the booking back
        // to whatever the amendment happened to mention.
        val complete = Booking(
            references = setOf(Reference(issuer = "Sixt", code = "9739400602")),
            travellers = setOf(varun),
            segments = listOf(rental(LocalDateTime(2026, 12, 24, 12, 0))),
        )
        val thin = Booking(
            references = setOf(Reference(issuer = "Sixt", code = "9739400602")),
            travellers = emptySet(),
            segments = emptyList(),
        )

        val merged = Bookings.merge(first = complete, later = thin)

        assertEquals(setOf(varun), merged.travellers)
        assertEquals(complete.segments, merged.segments)
    }

    @Test
    fun `an empty field on the first record is filled by the second`() {
        // The other half of the same rule. Booking.com names Alamo as the
        // supplier where its own confirmation does not, so a second record that
        // knows the operator is worth reading.
        val withoutOperator = Booking(
            references = setOf(Reference(issuer = "Booking.com", code = "721303130")),
            travellers = emptySet(),
            segments = listOf(rental(LocalDateTime(2026, 12, 24, 12, 0), operator = null)),
        )
        val withOperator = Booking(
            references = setOf(Reference(issuer = "Alamo", code = "721303130")),
            travellers = setOf(varun),
            segments = listOf(rental(LocalDateTime(2026, 12, 24, 12, 0))),
        )

        val merged = Bookings.merge(first = withoutOperator, later = withOperator)

        assertEquals(setOf(varun), merged.travellers)
    }
}
