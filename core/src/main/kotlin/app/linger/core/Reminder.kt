package app.linger.core

import kotlinx.datetime.Instant

/**
 * One notification, worked out from a Segment rather than set by hand.
 *
 * Carries the instant it fires because that is what a device scheduler needs,
 * and the rule that produced it so the same reminder can be recognised again
 * after the Booking moves. REMINDERS.md stores the offset, not the timestamp:
 * this is the offset already applied, recomputed whenever anything changes.
 */
data class Reminder(
    val rule: ReminderRule,
    val firesAt: Instant,
)

/**
 * The entries from REMINDERS.md that phase 0 can actually compute.
 *
 * The other seventeen are not deferred for being hard. They need live traffic,
 * an airline feed or check-in state, and none of those exist yet.
 */
enum class ReminderRule {
    /** An hour before an attended start: the rental counter is expecting you. */
    PICKUP,

    /**
     * Three hours before an attended end.
     *
     * Longer than the pickup warning because giving a car back is not turning
     * up. It is fuel, a detour to the branch, and a queue.
     */
    RETURN,
}
