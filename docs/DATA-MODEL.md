# Data model

Nineteen entities. The shape of this model is the reason a dinner reservation and
a transatlantic flight can share one timeline, one reminder engine and one
export.

## Five rules that hold

1. **A booking is not an event.** One car rental is one booking and two timeline
   events. One multi-leg flight is one booking, four events, and one check-in
   state.
2. **Derived is disposable.** Events, reminders, calendar rows, pins and passes
   are regenerated from bookings. Nothing downstream is hand-edited without being
   marked, and marked things survive regeneration.
3. **Identity is the reference or the journey, never reference alone.** Same
   Seller and same Reference is one Booking; so is the same journey, per Segment
   `(operator, start place, start local date, traveller)` with the service number
   as a tiebreaker. Reference catches an amendment that moved the date; journey
   catches the same flights arriving from two sellers. **The first record seen is
   primary**: later records fill empty fields and never overwrite set ones, and a
   field a Booking cannot work without is asked about rather than guessed. See
   [ADR-0003](adr/0003-identity-by-journey-not-reference.md).
4. **Versions, not overwrites.** A change email writes v2 and points v1 at it.
   Undo is walking back one version.
5. **Every instant carries a zone.** There is no naive datetime anywhere in the
   store, including in ICS output.

## Trip and people

### Account

| Field | Type |
| --- | --- |
| `home` | Place. Asked once during setup, never inferred from a Booking |
| `home_currency` | the currency combined totals are estimated in |

Added because two earlier decisions referred to things that did not exist.
[ADR-0004](adr/0004-freeze-the-exchange-rate.md) converts to "the account's home
currency", and the Trip needed a Home to decide where a trip begins and ends.

### Trip

| Field | Type |
| --- | --- |
| `id` | uuid |
| `name` | derived from Destinations in order, always editable |
| `range` | first Segment start to last Segment end |
| `home` | snapshot of the Account's Home at creation |
| `destinations` | Place[], ordered |
| `waypoints` | Place[], excluded from the name |
| `travellers` | Traveller[] |
| `state` | planning, live, past, archived |

`range` runs from the first Segment's start, not from arrival at the first
Destination. The countdown a traveller wants is "leave home in six days".

`home` is a snapshot rather than a live reference, so a trip taken while living
elsewhere still reads correctly after a move.

**Joining rule.** A Booking joins an existing Trip unless the traveller is Home
in between. If a Booking returns them Home and the next starts later, that is a
new Trip. See [ADR-0005](adr/0005-trips-are-bounded-by-being-home.md). The older
rule, overlap by date range and geography, is replaced.

**Naming.** Built from Destinations in the order they occur, so the sample trip
is "Medellin and Cartagena". Waypoints and Home are excluded. Never built from a
month or year: that trip runs 23 December to 3 January and any such name is wrong
at one end.

**State.** `past` when the last Segment ends. `archived` only by hand, never
automatically, because a past Trip still holds receipts worth keeping to hand.

A Booking whose Places are all Home creates no Trip at all.

### Traveller

| Field | Type |
| --- | --- |
| `id` | uuid |
| `legal_name` | exactly as printed on the passport |
| `common_name` | used where no document must match |
| `observed_names` | every rendering seen, each tagged with the vendor that used it |
| `relationship` | to the account holder, free text |
| `loyalty` | programme to number |
| `prefs` | meal, seat, accessibility |
| `role` | owner, editor, viewer |

Name variants are load-bearing, not cosmetic. [ADR-0003](adr/0003-identity-by-journey-not-reference.md)
makes the traveller part of how two Bookings are told apart, and the sample set
renders one person three ways: Sixt writes "Varun Sudhakar Ranipeta", Expedia
writes "Varun Sudhakar", Booking.com writes "Varun". Match on surname plus first
initial, and ask rather than guess when it is close. Never merge two people
silently: a wrong merge is far harder to notice than a wrong split.

Legal and Common names are both chosen by the traveller, never derived one from
the other. The sample set shows why: Sixt needs no passport yet holds the full
legal name, so which name a vendor received says nothing about which name the
traveller would have picked.

**No identity documents here.** Passport number, visa, date of birth and phone
are deliberately absent until Phase 3. See the Document entity and the ingestion
guardrail.

### ShareGrant

| Field | Type |
| --- | --- |
| `scope` | trip, day, segment |
| `audience` | link, account |
| `redactions` | codes, prices, documents |
| `expires_at` | timestamp |

## Bookings

### Booking (one table, typed payloads)

| Field | Type |
| --- | --- |
| `id` | uuid |
| `trip_id` | Trip |
| `kind` | flight, stay, car, rail, dining, ticket, insurance, other |
| `vendor` | Vendor |
| `codes` | confirmation, ticket, voucher, PNR |
| `money` | amount, currency, `vendor_label`, `rate_at_purchase`, optional `breakdown[]` |
| `travellers` | Traveller[] |
| `version` | int, `superseded_by` |
| `source` | IngestionItem |
| `state` | draft, confirmed, changed, at_risk, cancelled, completed |

