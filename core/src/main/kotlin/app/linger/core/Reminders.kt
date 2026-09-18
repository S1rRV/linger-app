package app.linger.core

import kotlinx.datetime.Instant
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

    private val BEFORE_COLLECTING = 1.hours
    private val BEFORE_GIVING_BACK = 3.hours

    fun forBooking(booking: Booking, now: Instant): List<Reminder> =
        booking.segments
            .filter { it.endMustBeAttended }
            .flatMap { segment ->
                listOf(
                    Reminder(ReminderRule.PICKUP, segment.startsAt - BEFORE_COLLECTING),
                    Reminder(ReminderRule.RETURN, segment.endsAt - BEFORE_GIVING_BACK),
                )
            }
}
