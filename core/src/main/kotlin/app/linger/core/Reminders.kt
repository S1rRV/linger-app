package app.linger.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

/**
 * Works out what a Booking should buzz about.
 *
 * Reads Segments rather than booking kinds. A rental's two ends are the same
 * thing the timeline already splits on, whether the traveller has to turn up
 * and do something, so the rules here need no switch on kind and a ferry with a
 * vehicle deck needs no new branch.
 */
object Reminders {

    private val MORNING = LocalTime(hour = 10, minute = 0)
    private val BEFORE_COLLECTING = 1.hours
    private val BEFORE_GIVING_BACK = 3.hours
    private val CHECK_IN_WINDOW = 24.hours

    fun forBooking(booking: Booking, now: Instant): List<Reminder> =
        booking.segments
            .flatMap { segment -> forSegment(booking, segment) }
            // Never in the past, and silently. Forwarding an old confirmation
            // should not buzz about a car collected last Christmas, and firing
            // late is worse than not firing: it teaches the traveller that the
            // notifications are noise. The fact stays on the Booking, so a
            // screen can still say what happened; only the buzz is dropped.
            .filter { it.firesAt > now }

    private fun forSegment(booking: Booking, segment: Segment): List<Reminder> =
        if (segment.endMustBeAttended) {
            listOfNotNull(
                Reminder(ReminderRule.PICKUP, segment.startsAt - BEFORE_COLLECTING),
                Reminder(ReminderRule.RETURN, segment.endsAt - BEFORE_GIVING_BACK),
                confirmFinalCost(booking, segment),
            )
        } else {
            // Nobody is waiting at a counter, so the obligation is the flight
            // itself and the only thing phase 0 can say about it is when
            // check-in usually opens.
            listOf(Reminder(ReminderRule.CHECK_IN_OPENS, segment.startsAt - CHECK_IN_WINDOW))
        }

    /**
     * Asks, the morning after, whether a hedged price moved.
     *
     * Only when the vendor hedged it. Sixt wrote "Estimated rental cost" and
     * Alamo wrote "Total Cost", and second-guessing the vendor that committed
     * would train the traveller to ignore the one that did not.
     *
     * The hour is read in the Place the car went back to, not on the phone.
     * This is the first rule anchored to a time of day rather than an offset,
     * so it is the first that would be wrong in a different country.
     */
    private fun confirmFinalCost(booking: Booking, segment: Segment): Reminder? {
        if (booking.money?.isAnEstimate != true) return null
        val dayAfter = segment.endsAt.toLocalDateTime(segment.to.zone).date.plus(1, DateTimeUnit.DAY)
        return Reminder(
            rule = ReminderRule.CONFIRM_FINAL_COST,
            firesAt = LocalDateTime(dayAfter, MORNING).toInstant(segment.to.zone),
        )
    }
}
