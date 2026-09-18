# Sample booking documents

Real confirmations from the author's December 2026 Colombia trip, used to build
and test the parser. **The raw files are deliberately not committed**, see
`.gitignore` in this directory. They contain full names, an email address,
booking references, ticket numbers and a reservation security code.

To work on the parser you need the originals locally. Ask the author.

Redacted fixtures safe to commit go in `fixtures/`.

## What the set covers

| File | Vendor | Kind | Why it is interesting |
| --- | --- | --- | --- |
| `expedia-arajet-ewr-mde.eml` | Expedia, Arajet | Flight | Two travellers, four legs, two layovers, no times marked with a zone |
| `expedia-jetsmart-mde-ctg.eml` | Expedia, JetSMART | Flight | Per-traveller ticket numbers, explicit fare rules, non-refundable |
| `sixt-car-medellin.eml` | Sixt | Car | Ships a `.ics` attachment and a PDF voucher. Downtown branch, not airport |
| `booking-alamo-car-cartagena.eml` | Booking.com, Alamo | Car | Broker and supplier differ. Details sit in prose, not a table |
| `sixt-voucher.pdf` | Sixt | Car | PDF attachment of the Sixt email |

No accommodation booking exists in this set.
