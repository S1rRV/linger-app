package app.linger.core

/**
 * What a confirmation states the Booking cost.
 *
 * The amount is kept as written rather than parsed into a number. Nothing in
 * phase 0 does arithmetic on it, and a decimal type on the JVM would be
 * java.math, which ADR-0002 keeps out of the domain. Combined totals are a
 * later problem and will bring their own type.
 *
 * [vendorLabel] is not decoration. It is the only evidence of whether the
 * vendor committed to the figure, which decides whether the traveller is asked
 * afterwards whether it moved.
 */
data class Money(
    val amount: String,
    val currency: String,
    val vendorLabel: String,
) {
    /**
     * Whether the vendor hedged the figure.
     *
     * Read off the label because that is where vendors put it: Sixt writes
     * "Estimated rental cost" and Alamo writes "Total Cost". A word match is
     * crude, and it is honest about being the only signal there is. The
     * alternative, treating every rental as provisional, would ask the
     * traveller about prices no vendor ever intended to change.
     */
    val isAnEstimate: Boolean
        get() = vendorLabel.contains("estimate", ignoreCase = true) ||
            vendorLabel.contains("estimated", ignoreCase = true)
}
