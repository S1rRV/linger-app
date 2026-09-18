package app.linger.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Seam 10: what a Booking cost, and what that is in the account's own currency.
 *
 * The conversion is an estimate the traveller can correct, never a fact the app
 * asserts. Three things in docs/research/historical-exchange-rates.md force
 * that. Fifty three official sources spread 1.75% on one date, so there is no
 * single correct rate to find. The card issuer, not the vendor, sets the rate
 * the traveller actually pays, and does it at settlement days later. And the
 * only place the true figure ever appears is a card statement.
 */
class MoneyTest {

    @Test
    fun `an amount is held exactly, so subtotals can be added without drifting`() {
        // docs/samples/sixt-car-medellin.eml: US$267.45. Held in cents, because
        // a total is the one thing a traveller will check against their own
        // arithmetic and a rounding slip is unforgivable there.
        val sixt = Money.of("267.45", "USD", vendorLabel = "Estimated rental cost")

        assertEquals(26745, sixt.minorUnits)
        assertEquals("267.45", sixt.stated)
    }

    @Test
    fun `a currency with no minor unit is not given one`() {
        // The yen has no sub-unit at all, so 12000 yen is 12000 and not
        // 1200000. Treating every currency as two decimal places is how a
        // Japanese total comes out a hundred times too big.
        val tokyo = Money.of("12000", "JPY", vendorLabel = "Total")

        assertEquals(12000, tokyo.minorUnits)
        assertEquals("12000", tokyo.stated)
    }

    @Test
    fun `a new booking has no conversion yet`() {
        assertNull(Money.of("267.45", "USD", vendorLabel = "Total").converted)
    }

    @Test
    fun `an estimate says which day it was measured and who measured it`() {
        // The day the rate was observed, not the day of the booking. Those
        // differ whenever a purchase falls on a weekend, and showing the
        // booking date over a weekday rate is the quiet lie.
        val cop = Money.of("1050000", "COP", vendorLabel = "Total")
            .estimatedAs(
                Conversion.Estimated(
                    minorUnits = 25300,
                    currency = "USD",
                    observedOn = LocalDate(2026, 12, 24),
                    source = "Banco de la Republica",
                ),
            )

        val estimate = cop.converted
        assertTrue(estimate is Conversion.Estimated)
        assertEquals(LocalDate(2026, 12, 24), estimate.observedOn)
        assertEquals("Banco de la Republica", estimate.source)
    }

    @Test
    fun `the traveller's own figure replaces an estimate`() {
        val estimated = Money.of("1050000", "COP", vendorLabel = "Total")
            .estimatedAs(
                Conversion.Estimated(25300, "USD", LocalDate(2026, 12, 24), "Banco de la Republica"),
            )

        val corrected = estimated.correctedTo(minorUnits = 25612, currency = "USD")

        assertEquals(Conversion.Stated(25612, "USD"), corrected.converted)
    }

    @Test
    fun `a figure the traveller gave is never overwritten by a later estimate`() {
        // The whole point of letting them correct it. A rate refresh, a
        // reinstall or a re-parse of the same email must not quietly undo the
        // number they read off their statement.
        val corrected = Money.of("1050000", "COP", vendorLabel = "Total")
            .correctedTo(minorUnits = 25612, currency = "USD")

        val afterAnotherEstimate = corrected.estimatedAs(
            Conversion.Estimated(24900, "USD", LocalDate(2027, 1, 5), "ECB"),
        )

        assertEquals(Conversion.Stated(25612, "USD"), afterAnotherEstimate.converted)
    }

    @Test
    fun `a stale estimate is replaced by a better one`() {
        val first = Money.of("1050000", "COP", vendorLabel = "Total")
            .estimatedAs(Conversion.Estimated(25300, "USD", LocalDate(2026, 12, 24), "blend"))

        val second = first.estimatedAs(
            Conversion.Estimated(25419, "USD", LocalDate(2026, 12, 24), "Banco de la Republica"),
        )

        assertEquals(25419, second.converted?.minorUnits)
    }

    @Test
    fun `the vendor hedging its price is a different thing from the conversion`() {
        // Two senses of "estimate" that must not be confused. Sixt hedged its
        // own figure, which is about the rental. The conversion is the app's
        // own guesswork, which is about the exchange rate. A booking can have
        // either, both or neither.
        val sixt = Money.of("267.45", "USD", vendorLabel = "Estimated rental cost")

        assertTrue(sixt.vendorHedgedIt)
        assertNull(sixt.converted)
    }
}
