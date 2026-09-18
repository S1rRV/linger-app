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
        return a.journey() == b.journey()
    }

    private fun Booking.journey(): List<JourneyKey> =
        segments.sortedBy { it.startsAt }.map { it.journeyKey() }

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
