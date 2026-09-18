package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * A Trip as an ICS calendar feed.
 *
 * Text in, text out. The webcal URL, the Android calendar writes and the one
 * off download are three ways of delivering this string, and none of them is
 * the domain's business. See CALENDAR-AND-PASSES.md.
 *
 * Everything here is regenerated from the Bookings on every change, per
 * DATA-MODEL.md invariant 2, which is why the feed can never drift.
 */
object IcsFeed {

    private const val CRLF = "\r\n"
    private const val FOLD_AT = 75
    private val MOMENT_BLOCK = 30.minutes

    fun forTrip(trip: Trip, generatedAt: Instant): String {
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Linger//Trip feed//EN",
            // A subscribed feed is read only. Saying so stops a client offering
            // edits it cannot send anywhere.
            "METHOD:PUBLISH",
        )
        lines += zoneDefinitions(trip)
        trip.bookings.forEach { booking ->
            booking.timeline().forEach { event -> lines += vevent(booking, event, generatedAt) }
        }
        lines += "END:VCALENDAR"
        return lines.joinToString(separator = CRLF, postfix = CRLF) { fold(it) }
    }

    /**
     * Breaks a line that runs past 75 octets, continuing it with one space.
     *
     * RFC 5545 is not advisory about the limit, and the samples are already
     * close: Booking.com writes the Alamo branch as "CARTAGENA RAFAEL NUNEZ
     * INTL AIRPORT, LOCAL 01-08, Cartagena, Colombia, 130002".
     *
     * Octets rather than characters, and never inside one. An accented branch
     * name costs two bytes a letter, so a character count folds too late and a
     * split through the middle of a letter produces a byte sequence that is not
     * text at all.
     */
    private fun fold(line: String): String {
        if (line.encodeToByteArray().size <= FOLD_AT) return line
        val folded = StringBuilder()
        var used = 0
        line.forEach { character ->
            val width = character.toString().encodeToByteArray().size
            // One octet of the budget goes on the leading space of a
            // continuation, so a continued line is 74 of content at most.
            if (used + width > FOLD_AT) {
                folded.append(CRLF).append(' ')
                used = 1
            }
            folded.append(character)
            used += width
        }
        return folded.toString()
    }

    /**
     * A definition for every zone the Trip's events name, and none it does not.
     *
     * Covering a day either side of the Trip, so an event at the very start has
     * its zone defined from before it rather than from the same instant.
     */
    private fun zoneDefinitions(trip: Trip): List<String> {
        val from = trip.range.start - 1.days
        val to = trip.range.endInclusive + 1.days
        return trip.bookings
            .flatMap { it.timeline() }
            // Both ends, because a leg lands in a different zone from the one
            // it left and the arrival names its own.
            .flatMap { listOf(it.startZone, it.endZone) }
            .distinctBy { it.id }
            .sortedBy { it.id }
            .flatMap { IcsTime.definition(it, from, to) }
    }

    private fun vevent(booking: Booking, event: TimelineEvent, generatedAt: Instant): List<String> = listOf(
        "BEGIN:VEVENT",
        "UID:${uid(booking, event)}",
        "DTSTAMP:${IcsTime.utcStamp(generatedAt)}",
        "DTSTART;TZID=${event.startZone.id}:${IcsTime.local(event.startsAt, event.startZone)}",
        "DTEND;TZID=${event.endZone.id}:${IcsTime.local(event.finishesAt, event.endZone)}",
        "SUMMARY:${escape(summary(event))}",
        "LOCATION:${escape(event.place.name)}",
        "END:VEVENT",
    )

    /**
     * When the event finishes, once a moment has been given something to draw.
     *
     * The timeline holds a collection and a return as moments, which is the
     * truth: the traveller is not at the branch for the four days between. A
     * moment renders as a hairline in a calendar grid, so each gets a readable
     * block here, at the edge, where it is a rendering decision rather than a
     * claim about how long a counter takes. Nothing upstream is told.
     */
    private val TimelineEvent.finishesAt: Instant
        get() = if (startsAt == endsAt) startsAt + MOMENT_BLOCK else endsAt

    /**
     * The one thing that answers "show it in my timezone".
     *
     * Calendar clients render every event in the viewer's current zone, and no
     * property in the format overrides that, whatever TZID suggests. Text is
     * the only part of an event that survives untouched, so the local reading
     * goes in the title and reads correctly from anywhere on earth.
     */
    private fun summary(event: TimelineEvent): String {
        val zone = event.startZone
        val clock = IcsTime.clock(event.startsAt, zone)
        val where = "$clock ${zoneLabel(zone)} time"
        val operator = event.segment.operator
        return when (event.part) {
            SegmentEnd.START -> "Collect the ${operator ?: ""} car".fix() + ", $where"
            SegmentEnd.END -> "Return the ${operator ?: ""} car".fix() + ", $where"
            SegmentEnd.WHOLE -> {
                val route = "${event.segment.from.code} to ${event.segment.to.code}"
                val service = event.segment.serviceNumber
                listOfNotNull(service, route).joinToString(" ") + ", departs $where"
            }
        }
    }

    /** Collapses the gap a missing operator leaves behind. */
    private fun String.fix(): String = replace("  ", " ")

    /**
     * The zone named the way a person would say it.
     *
     * Read off the zone rather than the Place, because the time really is New
     * York time even when the airport is in Newark, and a traveller who sees
     * "Newark time" beside a Bogota reading has to work out whether that is a
     * different thing.
     */
    private fun zoneLabel(zone: TimeZone): String =
        zone.id.substringAfterLast('/').replace('_', ' ')

    /**
     * A name for this event that survives being regenerated.
     *
     * A feed is fetched over and over, so an id that moved between fetches
     * would leave the traveller holding every version of every flight they ever
     * booked. Built from the Booking's Reference, which is the one thing
     * ADR-0003 says survives an amendment, plus which Segment and which end.
     */
    private fun uid(booking: Booking, event: TimelineEvent): String {
        val reference = booking.references
            .sortedWith(compareBy({ it.issuer }, { it.code }))
            .firstOrNull()
            ?.let { "${it.issuer}-${it.code}" }
            ?: "${event.segment.from.code}-${event.segment.startLocalDate}"
        val index = booking.segments.sortedBy { it.startsAt }.indexOfFirst { it === event.segment }
        return "${slug(reference)}-$index-${event.part.name.lowercase()}@lngr.app"
    }

    private fun slug(text: String): String =
        text.lowercase().map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-')

    /**
     * RFC 5545 escaping for a TEXT value.
     *
     * A comma is a value separator, so an unescaped "Cartagena, Colombia" in a
     * LOCATION turns one place into two and some parsers give up on the event.
     */
    private fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

    /** Where the traveller is standing when the event begins. */
    private val TimelineEvent.startZone: TimeZone
        get() = if (part == SegmentEnd.END) segment.to.zone else segment.from.zone

    /**
     * Where they are standing when it finishes.
     *
     * A flight ends where it lands, in that airport's zone, and the whole point
     * of storing a Place at each end of a Segment is that those two zones are
     * routinely different. Writing both ends in one zone would make every leg
     * the wrong length and contradict the arrival the confirmation printed.
     */
    private val TimelineEvent.endZone: TimeZone
        get() = if (part == SegmentEnd.START) segment.from.zone else segment.to.zone
}
