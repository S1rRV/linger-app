package app.linger.core

/**
 * A number quoted openly to identify a Booking.
 *
 * Labelled with who issued it, because a Booking holds several and the person
 * on the phone asks for theirs. The same four flights in docs/samples/ are
 * 73545609581279 to Expedia and AFGZ2M to Arajet, and both are worth keeping.
 *
 * Compared case-insensitively with the surrounding space removed, because a
 * code read off a PDF and a code parsed from HTML are the same code.
 */
data class Reference(val issuer: String, val code: String) {

    private val normalised: Pair<String, String>
        get() = issuer.trim().lowercase() to code.trim().uppercase()

    override fun equals(other: Any?): Boolean =
        other is Reference && normalised == other.normalised

    override fun hashCode(): Int = normalised.hashCode()
}
