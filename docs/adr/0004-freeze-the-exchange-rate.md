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

**A rate source is now a dependency.** Settled below, under "Amended after the
research".

**It needs the network exactly once per Booking**, at ingestion, which suits an
offline-first app. Once stored, every total is computed on the device with no
connection.

**A missing rate is a missing rate.** If the source is unreachable when a
Booking is saved, the rate stays empty and that currency is shown on its own
line rather than folded into the estimate. Fetching it later is allowed;
guessing it is not.

## Amended after the research

See [the research](../research/historical-exchange-rates.md), which changed
three things about this decision. The core of it stands: a frozen rate beats a
live one, for the reasons above. What changed is what a frozen rate is worth,
and who gets the last word.

**There is no single correct rate to freeze.** Fifty three official sources for
one USD/INR date spread 1.75%, which is wider than the gap between any two
sources considered. Picking a source is therefore a smaller decision than it
looked, and how the number is presented is a larger one.

**The traveller overrules the app.** A conversion is now either the app's
estimate, carrying the day the rate was observed and whose number it is, or the
traveller's own figure taken off their card statement. Theirs survives every
later estimate, a reinstall and a re-parse of the same email. This is a stronger
version of what this ADR wanted: their own figure cannot drift at all, where a
frozen estimate merely drifts slowly.

The app never asks for it. It is editable and the traveller corrects it if and
when they care.

**The observation date is not the booking date.** A Saturday purchase converts
at the previous trading day's rate, because that is the last day anything was
quoted. The two are stored separately, and the observed date is what is shown.
Presenting a booking date over a weekday rate is a quiet lie, and it is exactly
the lie the obvious free option tells: Frankfurter's blended endpoint stamps a
response with the date requested rather than the date anything was measured, so
USD/COP for Saturday 15 June 2024 comes back as 4119.52 when Colombia's own
central bank says 4151.55.

**The source, settled.** A trimmed ECB daily table ships inside the app, 119 KB
gzipped for the currencies the first regions need, which makes the common case
work with no network at all and nothing leaving the phone. Currencies it does
not carry, the Colombian peso among them, are fetched from Frankfurter **with a
single provider pinned**, never from the blended default. No keyed API is used:
a key inside an Android package can be decompiled out of it, and the free tiers
of the three brands that dominate the market are non-commercial anyway.

**A missing rate is still a missing rate,** unchanged from above. That currency
keeps its own line and the combined figure is withheld rather than computed
without it.