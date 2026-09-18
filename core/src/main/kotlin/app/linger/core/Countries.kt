package app.linger.core

import kotlinx.datetime.TimeZone

/**
 * Countries to their timezones, used only to answer when there is exactly one.
 *
 * The third step of ROADMAP.md's resolution chain, reached when a Place has no
 * airport code and the vendor attached no calendar file. It is what makes the
 * Sixt booking resolvable at all: its pickup is the street address
 * "Carrera 43b No 1a sur-184, Medellin", so there is nothing to look up.
 *
 * A country with more than one zone gets no answer. Returning any one of them
 * would be right for part of the country and wrong for the rest, which is the
 * guess the roadmap forbids. The caller flags the Booking instead.
 */
object Countries {

    private val zones: Map<String, List<TimeZone>> = mapOf(
        "CO" to listOf(TimeZone.of("America/Bogota")),
        "DO" to listOf(TimeZone.of("America/Santo_Domingo")),
        "US" to listOf(
            TimeZone.of("America/New_York"),
            TimeZone.of("America/Chicago"),
            TimeZone.of("America/Denver"),
            TimeZone.of("America/Los_Angeles"),
            TimeZone.of("America/Anchorage"),
            TimeZone.of("Pacific/Honolulu"),
        ),
    )

    fun soleZoneOf(countryCode: String): TimeZone? =
        zones[countryCode.trim().uppercase()]?.singleOrNull()
}
