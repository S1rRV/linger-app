package app.linger.core

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

    fun forTrip(trip: Trip): String {
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Linger//Trip feed//EN",
            // A subscribed feed is read only. Saying so stops a client offering
            // edits it cannot send anywhere.
            "METHOD:PUBLISH",
        )
        trip.bookings.forEach { booking ->
            booking.timeline().forEach { event -> lines += vevent(booking, event) }
        }
        lines += "END:VCALENDAR"
        return lines.joinToString(separator = CRLF, postfix = CRLF)
    }

    private fun vevent(booking: Booking, event: TimelineEvent): List<String> = listOf(
        "BEGIN:VEVENT",
        "END:VEVENT",
    )
}
