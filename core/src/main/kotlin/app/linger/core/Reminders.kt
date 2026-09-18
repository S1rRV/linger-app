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

    private val COUNTER_WARNING = 1.hours

    fun forBooking(booking: Booking, now: Instant): List<Reminder> =
        booking.segments
            .filter { it.endMustBeAttended }
            .map { Reminder(rule = ReminderRule.PICKUP, firesAt = it.startsAt - COUNTER_WARNING) }
}