Every category in the product is a row here, with one lifecycle. That is what
makes the reminder engine and the exporter category-agnostic.

### Flight payload

`marketing` (NH 819, `operated_by`), `legs` (departure and arrival airport,
terminal, times with zones), `seat` (number, class, group, sequence), `baggage`
(checked, cabin), `checkin` (`opens_at`, `closes_at`, state), `pass`
(PassArtifact).

### Stay payload

`address` (Place, geocoded), `in` / `out` (datetime with zone, early and late
options and their prices), `access` (door code, lockbox, host contact), `wifi`
(ssid, secret), `notes` (extracted[] with source spans).

### Car payload

`pickup` / `dropoff` (Place plus datetime with zone, counter detail), `vehicle`
(class, extras), `fuel_policy` and `insurance` (cover, excess) **only when the
confirmation states them**, `change_deadline`.

There is deliberately no `driver_requirements` field. What a driver must bring
is left to the traveller: see the note under Car in `CONTEXT.md`.

### Reservation and ticket payloads

`party_size`, `window` (start, latest entry), `dress_or_practical_notes`,
`cancellation` (deadline, fee), `tickets[]` (per-person number plus
PassArtifact).

### Money, on any Booking

`amount` and `currency` exactly as stated, plus `vendor_label` so the
confirmation's own wording survives ("Total paid", "Total Cost", "Estimated
rental cost"). The stated figure is treated as paid; see the Amount entry in
`CONTEXT.md`.

`rate_at_purchase` is captured once, when the Booking is first stored, and never
refetched. This is what keeps a Trip's combined total stable: a live rate would
make last year's trip cost a different number every morning.

`breakdown[]` is optional and kept only when given. The Arajet confirmation
itemises fare 2,666.78, bags 260.00 and fees 13.00; Alamo gives a single total.
A missing breakdown is never computed.

`Share[]` exists only when a Booking is being split: one row per Traveller, each
with an amount and a settled or outstanding state. Phase 3.

### Fare rules, on any Booking

`changeable` / `refundable` (tri-state: yes, no, unstated) and
`free_cancellation_until`. Both flight confirmations in `docs/samples/` say
"Change not allowed" and "Non Refundable", which is exactly what a traveller
wants to know before spending twenty minutes on hold. Unstated is a third value
and not a synonym for no: saying nothing is different from saying no.

## Derived layer

### TimelineEvent

| Field | Type |
| --- | --- |
| `id` | uuid |
| `trip_id` | Trip |
| `booking_id` | Booking, nullable |
| `kind` | booked, derived, plan, gap |
| `window` | start, end, timezone, `all_day` |
| `place` | Place |
| `derived_from` | rule id |

The only thing the UI renders. Bookings never draw themselves.

### Reminder

| Field | Type |
| --- | --- |
| `anchor` | booking field or event |
| `offset` | signed duration |
| `rule_id` | entry in the rule matrix |
| `channel` | push, critical, silent, calendar alarm |
| `state` | scheduled, fired, snoozed, void |

Stores an offset, never an absolute time. That is what makes re-anchoring free.

### PassArtifact

| Field | Type |
| --- | --- |
| `format` | pkpass, pdf417, aztec, qr, pdf |
| `payload` | original bytes, never re-encoded |
| `valid` | from, until, relevance |
| `wallet` | added, serial |

Legally and practically, you cannot rebuild a barcode from a PNR.

### Place

`coords` (lat, lon, timezone), `names` (local script plus latin), `kind`
(airport, station, lodging, venue, natural).

### CalendarLink

`target` (feed, google, apple, outlook), `external_id`, `categories_enabled`,
`user_edited_fields` preserved on update.

## Ingestion and audit

### IngestionItem

| Field | Type |
| --- | --- |
| `id` | uuid |
| `source` | gmail, graph, alias, upload, scan, manual |
| `dedupe` | message id, content hash |
| `raw` | blob, retained |
| `classified` | category, intent, vendor, language |
| `state` | see [STATE-MACHINES.md](STATE-MACHINES.md) |

The audit trail. Every booking can name the message that made it.

### ExtractionRevision

`item_id`, `engine` (`template@v` or `model@v`), `fields` (value plus confidence,
each), `corrections` (what the user changed), `applied_at` (undoable for 30
days).

User corrections are training data for that vendor's template.

### Vendor

`domain_patterns`, `layout_hashes`, `template_version`, `trust_level`,
`confidence_trend` (for detecting redesigns).

## Documents and money

### Document

`kind` (passport, visa, idp, insurance, voucher, receipt), `expiry` (date plus
rule flags such as the six-month passport rule per destination), `encrypted` (on
device, biometric).

### Expense

`booking_id` (nullable), `amount` (original currency plus the rate on the day),
`receipt` (Document), `claim` (insurance or carrier compensation status).
