package app.linger.core

import kotlinx.datetime.Instant

/**
 * A positioned thing on a Trip's day view.
 *
 * The only thing the UI renders: Bookings never draw themselves. Always has a
 * window, which is what the Gap calculation depends on and why an undated Idea
 * is a separate concept rather than an event with no time.
 */
data class TimelineEvent(
    val startsAt: Instant,
    val endsAt: Instant,
    val place: Place,
    /** The Segment this was drawn from. DATA-MODEL.md's `booking_id` reaches through it. */
    val segment: Segment,
    /** Which end of that Segment, or the whole of it. DATA-MODEL.md's `derived_from`. */
    val part: SegmentEnd,
)

/**
 * Which end of a Segment a TimelineEvent stands for.
 *
 * A Segment whose end is an Appointment draws a START and an END, because
 * collection and return are two things the traveller has to turn up and do. One
 * whose end is not draws a WHOLE, because boarding is the only appointment a
 * flight has.
 */
enum class SegmentEnd { WHOLE, START, END }
