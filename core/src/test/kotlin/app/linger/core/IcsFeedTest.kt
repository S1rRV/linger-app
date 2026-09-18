package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

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

    private fun events(ics: String): List<String> =
        ics.split("BEGIN:VEVENT").drop(1).map { it.substringBefore("END:VEVENT") }

    @Test
    fun `collecting and returning a car get half an hour each`() {
        // The timeline holds these as moments, because the traveller is not at
        // the branch for the four days between. A moment renders as a hairline
        // in a calendar grid, so the export gives each end a readable block.
        //
        // Half an hour is a rendering decision made here, at the edge, and not
        // a claim about how long a counter takes. Nothing upstream is told.
        val ics = IcsFeed.forTrip(trip(), generatedAt = generatedAt)
        val carEvents = events(ics).filter { it.contains("Sixt") }

        assertEquals(2, carEvents.size)
        assertTrue(carEvents.any { it.contains("DTSTART;TZID=America/Bogota:20261224T120000") }, ics)
        assertTrue(carEvents.any { it.contains("DTEND;TZID=America/Bogota:20261224T123000") }, ics)
        assertTrue(carEvents.any { it.contains("DTSTART;TZID=America/Bogota:20261228T120000") }, ics)
        assertTrue(carEvents.any { it.contains("DTEND;TZID=America/Bogota:20261228T123000") }, ics)
    }

    @Test
    fun `a flight keeps the length it actually is`() {
        // The block is only for moments. A leg already has a start and an end
        // worth drawing, and padding it would be inventing a time.
        val ics = IcsFeed.forTrip(trip(), generatedAt = generatedAt)

        assertTrue(ics.contains("DTSTART;TZID=America/New_York:20261223T235900"), ics)
        assertTrue(ics.contains("DTEND;TZID=America/Santo_Domingo:20261224T052500"), ics)
    }

    @Test
    fun `the title says the local time, because no client will`() {
        // The thing that actually answers "show it in the traveller's zone".
        // Calendar apps render every event in the viewer's current zone and no
        // property in the format overrides that, so the only place a local
        // reading survives untouched is the text.
        val ics = IcsFeed.forTrip(trip(), generatedAt = generatedAt)

        assertTrue(ics.contains("SUMMARY:DM 621 EWR to SDQ\\, departs 23:59 New York time"), ics)
        assertTrue(ics.contains("SUMMARY:Collect the Sixt car\\, 12:00 Bogota time"), ics)
        assertTrue(ics.contains("SUMMARY:Return the Sixt car\\, 12:00 Bogota time"), ics)
    }

    @Test
    fun `the same trip twice gives the same ids`() {
        // A feed is fetched over and over. An id that changed between fetches
        // would leave the traveller with a calendar full of every version of
        // every flight they ever booked.
        val once = IcsFeed.forTrip(trip(), generatedAt = generatedAt)
        val again = IcsFeed.forTrip(trip(), generatedAt = generatedAt.plus(7.days))

        assertEquals(uids(once), uids(again))
        assertEquals(4, uids(once).size)
    }

    @Test
    fun `moving a flight keeps its id and changes its time`() {
        // The other half of the same promise. A rescheduled flight has to
        // update in place, not arrive alongside the old one.
        val asBooked = IcsFeed.forTrip(trip(), generatedAt = generatedAt)
        val moved = Trips.group(
            listOf(
                Booking(
                    references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
                    segments = listOf(
                        leg("EWR", LocalDateTime(2026, 12, 23, 21, 30), "SDQ", LocalDateTime(2026, 12, 24, 2, 56), "DM 621"),
                        leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20), "DM 321"),
                    ),
                ),
                sixt(),
            ),
            home = newYork,
        ).single()

        val after = IcsFeed.forTrip(moved, generatedAt = generatedAt)

        assertEquals(uids(asBooked), uids(after))
        assertTrue(after.contains("DTSTART;TZID=America/New_York:20261223T213000"), after)
    }

    @Test
    fun `the description carries the numbers you would be asked for`() {
        // What is reachable from a calendar notification on a watch, standing
        // at a counter. Both numbers, because ADR-0003 keeps a set of them and
        // the desk asks for one while the agent asks for the other.
        val twoSellers = Booking(
            references = setOf(
                Reference(issuer = "Expedia", code = "73545609581279"),
                Reference(issuer = "Arajet", code = "AFGZ2M"),
            ),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
            ),
        )
        val ics = IcsFeed.forTrip(
            Trips.group(listOf(twoSellers), home = newYork).single(),
            generatedAt = generatedAt,
        )

        val unfolded = ics.replace("\r\n ", "")
        assertTrue(unfolded.contains("DESCRIPTION:Arajet AFGZ2M"), unfolded)
        assertTrue(unfolded.contains("Expedia 73545609581279"), unfolded)
    }

    @Test
    fun `a booking with no numbers has no description to give`() {
        // Rather than an empty DESCRIPTION, which reads in a calendar as though
        // something was lost.
        val anonymous = Booking(
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
            ),
        )
        val ics = IcsFeed.forTrip(
            Trips.group(listOf(anonymous), home = newYork).single(),
            generatedAt = generatedAt,
        )

        assertTrue(!ics.contains("DESCRIPTION:"), ics)
    }

    private fun uids(ics: String): Set<String> =
        Regex("UID:([^\r\n]+)").findAll(ics).map { it.groupValues[1] }.toSet()
}