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
     * DATA-MODEL.md invariant 1: one multi-leg flight is four events, one car
     * rental is two. Both fall out of the same question, which is not what kind
     * of Booking this is but whether the traveller has to turn up and do
     * something at the end of each Segment.
     *
     * A Segment whose end is unattended draws one event, spanning it: the
     * traveller boards at the start and the rest happens to them. A Segment
     * whose end is an appointment draws two, one at each end, because the four
     * days in between are theirs. Blocking out those days would put them at a
     * rental counter they left on Thursday and swallow every Gap the trip has.
     */
    fun timeline(): List<TimelineEvent> =
        segments
            .flatMap { segment ->
                if (segment.endMustBeAttended) {
                    listOf(
                        TimelineEvent(segment.startsAt, segment.startsAt, segment.from),
                        TimelineEvent(segment.endsAt, segment.endsAt, segment.to),
                    )
                } else {
                    listOf(TimelineEvent(segment.startsAt, segment.endsAt, segment.from))
                }
            }
            // Sorted after the split, not before: a rental collected on Thursday
            // is returned on Monday, and its two events belong at their own
            // places in the day view rather than side by side.
            .sortedBy { it.startsAt }
}
