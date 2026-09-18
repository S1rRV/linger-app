package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

/**
 * One timed leg of a Booking, with a start, an end, and a Place at each.
 *
 * See CONTEXT.md, and ADR-0001 for why this exists rather than legs living
 * inside each booking kind's payload. Segments are the only place times are
 * stored: a payload carrying its own departure time is a bug.
 *
 * A car rental is one Segment, not two. The traveller holds the car from
 * collection to return without a break, which is exactly one span with a Place
 * at each end. Two obligations is a different count from two Segments, and
 * [endMustBeAttended] is what carries the difference.
 */
data class Segment(
    val from: Place,
    val to: Place,
    val startsAt: Instant,
    val endsAt: Instant,
    /**
     * Whether the traveller has to turn up and do something at the end.
     *
     * A rental return does: the car has to be handed back at a counter at a
     * stated time. An arrival does not, because the plane lands whether or not
     * the traveller participates. This is one flag rather than a switch on
     * booking kind, so a ferry with a vehicle deck or a left-luggage locker
     * needs no new branch.
     */
    val endMustBeAttended: Boolean = false,
    /**
     * Who performs this leg on the day: the airline flying it, the company
     * handing over the keys. Part of ADR-0003's journey key.
     */
    val operator: String? = null,
    /**
     * The flight or train number, where one exists.
     *
     * Nullable because half of phase 0 has none: neither Sixt nor Alamo has an
     * equivalent, which is why ADR-0003 demoted this to a tiebreaker and keys
     * on the start place instead.
     */
    val serviceNumber: String? = null,
) {
    val duration: Duration get() = endsAt - startsAt

    /**
     * The calendar date this Segment starts on, where it starts.
     *
     * A date rather than an instant, per ADR-0003: the same service on the same
     * calendar day is one service even when a delay pushes the actual departure
     * past midnight. Read in the start Place's own zone, because a flight
     * leaving Newark at 23:59 left on the 23rd to everyone who was there.
     */
    val startLocalDate: LocalDate get() = startsAt.toLocalDateTime(from.zone).date

    /**
     * Whether the end has to be attended somewhere other than where it started.
     *
     * A one-way rental collected in Medellin and dropped in Cartagena is the
     * case: the traveller owes a counter visit at a Place they have not seen
     * yet. Returning to the branch you collected from is the ordinary case and
     * needs no warning.
     */
    val endIsElsewhere: Boolean get() = endMustBeAttended && from != to

    companion object {
        /**
         * Builds a Segment from the bare clock readings a confirmation actually
         * states, resolving each through its own Place's zone.
         *
         * The two ends can sit in different zones, so subtracting the stated
         * readings is not the duration.
         */
        fun between(
            from: Place,
            departingAt: LocalDateTime,
            to: Place,
            arrivingAt: LocalDateTime,
            operator: String? = null,
            serviceNumber: String? = null,
        ): Segment = Segment(
            from = from,
            to = to,
            startsAt = from.instantAt(departingAt),
            endsAt = to.instantAt(arrivingAt),
            operator = operator,
            serviceNumber = serviceNumber,
        )

        /**
         * Builds the Segment for something the traveller takes custody of and
         * gives back, where both ends are appointments they have to keep.
         *
         * [to] defaults to [from] because most rentals come back to the branch
         * they left, and a one-way is the thing worth having to say out loud.
         */
        fun held(
            from: Place,
            collectedAt: LocalDateTime,
            to: Place = from,
            returnedAt: LocalDateTime,
            operator: String? = null,
        ): Segment = Segment(
            from = from,
            to = to,
            startsAt = from.instantAt(collectedAt),
            endsAt = to.instantAt(returnedAt),
            endMustBeAttended = true,
            operator = operator,
        )
    }
}
