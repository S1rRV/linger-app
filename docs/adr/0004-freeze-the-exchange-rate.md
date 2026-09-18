# Freeze the exchange rate at purchase

A Booking's Amount is stored in the currency the vendor stated. Alongside it we
store the rate to the account's home currency, captured once when the Booking is
first saved and never refetched. A Trip's combined total is computed from those
frozen rates.

Worth recording because using a live rate is the obvious thing, and it produces
a number that will not sit still.

## Why not a live rate

A Trip's total would change every morning. Last December's trip would cost a
different amount each time it was opened, which makes the figure useless for the
one thing a past total is for: comparing trips, and remembering what something
cost. Worse, it would be silently wrong rather than visibly wrong.

There is no correct live answer anyway. The rate that actually applied is the
card issuer's on the day it settled, which we cannot see. Any rate we show is an
approximation, so it should at least be a stable one anchored to a stated date.

## What this is not

It is not a claim about what was charged. The combined total is labelled an
estimate, with its rate and date shown, because the traveller's bank applied a
different number and added a fee.

Per-Booking amounts are never converted for display. They appear in the
vendor's currency, as supplied. Only the Trip-level combined total uses the
frozen rates, and it is always marked as an estimate.

## Consequences

**A rate source is now a dependency.** "Google" is not callable: there is no
public Google FX API, and scraping it is neither permitted nor stable. A real
historical daily source is needed. The ECB reference set, reachable through
Frankfurter, is free and dated but euro-based and covers only major currencies.
A commercial feed covers more. Either way this is a decision that has not been
made, and it is recorded as an open question in the roadmap.

**It needs the network exactly once per Booking**, at ingestion, which suits an
offline-first app. Once stored, every total is computed on the device with no
connection.

**A missing rate is a missing rate.** If the source is unreachable when a
Booking is saved, the rate stays empty and that currency is shown on its own
line rather than folded into the estimate. Fetching it later is allowed;
guessing it is not.
