package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 6: a Trip as a calendar feed.
 *
 * Text in, text out. The webcal URL, the Android calendar writes and the
 * one-off download are delivery, and delivery is not domain.
 */
class IcsFeedTest {

    // Passed in rather than read from a clock, so the same Trip always
    // renders the same bytes and the domain keeps its hands off the platform.
    private val generatedAt = Instant.parse("2026-12-01T09:00:00Z")

    private val newYork = Home(assertNotNull(Airports.find("EWR")))

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime, flight: String) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
            operator = "Arajet",
            serviceNumber = flight,
        )

    /** docs/samples/expedia-arajet-ewr-mde.eml, outbound only. */
    private fun arajet() = Booking(
        references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
        segments = listOf(
            leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
            leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20), "DM 321"),
        ),
    )

    /** docs/samples/sixt-car-medellin.eml, reservation 9739400602. */
    private fun sixt() = Booking(
        references = setOf(Reference(issuer = "Sixt", code = "9739400602")),
        segments = listOf(
            Segment.held(
                from = Place(
                    code = "Sixt Medellin City El Poblado",
                    name = "Sixt Medellin City El Poblado",
                    zone = assertNotNull(Countries.soleZoneOf("CO")),
                ),
                collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                operator = "Sixt",
            ),
        ),
    )

    private fun trip() = Trips.group(listOf(arajet(), sixt()), home = newYork).single()

    @Test
    fun `two flight legs and one rental make four events`() {
        // The rental draws two because collection and return are two things to
        // turn up for. This is the timeline's answer, not a second opinion: the
        // export renders what the timeline already decided.
        val feed = IcsFeed.forTrip(trip(), generatedAt = generatedAt)

        assertEquals(4, feed.split("BEGIN:VEVENT").size - 1)
        assertEquals(4, feed.split("END:VEVENT").size - 1)
    }

    @Test
    fun `the file is a calendar a parser will accept`() {
        val feed = IcsFeed.forTrip(trip(), generatedAt = generatedAt)

        assertTrue(feed.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(feed.trimEnd('\r', '\n').endsWith("END:VCALENDAR"))
        // VERSION and PRODID are the two properties RFC 5545 requires of a
        // VCALENDAR. Without them some clients reject the whole file rather
        // than the one event they object to.
        assertTrue(feed.contains("VERSION:2.0\r\n"))
        assertTrue(feed.contains("PRODID:"))
    }

    @Test
    fun `every line ends with a carriage return and a newline`() {
        // RFC 5545 is explicit about CRLF, and a bare newline is the single
        // most common reason a hand-built feed fails to import at all.
        val feed = IcsFeed.forTrip(trip(), generatedAt = generatedAt)

        val bareNewlines = Regex("(?<!\r)\n").findAll(feed).count()
        assertEquals(0, bareNewlines)
    }
}
