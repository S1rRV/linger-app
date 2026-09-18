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
            // Where the traveller is standing once this Segment is over. If
            // that is Home and something else follows, the Trip ended here.
            val cameHome = segment.to in home
            if (cameHome && next != null) {
                trips += current
                current = mutableListOf()
            }
        }

        if (current.isNotEmpty()) trips += current
        return trips.map { Trip(bookings = it, home = home) }
    }
}
