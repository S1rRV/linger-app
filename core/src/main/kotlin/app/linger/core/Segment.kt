package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

/**
 * One timed leg of a Booking, with a start, an end, and a Place at each.
 *
 * See CONTEXT.md, and ADR-0001 for why this exists rather than legs living
 * inside each booking kind's payload. Segments are the only place times are
 * stored: a payload carrying its own departure time is a bug.
 */
data class Segment(
    val from: Place,
    val to: Place,
    val startsAt: Instant,
    val endsAt: Instant,
) {
    val duration: Duration get() = endsAt - startsAt

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
        ): Segment = Segment(
            from = from,
            to = to,
            startsAt = from.instantAt(departingAt),
            endsAt = to.instantAt(arrivingAt),
        )
    }
}
