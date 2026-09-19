package app.linger.tools

import app.linger.core.Airports
import app.linger.core.Booking
import app.linger.core.Countries
import app.linger.core.Home
import app.linger.core.Money
import app.linger.core.Place
import app.linger.core.Reference
import app.linger.core.Segment
import app.linger.core.TripJson
import app.linger.core.Trips
import kotlinx.datetime.LocalDateTime
import java.io.File

/**
 * Writes the sample trip out as JSON for the Expo app to render.
 *
 * This is the whole of the bridge between the two halves. The domain works out
 * what the trip is; the app draws what this produces. Neither knows anything
 * about the other, so the screens can be rewritten in a different framework, or
 * abandoned, without the domain being touched.
 *
 * The itinerary below is the one in docs/samples/, typed out rather than
 * parsed, because no parser exists yet. When one does, this file is what it
 * replaces.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "app/assets/trip.json")
    out.parentFile?.mkdirs()
    out.writeText(TripJson.of(sampleTrip(), homeCurrency = "USD"))
    println("Wrote ${out.path}, ${out.length()} bytes")
}

private fun airport(code: String) = requireNonNull(Airports.find(code), "airport $code")

private fun leg(
    from: String,
    departs: LocalDateTime,
    to: String,
    arrives: LocalDateTime,
    operator: String,
    flight: String,
) = Segment.between(
    from = airport(from),
    departingAt = departs,
    to = airport(to),
    arrivingAt = arrives,
    operator = operator,
    serviceNumber = flight,
)

private fun sampleTrip() = Trips.group(
    listOf(arajet(), jetsmart(), sixt(), alamo()),
    home = Home(airport("EWR"), airport("JFK"), airport("LGA")),
).single()

/** docs/samples/expedia-arajet-ewr-mde.eml, confirmation AFGZ2M. */
private fun arajet() = Booking(
    references = setOf(Reference("Expedia", "73545609581279"), Reference("Arajet", "AFGZ2M")),
    money = Money.of("2939.78", "USD", vendorLabel = "Total paid"),
    segments = listOf(
        leg("EWR", LocalDateTime(2026, 12, 23, 23, 59), "SDQ", LocalDateTime(2026, 12, 24, 5, 25), "Arajet", "DM 621"),
        leg("SDQ", LocalDateTime(2026, 12, 24, 7, 50), "MDE", LocalDateTime(2026, 12, 24, 9, 20), "Arajet", "DM 321"),
        leg("MDE", LocalDateTime(2027, 1, 3, 13, 19), "PUJ", LocalDateTime(2027, 1, 3, 17, 9), "Arajet", "DM 322"),
        leg("PUJ", LocalDateTime(2027, 1, 3, 20, 10), "EWR", LocalDateTime(2027, 1, 3, 23, 30), "Arajet", "DM 622"),
    ),
)

/** docs/samples/expedia-jetsmart-mde-ctg.eml, confirmation QCLZ7N. */
private fun jetsmart() = Booking(
    references = setOf(Reference("Expedia", "QCLZ7N")),
    money = Money.of("630.60", "USD", vendorLabel = "Total paid"),
    segments = listOf(
        leg("MDE", LocalDateTime(2026, 12, 28, 5, 0), "CTG", LocalDateTime(2026, 12, 28, 6, 14), "JetSMART", "JA 8021"),
        leg("CTG", LocalDateTime(2027, 1, 3, 7, 50), "MDE", LocalDateTime(2027, 1, 3, 9, 9), "JetSMART", "JA 8020"),
    ),
)

/** docs/samples/sixt-car-medellin.eml, reservation 9739400602. */
private fun sixt() = Booking(
    references = setOf(Reference("Sixt", "9739400602")),
    money = Money.of("267.45", "USD", vendorLabel = "Estimated rental cost"),
    segments = listOf(
        Segment.held(
            from = Place(
                code = "SIXT-MDE-POBLADO",
                name = "Sixt Medellin City, El Poblado",
                zone = requireNonNull(Countries.soleZoneOf("CO"), "Colombia"),
                city = "Medellin",
            ),
            collectedAt = LocalDateTime(2026, 12, 24, 12, 0),
            returnedAt = LocalDateTime(2026, 12, 28, 12, 0),
            operator = "Sixt",
        ),
    ),
)

/** docs/samples/booking-alamo-car-cartagena.eml, reference 721303130. */
private fun alamo() = Booking(
    references = setOf(Reference("Booking.com", "721303130")),
    money = Money.of("265.57", "USD", vendorLabel = "Total Cost"),
    segments = listOf(
        Segment.held(
            from = Place(
                code = "ALAMO-CTG",
                name = "Cartagena Rafael Nunez Intl Airport, Local 01-08",
                zone = requireNonNull(Countries.soleZoneOf("CO"), "Colombia"),
                city = "Cartagena",
            ),
            collectedAt = LocalDateTime(2026, 12, 29, 12, 0),
            returnedAt = LocalDateTime(2027, 1, 3, 12, 0),
            operator = "Alamo",
        ),
    ),
)

private fun <T : Any> requireNonNull(value: T?, what: String): T =
    value ?: error("The domain does not know about $what, so the fixture cannot be built")
