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

The one thing that is in: stating a relationship between two Bookings, such as a
self-connection warning. Saying "these are separate tickets, a delay on the first
is not covered by the second" is a fact about the pair. Saying "this booking is
wrong" is a judgement about one of them. The first is allowed, the second is not.

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
| Self-connection warnings | Boarding passes, untestable until the pass exists |
| A bundled airport list: code, city, timezone | |

The airport list is a hard dependency, not a nicety. Every time in both flight
confirmations is a bare local clock reading such as "11:59pm Wed, Dec 23", and
the airport code is the only route to its zone. Without the list, the sample trip
alone crosses four zones and every reminder is wrong.

Check-in opening times are per airline. Hard-code Arajet and JetSMART only, and
say so, rather than implying general coverage.

### Cars: in and out

Settled against the Sixt and Alamo confirmations in `docs/samples/`.

| In build one | Out of build one |
| --- | --- |
| One Segment per rental, drawing a collect and a return | Anything about what the driver must bring |
| Times from the vendor's `.ics` when one is attached | Fetching a voucher from behind a broker login |
| Timezone resolved by fallback, see below | |
| A flag when a required document is not in our hands | |

**Timezone fallback**, because a car branch has no airport code. Try in order:
the attached calendar file, the airport code when the branch sits at an airport,
then the country. Country resolves more often than it sounds: Colombia has one
zone all year, and so does India. It fails for the United States and Brazil, and
a booking that reaches the end of the chain is flagged rather than guessed.

**No driver requirements, deliberately.** Alamo lists three things to bring;
Sixt lists none, which does not mean none are needed. Rather than show one and
not the other, or invent per-country rules, the app says nothing. What to bring
is the traveller's own business. There is no field for it, so nothing can creep
back in.

**Vendor secrets are not references.** Sixt emails a security code beside its
reservation number. It is kept, shown behind a tap, and never put on a summary
or into a calendar export.

### Travellers: in and out

| In build one | Out of build one |
| --- | --- |
| Legal name, common name, observed variants | Passport, visa, date of birth, phone |
| Relationship to the account holder | Any document vault |
| Matching a name across vendors | Accounts or logins for companions |

Names are in build one because they are load-bearing, not because they are easy.
[ADR-0003](adr/0003-identity-by-journey-not-reference.md) makes the traveller
part of how two Bookings are told apart, and the sample set renders one person
three ways across four vendors, so deduplication does not work until name
matching does.

Identity documents stay out until something reads them. A stored passport number
that no feature consumes is liability with no upside. When a feature needs one,
it arrives with the rule already written: typed on the device, never parsed from
an email, never sent to the server, never in a cloud backup.

### Change and cancellation: in and out

| In build one | Out of build one |
| --- | --- |
| Recognising an amendment and applying it | Noticing a change you have not uploaded |
| Showing what moved, old version one tap away | |
| Cancelled bookings kept, struck through, reminders voided | |
| Prompting for a missing required field | |
| Fare rules shown: changeable, refundable | |

**The app only knows what it has been shown.** Build one has no mailbox
watching, so an amendment arrives only if you upload it. Say so plainly on
screen, in the same spirit as the offline screen: an app that silently shows
stale times is worse than one that admits what it has not seen.

**Cancelled is not deleted.** The Booking stays visible and struck through, its
reminders voided. A refund can be outstanding for weeks, and a trip that quietly
loses a row is worse than one showing a cancelled booking.

**Prompting is not manual entry.** "No typing in" rules out a screen for
building a Booking from nothing. Asking for one named field that a parsed
Booking is missing is a different act, and it is the only way an amendment that
arrives before its original can become usable.

### Money: in and out

| In build one | Out of build one |
| --- | --- |
| Amount and currency exactly as stated | Converting a per-Booking amount for display |
| The vendor's own label kept and shown | Tracking what actually hit the card |
| Breakdown when given, never computed | Per-line refund rules |
| A total per currency | Splitting a Booking between travellers |
| An estimated combined total at the frozen rate | |
| A post-return prompt when the vendor said "estimated" | |

**The stated amount is the paid amount.** Sixt labels its figure "Estimated
rental cost" because extras and fuel can move it at return. The app does not
hedge the number: it shows the figure and the vendor's word for it. What happens
after the car goes back is settled by one prompt the day after drop-off, asking
whether the amount changed. Answer it or ignore it.

**Combined totals use a frozen rate**, captured once per Booking and never
refetched, per [ADR-0004](adr/0004-freeze-the-exchange-rate.md). Individual
amounts are never converted. The combined figure is always labelled an estimate,
with its rate and date shown. A currency whose rate could not be fetched sits on
its own line rather than being folded in.

**Splitting is wanted, and it is Phase 3.** Not needed for a partner, genuinely
wanted for a friend, in the style of a shared-expense app. It needs a Share per
Traveller carrying its own settled or outstanding state, which is a settlement
model rather than a field, and it has no bearing on whether the timeline works.

### The Trip itself: in and out

| In build one | Out of build one |
| --- | --- |
| Home, asked once during setup | Automatic archiving |
| Trip range: first Segment start to last Segment end | |
| Destinations and Waypoints told apart | |
| Name derived from Destinations, always editable | |
| Joining rule: same Trip unless you were Home in between | |
| Merging and splitting Trips by hand | |

**Bounded by Home, not geography**, per
[ADR-0005](adr/0005-trips-are-bounded-by-being-home.md). The old rule, overlap by
date range and geography, had no answer for the sample trip: Medellin and
Cartagena are 1,100 km and one flight apart and are obviously one trip.

**Destination or Waypoint.** A Place is a Destination when a non-flight Booking
is anchored there, or when the traveller is there over eight hours with no onward
flight booked. The sample trip's layovers, 2 h 25 m at Santo Domingo and 3 h 1 m
at Punta Cana, are Waypoints and stay out of the Trip's name.

**Never name a Trip after a month.** The sample trip runs 23 December to 3
January, so any month or year in the name is wrong at one end.

**Merge and split by hand is in build one, not deferred.** The joining rule will
be wrong sometimes, and an automatic rule with no override leaves the traveller
with a mess they cannot fix.

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
- Document vault with expiry rules: passport, visa, date of birth, phone
  (deferred from Phase 0 deliberately, see Travellers above)
- Post-trip receipts, expenses and export
- Splitting a Booking between Travellers, shared-expense style: a Share per
  Traveller with settled or outstanding state

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
3. ~~Do self-connection warnings survive "show as supplied"?~~ **Answered: yes,
   they stay.** The line between them: a self-connection warning states a fact
   about the relationship between two Bookings, that they are separate tickets
   and a delay on one is not covered by the other. Conflict detection asserts
   that a Booking is itself wrong. Show-as-supplied forbids the second, not the
   first.
4. **One region first?** Vendor templates and IDP-style derived requirements are
   regional work. India plus Japan plus Europe is a different template set from
   US domestic.
5. **Which historical exchange-rate source?** Needed for the combined trip
   total, per [ADR-0004](adr/0004-freeze-the-exchange-rate.md). "Google" is not
   callable: there is no public Google FX API, and scraping it is neither
   permitted nor stable. The ECB set through Frankfurter is free and dated but
   euro-based and limited to major currencies, so it would not have covered a
   Colombian peso charge. A commercial feed covers more and costs money. Not
   urgent: all four sample Bookings are in dollars, so this can wait until one
   arrives in a second currency.
6. ~~Is rail a first-class category?~~ **Answered:** transport splits on who
   drives. Car if you drive it, Transit if you are carried, which covers rail,
   ferry, bus and booked airport transfers. See `CONTEXT.md`.
