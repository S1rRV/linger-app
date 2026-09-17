# Linger

One place for everything a trip is made of, and it fills itself in.

A trip arrives as a dozen unrelated emails: a PNR here, a lockbox code there, a
rental voucher in a PDF, a dinner at 19:30 you will forget. Linger watches for
them, reads them, and assembles one timeline you can act on, with the check-in
window open, the confirmation number under your thumb, the boarding pass on
screen with no bars of signal, and the reminder that fires before the
cancellation deadline rather than after it.

## The three claims

| Claim | What it means in practice |
| --- | --- |
| Nothing is typed twice | Forward it, connect a mailbox, photograph it, or drop the PDF in. A parser turns any confirmation into structured segments and attaches them to the right trip. |
| One spine: Trip to Booking to Event | Every category (flight, stay, car, rail, dinner, ticket, insurance) is a booking that materialises into timed events. Reminders, calendar entries, map pins and passes are all derived, never hand-maintained. |
| Reachable when the signal is not | Reference numbers are one tap to copy, barcodes are stored as issued and render offline, reminders are scheduled on device, and the whole trip downloads as a pack before you fly. |

## Scope

Covered per category:

- **Flights.** Check-in window and deep link, itinerary with copyable PNR
  and e-ticket number, dates and times with timezones, boarding pass barcode,
  reminders, calendar export.
- **Stays.** Hotels and Airbnbs, check-in and check-out times with their
  real edges, things to note, wifi password and door codes with copy, reminders,
  calendar export.
- **Cars and ground.** Reservation and voucher numbers, pickup and
  drop-off location and time, fuel and return sequence, reminders, calendar
  export. Rail, transfers and ferries reuse the same object.
- **Other bookings.** Dining reservations and attraction tickets with
  ticket numbers, QR codes and Wallet passes.
- **Explore.** Trip map, natural attractions, things to do, day trips, and
  suggestions fitted to real gaps in the itinerary.
- **Intake.** Email connect or a forwarding alias, uploads, scans, and a
  review queue for anything the parser is not sure about.

Deliberately not in scope: booking or paying for anything, affiliate links
dressed up as suggestions, a social feed, re-encoding barcodes, or contacting a
vendor without the traveller tapping send.

## Documents

| Document | What it holds |
| --- | --- |
| [docs/STORYBOARD.md](docs/STORYBOARD.md) | Frame by frame, all 10 acts: what the traveller sees, what they do, what Linger does behind it. |
| [docs/INGESTION.md](docs/INGESTION.md) | The eight-stage pipeline from "a confirmation landed" to "the trip changed", and the guardrails. |
| [docs/DATA-MODEL.md](docs/DATA-MODEL.md) | Nineteen entities, their fields, and the five rules that hold. |
| [docs/STATE-MACHINES.md](docs/STATE-MACHINES.md) | Ingestion, flight check-in, booking lifecycle, reminder lifecycle. |
| [docs/REMINDERS.md](docs/REMINDERS.md) | The 22-rule reminder matrix and the scheduling mechanics. |
| [docs/CALENDAR-AND-PASSES.md](docs/CALENDAR-AND-PASSES.md) | ICS feed vs direct write, exported event shape, pass and Wallet handling. |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Four phases, risks, and the open questions. |

A visual storyboard of all 40 boards is published as a Claude design canvas.
See [docs/STORYBOARD.md](docs/STORYBOARD.md) for the link.

## Assumptions

Stated rather than asked, and open to being overruled:

- Phone first, iOS and Android, with a read-only web view.
- Ingestion runs server side; the trip store is offline first on device.
- Linger never books or pays. It organises what you already booked.
- v1 targets one traveller with optional co-travellers, not group planning.

## Status

Storyboard and specification only. No application code yet. See
[docs/ROADMAP.md](docs/ROADMAP.md) for the build order, starting with Phase 0,
which proves the Trip / Booking / TimelineEvent / Reminder spine with manual
entry and no parsing at all.
