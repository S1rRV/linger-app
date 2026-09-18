package app.linger.core

import kotlinx.datetime.LocalDate

/**
 * Whether two records describe the same Booking.
 *
 * ADR-0003: the same Seller and Reference, or the same journey. Either is
 * enough and neither is enough alone. Reference catches an amendment that moved
 * the dates; journey catches the same flights arriving from two sellers.
 */
object Bookings {

    fun sameBooking(a: Booking, b: Booking): Boolean =
        shareAReference(a, b) || shareAJourney(a, b)

    /**
     * Folds a later record into the one already held.
     *
     * ADR-0003's first-record-wins rule. [later] fills what [first] is missing
     * and overwrites nothing, because an amendment is usually partial: Sixt's
     * "your booking has moved" states the new date and may say nothing about
     * the vehicle, the price or the insurance. Without this a thinner record
     * would hollow out a complete one.
     *
     * References are the exception, and they are not really one: gaining a
     * second number is filling a gap, not overwriting the first. The traveller
     * wants both, because the desk asks for one and the agent for the other.
     *
     * Pure. Nothing here writes a version, because there is no store yet to
     * hold the chain that DATA-MODEL.md invariant 4 describes.
     */
    fun merge(first: Booking, later: Booking): Booking = first.copy(
        references = first.references + later.references,
        travellers = first.travellers.ifEmpty { later.travellers },
        segments = first.segments.ifEmpty { later.segments },
    )

    /**
     * Whether the two records were issued the same number by the same issuer.
     *
     * Survives everything else changing. Sixt keeps reservation 9739400602
     * across a reschedule, which is what stops the amendment landing as a
     * second Booking.
     */
    private fun shareAReference(a: Booking, b: Booking): Boolean =
        a.references.any { it in b.references }

    /**
     * Whether the two records describe the same people making the same trip.
     *
     * The traveller is in the key rather than outside it because content
     * matching can produce a false positive that reference matching cannot: two
     * people on genuinely separate bookings for the same flight are told apart
     * by nothing else.
     */
    private fun shareAJourney(a: Booking, b: Booking): Boolean {
        if (a.segments.isEmpty() || b.segments.isEmpty()) return false
        if (a.travellers != b.travellers || a.travellers.isEmpty()) return false
        if (a.segments.size != b.segments.size) return false
        return a.inOrder().zip(b.inOrder()).all { (mine, theirs) -> mine.isSameJourneyAs(theirs) }
    }

    private fun Booking.inOrder(): List<Segment> = segments.sortedBy { it.startsAt }

    private fun Segment.isSameJourneyAs(other: Segment): Boolean =
        journeyKey() == other.journeyKey() && serviceNumberAgreesWith(other)

    /**
     * Whether the two Segments' service numbers rule out a match.
     *
     * Only speaks when both records state one. A seller that omits the flight
     * number should not block a match it would otherwise make, and neither Sixt
     * nor Alamo states anything of the kind, which is why ADR-0003 has this as
     * a tiebreaker rather than part of the key.
     */
    private fun Segment.serviceNumberAgreesWith(other: Segment): Boolean {
        val mine = serviceNumber?.trim()?.uppercase() ?: return true
        val theirs = other.serviceNumber?.trim()?.uppercase() ?: return true
        return mine == theirs
    }

    private fun Segment.journeyKey() = JourneyKey(
        operator = operator,
        startPlace = from,
        startLocalDate = startLocalDate,
    )

    /**
     * One Segment as ADR-0003 identifies it, and nothing else.
     *
     * Start place rather than service number, because only some Segments have a
     * service number and every one has a place it starts from. Neither Sixt nor
     * Alamo has a flight number.
     *
     * What is absent matters as much as what is here. No arrival time and no
     * end place: two sellers describing one flight disagree about the arrival
     * often enough, and a Segment is the same journey whatever time it is
     * currently believed to land.
     */
    private data class JourneyKey(
        val operator: String?,
        val startPlace: Place,
        val startLocalDate: LocalDate,
    )
}
