# Reminder rules

Twenty-one rules, all derived from booking fields rather than set by hand. This
table is the contract: if a booking carries the field, the reminder exists, and
if the field moves, so does the reminder.

| Category | Rule | Anchored to | Offset | Channel | Only when | Behaviour |
| --- | --- | --- | --- | --- | --- | --- |
| Flight | Check-in opens | Airline check-in window opens | At T-24 h, or the airline's own window | Push | Not already checked in | Cancels the moment check-in is detected |
| Flight | Check-in closing | Online check-in cutoff | T-4 h before cutoff | Push, critical | Still not checked in | Escalates: this one breaks quiet hours |
| Flight | Leave for the airport | Departure time | Drive time + airport buffer + 15 min slack | Push, live activity | Location permission granted | Recomputed every 10 min from live traffic |
| Flight | Bag drop closes | Counter close time | T-90 min | Push | Checked bags on the booking | Only for hold luggage |
| Flight | Gate or delay change | Airline or schedule feed event | Immediately | Push, critical | Always | Ignores quiet hours entirely |
| Flight | Seat not selected | Free-seat-selection deadline | T-36 h | Push | Seat is null or auto-assigned | Silent if a seat already exists |
| Flight | Connection at risk | Recomputed arrival vs next departure | On any change | Push | Gap below minimum connection + 20 min | Fires once per change, not per poll |
| Stay | Free cancellation ends | Policy deadline in the confirmation | T-30 h, at 18:00 local | Push | Refundable rate | The single most valuable reminder in the app |
| Stay | Arrival window | Check-in time + geofence | On arrival, or at check-in time | Push | Self check-in or a door code | Opens the arrival card with the code |
| Stay | Checkout tomorrow | Check-out time | Day before, 20:00 local | Push | Always | Mentions the late-checkout price |
| Stay | Pack up | Check-out time | T-60 min | Push | No late checkout booked | Suppressed if checkout is after 12:00 |
| Car | Pickup | Pickup time | T-60 min | Push | Always | Includes the counter location, not just the branch |
| Car | Return | Drop-off time | T-3 h | Push | Always | Mentions fuel only when a fuel policy was actually stated |
| Car | Free change ends | Vendor change deadline | T-6 h before the deadline | Push | Changeable reservation | |
| Rail | Platform and boarding | Departure time | T-45 min | Push | Always | Platform data arrives late, so the push waits for it |
| Dining | Cancellation fee starts | Policy deadline | T-6 h, at 12:00 local | Push | A fee applies | |
| Dining | Leave for dinner | Reservation time | Travel time + 10 min | Push | Always | Re-anchors if the afternoon overruns |
| Ticket | Timed entry | Entry window start | T-90 min and T-30 min | Push | Timed admission | Second one only if not yet scanned |
| Trip | Trip pack download | Departure date | T-48 h | Silent | On wifi | Repeats on any change while on wifi |
| Trip | Visa and passport check | Departure date | T-30 d and T-7 d | Push | Passport expiry inside destination rules | The six-month rule, per destination |
| Trip | Post-trip receipts | Return date | T+1 d, 10:00 | Push | Any expense parsed | Opens the wrap-up screen |

## Scheduling mechanics

**Anchored, not absolute.** Every reminder stores an offset against a booking
field. Move the booking and the reminder moves; it is never a fixed timestamp.

**Zone of the place.** Offsets resolve in the timezone of the airport, hotel or
restaurant, not the phone. A 23:59 deadline in Kyoto is 20:29 in Bengaluru, and
both are shown.

**Scheduled on device.** Local notifications, written at ingestion time. They
fire in airplane mode, on a dead server, and in a foreign country with no data.

**Quiet hours.** 22:00 to 07:00 local by default. Gate changes, delays and the
check-in cutoff are allowed through; nothing else is.

**One per fact.** A delay that shifts five reminders sends one summary, not five
pushes. Re-anchoring is silent.

**Snooze is a decision.** Snoozing a leave-by reminder recomputes rather than
repeats: the next one reflects traffic 15 minutes later, which may be a different
number.

**Never in the past.** A deadline that has already passed produces no reminder,
silently. The JetSMART confirmation in `docs/samples/` says "Free cancellation
expires 24 hours after confirmation", so importing it a week later would
otherwise schedule something for last Tuesday. The fact stays on the Booking so
a screen can say "free cancellation expired"; only the notification is dropped.

**Conditions are re-evaluated.** A reminder whose condition stops holding is
voided, not fired. Select a seat and the "seat not selected" reminder disappears
without a notification.
