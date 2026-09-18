package app.linger.core

/**
 * Whether two records describe the same Booking.
 *
 * ADR-0003: the same Seller and Reference, or the same journey. Either is
 * enough and neither is enough alone. Reference catches an amendment that moved
 * the dates; journey catches the same flights arriving from two sellers.
 */
object Bookings {

    fun sameBooking(a: Booking, b: Booking): Boolean =
        shareAReference(a, b)

    /**
     * Whether the two records were issued the same number by the same issuer.
     *
     * Survives everything else changing. Sixt keeps reservation 9739400602
     * across a reschedule, which is what stops the amendment landing as a
     * second Booking.
     */
    private fun shareAReference(a: Booking, b: Booking): Boolean =
        a.references.any { it in b.references }
}
