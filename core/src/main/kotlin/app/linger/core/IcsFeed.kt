package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days

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
        return lines.joinToString(separator = CRLF, postfix = CRLF)
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
        "DTSTAMP:${IcsTime.utcStamp(generatedAt)}",
        "DTSTART;TZID=${event.startZone.id}:${IcsTime.local(event.startsAt, event.startZone)}",
        "DTEND;TZID=${event.endZone.id}:${IcsTime.local(event.endsAt, event.endZone)}",
        "END:VEVENT",
    )

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
