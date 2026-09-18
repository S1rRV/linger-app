# Android first, despite owning a Mac

The first build targets Android only. The domain layer is written so an iOS
shell can be added later without rewriting it. This is worth recording because
the author owns a Mac, so a reader could reasonably assume iOS was skipped by
oversight rather than on purpose.

## Why

The author carries an Android phone. An iOS build would be untestable in the one
situation the product exists for, which is a real trip.

Apple's free provisioning expires a self-installed app after seven days, so
using it personally means either re-signing weekly or paying the annual
developer fee. Android sideloading is free and does not expire.

Android is also the harsher environment for the product's core promise. Its
background restrictions are documented in
`docs/research/android-alarm-reliability.md`: an app not opened for about eight
days drops into a low-power state allowing one alert per day, which lands
exactly on the pattern of booking a trip, ignoring the app, then depending on it
on travel day. Proving reminders there retires more risk than proving them on
iOS, which the same research found materially more reliable.

## Considered options

**Both platforms at once, sharing a domain layer.** Rejected for the first build
only. It roughly doubles the interface work at the stage where the design is
most likely to be wrong, so every correction costs twice.

**A shared UI toolkit across both platforms.** Rejected because the surfaces
that make this app distinctive are exactly the platform-specific ones: exact
alarm scheduling, wallet passes, calendar writes, live activities. A shared UI
layer still requires native work for all of them, so it would mean debugging an
abstraction on top of the hard parts with no second platform yet in play to pay
for it.

## Consequences

The domain layer must not import anything platform-specific. This is a
constraint on the first build even though nothing depends on it yet.

iOS is deferred, not abandoned. Revisit once the model has stopped changing.
