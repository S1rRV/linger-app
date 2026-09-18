package app.linger.core

import kotlinx.datetime.TimeZone

/**
 * Airport codes to Places.
 *
 * ROADMAP.md records the bundled airport list as a hard dependency rather than
 * a nicety: the code is the only route to a time's zone.
 *
 * Lookup is nullable rather than throwing. The table is small and will always
 * be incomplete, so an unknown code is an ordinary outcome, not an exceptional
 * one. ROADMAP.md says such a Booking is flagged rather than guessed, and a
 * thrown error is neither. The caller turns the gap into a prompt; nothing here
 * invents a zone.
 */
object Airports {

    private val byCode: Map<String, Place> = listOf(
        Place("EWR", "Newark Liberty International", TimeZone.of("America/New_York")),
        Place("SDQ", "Las Americas International", TimeZone.of("America/Santo_Domingo")),
        Place("MDE", "Jose Maria Cordova International", TimeZone.of("America/Bogota")),
        Place("PUJ", "Punta Cana International", TimeZone.of("America/Santo_Domingo")),
    ).associateBy { it.code }

    fun find(code: String): Place? = byCode[code.trim().uppercase()]
}
