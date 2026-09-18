package app.linger.core

/**
 * A person who appears on a Booking.
 *
 * Only the two names, which is all seam 3 reads. Passport, visa and date of
 * birth are deliberately absent until phase 3: INGESTION.md promises the
 * pipeline never extracts them, so they arrive typed on the device or not at
 * all.
 *
 * Legal and Common are both chosen by the traveller and neither is derived from
 * the other. Sixt needs no passport yet holds "Varun Sudhakar Ranipeta", so
 * which name a vendor received says nothing about which the traveller uses.
 */
data class Traveller(
    val legalName: String,
    val commonName: String,
)
