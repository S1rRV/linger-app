# Roadmap

Sequenced so that each phase is usable on its own and the riskiest dependency,
mailbox access, is never on the critical path.

## Phase 0. Prove the spine (2 weeks)

- Trip, Booking, TimelineEvent, Reminder tables
- Manual entry for all seven categories
- Day timeline and trip home
- Local notifications from the rule matrix
- Per-trip ICS feed

No parsing at all. If the model is wrong, everything after this is wasted.

Storyboard frames: D1, D2, D4, J1, J2.

## Phase 1. MVP, the inbox trick (6 to 8 weeks)

- Forwarding alias with inbound SMTP
- 15 vendor templates: 6 airlines, 5 hotel groups, Airbnb, 3 OTAs
- Schema-constrained model fallback with per-field confidence
- Review queue and the confirm screen
- Flights and stays end to end, including wifi and door codes
- Pass storage from `.pkpass` and PDF barcodes
- Offline trip pack

Alias before OAuth: it ships without a Google review cycle and proves the value
on its own.

Storyboard frames: A2, C1, C2, C3, E1, E3, F1, F2, J3.

## Phase 2. Complete the categories (8 weeks)

- Gmail and Microsoft Graph connect, with historical backfill
- Cars, rail, dining, tickets, insurance
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

1. **Which platform first, or both?** The offline and Wallet stories differ
   enough to matter.
2. **Alias-only at launch, or hold for Gmail verification?** Alias ships months
   earlier and needs no review, but mailbox connect is what finds past trips and
   catches changes without a forward rule.
3. **One region first?** Vendor templates and IDP-style derived requirements are
   regional work. India plus Japan plus Europe is a different template set from
   US domestic.
4. **Is rail a first-class category, or does it live under "other"?** It matters
   for Japan, India and Europe, and much less for the US.
