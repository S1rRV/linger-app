package app.linger.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Seam 11: what a Trip cost.
 *
 * Per-currency subtotals are exact and always shown, because they are simply
 * true and need no source. The single combined figure sits above them as a
 * guess, with the working shown, and is absent rather than wrong when
 * something has not been converted.
 */
class TripTotalTest {

    private val newYork = Home(assertNotNull(Airports.find("EWR")))
    private val bogota = assertNotNull(Countries.soleZoneOf("CO"))

    private fun flight(money: Money?) = Booking(
        references = setOf(Reference("Expedia", "73545609581279")),
        money = money,
        segments = listOf(
            Segment.between(
                from = assertNotNull(Airports.find("EWR")),
                departingAt = LocalDateTime(2026, 12, 23, 23, 59),
                to = assertNotNull(Airports.find("MDE")),
                arrivingAt = LocalDateTime(2026, 12, 24, 9, 20),
                operator = "Arajet",
            ),
        ),
    )

    private fun car(money: Money?, issuer: String) = Booking(
        references = setOf(Reference(issuer, "9739400602")),
        money = money,
        segments = listOf(
            Segment.held(
                from = Place("$issuer branch", "$issuer branch", bogota, city = "Medellin"),
                collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
                returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
                operator = issuer,
            ),
        ),
    )

    private fun tripOf(vararg bookings: Booking) =
        Trips.group(bookings.toList(), home = newYork).single()

    @Test
    fun `the real sample trip adds up exactly, because it is all one currency`() {
        // docs/samples/: Arajet US$2,939.78, JetSMART US$630.60, Sixt
        // US$267.45, Alamo US$265.57. No conversion is involved anywhere, so
        // nothing here is a guess.
        val trip = tripOf(
            flight(Money.of("2939.78", "USD", vendorLabel = "Total paid")),
            car(Money.of("267.45", "USD", vendorLabel = "Estimated rental cost"), "Sixt"),
            car(Money.of("265.57", "USD", vendorLabel = "Total Cost"), "Alamo"),
        )

        val totals = trip.totals(homeCurrency = "USD")

        assertEquals(mapOf("USD" to 347280L), totals.perCurrency)
        assertEquals("3472.80", totals.perCurrencyStated["USD"])
    }

    @Test
    fun `two currencies stay two lines, and are never quietly added`() {
        val trip = tripOf(
            flight(Money.of("2939.78", "USD", vendorLabel = "Total paid")),
            car(Money.of("1050000", "COP", vendorLabel = "Total"), "Sixt"),
        )

        val totals = trip.totals(homeCurrency = "USD")

        assertEquals(mapOf("USD" to 293978L, "COP" to 105000000L), totals.perCurrency)
    }

    @Test
    fun `the combined figure appears once everything foreign has been converted`() {
        val trip = tripOf(
            flight(Money.of("2939.78", "USD", vendorLabel = "Total paid")),
            car(
                Money.of("1050000", "COP", vendorLabel = "Total").estimatedAs(
                    Conversion.Estimated(25300, "USD", LocalDate(2026, 12, 24), "Banco de la Republica"),
                ),
                "Sixt",
            ),
        )

        val combined = assertNotNull(trip.totals(homeCurrency = "USD").combined)

        // 2,939.78 in the home currency plus 253.00 converted.
        assertEquals(319278, combined.minorUnits)
        assertEquals("USD", combined.currency)
        assertTrue(combined.isAGuess)
    }

    @Test
    fun `one unconverted booking withholds the combined figure entirely`() {
        // Rather than a total that silently leaves something out. A number
        // missing a booking is worse than no number, because it looks complete.
        val trip = tripOf(
            flight(Money.of("2939.78", "USD", vendorLabel = "Total paid")),
            car(Money.of("1050000", "COP", vendorLabel = "Total"), "Sixt"),
        )

        val totals = trip.totals(homeCurrency = "USD")

        assertNull(totals.combined)
        // The subtotals still stand. They were never a guess.
        assertEquals(2, totals.perCurrency.size)
    }

    @Test
    fun `a total built only from stated figures is not called a guess`() {
        // Once the traveller has put in their statement figures, the total
        // stops being an estimate and should stop apologising for itself.
        val trip = tripOf(
            flight(Money.of("2939.78", "USD", vendorLabel = "Total paid")),
            car(
                Money.of("1050000", "COP", vendorLabel = "Total")
                    .correctedTo(minorUnits = 25612, currency = "USD"),
                "Sixt",
            ),
        )

        val combined = assertNotNull(trip.totals(homeCurrency = "USD").combined)

        assertEquals(319590, combined.minorUnits)
        assertEquals(false, combined.isAGuess)
    }

    @Test
    fun `a booking with no price at all is counted as unknown, not as zero`() {
        // An operator's own confirmation for flights a seller was paid for
        // routinely states no price. Treating that as zero makes the trip look
        // cheaper than it was.
        val trip = tripOf(
            flight(Money.of("2939.78", "USD", vendorLabel = "Total paid")),
            car(null, "Sixt"),
        )

        val totals = trip.totals(homeCurrency = "USD")

        assertNull(totals.combined)
        assertEquals(1, totals.bookingsWithNoPrice)
    }
}
