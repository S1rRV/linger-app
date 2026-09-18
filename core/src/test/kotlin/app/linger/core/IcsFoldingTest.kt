package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 6: no line in the file runs past 75 octets.
 *
 * RFC 5545 is not advisory about this. A long LOCATION is the realistic way to
 * hit it, and the samples are already close: Booking.com writes the Alamo
 * branch as "CARTAGENA RAFAEL NUNEZ INTL AIRPORT, LOCAL 01-08, Cartagena,
 * Colombia, 130002".
 */
class IcsFoldingTest {

    private val generatedAt = Instant.parse("2026-12-01T09:00:00Z")

    private fun feedWithBranchNamed(name: String): String {
        val booking = Booking(
            references = setOf(Reference(issuer = "Booking.com", code = "721303130")),
            segments = listOf(
                Segment.held(
                    from = Place(
                        code = "Alamo Cartagena",
                        name = name,
                        zone = assertNotNull(Countries.soleZoneOf("CO")),
                    ),
                    collectedAt = LocalDateTime(2026, 12, 29, 12, 0),
                    returnedAt = LocalDateTime(2027, 1, 3, 12, 0),
                    operator = "Alamo",
                ),
            ),
        )
        val trip = Trips.group(listOf(booking), home = Home(assertNotNull(Airports.find("EWR")))).single()
        return IcsFeed.forTrip(trip, generatedAt = generatedAt)
    }

    @Test
    fun `a long location is folded rather than sent as one long line`() {
        val ics = feedWithBranchNamed(
            "CARTAGENA RAFAEL NUNEZ INTL AIRPORT, LOCAL 01-08, Cartagena, Colombia, 130002",
        )

        val tooLong = ics.split("\r\n").filter { it.encodeToByteArray().size > 75 }
        assertEquals(emptyList(), tooLong)
    }

    @Test
    fun `a folded line continues with a single space, which is how it is read back`() {
        val ics = feedWithBranchNamed(
            "CARTAGENA RAFAEL NUNEZ INTL AIRPORT, LOCAL 01-08, Cartagena, Colombia, 130002",
        )

        // Unfolding is defined as deleting CRLF followed by one whitespace, so
        // the continuation has to start with exactly one space and nothing in
        // the value may be lost or gained by the round trip.
        val unfolded = ics.replace("\r\n ", "")
        assertTrue(
            unfolded.contains(
                "LOCATION:CARTAGENA RAFAEL NUNEZ INTL AIRPORT\\, LOCAL 01-08\\, Cartagena\\, Colombia\\, 130002",
            ),
            unfolded,
        )
    }

    @Test
    fun `a short line is left alone`() {
        val ics = feedWithBranchNamed("Alamo CTG")

        assertTrue(ics.contains("\r\nLOCATION:Alamo CTG\r\n"), ics)
    }

    @Test
    fun `folding counts bytes, not characters`() {
        // An accented branch name is where a character count and a byte count
        // disagree. RFC 5545 counts octets, so a name of accented letters is
        // half as long as it looks to a naive fold.
        val ics = feedWithBranchNamed("Á".repeat(70))

        val tooLong = ics.split("\r\n").filter { it.encodeToByteArray().size > 75 }
        assertEquals(emptyList(), tooLong)
    }
}
