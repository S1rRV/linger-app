# State machines

Four lifecycles carry the whole product. Writing them down is what stops a
forwarded email becoming a duplicate booking, and a delay becoming eleven
contradictory notifications.

## IngestionItem

From "something arrived" to "the trip changed".

```
received -> screened -> classified -> extracted -> matched -> auto_applied
                |            |            |           |
                v            v            v           +----> needs_review -> applied
             rejected     rejected    needs_review                        \-> rejected
                                                     any -> superseded
```

| States | Meaning |
| --- | --- |
| `received` | Hashed and stored raw |
| `screened` | Deduped, scanned, attachments split |
| `classified` | Category, intent and vendor known |
| `extracted` | Fields plus per-field confidence |
| `matched` | Target trip and booking identified |
| `auto_applied` | Every critical field confident: applied silently |
| `needs_review` | Waiting on a human |
| `applied` | Confirmed by the traveller |
| `rejected` | Not travel, duplicate, or dismissed |
| `superseded` | A newer revision for the same key won |

Transitions:

- `received -> rejected` on a duplicate hash, oversize payload, or malware
- `classified -> rejected` when it is not a travel confirmation; the sender is
  remembered
- `extracted -> needs_review` when any critical field is below threshold
- `matched -> auto_applied` when every critical field is confident and there is
  no conflict
- `needs_review -> applied` when the traveller confirms; corrections feed the
  vendor template
- `needs_review -> rejected` after 30 days untouched, or on dismissal
- `any -> superseded` when a later message for the same idempotency key arrives
- `applied -> reverted` on undo within 30 days, restoring the prior version

## Flight check-in

The one state machine users can feel.

```
locked -> open -> reminded -> checked_in -> pass_stored -> boarding -> flown
             \-> closed_missed
   any -> rebooked | cancelled
```

| State | Meaning |
| --- | --- |
| `locked` | Before the airline's window |
| `open` | `opens_at` reached |
| `reminded` | Push sent, still not checked in |
| `checked_in` | Detected from the airline's mail, or declared by the traveller |
| `pass_stored` | Barcode cached offline |
| `boarding` | Live activity running |
| `flown` | Arrived |
| `closed_missed` | Cutoff passed without check-in |
| `rebooked` | A new segment supersedes this one |
| `cancelled` | Flight cancelled |

Transitions:

- `locked -> open` at T-24 h, or the carrier's own window when it differs
- `open -> reminded` at T-12 h, then a critical alert at T-4 h before the cutoff
- `reminded -> checked_in` when the airline confirmation is parsed, or the
  traveller taps "I checked in another way"
- `checked_in -> pass_stored` when a `.pkpass` or barcode is captured
- `open -> closed_missed` when the cutoff passes. The app stops nagging and
  offers the airport desk instead.
- `any -> rebooked` when a new segment for the same trip supersedes this one

## Booking lifecycle

Shared by every category.

```
draft -> confirmed -> changed
              |  \-> at_risk -> confirmed
              |
              +-> cancelled
              +-> completed
```

| State | Meaning |
| --- | --- |
| `draft` | Manual entry, incomplete |
| `confirmed` | Has a code and times |
| `changed` | v2 written, v1 kept and linked |
| `at_risk` | A dependency moved and a buffer went below threshold |
| `cancelled` | By vendor or by the traveller |
| `completed` | Its last event has passed |

Transitions:

- `confirmed -> changed` on a change email or feed event; downstream is
  re-derived, not patched
- `confirmed -> at_risk` when a neighbouring booking moves and the buffer drops
  below its threshold
- `at_risk -> confirmed` when the buffer recovers, silently
- `any -> cancelled`: refund and expense rows stay, reminders are voided
- `confirmed -> completed` by a nightly sweep, plus the post-trip roll-up

## Reminder

Cheap to create, cheap to move.

```
scheduled -> fired -> acknowledged
     |          \-> snoozed -> scheduled
     +-> void -> scheduled   (re-anchor: the common case, and silent)
```

| State | Meaning |
| --- | --- |
| `scheduled` | Written to the device |
| `fired` | Delivered |
| `acknowledged` | Tapped |
| `snoozed` | Recomputed, not repeated |
| `void` | Anchor moved or condition no longer holds |

Transitions:

- `scheduled -> void -> scheduled` when the anchor moves: the old notification is
  cancelled and a new one written. This is the common case and it is silent.
- `fired -> snoozed`: a leave-by reminder recomputes from current traffic rather
  than adding a flat 15 minutes
- `scheduled -> void` when the booking is cancelled, or its condition stops
  holding (for example a seat gets selected, so the "select a seat" reminder
  disappears)
