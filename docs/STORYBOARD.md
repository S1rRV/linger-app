# Linger storyboard

Visual canvas (40 boards, pan and zoom):
<https://claude.ai/artifact/WDuytRy7XTR9comwHjZQrw>

Conventions used throughout:

- Phone frames are 390 x 844. No status bars are drawn; app content only.
- Every frame reads **Sees** (what is on screen), **Does** (the one action), and
  **Linger** (the work happening behind it).
- Five wide boards are systems rather than screens: ingestion pipeline, reminder
  matrix, data model, state machines, roadmap. Those have their own documents in
  this folder.
- All names, codes, times and prices are illustrative.

The running example: Varun and Priya fly Bengaluru to Tokyo on 3 November, spend
three nights in Tokyo and six in Kyoto, rent a car in Kyoto, and return on
different dates.

---

## Act 1. Onboarding and the always-on inbox

### A1. Welcome

A single claim, "Every booking. One timeline.", and four concrete proofs: it
reads your confirmations, it builds the timeline, it nudges you in time, it works
with no signal. Three ways forward: connect a mailbox, use a forwarding address,
or add a trip by hand. The privacy line sits under the buttons rather than buried
in a policy.

No account is created until a path is chosen, so this screen can be re-entered
from a share sheet later.

### A2. Connect or forward

Two doors, side by side, one of which needs no trust at all.

- **Connect a mailbox** (Gmail, Outlook, iCloud). The consent screen states the
  scope on the screen where consent is given: travel senders and subjects only,
  read only, and the retention answer (the extracted booking plus the original
  attachment; message bodies discarded after parsing).
- **Forward to your alias**, for example `varun.7k2@in.linger.app`, with a copy
  chip and a link to the exact Gmail filter to set. No mailbox access.

Linger provisions the alias immediately so it works even while OAuth
verification is pending, registers a Gmail `users.watch` or Graph subscription on
connect, and starts a bounded historical backfill.

### A3. Trips found

The first payoff, before anything is typed. "We found 3 trips in your last 180
days", grouped by tense, each showing what was found and how sure the parser is.
Toggles to exclude. Anything switched off is never re-suggested.

Behind it: 19 confirmations grouped into trips by date-range and geography
overlap, deduplicated by message id and content hash, with the 5 uncertain items
queued for the review list.

### A4. Permissions

Four permissions, each priced by the feature it buys and the limit it respects:
notifications (required, the whole point), calendar (writes only to a separate
`Linger . Tokyo` calendar), location (while-using, for leave-by and nearby), and
Wallet. Say no and only that feature goes quiet. OS prompts are deferred to first
use of each feature.

---

## Act 2. Capture, review, and change

The pipeline board behind this act is documented in
[INGESTION.md](INGESTION.md).

### C1. Capture

Four doors in, one queue out: share into Linger from any app, scan a paper
voucher, forward to the alias, or paste and type. Below that, the live state of
each watcher, and what the last few captures became, including the one that was
dropped as "not travel".

### C2. Review queue

Only the genuinely uncertain reaches the traveller. Three items, each stating
exactly why it is there:

1. A scanned car voucher with two unreadable fields.
2. A confident dinner parse whose date falls between two trips.
3. A hotel email that looks like a change to an existing stay, not a new one.

The silent applies collapse into one line ("4 applied without asking"). The queue
is ranked by consequence, not arrival time: conflicts, then unmatched, then soft
fields. Items age out to Dropped after 30 days. Anything applied silently is
undoable from the trip Activity log for 30 days.

### C3. Extract and confirm

Source on the left, reading on the right. The original scan is quoted verbatim
with the unreadable characters highlighted; the parsed fields sit beside it with
per-field confidence. Only the two weak fields ask for a tap. A trip picker
states which trip it will attach to and why.

Corrections are learned against the vendor template. "Always trust this sender"
raises its silent-apply threshold.

### C4. Change detected

A delay is a graph problem, not a notification. "Delayed 2 h 10 m", old and new
times struck through and bold, then every downstream item it touches, each marked
Auto, Tight, OK or unaffected:

| Item | Effect |
| --- | --- |
| Leave-by reminder | Re-anchored automatically |
| 3 calendar events | Updated automatically |
| Car pickup, Kyoto 14:00 | Buffer collapses from 3 h 20 m to 55 m: **tight** |
| Hotel check-in 15:00 | Still fine |
| Dinner, 5 Nov 19:30 | Unaffected |

