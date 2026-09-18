package app.linger.core

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
)
