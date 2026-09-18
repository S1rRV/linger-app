package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Seam 1: a Segment built from the two bare clock readings a confirmation
 * actually states.
 *
 * ADR-0001 is why Segments exist at all rather than legs living inside each
 * booking kind's payload.
 */
class SegmentTest {

    @Test
    fun `a segment crossing a zone boundary has the duration the clocks hide`() {
        // docs/samples/expedia-arajet-ewr-mde.eml, first leg:
        //   "EWR  11:59pm Wed, Dec 23"  ->  "SDQ  5:25am Thu, Dec 24"
        // Newark is UTC-5 in December, Santo Domingo UTC-4 all year, so the leg
        // runs 04:59Z to 09:25Z: 4 h 26 m.
        //
        // Subtracting the stated clock readings gives 5 h 26 m. This assertion
        // is wrong by exactly one hour if the zones are ignored, so it cannot
        // pass by accident.
        val leg = Segment.between(
            from = assertNotNull(Airports.find("EWR")),
            departingAt = LocalDateTime(2026, 12, 23, 23, 59),
            to = assertNotNull(Airports.find("SDQ")),
            arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
        )

        assertEquals(4.hours + 26.minutes, leg.duration)
    }
}
