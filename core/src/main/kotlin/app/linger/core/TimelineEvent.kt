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
)
