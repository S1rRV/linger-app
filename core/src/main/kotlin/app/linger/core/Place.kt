package app.linger.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Somewhere a Segment starts or ends.
 *
 * See CONTEXT.md. Only what the current tests need is modelled: coordinates,
 * local-script names and a kind arrive when something reads them.
 */
data class Place(
    val code: String,
    val name: String,
    val zone: TimeZone,
    /**
     * The town a person would say they were in.
     *
     * Separate from [name], which is the airport or the branch. A Trip is named
     * "Medellin and Cartagena" and never "Jose Maria Cordova International",
     * and it is what tells an airport and a car branch in the same city that
     * they are one place.
     *
     * Defaults to [name] so a Place nobody has given a town to still answers.
     */
    val city: String = name,
) {
    /**
     * Turns a bare wall-clock reading at this Place into an instant.
     *
     * Confirmations state times without zones, so this is the only honest way
     * to get from what a vendor wrote to a moment that can be compared or
     * reminded against.
     */
    fun instantAt(local: LocalDateTime): Instant = local.toInstant(zone)
}
