package app.linger.core

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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

    @Test
    fun `a rental is one segment whose end is an appointment`() {
        // docs/samples/sixt-car-medellin.eml, reservation 9739400602: collected
        // and returned at Medellin City El Poblado, 24 to 28 December, 12:00
        // both ends. The traveller holds the car for the whole stretch, so this
        // is one span and not two moments.
        val poblado = Place(
            code = "Sixt Medellin City El Poblado",
            name = "Sixt Medellin City El Poblado",
            zone = assertNotNull(Countries.soleZoneOf("CO")),
        )

        val rental = Segment.held(
            from = poblado,
            collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
            returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
        )

        assertEquals(96.hours, rental.duration)
        assertTrue(rental.endMustBeAttended)
        // Back to the branch it came from, so nothing to warn about.
        assertFalse(rental.endIsElsewhere)
    }

    @Test
    fun `a one-way rental owes a counter visit somewhere it has not been`() {
        // No sample does this, so the case is written from the product rule
        // rather than from a confirmation: collect in Medellin, drop in
        // Cartagena. The return is at a Place the traveller has to find.
        val zone = assertNotNull(Countries.soleZoneOf("CO"))
        val poblado = Place("Sixt Medellin City El Poblado", "Sixt Medellin City El Poblado", zone)
        val cartagena = Place("Sixt Cartagena Airport", "Sixt Cartagena Airport", zone)

        val rental = Segment.held(
            from = poblado,
            collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
            to = cartagena,
            returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
        )

        assertTrue(rental.endIsElsewhere)
    }

    @Test
    fun `a flight end is not an appointment, because the plane lands anyway`() {
        val leg = Segment.between(
            from = assertNotNull(Airports.find("EWR")),
            departingAt = LocalDateTime(2026, 12, 23, 23, 59),
            to = assertNotNull(Airports.find("SDQ")),
            arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
        )

        assertFalse(leg.endMustBeAttended)
        assertFalse(leg.endIsElsewhere)
    }
}