One suggestion with the vendor's own rule cited ("Toyota allows free changes up
to 2 h before"), a phone number, and a drafted message. Linger does not change a
vendor booking on the traveller's behalf. One button applies all five internal
updates; v1 of the booking is kept so the change is reversible.

---

## Act 3. Trip home

### D1. Trips

Trips by tense, not by date added. Happening now sits first with a day counter, a
progress bar and a live next-thing line. Upcoming trips carry their own
outstanding count. History is one row. The inbox badge lives here rather than
interrupting as a notification.

### D2. Pre-trip readiness

Six days out the app has one job. A countdown anchored to the next thing that
actually happens (check-in opening, not departure), then a to-do list that
separates blocking from merely open:

- Select a seat on NH 819 (free until 24 h before): open
- Carry your International Driving Permit: **blocking**, Japan will not release
  the car without it
- eVisa approved: done
- Travel insurance active: done

The list is derived from the bookings themselves. A Japanese car rental implies
an IDP; an unselected seat implies a free-until deadline.

### D3. Travel-day now card

One number, and the arithmetic behind it. "Leave for BLR airport in 38 minutes",
with the reasoning shown because a traveller will not trust a number they cannot
check: 52 min drive at this hour, 2 h 15 m before an international departure, T2
kerb-to-gate averages 41 min today. Recomputed every 10 minutes from live
traffic.

Below it, the day as an ordered checklist: checked in, bag drop window, security,
boarding at gate 74A, departure. The card promotes itself to a live activity from
20:00.

### D4. Day timeline

A day is a single column with time on the left. Every category on one rail.
Hollow dots mark derived events (arrive, leave-by) as distinct from booked ones.
The unplanned gap is a first-class item rather than empty space, with a "See what
fits" action. All times render in the destination's zone, and the gap
recalculates whenever a neighbouring booking moves.

### D5. Trip map

The trip as geography, layered. Pins numbered in the order they occur, a dashed
route between the day's anchors, and layer chips separating Stays, Booked,
Nature, Food and Saved. A bottom sheet for the selected pin with walking and
driving times to the other anchors, "Open in Maps", and "Copy address" in local
script for the taxi. An offline-map button caches the tile pack for the trip's
bounding box.

---

## Act 4. Flights

### E1. Flight detail

A route block that reads like a departure board (23:55 BLR T2, 9 h 40 m, 11:35+1
HND T3, 787-9), then the four numbers an agent or a website will ask for, each a
copy target sized for a thumb:

- Booking ref / PNR
- E-ticket number
- Frequent flyer number
- Seat

Then the fare facts that settle arguments at a counter: fare class and
changeability, checked and cabin allowance, meal request. Terminal, gate,
aircraft and duration come from a schedule lookup; none of it was in the
confirmation email.

### E2. Check-in window

A countdown to the **close**, not the open, one primary action, and an honest
limit: airlines do not let apps check in on your behalf. Linger opens the
airline's page with the booking reference and surname already on the clipboard
and in the deep link where the carrier supports it, then watches for the
confirmation email to advance the state.

The six states are shown as a timeline on the screen itself: locked, opens,
reminded, checked in, pass stored, boarding. A manual "I checked in another way"
escape hatch stays visible.

### E3. Boarding pass

Code first and large, brightness already raised, screen kept awake, the
human-readable BCBP string underneath for when the scanner fails, and the twelve
fields a gate agent might read aloud. Page dots for the second passenger.

The pass is stored exactly as the airline issued it. Linger never re-encodes a
barcode from the PNR, because a regenerated code will not scan.

### E4. Disruption

What broke, what it breaks, what you can do. The gate change carries the walk
time, not just the number. Below it the real consequence: an onward connection
whose 55-minute gap no longer clears the minimum connection time plus
immigration. Then three concrete outs with their cost:

| Option | Cost |
| --- | --- |
| NH 31, HND to ITM 16:00 | Free change on this fare |
| Shinkansen Nozomi 15:20 | JPY 14,720 |
| Stay in Tokyo tonight | No fee; Kyoto hotel allows 18:00 late arrival |

Messages to the hotel and the rental desk are drafted and not sent.

---

## Act 5. Stays

### F1. Stay detail

