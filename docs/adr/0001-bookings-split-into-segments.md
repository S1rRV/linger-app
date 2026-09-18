# Bookings split into Segments

A Booking is one commercial agreement with one vendor. A Segment is one timed
leg of it, with a start, an end and a place at each. A return flight with a
stopover each way is one Booking and four Segments; a car rental is two; a
dinner is one. Times are stored only on Segments, never on the Booking.

We chose this because the two hardest features in the product both operate on
timed intervals across bookings of different kinds, and neither works without a
uniform interval type.

## Considered options

**Legs inside each booking type's own payload.** Fewer tables, and obvious for
single-leg things. Rejected for three reasons.

The idempotency key in `docs/DATA-MODEL.md` is `(vendor, confirmation code,
segment key)`. Without Segment that names nothing, so forwarding the same
confirmation twice cannot reliably be detected as a duplicate.

Six consumers need "the timed things": the day timeline, reminder anchoring, gap
detection, buffer and conflict checks, calendar export, and map pins. With legs
buried in per-kind payloads, each one grows a branch per booking kind, and every
new kind costs six more branches.

Most decisive: reminders are defined as an offset from a field, so that moving
the booking moves the reminder. That requires a stable row to point at. With
payload-held legs the anchor is a path into a JSON blob, so a change email that
rewrites the payload forces every reminder to be deleted and recreated, silently
discarding snooze state, acknowledgements and any edit the traveller made.

## Consequences

One extra table and one extra join on the most-read path, the trip's day view.

A dinner reservation becomes three rows: Booking, Segment, TimelineEvent. That
is deliberate ceremony. It is what lets a dinner and a flight leg be treated
identically by the disruption logic.

A rule follows from this and must hold: **no payload may store a time.** A
`departs_at` on a flight payload is a bug, because it creates a second place
that claims to know when the flight leaves.
