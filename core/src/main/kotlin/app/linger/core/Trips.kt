package app.linger.core

/**
 * Works out which Bookings are the same Trip.
 *
 * ADR-0005: a Booking joins an existing Trip unless the traveller was Home in
 * between. There is no distance rule and no time threshold, because nobody
 * decides whether two bookings are one trip by measuring the distance between
 * them. They ask whether they went home first.
 */
object Trips {

    fun group(bookings: List<Booking>, home: Home): List<Trip> {
        // Every Segment of every Booking in the order they happen, each still
        // knowing which Booking it came from. Sorting by Booking would not do:
        // a car collected on the 24th sits inside a flight bought in November.
        val steps = bookings
            .flatMap { booking -> booking.segments.map { segment -> booking to segment } }
            .sortedBy { (_, segment) -> segment.startsAt }

        val trips = mutableListOf<List<Booking>>()
        var current = mutableListOf<Booking>()

        steps.forEachIndexed { index, (booking, segment) ->
            if (booking !in current) current += booking

            val next = steps.getOrNull(index + 1)
            // Where the traveller is standing once this Segment is over.
            val cameHome = segment.to in home
            // Touching Home inside one purchase is a connection, not a
            // homecoming: two hours in Terminal C on a single ticket is not
            // going home, whatever the airport code says. Reading the Booking
            // settles it without the time threshold ADR-0005 refuses to have.
            val somethingElseFollows = next != null && next.first != booking
            if (cameHome && somethingElseFollows) {
                trips += current
                current = mutableListOf()
            }
        }

        if (current.isNotEmpty()) trips += current
        return trips.map { Trip(bookings = it, home = home) }
    }
}
