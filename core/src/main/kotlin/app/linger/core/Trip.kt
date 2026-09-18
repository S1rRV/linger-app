package app.linger.core

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

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

    /**
     * The Places this Trip is actually about, in the order they are reached.
     *
     * Two ways in. Landing somewhere and staying [LONG_ENOUGH_TO_LEAVE] or more
     * before the next flight out is one, because eight hours is time enough to
     * leave the terminal and see something. Collecting or returning a car is
     * the other, whatever the clock says: a traveller who lands, takes the keys
     * and drives off within the hour has still arrived somewhere.
     */
    val destinations: List<Place> get() = visited().filter { it.isDestination }.map { it.place }

    /**
     * The Places passed through without the Trip being about them.
     *
     * Kept rather than discarded: a layover is where a delay bites, and the day
     * view has to show the traveller sitting in Santo Domingo at 6am whether or
     * not it names the Trip after it.
     */
    val waypoints: List<Place> get() = visited().filterNot { it.isDestination }.map { it.place }

    /**
     * What to call this Trip, until the traveller renames it.
     *
     * The Destinations in the order they occur, so the sample trip is "Medellin
     * and Cartagena". Waypoints and Home are left out: nobody describes their
     * holiday by the airport they changed planes in.
     *
     * Never built from a month or a year. That trip runs 23 December to 3
     * January, and any name of that shape is wrong at one end.
     */
    val name: String
        get() {
            val towns = destinations.map { it.city }.distinct()
            return when (towns.size) {
                0 -> "Trip"
                1 -> towns.single()
                // "A, B and C" rather than a bare join, because this is read as
                // a sentence in a list of trips.
                else -> towns.dropLast(1).joinToString(", ") + " and " + towns.last()
            }
        }

    /**
     * What this Trip cost, in [homeCurrency].
     *
     * The subtotals always stand, because they are what vendors charged and
     * involve no arithmetic anyone can argue with. The combined figure is
     * withheld entirely when anything is unpriced or unconverted, rather than
     * quietly leaving that booking out: a total missing a booking is worse than
     * no total, because it looks complete.
     */
    fun totals(homeCurrency: String): TripTotals {
        val home = homeCurrency.trim().uppercase()
        val priced = bookings.mapNotNull { it.money }

        val perCurrency = priced
            .groupBy { it.currency }
            .mapValues { (_, amounts) -> amounts.sumOf { it.minorUnits } }

        // What each Booking contributes to one figure: itself when it is
        // already in the home currency, otherwise whatever its conversion says.
        val contributions = priced.map { money ->
            when {
                money.currency == home -> Conversion.Stated(money.minorUnits, home)
                else -> money.converted
            }
        }
        val everythingCounted = bookings.size == priced.size && contributions.none { it == null }

        return TripTotals(
            perCurrency = perCurrency,
            combined = if (!everythingCounted) null else TripTotals.Combined(
                minorUnits = contributions.filterNotNull().sumOf { it.minorUnits },
                currency = home,
                isAGuess = contributions.any { it is Conversion.Estimated },
            ),
            bookingsWithNoPrice = bookings.size - priced.size,
        )
    }

    private data class Stop(val place: Place, val at: Instant, val isDestination: Boolean)

    /**
     * Every Place the traveller stands in, told apart into the two kinds.
     *
     * Ordered by when each was first reached, and deduplicated by city so that
     * an airport and a car branch in the same town are one entry. Home is
     * neither kind: it is where the Trip is measured from.
     */
    private fun visited(): List<Stop> {
        val flights = bookings.flatMap { it.segments }.filterNot { it.endMustBeAttended }.sortedBy { it.startsAt }
        val anchors = bookings.flatMap { it.segments }.filter { it.endMustBeAttended }

        val arrivals = flights.map { flight ->
            // How long before the next flight out of the same town. A landing
            // with nothing booked after it is somewhere the traveller went, not
            // a wait of zero: having no onward flight is the strongest evidence
            // there is that they stopped here.
            val onward = flights
                .firstOrNull { it.startsAt >= flight.endsAt && it.from.city == flight.to.city }
                ?.startsAt
            Stop(
                place = flight.to,
                at = flight.endsAt,
                isDestination = onward == null || onward - flight.endsAt >= LONG_ENOUGH_TO_LEAVE,
            )
        }
        val kept = anchors.flatMap { anchor ->
            listOf(Stop(anchor.from, anchor.startsAt, true), Stop(anchor.to, anchor.endsAt, true))
        }

        return (arrivals + kept)
            .filterNot { it.place in home }
            .sortedBy { it.at }
            // A city reached twice is one entry, and a city ever stayed in is a
            // Destination even if another visit was only a connection.
            .groupBy { it.place.city }
            .map { (_, stops) -> stops.first().copy(isDestination = stops.any { it.isDestination }) }
            .sortedBy { it.at }
    }

    private companion object {
        val LONG_ENOUGH_TO_LEAVE = 8.hours
    }
}
