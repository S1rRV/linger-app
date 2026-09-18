package app.linger.core

/**
 * What a name a vendor wrote turned out to be.
 *
 * Three outcomes and no fourth. DATA-MODEL.md is firm that two people are never
 * merged silently, because a wrong merge is far harder to notice than a wrong
 * split: the traveller sees one profile and no sign that anything was joined.
 */
sealed interface Resolution {

    /** Exactly one profile owns this name. */
    data class Known(val traveller: Traveller) : Resolution

    /** Nobody owns it. The traveller is asked to set a profile up. */
    data class Unknown(val written: String) : Resolution

    /** More than one profile owns it, so the traveller picks. */
    data class Ambiguous(val candidates: Set<Traveller>) : Resolution
}

/**
 * Matching the name a vendor printed to a person the account already knows.
 *
 * Once a profile exists, any rendering of that profile's name is taken at face
 * value: "Varun Sudhakar Ranipeta", "Varun Sudhakar", "Varun Ranipeta" and
 * "V Ranipeta" are one person. Three vendors in docs/samples/ write that person
 * three different ways, and ADR-0003's journey match compares Travellers, so
 * without this the cross-seller match only worked when two vendors happened to
 * agree on a spelling.
 *
 * Nothing here creates a person. A name with no profile behind it is a question
 * for the traveller, which is the same stance as INGESTION.md's guardrail
 * against the pipeline ever inventing identity.
 */
object Travellers {

    fun resolve(written: String, profiles: Set<Traveller>): Resolution {
        val parts = parts(written)
        // One initial and nothing else identifies nobody. A vendor that printed
        // only "V" has told us less than it looks.
        if (parts.size == 1 && parts.single().length == 1) return Resolution.Unknown(written)

        val matches = profiles.filter { it.answersTo(parts) }.toSet()
        return when (matches.size) {
            0 -> Resolution.Unknown(written)
            1 -> Resolution.Known(matches.single())
            else -> Resolution.Ambiguous(matches)
        }
    }

    private fun Traveller.answersTo(written: List<String>): Boolean =
        covers(parts(legalName), written) || covers(parts(commonName), written)

    /**
     * Whether the written parts appear in the profile's own name, in order.
     *
     * In order, and that is the load-bearing half. "Varun Ranipeta" drops a
     * middle name and "V Ranipeta" initialises a first, and both are safe
     * because the surname still follows the given name. Allow the order to
     * float and an initial plus a common surname starts matching most of a
     * household, which is precisely where a wrong merge does its damage.
     *
     * The cost is that "Ranipeta Varun", which many systems produce, is not
     * recognised. Asking is the honest answer there rather than a rule loose
     * enough to be wrong quietly.
     */
    private fun covers(profile: List<String>, written: List<String>): Boolean {
        if (written.isEmpty() || written.size > profile.size) return false
        var cursor = 0
        written.forEach { part ->
            val found = (cursor until profile.size).firstOrNull { profile[it].matches(part) }
                ?: return false
            cursor = found + 1
        }
        return true
    }

    /** A written part matches a profile part outright, or as its initial. */
    private fun String.matches(part: String): Boolean =
        equals(part, ignoreCase = true) || (part.length == 1 && startsWith(part, ignoreCase = true))

    private fun parts(name: String): List<String> =
        name.split(' ', '\t').map { it.trim() }.filter { it.isNotEmpty() }
}
