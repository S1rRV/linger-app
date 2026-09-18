package app.linger.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Seam 9: turning a name a vendor wrote into a person.
 *
 * Where a profile exists, any rendering of that profile's name is taken at face
 * value. Where none does, the traveller is asked to set one up rather than
 * having a person invented for them.
 *
 * This is what makes ADR-0003's journey match work across sellers at all: it
 * compares Travellers, and three vendors in docs/samples/ write one person
 * three different ways.
 */
class TravellerResolutionTest {

    private val varun = Traveller(legalName = "Varun Sudhakar Ranipeta", commonName = "Varun")
    private val profiles = setOf(varun)

    @Test
    fun `every rendering the sample vendors use points at the one profile`() {
        // The three that actually occur: Sixt writes the full legal name,
        // Expedia drops the surname, Booking.com keeps only the first name.
        listOf("Varun Sudhakar Ranipeta", "Varun Sudhakar", "Varun").forEach { written ->
            assertEquals(Resolution.Known(varun), Travellers.resolve(written, profiles), written)
        }
    }

    @Test
    fun `dropping a middle name or initialising a first name still points there`() {
        listOf("Varun Ranipeta", "V Ranipeta", "V S Ranipeta", "Varun S Ranipeta").forEach { written ->
            assertEquals(Resolution.Known(varun), Travellers.resolve(written, profiles), written)
        }
    }

    @Test
    fun `case and spacing a vendor invents are not a different person`() {
        listOf("VARUN SUDHAKAR RANIPETA", "  varun   ranipeta  ", "Varun  Sudhakar").forEach { written ->
            assertEquals(Resolution.Known(varun), Travellers.resolve(written, profiles), written)
        }
    }

    @Test
    fun `the order the name is written in has to hold`() {
        // "Ranipeta Varun" is how plenty of systems render a name, and it may
        // well be the same person. It is not taken at face value, because the
        // rule that makes "V Ranipeta" safe is that the parts appear in the
        // profile's own order, and dropping that lets initials match almost
        // anyone. Asking is the honest outcome.
        assertEquals(Resolution.Unknown("Ranipeta Varun"), Travellers.resolve("Ranipeta Varun", profiles))
    }

    @Test
    fun `a name nobody has a profile for is asked about, never invented`() {
        // DATA-MODEL.md: never merge two people silently. Arunima travels on
        // the same bookings and is a different person, so the app asks rather
        // than attaching her flights to the account holder.
        assertEquals(Resolution.Unknown("Arunima Nair"), Travellers.resolve("Arunima Nair", profiles))
    }

    @Test
    fun `with no profiles at all, the first name seen is asked about`() {
        assertEquals(Resolution.Unknown("Varun"), Travellers.resolve("Varun", profiles = emptySet()))
    }

    @Test
    fun `a name that fits two profiles is asked about rather than guessed`() {
        // Two people in one household sharing a surname and an initial is
        // ordinary. A wrong merge is far harder to notice than a wrong split,
        // so this is the case where guessing costs most.
        val vikram = Traveller(legalName = "Vikram Sudhakar Ranipeta", commonName = "Vikram")
        val both = setOf(varun, vikram)

        assertEquals(Resolution.Ambiguous(both), Travellers.resolve("V Ranipeta", both))
        // The full name is not ambiguous, because only one profile carries it.
        assertEquals(Resolution.Known(varun), Travellers.resolve("Varun Ranipeta", both))
    }

    @Test
    fun `a single initial on its own is not a person`() {
        // "V" matches the profile by the letter of the rule and identifies
        // nobody. A vendor that prints only an initial has told us nothing.
        assertEquals(Resolution.Unknown("V"), Travellers.resolve("V", profiles))
    }

    @Test
    fun `the common name resolves as readily as the legal one`() {
        // The two are chosen independently, so a common name is not always a
        // shortening of the legal name and cannot be derived from it.
        val arunima = Traveller(legalName = "Arunima Nair", commonName = "Anu")

        assertEquals(Resolution.Known(arunima), Travellers.resolve("Anu", setOf(arunima)))
    }
}
