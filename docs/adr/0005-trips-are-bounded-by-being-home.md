# Trips are bounded by being Home, not by geography

A Booking joins an existing Trip unless the traveller is Home in between. If a
Booking brings them Home and the next one starts later, that is a new Trip.

This replaces the earlier rule, "created by ingestion when no existing trip
overlaps by date range and geography", which was written before there were real
bookings to test it against.

## Why geography overlap fails

It has no answer for the sample trip in `docs/samples/`. Medellin runs 24 to 28
December, Cartagena 29 December to 3 January, with a flight between them and
1,100 km of separation. Geographically they barely overlap; as an experience they
are obviously one trip. Any threshold generous enough to join them also joins two
genuinely separate trips to the same country a month apart.

It is also the wrong question. Nobody decides whether two bookings belong to one
trip by measuring the distance between them. They ask whether they went home in
between.

## Why Home works

It is the thing people actually mean. "Two trips" means unpacking and repacking.
The sample trip never touches Newark between Medellin and Cartagena, so it is
one trip, which matches what anyone would say out loud.

It needs no threshold and no tuning. A distance rule needs a number that will be
wrong for someone; this rule needs only a Home, which the Account now carries.

It generalises. A five-week tour of nine cities is one Trip. Two weekends away in
the same month are two Trips. Neither needs a special case.

## Consequences

**Home must exist before the first Trip can be bounded.** This forced an Account
entity into the model, which also gave ADR-0004's "the account's home currency" a
place to live. Home is asked for during setup and never inferred from a Booking,
because a trip can legitimately start somewhere else.

**Home is snapshotted onto each Trip.** Otherwise moving house would silently
rewrite the boundaries of every past Trip.

**The rule will still be wrong sometimes.** A second home is one case. This is
why merging and splitting Trips by hand is in build one rather than deferred: an
automatic rule with no manual override leaves the traveller looking at a mess
they cannot fix.

**Amended while building seam 4.** Two things this ADR got wrong.

*Home is a set of Places, not one.* New York is one place to the person who
lives there and three airports to an airline. With a single Place, flying back
into JFK never counts as coming home and two Trips are silently glued into one.

*A connection through the home airport is solved, not tolerated.* This ADR
listed it as a case the rule would get wrong. It is not: a Home touched inside a
single Booking is a connection, because two hours in Terminal C on one ticket is
not going home whatever the airport code says. Reading the Booking settles it
and adds no threshold, so the "no tuning" claim above still holds. Only two
separate tickets meeting at the home airport remain ambiguous, and that case is
genuinely ambiguous to a human too.

**A Booking entirely at Home creates no Trip.** A restaurant in your own city is
not the start of a holiday.