Check-in and check-out as a pair with their real edges: bag drop from 11:00, late
checkout until 14:00 for JPY 4,000. Then the two things actually needed on
arrival, both one tap to copy: the confirmation number and the wifi password,
with a "show a join-wifi code" QR. The network is saved so the phone joins by
itself.

Address with a map thumbnail, walking time from the station, "Copy in Japanese"
for a taxi driver. Free cancellation deadline in a warning card with the reminder
already set for the last comfortable moment to decide.

### F2. Things to note

The details that ruin an arrival, extracted and attributed. Six notes pulled from
the listing, the confirmation and two host messages, each marked Watch or Note so
the no-lift and quiet-hours warnings do not sit at the same weight as the
slippers:

- No lift; the room is on the third floor (listing)
- Quiet hours 22:00 to 07:00, strictly enforced (house rules)
- Self check-in: lockbox 7, left of the gate (host message, 2 Nov)
- Rubbish must be sorted: burnable, plastics, cans (house rules)
- Bedroom has no air conditioning, only a fan (amenities)
- No shoes indoors (house rules)

Plus the traveller's own free-text notes, the host's contact and reply time, and
the attached source documents. Every note links back to the sentence it came
from.

### F3. Arrival

At the moment of arrival the screen collapses to the single thing needed: the
lockbox code at 40px with a copy button and a photo of the box. Underneath, the
entry sequence as a checklist, a pre-drafted "arriving in 15 minutes" message in
both languages, and a preview of the next reminder (pack up, checkout is 10:00
and there is no late option here).

Triggered by geofence plus check-in time. Code and photo are cached offline.

---

## Act 6. Cars and ground transport

### G1. Car rental

Pickup and drop-off are one object, not two bookings. Both ends in a single block
with their counters named (Kyoto Stn Hachijo, counter B1), reservation and
voucher numbers copyable, and a blocking requirement in red because forgetting it
ends the rental before it starts: no International Driving Permit, no car.

Then the terms that cost money, read out of the voucher: fuel policy (return
full), insurance (CDW included, JPY 110,000 excess), free-change deadline, second
driver not added. Three reminders anchored to those fields. "Add both to
calendar" writes two timed events.

### G2. Return and ground transport

A return is a sequence, so the screen shows the sequence, in the order that
avoids a charge:

1. Refuel to full (nearest open station, 1.2 km, 4 min)
2. Photograph the odometer and fuel gauge (evidence if they dispute it)
3. Return to Hachijo B1
4. Hand over keys and the ETC card (toll total so far: JPY 4,180)

Anchored to three hours before drop-off. Below, the same card shape serving rail,
airport transfers and ferries: a reference number, two timed ends, two places.
Only the labels change.

---

## Act 7. Reservations, tickets and passes

### H1. Dining reservation

