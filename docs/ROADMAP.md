# Roadmap

Sequenced so that each phase is usable on its own and the riskiest dependency,
mailbox access, is never on the critical path.

## Phase 0. Prove the spine, on real documents

Android only, per [ADR-0002](adr/0002-android-first.md).

Two booking kinds: **flights and cars**. Not seven, and no accommodation. The
sample set contains no accommodation booking, so a stay is a kind we could build
but could not test. Flights and cars between them still exercise everything
risky: time zones, a multi-Segment booking, and a deadline reminder.

**Bookings are shown as supplied.** Build one renders the dates, times and
places exactly as the vendor stated them. It does not second-guess them, adjust
them, or tell the traveller that a booking looks wrong. Detecting that one
booking contradicts another is real work and it is not in build one.

- Trip, Booking, Segment, TimelineEvent, Reminder tables
  (see [ADR-0001](adr/0001-bookings-split-into-segments.md))
- Day timeline and trip home
- Local notifications, per the [reminder rules](REMINDERS.md)
- Per-trip ICS feed

**Intake is upload and scan, not typing.** Share a PDF or an email into the app,
or photograph a paper voucher. No manual-entry screen ships to users.

### Why the original plan changed

Phase 0 was written as manual entry with no parsing, so that a wrong model would
be discovered before any parser work was wasted. That is no longer the plan: the
author is planning a real trip now and has real confirmations to feed it, and an
app that requires typing would not get used, so it would not get tested.

The cost is that two unproven things now ship together, the model and the
parser. When a booking comes out wrong, the cause is ambiguous.

Mitigation, and it is not optional: **keep a manual-entry path as a developer
tool**, reachable from a debug menu and never shown to users. It costs almost
nothing and it is the only way to test the timeline and reminders when the
parser is the thing that is broken. Without it, every model bug looks like a
parser bug.

### Flights: in and out

Settled against the real confirmations in `docs/samples/`.

| In build one | Out of build one |
| --- | --- |
| Scheduled times from the confirmation | Live delay and gate tracking, which needs a paid feed |
| When check-in opens, and a link to the airline | Checking in on the traveller's behalf, which airlines do not permit |
| (self-connection warnings: see open question 5) | Boarding passes, untestable until the pass exists |
| A bundled airport list: code, city, timezone | |

The airport list is a hard dependency, not a nicety. Every time in both flight
confirmations is a bare local clock reading such as "11:59pm Wed, Dec 23", and
the airport code is the only route to its zone. Without the list, the sample trip
alone crosses four zones and every reminder is wrong.

Check-in opening times are per airline. Hard-code Arajet and JetSMART only, and
say so, rather than implying general coverage.

### Done means

One real trip of the author's, added entirely by upload and scan, where every
reminder fires at the correct local time with the phone in aeroplane mode.

That is the whole bet. The aeroplane-mode clause is the point: it is what
separates this from a calendar.

Storyboard frames: C1, C3, D1, D2, D4, E1, F1, G1, J1, J2.

## Phase 1. Widen the intake (6 to 8 weeks)

- Forwarding alias with inbound SMTP
- 15 vendor templates: 6 airlines, 5 hotel groups, Airbnb, 3 OTAs
- Schema-constrained model fallback with per-field confidence
- Review queue and the confirm screen
- Stays end to end, including wifi and door codes (dropped from Phase 0 for lack
  of a sample to test against)
- Cross-booking conflict detection, deferred from Phase 0's show-as-supplied rule
- Pass storage from `.pkpass` and PDF barcodes
- Offline trip pack

Alias before OAuth: it ships without a Google review cycle and proves the value
on its own.

Storyboard frames: A2, C2, C4, E3, F2, F3, G2, J3.

## Phase 2. Complete the categories (8 weeks)

- Gmail and Microsoft Graph connect, with historical backfill
- Transit, dining, tickets, insurance (car already landed in Phase 0)
- Change detection and downstream impact
- Check-in window tracking and airline deep links
- Wallet passes, live activity on travel day
- Map view with geocoded pins

This is where it stops being a flight app and becomes a trip app.

Storyboard frames: A3, C4, E2, E4, G1, G2, H1, H2, H3, D3, D5.

## Phase 3. The reasons to keep it (6 weeks)

- Explore: natural attractions, things to do, day trips
- Gap detection and the fitted-suggestion engine
- Co-travellers and per-segment membership
- Read-only share links and the printed packet
- Document vault with expiry rules
- Post-trip receipts, expenses and export

Retention features. None of them work before the timeline is reliable.

Storyboard frames: I1, I2, I3, L1, L2, L3.

## Later, deliberately

- Airline partner check-in where an API genuinely exists
- Offline vector map packs, not just cached tiles
- Delay compensation claims (EU 261, Japanese carrier rules)
- Group trips: shared costs, votes, multiple owners
- A web view for planning on a laptop

## Not building

- Booking or paying for anything. Linger organises what you already bought.
- OTA affiliate links dressed up as suggestions.
- A social feed, followers, or public trip profiles.
- Re-encoding a barcode from a reference number. It will not scan.
- Auto-cancelling, auto-rebooking, or messaging a vendor unprompted.

## Risks worth naming

| Risk | Mitigation |
| --- | --- |
| Mailbox scope review | Gmail restricted-scope verification takes weeks plus an annual security assessment. The alias path ships first and is never deprecated. |
| Template rot | Vendors redesign without notice. Confidence is monitored per sender, a drop alerts before users notice, and the model fallback holds the line meanwhile. |
| Barcode fidelity | A pass that will not scan is worse than no app. Store original bytes, keep a PDF fallback, never regenerate. |
| Silent wrongness | A confidently wrong time is the one unrecoverable failure. Thresholds on critical fields only, the source always one tap away, 30-day undo. |
| Timezone bugs | No naive datetimes anywhere, including in ICS output. Property-test every red-eye and every DST boundary. |

## Open questions

These change what gets built, so they are worth answering before Phase 1.

1. ~~Which platform first?~~ **Answered:** Android only.
   See [ADR-0002](adr/0002-android-first.md).
2. **Alias-only at launch, or hold for Gmail verification?** Alias ships months
   earlier and needs no review, but mailbox connect is what finds past trips and
   catches changes without a forward rule. Still open, and now a Phase 1
   question rather than a Phase 0 one, since Phase 0 intake is upload and scan.
3. **Do self-connection warnings survive "show as supplied"?** Round two of the
   flights grilling put them in build one: when one flight lands and another
   leaves the same airport within six hours on a separate booking, say that a
   delay on the first is not covered by the second. A later instruction says
   bookings are shown as supplied, with no conflict detection. These pull in
   opposite directions, and the warning has not been removed pending an answer.
   The distinction available, if wanted: a self-connection warning states a fact
   about two bookings, whereas conflict detection asserts that one of them is
   wrong.
4. **One region first?** Vendor templates and IDP-style derived requirements are
   regional work. India plus Japan plus Europe is a different template set from
   US domestic.
5. ~~Is rail a first-class category?~~ **Answered:** transport splits on who
   drives. Car if you drive it, Transit if you are carried, which covers rail,
   ferry, bus and booked airport transfers. See `CONTEXT.md`.
