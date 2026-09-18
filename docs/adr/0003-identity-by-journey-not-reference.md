# A Booking is identified by its journey or its reference, never by reference alone

Two records describe the same Booking when **either** of these holds:

1. **Same Seller and same Reference.** Sixt reservation `9739400602` is the same
   Booking whatever it now says.
2. **Same journey.** Per Segment, the same
   `(operator, start place, start local date, traveller)`, plus the service
   number as a tiebreaker where one exists.

The first record seen becomes the primary. Later records fill fields that are
empty and never overwrite fields that are already set.

The filename says "not by reference", which was the original title. It is kept so
existing links still resolve. The amendment below explains why reference alone is
wrong and reference plus journey is right.

## Why reference alone fails

The sample set in `docs/samples/` has the same four flights arriving under two
different references. Expedia calls the booking `73545609581279`; Arajet calls it
`AFGZ2M`. If the airline also emails a confirmation, which airlines routinely do,
reference matching produces two Bookings for one journey. The traveller then sees
every flight twice, gets every reminder twice, and the disruption logic compares a
flight against its own duplicate.

The same split appears on the car side: Booking.com issues `721303130` for a
rental that Alamo holds under its own number.

## Why journey alone fails

Found while grilling change and cancellation, after the original decision was
committed. Journey matching has two holes, and both sit inside build one.

**Cars have no service number.** Neither Sixt nor Alamo has an equivalent of a
flight number, so the key as first written did not apply to half of build one.
This is why the key now says `start place` rather than `service number`, with the
service number demoted to a tiebreaker: every Segment has a start place and a
start date, only some have a service number.

**A change moves the date, which moves the key.** If Sixt reschedules the pickup
from 28 December to 27 December, the journey key no longer matches, so a pure
journey match creates a second Booking rather than amending the first. That is
precisely the duplicate this decision exists to prevent. Reference matching
catches it, because Sixt keeps reservation `9739400602` across the change.

Neither rule is sufficient alone. Reference catches the amendment; journey
catches the cross-seller duplicate.

## Consequences

A Booking carries a set of References, not one. Each is labelled with who issued
it, so the traveller can quote whichever the person on the phone asks for.

**First record wins.** Whatever arrives first is the primary, and later arrivals
fill only what is missing. This matters because an amendment is usually partial:
Sixt's "your booking has moved" states the new date and may say nothing about the
vehicle, the price or the insurance. Without this rule a later, thinner record
would hollow out a complete one.

**A gap is asked about, not guessed.** When the primary record lacks something a
Booking cannot work without, the traveller is prompted for that field and only
that field. This is not a retreat from "no typing in", which rules out a
manual-entry screen for creating a Booking from nothing. Filling one named gap in
a parsed Booking is a different act.

Content matching can produce false positives, which reference matching cannot.
Two travellers on genuinely separate bookings for the same flight are
distinguished by the traveller in the key, which is why the traveller is part of
it rather than just the journey.

`local date` rather than an instant, because the same service on the same
calendar day is one service even when a delay pushes the actual departure past
midnight.

## Amended while building seam 3

**The journey key holds the start and not the end.** Two sellers quoting one
flight disagree about the arrival time often enough, and a flight is the same
flight whatever time it is currently believed to land. Putting the arrival in
the key turns every such disagreement into a duplicate, which is the failure
this decision exists to prevent.

**The service number is consulted only when both records state one.** "A
tiebreaker" left this open. A seller that omits the flight number must not block
a match it would otherwise make, and no car confirmation states anything of the
kind, so a tiebreaker that votes when only one side has an opinion would exclude
half of phase 0.

**One matching Segment is enough.** This was left open and is now settled. The
rule first required the two journeys to match whole, so an operator emailing
only the outbound half of a return booking did not match the seller's record of
all four legs and landed as a second Booking. That is the duplicate this
decision exists to prevent, and airlines confirm one direction at a time often
enough that it was not a corner case.

It is sound because the same person cannot be on the same flight, on the same
day, under two genuinely separate bookings. The consequence is that the
traveller in the key now carries more weight than it did: two people on one
flight share every Segment, and only the traveller keeps their Bookings apart.
Which means this rule depends on traveller resolution being right, and that is
its own rule rather than a detail of this one.
