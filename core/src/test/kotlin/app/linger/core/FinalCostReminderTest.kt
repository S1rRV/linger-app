package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seam 5: asking afterwards whether the price moved.
 *
 * The one rule the traveller asked for by name. A rental total stated before
 * the car is driven can change, and the answer settled during the money
 * grilling was not to hedge the figure but to ask once, afterwards.
 */
class FinalCostReminderTest {

    private val wellBefore = Instant.parse("2026-12-01T00:00:00Z")

    private fun rental(money: Money, code: String, issuer: String) = Booking(
        references = setOf(Reference(issuer = issuer, code = code)),
        money = money,
        segments = listOf(
            Segment.held(
                from = Place(
                    code = "$issuer branch",
                    name = "$issuer branch",
                    zone = assertNotNull(Countries.soleZoneOf("CO")),
                ),
                collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                operator = issuer,
            ),
        ),
    )

    @Test
    fun `Sixt called it an estimate, so ask the morning after`() {
        // docs/samples/sixt-car-medellin.eml says "Estimated rental cost" over
        // US$267.45. The car comes back at 12:00 on the 28th, so the question
        // lands at 10:00 on the 29th in Medellin, which is 15:00Z.
        //
        // This is the first rule anchored to a time of day rather than an
        // offset, so it is the first one that would be wrong if the zone came
        // from the phone. Asked at 10:00 wherever the car was returned.
        val sixt = rental(
            money = Money(amount = "267.45", currency = "USD", vendorLabel = "Estimated rental cost"),
            code = "9739400602",
            issuer = "Sixt",
        )

        val reminders = Reminders.forBooking(sixt, now = wellBefore)

        val ask = reminders.single { it.rule == ReminderRule.CONFIRM_FINAL_COST }
        assertEquals(Instant.parse("2026-12-29T15:00:00Z"), ask.firesAt)
    }

    @Test
    fun `Alamo called it a total, so do not ask at all`() {
        // docs/samples/booking-alamo-car-cartagena.eml says "Total Cost:
        // US$265.57". A vendor that committed to a figure should not be
        // second-guessed by a notification, and asking anyway would train the
        // traveller to ignore the one time it matters.
        val alamo = rental(
            money = Money(amount = "265.57", currency = "USD", vendorLabel = "Total Cost"),
            code = "721303130",
            issuer = "Alamo",
        )

        val reminders = Reminders.forBooking(alamo, now = wellBefore)

        assertTrue(reminders.none { it.rule == ReminderRule.CONFIRM_FINAL_COST })
    }

    @Test
    fun `a booking with no money stated asks nothing`() {
        val noPrice = Booking(
            segments = listOf(
                Segment.held(
                    from = Place("branch", "branch", assertNotNull(Countries.soleZoneOf("CO"))),
                    collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                    returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                ),
            ),
        )

        val reminders = Reminders.forBooking(noPrice, now = wellBefore)

        assertTrue(reminders.none { it.rule == ReminderRule.CONFIRM_FINAL_COST })
    }
}
