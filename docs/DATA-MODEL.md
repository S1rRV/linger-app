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
3. **Idempotency key is `(vendor, confirmation code, segment key)`.** Forward the
   same email three times and you get one booking at version 1.
4. **Versions, not overwrites.** A change email writes v2 and points v1 at it.
   Undo is walking back one version.
5. **Every instant carries a zone.** There is no naive datetime anywhere in the
   store, including in ICS output.

## Trip and people

### Trip

| Field | Type |
| --- | --- |
| `id` | uuid |
| `name` | text, e.g. "Tokyo and Kyoto" |
| `range` | start, end, home timezone |
| `places` | destination list, bounding box |
| `travellers` | Traveller[] |
| `state` | planning, live, past, archived |

Created by ingestion when no existing trip overlaps by date range and geography.

### Traveller

| Field | Type |
| --- | --- |
| `id` | uuid |
| `names` | given, surname as printed on documents |
| `loyalty` | programme to number |
| `prefs` | meal, seat, accessibility |
| `role` | owner, editor, viewer |

Name variants matter: the parser must match `RAMACHANDRAN/VARUN` to a person.

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
| `money` | amount, currency, paid, `refundable_until` |
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
(class, extras such as ETC), `driver_requirements` (IDP, minimum age, deposit
card), `fuel_policy`, `insurance` (cover, excess), `change_deadline`.

### Reservation and ticket payloads

`party_size`, `window` (start, latest entry), `dress_or_practical_notes`,
`cancellation` (deadline, fee), `tickets[]` (per-person number plus
PassArtifact).

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
