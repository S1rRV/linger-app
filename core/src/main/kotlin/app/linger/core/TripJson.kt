package app.linger.core

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A Trip as the JSON a phone screen reads.
 *
 * The domain stays the single source of truth for what a Trip is. The app
 * renders what this produces and works nothing out for itself, so a day view on
 * a phone cannot quietly disagree with the tests in this module. That is the
 * whole point of the split: the screens can be rewritten, or thrown away,
 * without any of this being touched.
 *
 * Written by hand rather than through a serialisation library, because the
 * shape is small, fixed, and read by exactly one consumer. What it costs is the
 * escaping, which has a test of its own.
 */
object TripJson {

    fun of(trip: Trip, homeCurrency: String): String {
        val days = trip.bookings
            .flatMap { booking -> booking.timeline().map { booking to it } }
            .sortedBy { (_, event) -> event.startsAt }
            .groupBy { (_, event) -> event.startsAt.toLocalDateTime(event.zone).date.toString() }

        return obj(
            "name" to str(trip.name),
            "startsAt" to str(trip.range.start.toString()),
            "endsAt" to str(trip.range.endInclusive.toString()),
            "destinations" to arr(trip.destinations.map { str(it.city) }),
            "waypoints" to arr(trip.waypoints.map { str(it.city) }),
            "totals" to totals(trip, homeCurrency),
            "days" to arr(
                days.map { (date, entries) ->
                    obj(
                        "date" to str(date),
                        "events" to arr(entries.map { (booking, event) -> event(booking, event) }),
                    )
                },
            ),
        )
    }

    private fun event(booking: Booking, event: TimelineEvent): String {
        val local = event.startsAt.toLocalDateTime(event.zone)
        return obj(
            // The reading is what the traveller sees and it never moves. The
            // instant is what sorts and counts down. A phone cannot derive one
            // from the other without a timezone database, so both are sent.
            "clock" to str("${two(local.hour)}:${two(local.minute)}"),
            "zone" to str(event.zone.id),
            "at" to str(event.startsAt.toString()),
            "title" to str(title(event)),
            "detail" to str(detail(event)),
            "place" to str(event.place.name),
            "kind" to str(if (event.part == SegmentEnd.WHOLE) "flight" else "appointment"),
            "references" to arr(
                booking.references
                    .sortedWith(compareBy({ it.issuer }, { it.code }))
                    .map { obj("issuer" to str(it.issuer), "code" to str(it.code)) },
            ),
        )
    }

    /** Where the traveller is standing when the event begins. */
    private val TimelineEvent.zone: TimeZone
        get() = if (part == SegmentEnd.END) segment.to.zone else segment.from.zone

    private fun title(event: TimelineEvent): String {
        val operator = event.segment.operator
        return when (event.part) {
            SegmentEnd.START -> "Collect the ${operator ?: "hire"} car"
            SegmentEnd.END -> "Return the ${operator ?: "hire"} car"
            SegmentEnd.WHOLE -> event.segment.serviceNumber
                ?: listOfNotNull(operator, "flight").joinToString(" ")
        }
    }

    /** The second line: where a flight is going, or where an appointment is. */
    private fun detail(event: TimelineEvent): String = when (event.part) {
        SegmentEnd.WHOLE -> "${event.segment.from.code} to ${event.segment.to.code}"
        else -> event.place.name
    }

    private fun totals(trip: Trip, homeCurrency: String): String {
        val totals = trip.totals(homeCurrency)
        val combined = totals.combined
        return obj(
            "perCurrency" to obj(
                *totals.perCurrencyStated.map { (currency, stated) -> currency to str(stated) }.toTypedArray(),
            ),
            "combined" to (
                combined?.let {
                    obj(
                        "currency" to str(it.currency),
                        "stated" to str(Money(it.minorUnits, it.currency, vendorLabel = "").stated),
                        // Shown to the traveller so a figure the app guessed at
                        // never passes itself off as one they were charged.
                        "isAGuess" to it.isAGuess.toString(),
                    )
                } ?: "null"
                ),
            "bookingsWithNoPrice" to totals.bookingsWithNoPrice.toString(),
        )
    }

    private fun obj(vararg fields: Pair<String, String>): String =
        fields.joinToString(",", prefix = "{", postfix = "}") { (key, value) -> "${str(key)}:$value" }

    private fun arr(values: List<String>): String = values.joinToString(",", prefix = "[", postfix = "]")

    /**
     * A JSON string, escaped.
     *
     * Sixt writes its branch as "Sixt Medellin City, El Poblado" and hotels put
     * quotes in their own names, so this is not hypothetical. Control
     * characters go out as \u escapes because a raw one is invalid in JSON and
     * some parsers accept it silently, which is worse.
     */
    private fun str(text: String): String = buildString {
        append('"')
        text.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> append(c)
            }
        }
        append('"')
    }

    private fun two(value: Int): String = value.toString().padStart(2, '0')
}
