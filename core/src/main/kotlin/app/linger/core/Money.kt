package app.linger.core

/**
 * What a confirmation states the Booking cost.
 *
 * Held in the currency's smallest unit rather than as text, because a Trip's
 * subtotal is the one number a traveller checks against their own arithmetic
 * and a rounding slip there is unforgivable. [stated] gives back exactly what
 * the vendor printed.
 *
 * [vendorLabel] is not decoration. It is the only evidence of whether the
 * vendor committed to its own figure.
 */
data class Money(
    val minorUnits: Long,
    val currency: String,
    val vendorLabel: String,
    /** What this comes to in the account's currency, once anything knows. */
    val converted: Conversion? = null,
) {
    /** The amount as the vendor printed it. */
    val stated: String
        get() {
            val places = Currencies.decimalPlaces(currency)
            if (places == 0) return minorUnits.toString()
            val divisor = generateSequence(1L) { it * 10 }.elementAt(places)
            return "${minorUnits / divisor}.${(minorUnits % divisor).toString().padStart(places, '0')}"
        }

    /**
     * Whether the vendor hedged its own price.
     *
     * A different question from whether the conversion is a guess, and the two
     * get confused if they share a word. Sixt writes "Estimated rental cost"
     * and Alamo writes "Total Cost": that is about the rental. The conversion
     * is about the exchange rate, and is the app's guesswork rather than the
     * vendor's.
     *
     * Read off the label because that is where vendors put it. A word match is
     * crude and it is the only signal a confirmation carries.
     */
    val vendorHedgedIt: Boolean
        get() = vendorLabel.contains("estimat", ignoreCase = true)

    /**
     * Records the app's own conversion, unless the traveller has already given
     * theirs.
     *
     * A rate refresh, a reinstall or a re-parse of the same email must not
     * quietly undo the number they read off their statement. That guarantee is
     * the whole reason offering the correction is worth anything.
     */
    fun estimatedAs(estimate: Conversion.Estimated): Money =
        if (converted is Conversion.Stated) this else copy(converted = estimate)

    /** Records what the traveller was actually charged. */
    fun correctedTo(minorUnits: Long, currency: String): Money =
        copy(converted = Conversion.Stated(minorUnits, currency))

    companion object {
        /**
         * Reads an amount the way a confirmation printed it.
         *
         * Takes the digits and the currency's own number of decimal places, so
         * "12000" in yen is twelve thousand yen rather than a hundred and
         * twenty.
         */
        fun of(stated: String, currency: String, vendorLabel: String): Money {
            val places = Currencies.decimalPlaces(currency)
            val digits = stated.filter { it.isDigit() }
            val written = stated.substringAfter('.', missingDelimiterValue = "").count { it.isDigit() }
            val scaled = digits.toLong() * generateSequence(1L) { it * 10 }.elementAt(places - written)
            return Money(minorUnits = scaled, currency = currency.trim().uppercase(), vendorLabel = vendorLabel)
        }
    }
}
