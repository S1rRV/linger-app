package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 5: what the phone should buzz about, and when.
 *
 * REMINDERS.md is the contract: if a Booking carries the field, the reminder
 * exists, and if the field moves the reminder moves. Offsets, never timestamps.
 *
 * The two car rules are really one rule about Segments. Buzz an hour before an
 * attended start and three hours before an attended end, which needs no switch
 * on booking kind, and is the reason DATA-MODEL.md can promise one reminder
 * engine for a dinner and a transatlantic flight.
 */
class CarReminderTest {

    private val wellBefore = Instant.parse("2026-12-01T00:00:00Z")

    private fun branch() = Place(
        code = "Sixt Medellin City El Poblado",
        name = "Sixt Medellin City El Poblado",
        zone = assertNotNull(Countries.soleZoneOf("CO")),
    )

    /** docs/samples/sixt-car-medellin.eml, reservation 9739400602. */
    private fun sixt() = Booking(
        references = setOf(Reference(issuer = "Sixt", code = "9739400602")),
        segments = listOf(
            Segment.held(
                from = branch(),
                collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                operator = "Sixt",
            ),
        ),
    )

    @Test
    fun `an hour before you have to be at the counter`() {
        // Collection is 12:00 on 24 December in Medellin, which is UTC-5 all
        // year, so the counter is expecting them at 17:00Z and the phone should
        // buzz at 16:00Z. Asserting the instant is what proves the local
        // reading went through Colombia's zone rather than the phone's.
        val reminders = Reminders.forBooking(sixt(), now = wellBefore)

        val pickup = reminders.single { it.rule == ReminderRule.PICKUP }
        assertEquals(Instant.parse("2026-12-24T16:00:00Z"), pickup.firesAt)
    }

    @Test
    fun `three hours before you have to give the car back`() {
        // Return is 12:00 on 28 December, so 17:00Z, and the warning is at
        // 14:00Z. Three hours rather than one because handing a car back is not
        // turning up: it is fuel, a detour to the branch, and a queue.
        val reminders = Reminders.forBooking(sixt(), now = wellBefore)

        val ret = reminders.single { it.rule == ReminderRule.RETURN }
        assertEquals(Instant.parse("2026-12-28T14:00:00Z"), ret.firesAt)
    }

    @Test
    fun `a flight asks for neither, because nobody is waiting at a counter`() {
        // The rules are about attended ends, not about cars. A flight leg has
        // neither, so it produces neither, and the engine needed no opinion
        // about booking kinds to get there.
        val flight = Booking(
            segments = listOf(
                Segment.between(
                    from = assertNotNull(Airports.find("MDE")),
                    departingAt = LocalDateTime(2026, 12, 28, 5, 0),
                    to = assertNotNull(Airports.find("CTG")),
                    arrivingAt = LocalDateTime(2026, 12, 28, 6, 14),
                    operator = "JetSMART",
                ),
            ),
        )

        val reminders = Reminders.forBooking(flight, now = wellBefore)

        assertTrue(reminders.none { it.rule == ReminderRule.PICKUP || it.rule == ReminderRule.RETURN })
    }
}
