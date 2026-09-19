package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 12: a Trip as the JSON a phone screen reads.
 *
 * The domain stays the single source of truth for what a Trip is. The app
 * renders what this produces and works nothing out for itself, so a day view on
 * a phone cannot disagree with the tests in this module.
 *
 * Every time is written twice over: a wall clock reading with the zone it was
 * read in, which is what a traveller is shown, and the instant, which is what
 * sorting and countdowns use. Neither is derivable from the other on a phone
 * without a timezone database, so both are sent.
 */
class TripJsonTest {

    private val newYork = Home(assertNotNull(Airports.find("EWR")))

    private fun leg(from: String, departs: LocalDateTime, to: String, arrives: LocalDateTime, flight: String?) =
        Segment.between(
            from = assertNotNull(Airports.find(from)),
            departingAt = departs,
            to = assertNotNull(Airports.find(to)),
            arrivingAt = arrives,
            operator = "Arajet",
            serviceNumber = flight,
        )

    private fun sampleTrip(): Trip {
        val colombia = assertNotNull(Countries.soleZoneOf("CO"))
        val arajet = Booking(
            references = setOf(Reference("Expedia", "73545609581279")),
            money = Money.of("2939.78", "USD", vendorLabel = "Total paid"),
            segments = listOf(
                leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "DM 621"),
                leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20), "DM 321"),
            ),
        )
        val sixt = Booking(
            references = setOf(Reference("Sixt", "9739400602")),
            money = Money.of("267.45", "USD", vendorLabel = "Estimated rental cost"),
            segments = listOf(
                Segment.held(
                    from = Place("Sixt Medellin", "Sixt Medellin City, El Poblado", colombia, city = "Medellin"),
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                    operator = "Sixt",
                ),
            ),
        )
        return Trips.group(listOf(arajet, sixt), home = newYork).single()
    }

    private fun json() = TripJson.of(sampleTrip(), homeCurrency = "USD")

    @Test
    fun `the trip carries the name the domain worked out`() {
        assertTrue(json().contains("\"name\":\"Medellin\""), json())
    }

    @Test
    fun `events are grouped into the local day they happen on`() {
        // The leg out of Newark leaves at 23:59 on the 23rd and lands on the
        // 24th. It belongs to the 23rd, because that is the day the traveller
        // has to be at the airport. Grouping on the instant would file it under
        // the 24th, since 23:59 in Newark is already 04:59 UTC.
        val ics = json()

        assertTrue(ics.contains("\"date\":\"2026-12-23\""), ics)
        assertTrue(ics.contains("\"date\":\"2026-12-24\""), ics)
    }

    @Test
    fun `a time is sent as both a reading and an instant`() {
        // The reading is what the traveller sees and it is fixed. The instant
        // is what sorts and counts down. A phone cannot derive one from the
        // other without a timezone database, so both are written.
        val ics = json()

        assertTrue(ics.contains("\"clock\":\"23:59\""), ics)
        assertTrue(ics.contains("\"zone\":\"America/New_York\""), ics)
        assertTrue(ics.contains("\"at\":\"2026-12-24T04:59:00Z\""), ics)
    }

    @Test
    fun `a flight reads as a flight and a car reads as two appointments`() {
        val ics = json()

        assertTrue(ics.contains("\"title\":\"DM 621\""), ics)
        assertTrue(ics.contains("\"title\":\"Collect the Sixt car\""), ics)
        assertTrue(ics.contains("\"title\":\"Return the Sixt car\""), ics)
    }

    @Test
    fun `the numbers you would be asked for travel with the event`() {
        // The point of the whole app on the day: standing at a counter, the
        // number is on the screen you are already looking at.
        assertTrue(json().contains("\"code\":\"9739400602\""), json())
    }

    @Test
    fun `a place with a comma in it survives being written out`() {
        // "Sixt Medellin City, El Poblado" is exactly as Sixt writes it, and a
        // naive writer would end the string early or produce invalid JSON.
        assertTrue(json().contains("Sixt Medellin City, El Poblado"), json())
    }

    @Test
    fun `a quote or a backslash in a name cannot break the file`() {
        val awkward = Booking(
            references = setOf(Reference("Test", "1")),
            segments = listOf(
                Segment.held(
                    from = Place("x", """The "Grand" Hotel \ Annex""", assertNotNull(Countries.soleZoneOf("CO"))),
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 25, 12, 0),
                ),
            ),
        )
        val trip = Trips.group(listOf(awkward), home = newYork).single()

        val written = TripJson.of(trip, homeCurrency = "USD")

        assertTrue(written.contains("""The \"Grand\" Hotel \\ Annex"""), written)
    }

    @Test
    fun `the totals come through as the domain computed them`() {
        val ics = json()

        assertTrue(ics.contains("\"USD\":\"3207.23\""), ics)
    }

    @Test
    fun `what it writes is valid JSON`() {
        // Checked by shape rather than by eye: balanced braces and brackets,
        // outside of strings. A file the app cannot parse is the one failure
        // that makes every other assertion here pointless.
        val written = json()
        var braces = 0
        var brackets = 0
        var inString = false
        var escaped = false
        written.forEach { c ->
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> braces++
                c == '}' -> braces--
                c == '[' -> brackets++
                c == ']' -> brackets--
            }
            assertTrue(braces >= 0 && brackets >= 0, "closed something that was not open")
        }
        assertEquals(0, braces)
        assertEquals(0, brackets)
        assertTrue(!inString)
    }
}
