package app.linger.core

/**
 * How many decimal places a currency actually has.
 *
 * Not a detail. The yen has no sub-unit, so treating every currency as two
 * places makes a Japanese total a hundred times too large. Only the currencies
 * the first regions bring are listed, and anything unknown gets two, which is
 * what most of the world uses.
 */
internal object Currencies {

    private val places = mapOf(
        "JPY" to 0,
        "KRW" to 0,
        "VND" to 0,
        "CLP" to 0,
        "ISK" to 0,
    )

    fun decimalPlaces(currency: String): Int = places[currency.trim().uppercase()] ?: 2
}
