package app.linger.core

/**
 * What a Trip cost, in the two ways that can honestly be said.
 *
 * [perCurrency] is simply true: it adds up what vendors charged, in the
 * currencies they charged it in, and needs no exchange rate and no source.
 * [combined] is the single figure a traveller asks for, and is a guess until
 * they have replaced every conversion with what their statement said.
 */
data class TripTotals(
    val perCurrency: Map<String, Long>,
    val combined: Combined?,
    /**
     * How many Bookings state no price at all.
     *
     * Surfaced rather than swallowed. An operator's own confirmation for
     * flights a seller was paid for routinely states nothing, and treating that
     * as zero makes the Trip look cheaper than it was.
     */
    val bookingsWithNoPrice: Int,
) {
    /** The subtotals as a vendor would have written them. */
    val perCurrencyStated: Map<String, String>
        get() = perCurrency.mapValues { (currency, minorUnits) ->
            Money(minorUnits, currency, vendorLabel = "").stated
        }

    /**
     * The one figure, and whether it is still guesswork.
     *
     * [isAGuess] is false only once every conversion involved is the
     * traveller's own. A total that has stopped estimating should stop
     * apologising for itself.
     */
    data class Combined(
        val minorUnits: Long,
        val currency: String,
        val isAGuess: Boolean,
    )
}
