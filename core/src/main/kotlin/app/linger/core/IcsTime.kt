package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Turning instants into the text a calendar file wants, and telling the file
 * what the zones it names actually do.
 *
 * A TZID with no VTIMEZONE beside it is the usual shortcut. It is malformed,
 * and clients that resolve it anyway are guessing from the name. Generating the
 * definition is the difference between a file that happens to work in Google
 * and one that is correct.
 */
internal object IcsTime {

    /** `20261223T235900`, the basic format every ICS date-time uses. */
    fun basic(local: LocalDateTime): String =
        "${pad(local.year, 4)}${pad(local.monthNumber)}${pad(local.dayOfMonth)}" +
            "T${pad(local.hour)}${pad(local.minute)}${pad(local.second)}"

    fun utcStamp(instant: Instant): String = "${basic(instant.toLocalDateTime(TimeZone.UTC))}Z"

    fun local(instant: Instant, zone: TimeZone): String = basic(instant.toLocalDateTime(zone))

    /** `23:59`, for a human reading the title rather than a parser reading the field. */
    fun clock(instant: Instant, zone: TimeZone): String {
        val local = instant.toLocalDateTime(zone)
        return "${pad(local.hour)}:${pad(local.minute)}"
    }

    /**
     * What a zone is doing over the window, as the sub-components RFC 5545 wants.
     *
     * Built by asking the zone rather than by reading a table, so a zone that
     * changes its rules is followed rather than remembered wrongly. A window
     * with no change produces one STANDARD component, which is the whole truth
     * about Bogota and Santo Domingo: inventing a DAYLIGHT component for them
     * would be a lie a client would act on.
     */
    fun definition(zone: TimeZone, from: Instant, to: Instant): List<String> {
        val baseOffset = offsetSecondsAt(zone, from)
        val lines = mutableListOf("BEGIN:VTIMEZONE", "TZID:${zone.id}")
        lines += component(
            daylight = false,
            startsAt = from,
            offsetBefore = baseOffset,
            offsetAfter = baseOffset,
        )
        transitions(zone, from, to).forEach { shift ->
            lines += component(
                daylight = shift.after > shift.before,
                startsAt = shift.at,
                offsetBefore = shift.before,
                offsetAfter = shift.after,
            )
        }
        lines += "END:VTIMEZONE"
        return lines
    }

    private fun component(
        daylight: Boolean,
        startsAt: Instant,
        offsetBefore: Int,
        offsetAfter: Int,
    ): List<String> {
        val name = if (daylight) "DAYLIGHT" else "STANDARD"
        // A VTIMEZONE's own DTSTART is a local time read in the offset that was
        // in force just before the change, which is the one hour of the year
        // where using the wrong side puts the transition an hour out.
        val wallClock = (startsAt + offsetBefore.seconds).toLocalDateTime(TimeZone.UTC)
        return listOf(
            "BEGIN:$name",
            "DTSTART:${basic(wallClock)}",
            "TZOFFSETFROM:${offset(offsetBefore)}",
            "TZOFFSETTO:${offset(offsetAfter)}",
            "END:$name",
        )
    }

    private data class Shift(val at: Instant, val before: Int, val after: Int)

    /**
     * Every offset change the zone makes inside the window.
     *
     * Coarse steps to find that one happened, then a bisection to say when. A
     * scan fine enough to land on the transition directly would be tens of
     * thousands of probes for a two-week trip.
     */
    private fun transitions(zone: TimeZone, from: Instant, to: Instant): List<Shift> {
        val shifts = mutableListOf<Shift>()
        var cursor = from
        var held = offsetSecondsAt(zone, from)
        while (cursor < to) {
            val next = minOf(cursor + 6.hours, to)
            val offset = offsetSecondsAt(zone, next)
            if (offset != held) {
                shifts += Shift(at = pinpoint(zone, cursor, next, held), before = held, after = offset)
                held = offset
            }
            cursor = next
        }
        return shifts
    }

    private fun pinpoint(zone: TimeZone, before: Instant, after: Instant, offsetBefore: Int): Instant {
        var low = before
        var high = after
        while (high - low > 1.seconds) {
            val middle = low + (high - low) / 2
            if (offsetSecondsAt(zone, middle) == offsetBefore) low = middle else high = middle
        }
        return high
    }

    /**
     * How far ahead of UTC the zone is at that moment.
     *
     * Read by asking what the wall clock says and comparing, because that is
     * the one question a TimeZone always answers the same way whatever the
     * platform underneath it.
     */
    private fun offsetSecondsAt(zone: TimeZone, instant: Instant): Int =
        (instant.toLocalDateTime(zone).toInstant(TimeZone.UTC) - instant).inWholeSeconds.toInt()

    private fun offset(seconds: Int): String {
        val sign = if (seconds < 0) "-" else "+"
        return "$sign${pad(abs(seconds) / 3600)}${pad(abs(seconds) % 3600 / 60)}"
    }

    private fun pad(value: Int, width: Int = 2): String = value.toString().padStart(width, '0')
}