Party size and time first, then the two numbers to quote (Resy confirmation and
the restaurant's phone), then the two things that actually cost you:

- **Leave by 19:05.** 22 min by taxi on a Thursday evening; walking is 48 min.
  Computed from wherever the timeline says they will be at 19:00, and re-anchored
  if the afternoon runs late.
- **Free to cancel until Tue 4 Nov 19:30**, then JPY 5,000 per person. Reminder
  at 12:00 on the 4th.

Plus the dress note ("no shorts") parsed from the confirmation, and the address
in both scripts.

### H2. Attraction ticket

One code to scan, two numbers to prove it. The entry QR big and central with the
timed window stated as a deadline ("enter by 14:30"), then a row per person
because venues scan individually, then the one practical warning that changes
what you wear: you will be barefoot and knee-deep in water, shorts are provided,
bags go in a locker.

The vendor's own QR payload is kept rather than re-encoded. Wallet passes carry
the entry window as their relevance time. Cached for offline, because Toyosu has
patchy signal underground.

### H3. Passes shelf

Every barcode on the trip in one place, sorted by when it is needed rather than
what it is: two boarding passes, a rail QR, two museum tickets, a lounge pass, an
insurance card. Each row shows its code format (Aztec, QR, PDF, code) and its
Wallet status. Filters for Today, Whole trip, Expired.

Anything with a barcode lands here automatically, from a `.pkpass`, a PDF, an
image or an HTML mail. Original payload stored; PDF fallback kept in case a
scanner refuses the screen.

---

## Act 8. Explore

### I1. Explore

Suggestions anchored to a real gap and a real place. Weather with the constraint
that matters (clear until 17:00, dark at 16:52), then one suggestion that fits
the actual free window, then sections for natural attractions, interest-filtered
things to do, and day trips. Every card carries travel time from where the
timeline says the traveller is, not from the city centre.

### I2. Place detail

Facts that decide, not a brochure. Cost, travel time from the actual base, time
needed, and the one piece of timing advice that separates a good visit from a
queue ("06:30 to 08:00 it is empty; by 10:00 it is shoulder to shoulder"). Then
why it was suggested, in plain language the traveller can disagree with, a map
with a worthwhile neighbour pinned, and things it pairs well with.

"Add to Day 5, 06:45" writes a **plan** block onto the timeline, not a booking:
no confirmation number, no counter reminder, just a placed intention that the gap
detector now respects.

### I3. Gap filler

Empty space, priced in minutes. The gap named by its two fixed edges ("checked in
13:00 Granvia to leave 19:05 for Kichisen"), filters for walkable, indoors and
free, then four options each scored by how much slack is left after getting back:

| Option | Verdict |
| --- | --- |
| Tofuku-ji gardens | Back with 55 min spare |
| Nishiki market walk | Back with 1 h 40 m spare |
| Kyoto Tower onsen | Back with 1 h 20 m spare |
| Fushimi Inari upper trail | Tight: 20 min spare, and dark by 16:52 |

Found by differencing consecutive timeline events, solved round-trip against both
edges, and checked against opening hours, sunset and weather. A gap is offered
once. "Keep it free" is a legitimate plan.

---

## Act 9. Reminders, calendar and offline

The rule matrix behind this act is documented in [REMINDERS.md](REMINDERS.md);
export and pass mechanics in
[CALENDAR-AND-PASSES.md](CALENDAR-AND-PASSES.md).

### J1. Notifications

The lock screen is the real interface. Most days Linger is only ever seen here: a
live activity for the flight ("Boards 23:05, gate 74A, T-18m"), then reminders
that each carry the one fact needed to act without opening the app. Settings
shown inline: quiet hours 22:00 to 07:00 local, timezone follows the trip and not
the phone, critical alerts limited to gate changes and delays.

### J2. Calendar export

Subscribe beats sync, and both are offered. A read-only `webcal://` feed per trip
that updates itself, or direct writes into a calendar named `Linger . Tokyo` with
per-category toggles. The exported event is shown literally, `TZID` on both ends
of a red-eye and alarms included, so there is no surprise about what lands in a
shared work calendar. Linger only edits events it created and preserves the
traveller's own edits to title and notes.

### J3. Offline

Two honest lists rather than a spinner. What is genuinely on the device: 7 passes,
12 documents, the full itinerary, a 38 MB offline map of Kyoto, 3 wifi passwords,
11 locally scheduled reminders. What is stale, with the timestamp of its last
truth: live flight status (last checked 23:41), Explore (cached only), new
confirmations (3 queued server side). Plus the one button a taxi driver needs:
show my hotel address in Japanese.

The trip pack downloads automatically 48 h before departure and again on any
change while on wifi.

---

## Act 10. Share, documents, wrap-up

### L1. Travellers and sharing

Per-segment membership, because trips diverge. Two travellers with different
return flights, shown segment by segment rather than as one shared blob. Reminder
sets are kept per traveller, so Priya gets her own check-in push for her own
flight and nobody gets the other's.

A read-only link that explicitly names what it excludes (codes, prices,
documents) and when it expires. "Send today's plan" and a printable packet: one
page per day with addresses in both scripts, for the phone-runs-out case.

### L2. Document vault

The vault earns its place by being checkable. Identity and permits, cover, then
the source file behind every booking, grouped so the airport questions and the
insurance questions are in different piles. The passport row carries the
destination rule, not just the expiry date ("6-month rule OK"). The IDP is
flagged as a physical item. Encrypted on device behind biometrics, never uploaded
unless shared.

### L3. Post-trip

The trip is over; the paperwork is not. Spend by category from receipts that were
already in the mailbox, converted at the day's rate. Then the three items still
owed money: possible care costs for the 2 h 10 m delay, an unused late-checkout
charge, an open insurance claim for the delayed bag, each with its receipt
attached. Export as PDF or CSV, and "reuse this trip" to keep the shape, notes
and places while dropping the dates and bookings.
