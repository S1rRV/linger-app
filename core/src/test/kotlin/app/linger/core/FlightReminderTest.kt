package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Seam 5: the one flight rule phase 0 can compute.
 *
 * The other six need an airline feed, live traffic or check-in state. This one
 * needs only the departure, which is sitting in the confirmation.
 */
class FlightReminderTest {

    private val wellBefore = Instant.parse("2026-12-01T00:00:00Z")

    private fun firstLeg(departingAt: LocalDateTime) = Booking(
        references = setOf(Reference(issuer = "Expedia", code = "73545609581279")),
        segments = listOf(
            Segment.between(
                from = assertNotNull(Airports.find("EWR")),
                departingAt = departingAt,
                to = assertNotNull(Airports.find("SDQ")),
                arrivingAt = LocalDateTime(2026, 12, 24, 5, 25),
                operator = "Arajet",
                serviceNumber = "DM 621",
            ),
        ),
    )

    @Test
    fun `check-in opens a day before departure`() {
        // docs/samples/expedia-arajet-ewr-mde.eml leaves Newark at 23:59 on 23
        // December, which is 04:59Z on the 24th, so check-in opens at 04:59Z on
        // the 23rd.
        //
        // T-24h is the fallback, not the truth: the airline's own window wins
        // where one is known, and none is known here because nothing fetches
        // airline schedules yet.
        val reminders = Reminders.forBooking(firstLeg(LocalDateTime(2026, 12, 23, 23, 59)), now = wellBefore)

        val checkIn = reminders.single { it.rule == ReminderRule.CHECK_IN_OPENS }
        assertEquals(Instant.parse("2026-12-23T04:59:00Z"), checkIn.firesAt)
    }

    @Test
    fun `moving the flight moves the reminder`() {
        // REMINDERS.md's central promise: an offset against a field, never a
        // timestamp. Arajet moves the departure an hour earlier and check-in
        // follows without anything being rescheduled by hand.
        //
        // Nothing is stored between these two calls, which is the point:
        // reminders are derived, so re-deriving them is the whole of
        // re-anchoring.
        val asBooked = Reminders.forBooking(firstLeg(LocalDateTime(2026, 12, 23, 23, 59)), now = wellBefore)
        val asMoved = Reminders.forBooking(firstLeg(LocalDateTime(2026, 12, 23, 22, 59)), now = wellBefore)

        assertEquals(Instant.parse("2026-12-23T04:59:00Z"), asBooked.single().firesAt)
        assertEquals(Instant.parse("2026-12-23T03:59:00Z"), asMoved.single().firesAt)
    }
}
