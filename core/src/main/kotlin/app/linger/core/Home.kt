package app.linger.core

/**
 * Where the account holder lives, as every Place that counts as being back.
 *
 * A set rather than a single Place. New York is one place to the person who
 * lives there and three airports to an airline, so a Home of only Newark would
 * decide that a flight into JFK never brought them home, and ADR-0005 bounds a
 * Trip on exactly that judgement. Asked once during setup and never inferred
 * from a Booking, because a trip can legitimately start somewhere else.
 */
data class Home(val places: Set<Place>) {

    constructor(vararg places: Place) : this(places.toSet())

    operator fun contains(place: Place): Boolean = place in places
}
