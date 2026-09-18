package app.linger.core

import kotlinx.datetime.Instant

/**
 * A dated stay away from home, holding everything booked for it.
 *
 * The container every other concept hangs off. Never built by hand: Trips are
 * worked out from the Bookings by [Trips.group], because a traveller forwards a
 * confirmation, not a trip.
 */
data class Trip(
    val bookings: List<Booking>,
    /**
     * A snapshot of Home as it was when this Trip was worked out.
     *
     * Not a live reference. Moving house would otherwise silently redraw the
     * boundaries of every Trip already taken.
     */
    val home: Home,
) {
    /**
     * First Segment start to last Segment end.
     *
     * Starts at leaving home rather than at arriving anywhere, because the
     * countdown a traveller wants is "leave home in six days" and that clock
     * runs from the front door. Ends at the last Segment to finish, which is
     * not always the last one to start: a car handed back at noon in Cartagena
     * and a flight landing at Newark that night are the same day in opposite
     * orders.
     */
    val range: ClosedRange<Instant>
        get() {
            val segments = bookings.flatMap { it.segments }
            return segments.minOf { it.startsAt }..segments.maxOf { it.endsAt }
        }
}
