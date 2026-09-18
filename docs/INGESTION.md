# Ingestion

The feature the whole app rests on: a confirmation lands somewhere, and a trip
quietly becomes more complete. Eight stages, two of which are allowed to ask the
traveller a question. Everything a screen shows later is derived from what
happens here.

```
  sources        01 Watch
                    |
                 02 Screen  ---- duplicate / oversize / malware --> rejected
                    |
                 03 Classify ---- not travel --------------------> rejected
                    |
                 04 Extract  (template first, model fallback)
                    |
                 05 Resolve  (codes, geocode, timezone, schedule)
                    |
                 06 Match    (trip, booking, version)
                    |
                 07 Materialise (events, reminders, calendar, passes, pins)
                    |
                 08 Decide   ---- all confident ----> applied silently + logged
                                \- any soft field --> review queue
                                \- conflict --------> change card
```

## 01. Watch

A trigger is a message id or a file, never a mailbox scrape.

- Gmail `users.watch` push into Pub/Sub
- Microsoft Graph subscription webhook
- Inbound SMTP on `*@in.linger.app`, accepted only from verified forwarders
- Share-sheet upload: PDF, image, `.ics`, `.pkpass`
- Camera scan of a paper voucher
- Scheduled re-poll of live schedules (T-7 d, T-2 d, T-1 d, then hourly inside
  6 h of departure)

## 02. Screen

Roughly 40 percent of intake is resolved here without a model: structured formats
and exact duplicates.

- Dedupe on message id plus SHA-256 of the normalised body
- Size, MIME and malware guard, 20 MB cap
- Split attachments into candidate parts
- OCR scans and photos (Tesseract, with a vision model fallback)
- Direct parse for `.ics` and `.pkpass`, no LLM needed

## 03. Classify

Intent matters more than category: a change email must update a booking, not
create a second one.

- Is this a travel confirmation at all?
- Category: flight, stay, car, rail, dining, ticket, insurance, other
- Vendor fingerprint from sender domain plus layout hash
- Intent: new, change, cancellation, reminder, receipt
- Language and locale, which decides how dates parse

## 04. Extract

Templates are cheap, exact and auditable. The model is the safety net and the
thing that keeps working when a vendor redesigns.

- Vendor template first: deterministic, target around 200 of them
- Schema-constrained model extraction as fallback
- Confidence per field, not one score for the message
- Never invents a field: absent stays null
- Barcodes kept as received bytes, never re-encoded

## 05. Resolve

This is where "11:35" stops being ambiguous.

- IATA, station and port codes to canonical places
- Geocode addresses, derive timezone from coordinates
- Airline plus flight number plus date to a schedule lookup, which fills
  terminal, gate, aircraft, duration and on-time history
- Currency normalisation, traveller-name and loyalty matching

Every local time is stored with its zone.

## 06. Match

The idempotency key is why a forwarded-three-times email produces one booking,
and why a rebooking supersedes rather than duplicates.

- Find the Trip: a Booking joins an existing one unless the traveller is Home
  in between. Returning Home with a later start means a new Trip
- No trip: create one, named from the destination
- Upsert on either match: same Seller and Reference, or same journey per Segment
  `(operator, start place, start local date, traveller)` with the service number
  as a tiebreaker. Reference catches an amendment; journey catches a duplicate
  from a second seller
- First record seen is primary. Later records fill empty fields only, never
  overwrite
- A field the Booking cannot work without, still missing after the merge, is
  asked about rather than guessed
- Revisions are versioned v1, v2, v3; nothing is overwritten
- Cross-booking conflict scan (buffers, overlaps, impossible sequences)

## 07. Materialise

Everything downstream is generated. Change one booking and the reminders,
calendar entries and pins are re-derived, not patched by hand.

- Booking to timed events on the trip timeline
- Derived events: leave-by, check-in window, cancellation deadline, gap
- Reminder rows from the [rule matrix](REMINDERS.md)
- Calendar rows plus the per-trip ICS feed
- Passes, map pins, document vault entries

## 08. Decide

Silence is the goal and the risk. The log makes every silent apply reversible.

| Outcome | Condition |
| --- | --- |
| Applied silently, logged | Every critical field (date, time, code, place) above threshold, no conflict |
| Review queue | Any critical field below threshold, or no unambiguous trip match |
| Change card | Conflict with an existing booking, or a downstream buffer breach |
| Dropped | Not travel. The sender is remembered so it is not asked again. |
| Stored raw | Parse failure. Offered as a pre-filled manual entry. |

## Guardrails

An app that reads your mail earns trust by being boring about it.

| Guardrail | Commitment |
| --- | --- |
| Thresholds | Silent apply needs every critical field at high confidence. One soft field is enough to ask. |
| Never destructive | Ingestion can add and supersede. It cannot delete a booking, a document, or anything edited by hand. |
| Always auditable | Trip Activity shows every applied change, its source message, and a one-tap undo for 30 days. |
| Source retained | The original PDF or image stays attached to the booking. If the parse was wrong, the truth is still visible. |
| Never reads identity | The pipeline does not extract a passport number, visa, date of birth or personal phone number, even when a vendor includes one. Identity data is typed by the traveller on their device or it does not exist, which keeps the server permanently free of it. That is a far easier promise to keep than deleting it afterwards. |
| No data out | No vendor, airline or hotel is contacted on the traveller's behalf. Schedule lookups are read only and carry no traveller data. |
| Template rot watched | A vendor redesign shows up as a confidence drop across one sender, which alerts the template owner before users notice. |

## Known risks

- **Mailbox scope review.** Gmail restricted-scope verification takes weeks and
  an annual security assessment. The alias path ships first and is never
  deprecated, so mailbox access is never on the critical path.
- **Silent wrongness.** A confidently wrong time is the one unrecoverable
  failure. Mitigated by thresholds on critical fields only, the source always one
  tap away, and 30-day undo.
- **Barcode fidelity.** A pass that will not scan is worse than no app. Original
  bytes only, with a PDF fallback, and never a regenerated code.
