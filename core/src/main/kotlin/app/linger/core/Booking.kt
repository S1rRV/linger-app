package app.linger.core

/**
 * One commercial agreement with one vendor.
 *
 * A return flight bought together is one Booking, not two: the Arajet purchase
 * in docs/samples/ covers all four legs under confirmation AFGZ2M.
 */
data class Booking(
    val segments: List<Segment>,
) {
    /**
     * Projects this Booking onto the timeline, earliest first.
     *
     * One event per Segment, per ADR-0001 and DATA-MODEL.md invariant 1. The
     * event sits at the Segment's start, which is where the traveller has to
     * be; its window runs to the arrival.
     */
    fun timeline(): List<TimelineEvent> =
        segments
            .sortedBy { it.startsAt }
            .map { TimelineEvent(startsAt = it.startsAt, endsAt = it.endsAt, place = it.from) }
}
