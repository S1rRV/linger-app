# The Linger app

A day view of one trip, and nothing else yet.

## Running it

From the repository root, on either machine:

```
./run.sh
```

It works out where it is running and does the right thing.

### From the phone itself, in Termux

The one-time setup:

```
pkg update && pkg install nodejs-lts git
git clone https://github.com/S1rRV/linger-app
cd linger-app
./run.sh
```

Take `nodejs-lts` rather than `nodejs`. The plain package tracks the newest
release, currently Node 26, which Metro does not always support yet.

There is no QR code in this mode and no wifi to match, because the server and
Expo Go are the same device. The script serves on loopback, prints
`exp://127.0.0.1:8081` and nudges Expo Go into the foreground pointed at it. If
that nudge does not land, open Expo Go and use "Enter URL manually".

Termux has no JDK 21, so the trip export is skipped and the committed
`assets/trip.json` is used. That is the same file the build produces, so
nothing is lost until the domain changes, at which point the export has to be
rerun on a machine with Java and committed.

Keep Termux in the foreground or give it a wake lock. Swiping it away stops the
server.

### From a computer

Scan the QR code with Expo Go on a phone on the same wifi. If the phone and the
computer cannot see each other, `./run.sh --tunnel` works from anywhere and is
slower.

## What this is, and what it is not

**The Kotlin domain decides what a trip is.** `gradle exportTrip` runs the
domain over the itinerary in `docs/samples/` and writes `assets/trip.json`. This
app renders that file and computes nothing of its own: no date arithmetic, no
grouping, no titles. A screen that worked those out for itself could disagree
with the tests that define a trip, and it would be the screen that was wrong.

So if a time reads wrong on the phone, the fix belongs in `core` with a test,
and `./run.sh` will carry it through.

**Expo Go cannot do the hard parts.** See
`docs/research/expo-go-constraints.md`. Share-sheet intake is structurally
impossible there, `expo-calendar` is excluded, and the `setAlarmClock` path the
reminder design rests on is not in SDK 57. More importantly, permissions and
battery exemptions belong to Expo Go's package rather than to Linger's, so any
reminder reliability measured here is Expo Go's and not ours.

This app is therefore for looking at screens. Nothing about reminders, intake
or the calendar can be judged from it, and a development build is the next step
when those matter.

## Files

| | |
| --- | --- |
| `App.tsx` | The day view. All of it |
| `trip.ts` | The shape the domain sends, mirroring `TripJson.kt` |
| `assets/trip.json` | Generated. Do not edit by hand |
