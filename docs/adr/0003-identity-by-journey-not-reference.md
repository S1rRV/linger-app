# A Booking is identified by its journey, not by its reference

Two bookings are the same booking when they describe the same journey: for each
Segment, the same `(operator, service number, local date, traveller)`. The
confirmation code is stored and displayed but is not the identity.

This is worth recording because deduplicating on the reference number is the
obvious thing to do, and it is wrong.

## Why the obvious approach fails

The sample set in `docs/samples/` has the same four flights arriving under two
different references. Expedia calls the booking `73545609581279`; Arajet calls it
`AFGZ2M`. If the airline also emails a confirmation, which airlines routinely do,
reference matching produces two Bookings for one journey. The traveller then sees
every flight twice, gets every reminder twice, and the disruption logic compares a
flight against its own duplicate.

The same split appears on the car side: Booking.com issues `721303130` for a
rental that Alamo holds under its own number.

## Consequences

A Booking carries a set of references, not one. Each is labelled with who issued
it, so the traveller can quote whichever the person on the phone asks for.

Merging is not free. Two records that match on journey must be reconciled, and
they will disagree: the seller's email and the operator's email carry different
fare detail, different wording, sometimes different terminal information. Later
arrival wins per field only where the earlier record had nothing, so a silent
merge never destroys a value that was already there.

Content matching can produce false positives, which reference matching cannot.
Two travellers on genuinely separate bookings for the same flight are
distinguished by the traveller in the key, which is why the traveller is part of
it rather than just the flight.

`local date` rather than an instant, because the same flight number on the same
calendar day is one service even when a delay pushes the actual departure past
midnight.
