# Linger

A travel companion that assembles a trip from the confirmations a traveller
already receives, and makes it actionable on the day. This glossary fixes the
vocabulary the model and the UI both use.

## Language

**Trip**:
A dated stay away from home, holding everything booked for it. The container
every other concept hangs off.
_Avoid_: Journey, vacation, holiday

**Traveller**:
A person who appears on a booking. Distinct from the account holder, who may
book for others.
_Avoid_: User, passenger, guest, customer

**Booking**:
One commercial agreement with one vendor, identified by one confirmation code.
A return flight bought together is one Booking, not two.
_Avoid_: Reservation, itinerary item, ticket

**Segment**:
One timed leg of a Booking, with a start, an end, and a place at each. A
two-leg flight has two Segments; a car rental has two (collection and return);
a dinner has one. Segments are the only place times are stored.
_Avoid_: Leg, event, slice

**TimelineEvent**:
A positioned thing on a Trip's day view. Every Segment projects to exactly one
TimelineEvent, and some TimelineEvents are derived instead (leave-by, arrive)
with no Segment behind them. Always has a time window.
_Avoid_: Event, entry, item, activity

**Reminder**:
A notification defined as a signed offset from a field on a Segment, never as a
fixed timestamp. Moving the Segment moves the Reminder.
_Avoid_: Alert, notification, alarm

**PassArtifact**:
A scannable credential exactly as its issuer produced it: a boarding pass, a
rail QR, a museum ticket. Stored as received bytes and never re-encoded,
because a regenerated barcode does not scan.
_Avoid_: Ticket, barcode, pass

**Vendor**:
The organisation a Booking is with, and the sender whose confirmation format
the parser recognises.
_Avoid_: Provider, supplier, merchant

**IngestionItem**:
One inbound thing to be read: an email, an attachment, a scan, an upload. The
audit record that explains why a Booking exists.
_Avoid_: Message, import, source

**Idea**:
A place someone saved for a Trip without choosing when. Has no time. Promoting
an Idea puts it on a day and leaves the Idea marked as promoted.
_Avoid_: Saved place, bookmark, wishlist item

**Plan**:
A TimelineEvent the traveller placed themselves, with no Booking behind it. A
visit they intend, not a thing they bought.
_Avoid_: Draft, tentative booking, pencilled-in

**Gap**:
Unclaimed time between two consecutive TimelineEvents. Always calculated when
read, never stored, so it cannot go stale when something moves.
_Avoid_: Free time, hole, window

**Car**:
A Booking where the traveller takes custody of a vehicle and drives it. Carries
fuel policy, insurance excess and licence requirements.
_Avoid_: Rental, hire, vehicle

**Transit**:
A Booking where the traveller is carried on a booked service: rail, ferry, bus,
or a pre-arranged airport transfer. Carries service number, coach and seat,
platform or pickup point. Split from Car on who drives, not on what the vehicle
is.
_Avoid_: Rail, ground transport, transfer

**Seller**:
Who the Booking was bought from and who holds the money. Expedia, Booking.com,
or the operator itself when booked direct.
_Avoid_: Agent, OTA, reseller, provider

**Operator**:
Who actually performs the service on the day: the airline flying the aircraft,
the company handing over the car keys. Who the traveller deals with at the
airport or the counter, and who check-in happens with.
_Avoid_: Carrier, supplier, airline

**Layover**:
The wait between two consecutive Segments of the SAME Booking. Protected: if
the first runs late, the operator carries responsibility for the second.
_Avoid_: Stopover, connection

**Self-connection**:
A wait between two Segments of DIFFERENT Bookings at the same place. Looks like
a Layover to the traveller and is not one: a delay on the first leaves the
second operator owing nothing. Recognised when the second departs the same place
within six hours of the first arriving, with no Stay booked in between. Always
surfaced, never silently treated as a Layover.
_Avoid_: Connection, transfer, self-transfer

## Terms deliberately not used

**Reservation** is the ordinary English word for any Booking, so it cannot also
name one kind of Booking. Use Booking in the model. "Reservations" survives only
as a label on one UI section.

**Ticket** is a code printed on a Booking, not a thing in its own right. The
scannable artefact is a PassArtifact; the number beside it belongs to one
Traveller on one Booking, and is optional: the Arajet confirmation in
`docs/samples/` carries no ticket number at all, only a confirmation code. The
confirmation code is the required identifier; a ticket number is not.

**Event** on its own is ambiguous between a Segment and a TimelineEvent. Always
say which.

**Rail** is too narrow: it excluded ferries, buses and booked airport transfers,
which had nowhere to live. Use Transit.
