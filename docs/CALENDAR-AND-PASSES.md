# Calendar export, Wallet and offline

## Calendar: subscribe beats sync

Three routes, offered in this order of preference.

### 1. Live ICS feed per trip (recommended)

A read-only `webcal://` URL the traveller subscribes to once. It never drifts,
because it is regenerated from the bookings on every change. Nothing is written
into anyone's calendar store, so nothing can conflict.

Trade-off: most clients refresh on their own schedule, which can be hours. For
same-day changes the push reminder is the reliable channel, not the calendar.

### 2. Direct write into a named calendar

Events are written into a calendar named `Linger . Tokyo`, separate from the
traveller's own calendars so it can be hidden or deleted wholesale. Per-category
toggles: flights, stays, cars and rail, reservations, and plans (off by default,
because unbooked ideas in a shared work calendar are noise).

Linger only ever edits events it created. Where a traveller has edited the title
or notes of a Linger event, those edits are preserved when the times are updated.

### 3. One-off `.ics` download

For forwarding to someone who is not using the app.

## The exported event shape

```
BEGIN:VEVENT
UID:linger-8a2c-nh819-leg1@lngr.app
SUMMARY:NH 819 . BLR to HND
DTSTART;TZID=Asia/Kolkata:20261103T235500
DTEND;TZID=Asia/Tokyo:20261104T113500
LOCATION:Kempegowda International Airport, Terminal 2, Bengaluru
DESCRIPTION:PNR K7X2QM . e-ticket 205-2345678901\n
  Seat 32A . gate 74A . group 4\n
  Open in Linger: https://lngr.app/b/8a2c-nh819
BEGIN:VALARM
TRIGGER:-PT3H
DESCRIPTION:Leave for the airport
END:VALARM
BEGIN:VALARM
TRIGGER:-PT24H
DESCRIPTION:Check-in opens
END:VALARM
END:VEVENT
```

Rules that matter:

- **`TZID` on both ends.** A red-eye departs in one zone and arrives in another.
  Getting this wrong is the classic travel-app bug.
- **Stable `UID` per segment**, so an update replaces rather than duplicates.
- **Reference numbers in the description**, because that is what is reachable
  from a calendar notification on a watch.
- **Alarms come from the same [rule matrix](REMINDERS.md)** that drives push, so
  the two channels never disagree.
- Stays get a timed check-in event and a timed check-out event, not one all-day
  block spanning the nights. All-day blocks hide the 15:00 and 11:00 that matter.
- Car rentals get two events, one per end, from one booking.

## Passes and Wallet

**Store the original, never re-encode.** A boarding pass barcode encodes an
IATA BCBP string that was signed and issued by the carrier. It cannot be rebuilt
from a PNR and a seat number, and a regenerated code will not scan. Linger keeps
the bytes it was given.

| Source | What is stored |
| --- | --- |
| `.pkpass` | The package as received, added to Wallet, and rendered natively in-app |
| PDF with a barcode | The PDF plus the cropped barcode region for full-screen display |
| HTML mail with an image barcode | The image at original resolution |
| Vendor QR payload | The payload string, re-rendered locally at display size |

Display behaviour: screen brightness raised while a pass is open, sleep
suppressed, the human-readable BCBP string shown underneath for when a scanner
fails, and page dots when a booking carries several passengers or legs.

Wallet passes carry the relevance time of the event, so they surface on the lock
screen at the airport or at the museum door without being opened.

## Offline contract

The trip pack downloads automatically 48 hours before departure, and again on any
change while the device is on wifi.

Available with the radio off:

- All passes and barcodes
- All documents, encrypted on device behind biometrics
- The full itinerary, every address and reference number
- An offline map tile pack for the trip bounding box
- Wifi passwords and door codes
- All reminders, because they are scheduled locally

Explicitly stale, with the timestamp of its last truth shown:

- Live flight status
- Explore suggestions (cached list only, no new search)
- New confirmations, which queue server side until there is a connection

The offline screen states both lists rather than showing a spinner, plus a
"show my address in local script" full-screen view for handing a phone to a taxi
driver.
