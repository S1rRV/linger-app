package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 6: times in the file.
 *
 * Every event carries the zone of the place it happens in, and every zone
 * named is defined in the same file. A TZID without a VTIMEZONE is the usual
 * shortcut and it is malformed: clients that resolve it do so by guessing.
 *
 * ROADMAP.md names timezone bugs as the top risk in the whole build, so these
 * are the assertions that matter most in seam 6.
 */
class IcsTimeTest {

    // Passed in rather than read from a clock, so the same Trip always
    // renders the same bytes and the domain keeps its hands off the platform.
    private val generatedAt = Instant.parse("2026-12-01T09:00:00Z")

    private val newYork = Home(assertNotNull(Airports.find("EWR")))

    private fun overnightFlight() = Booking(
        references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
        segments = listOf(
            Segment.between(
                from = assertNotNull(Airports.find("EWR")),
                departingAt = LocalDateTime(2026, 12, 23, 23, 59),
                to = assertNotNull(Airports.find("SDQ")),
                arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
                operator = "Arajet",
                serviceNumber = "DM 621",
            ),
        ),
    )

    private fun feed(vararg bookings: Booking) =
        IcsFeed.forTrip(Trips.group(bookings.toList(), home = newYork).single(), generatedAt = generatedAt)

    @Test
    fun `a leg starts and ends in its own zone, not one zone for both`() {
        // docs/samples/expedia-arajet-ewr-mde.eml leaves Newark at 23:59 on the
        // 23rd and lands in Santo Domingo at 5:25 on the 24th. Newark is UTC-5
        // in December and Santo Domingo is UTC-4 all year.
        //
        // Writing both ends in one zone is the bug this asserts against: it
        // would make the leg an hour longer or shorter than it is, and the
        // stated arrival would not match what the confirmation printed.
        val ics = feed(overnightFlight())

        assertTrue(ics.contains("DTSTART;TZID=America/New_York:20261223T235900\r\n"), ics)
        assertTrue(ics.contains("DTEND;TZID=America/Santo_Domingo:20261224T052500\r\n"), ics)
    }

    @Test
    fun `no naive datetime reaches the file`() {
        // DATA-MODEL.md invariant 5. Every DTSTART, DTEND and DTSTAMP either
        // names a zone or ends in Z. A bare local time is the one thing that
        // can silently be read as the wrong moment.
        val ics = feed(overnightFlight())

        // A VTIMEZONE's own DTSTART is a bare local time by RFC 5545, and has
        // to be: it is the wall clock reading at which a rule starts applying,
        // so naming a zone for it would be circular. Those blocks are cut out
        // rather than excused, so this stays an assertion about events.
        val events = ics.replace(Regex("BEGIN:VTIMEZONE.*?END:VTIMEZONE\r\n", RegexOption.DOT_MATCHES_ALL), "")
        assertTrue(events.contains("BEGIN:VEVENT"))
        val stamps = Regex("(DTSTART|DTEND|DTSTAMP)[^:\r\n]*:[^\r\n]+").findAll(events).map { it.value }.toList()
        assertTrue(stamps.isNotEmpty())
        stamps.forEach { line ->
            val isZoned = line.contains(";TZID=") || line.substringAfter(":").endsWith("Z")
            assertTrue(isZoned, "naive datetime in the feed: $line")
        }
    }

    @Test
    fun `every zone the file names is defined in the file`() {
        val ics = feed(overnightFlight())

        val named = Regex("TZID=([^:;\r\n]+)").findAll(ics).map { it.groupValues[1] }.toSet()
        val defined = Regex("BEGIN:VTIMEZONE\r\nTZID:([^\r\n]+)").findAll(ics).map { it.groupValues[1] }.toSet()
        assertEquals(named, defined)
    }

    @Test
    fun `a zone with no daylight saving is defined once and simply`() {
        // Santo Domingo has not observed daylight saving since 1974, and
        // Colombia never has. A single STANDARD component is the whole truth
        // about them, and inventing a DAYLIGHT component would be a lie a
        // client would act on.
        val ics = feed(overnightFlight())

        val block = ics.substringAfter("TZID:America/Santo_Domingo").substringBefore("END:VTIMEZONE")
        assertEquals(1, Regex("BEGIN:STANDARD").findAll(block).count())
        assertEquals(0, Regex("BEGIN:DAYLIGHT").findAll(block).count())
        assertTrue(block.contains("TZOFFSETTO:-0400"), block)
    }

    @Test
    fun `a trip across a daylight saving change carries both offsets`() {
        // Invented, because the Colombia trip crosses no boundary: nothing in
        // docs/samples/ would catch this. The United States moves its clocks on
        // 8 March 2027, so a trip from 5 to 15 March straddles it.
        //
        // With one STANDARD component covering the whole trip, every event
        // after the 8th is an hour out. This is precisely the bug ROADMAP.md
        // flags as the top risk in the build.
        val marchTrip = Booking(
            references = setOf(Reference(issuer = "Expedia", code = "11122233344455")),
            segments = listOf(
                Segment.between(
                    from = assertNotNull(Airports.find("EWR")),
                    departingAt = LocalDateTime(2027, 3, 5, 9, 0),
                    to = assertNotNull(Airports.find("MDE")),
                    arrivingAt = LocalDateTime(2027, 3, 5, 15, 30),
                    operator = "Arajet",
                ),
                Segment.between(
                    from = assertNotNull(Airports.find("MDE")),
                    departingAt = LocalDateTime(2027, 3, 15, 8, 0),
                    to = assertNotNull(Airports.find("EWR")),
                    arrivingAt = LocalDateTime(2027, 3, 15, 15, 10),
                    operator = "Arajet",
                ),
            ),
        )

        val ics = feed(marchTrip)
        val block = ics.substringAfter("TZID:America/New_York").substringBefore("END:VTIMEZONE")

        assertEquals(1, Regex("BEGIN:STANDARD").findAll(block).count(), block)
        assertEquals(1, Regex("BEGIN:DAYLIGHT").findAll(block).count(), block)
        assertTrue(block.contains("TZOFFSETFROM:-0500"), block)
        assertTrue(block.contains("TZOFFSETTO:-0400"), block)
    }

    @Test
    fun `the offsets in a zone definition are the ones the zone really has`() {
        // Read straight off kotlinx-datetime rather than off a table written
        // here, so the assertion cannot agree with a mistake in the generator.
        val ics = feed(overnightFlight())
        val block = ics.substringAfter("TZID:America/New_York").substringBefore("END:VTIMEZONE")

        val zone = TimeZone.of("America/New_York")
        val moment = Instant.parse("2026-12-23T12:00:00Z")
        val actual = (moment.toLocalDateTime(zone).toInstant(TimeZone.UTC) - moment).inWholeHours.toInt()
        assertTrue(block.contains("TZOFFSETTO:${if (actual < 0) "-" else "+"}0${-actual}00"), block)
    }
}
