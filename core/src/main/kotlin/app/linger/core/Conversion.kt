package app.linger.core

import kotlinx.datetime.LocalDate

/**
 * What a Booking's cost comes to in the account's own currency.
 *
 * Two kinds, and the difference is who is speaking. An [Estimated] is the app's
 * own arithmetic and is allowed to be wrong. A [Stated] is the traveller
 * reading their card statement, and is not.
 *
 * See docs/research/historical-exchange-rates.md. There is no single correct
 * rate to find: 53 official sources spread 1.75% on one date. The card issuer
 * sets the rate the traveller actually pays, at settlement, days after the
 * purchase, and may add a markup of its own. So the best the app can do from a
 * confirmation is a good guess with its working shown.
 */
sealed interface Conversion {

    val minorUnits: Long
    val currency: String

    /**
     * The app's own guess, with its working shown.
     *
     * [observedOn] is the day the rate was measured, which is not always the
     * day of the purchase: a Saturday booking is converted at Friday's rate
     * because that is the last day anything was quoted. Showing the booking
     * date over a weekday rate is the quiet lie, so the two are kept apart.
     *
     * [source] is named because the traveller is entitled to know whose number
     * this is before deciding whether to correct it.
     */
    data class Estimated(
        override val minorUnits: Long,
        override val currency: String,
        val observedOn: LocalDate,
        val source: String,
    ) : Conversion

    /**
     * The traveller's own figure, off their statement.
     *
     * Carries no date and no source because it needs neither: it is not a
     * conversion of anything, it is what they were charged. Once set it
     * survives every re-estimate, which is the whole reason correcting it is
     * worth offering.
     */
    data class Stated(
        override val minorUnits: Long,
        override val currency: String,
    ) : Conversion
}
