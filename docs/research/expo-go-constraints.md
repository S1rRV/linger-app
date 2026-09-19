# Expo Go constraints for Linger

Research date: 2026-09-19. Expo SDK 57, React Native 0.86.

Question: if Linger is rebuilt as a React Native / TypeScript app so the owner can
run it on his own Android phone through Expo Go, which of the product's five
promises survive, and which of them need a development build instead?

Every claim below carries a URL. Sources are limited to docs.expo.dev,
expo.dev/changelog, reactnative.dev, developer.android.com, the npm registry,
and github.com/expo/expo where the source tree and changelog are the
authoritative record. Anything the docs leave open is collected in "Ambiguities
and unconfirmed items" at the end.

Companion document: `docs/research/android-alarm-reliability.md`, which
establishes what the Android platform itself will and will not do. This document
only asks what Expo Go can reach of that.

## Sources consulted

Expo documentation, fetched as Markdown by appending `.md` to the page path, a
mechanism the docs themselves advertise
([llms.txt](https://docs.expo.dev/llms.txt)):

| Page | URL |
| --- | --- |
| SDK reference index | https://docs.expo.dev/versions/latest/ |
| Expo Notifications | https://docs.expo.dev/versions/latest/sdk/notifications/ |
| Expo Notifications, unversioned | https://docs.expo.dev/versions/unversioned/sdk/notifications/ |
| Expo Calendar | https://docs.expo.dev/versions/latest/sdk/calendar/ |
| Expo SQLite | https://docs.expo.dev/versions/latest/sdk/sqlite/ |
| Expo DocumentPicker | https://docs.expo.dev/versions/latest/sdk/document-picker/ |
| Expo FileSystem | https://docs.expo.dev/versions/latest/sdk/filesystem/ |
| Expo Sharing | https://docs.expo.dev/versions/latest/sdk/sharing/ |
| Async Storage | https://docs.expo.dev/versions/latest/sdk/async-storage/ |
| Third-party libraries in Expo Go | https://docs.expo.dev/versions/latest/sdk/third-party-overview/ |
| Set up your environment | https://docs.expo.dev/get-started/set-up-your-environment/ |
| Start developing | https://docs.expo.dev/get-started/start-developing/ |
| Development builds introduction | https://docs.expo.dev/develop/development-builds/introduction/ |
| Development builds FAQ | https://docs.expo.dev/develop/development-builds/faq/ |
| Using libraries | https://docs.expo.dev/workflow/using-libraries/ |
| Store data | https://docs.expo.dev/develop/user-interface/store-data/ |
| Linking into your app | https://docs.expo.dev/linking/into-your-app/ |
| app config reference | https://docs.expo.dev/versions/latest/config/app/ |
| Expo CLI | https://docs.expo.dev/more/expo-cli/ |
| Tools for development | https://docs.expo.dev/develop/tools/ |
| Glossary of terms | https://docs.expo.dev/more/glossary-of-terms/ |
| Expo Go version mismatch | https://docs.expo.dev/troubleshooting/expo-go-version-mismatch/ |
| Continuous Native Generation | https://docs.expo.dev/workflow/continuous-native-generation/ |
| What you need to know about notifications | https://docs.expo.dev/push-notifications/what-you-need-to-know/ |

Other primary sources:

| Source | URL |
| --- | --- |
| Expo SDK 57 changelog | https://expo.dev/changelog/sdk-57 |
| React Native 0.86 release | https://reactnative.dev/blog/2026/06/11/react-native-0.86 |
| React Native environment setup | https://reactnative.dev/docs/environment-setup |
| npm registry metadata for `expo` | https://registry.npmjs.org/expo |
| `expo-notifications` CHANGELOG | https://github.com/expo/expo/blob/main/packages/expo-notifications/CHANGELOG.md |
| `ExpoSchedulingDelegate.kt`, sdk-57 | https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/java/expo/modules/notifications/service/delegates/ExpoSchedulingDelegate.kt |
| `ExpoSchedulingDelegate.kt`, main | https://github.com/expo/expo/blob/main/packages/expo-notifications/android/src/main/java/expo/modules/notifications/service/delegates/ExpoSchedulingDelegate.kt |
| `expo-notifications` AndroidManifest | https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/AndroidManifest.xml |
| Expo Go Android manifest template | https://github.com/expo/expo/blob/sdk-57/template-files/android/AndroidManifest.xml |
| `DevicePushTokenAutoRegistration.fx.ts` | https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/src/DevicePushTokenAutoRegistration.fx.ts |
| `warnOfExpoGoPushUsage.ts` | https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/src/warnOfExpoGoPushUsage.ts |
| expo/expo PR #49062 | https://github.com/expo/expo/pull/49062 |

Pages that could not be reached from this environment: none. Everything cited
here was fetched successfully.

## 1. The current Expo SDK

| Fact | Value | Source |
| --- | --- | --- |
| Current SDK | 57.0.0 | [SDK reference index](https://docs.expo.dev/versions/latest/) |
| Release date | June 30, 2026 | [SDK 57 changelog](https://expo.dev/changelog/sdk-57) |
| React Native | 0.86 | [SDK reference index](https://docs.expo.dev/versions/latest/), [SDK 57 changelog](https://expo.dev/changelog/sdk-57) |
| React | 19.2.3 | [SDK reference index](https://docs.expo.dev/versions/latest/) |
| React Native Web | 0.21.0 | [SDK reference index](https://docs.expo.dev/versions/latest/) |
| Minimum Node | 22.13.x | [SDK reference index](https://docs.expo.dev/versions/latest/) |
| Latest patch on npm | `expo@57.0.24`, published 2026-09-18 | [registry.npmjs.org/expo](https://registry.npmjs.org/expo) |
| Next SDK | 58.0.0-preview.3, unreleased | [registry.npmjs.org/expo](https://registry.npmjs.org/expo) |

React Native 0.86 was released on June 11, 2026, and is described as "the second
React Native release with no user-facing breaking changes"
([RN 0.86 release](https://reactnative.dev/blog/2026/06/11/react-native-0.86)).
The SDK 57 changelog frames the whole release around it: "This is a small,
focused release: it brings React Native 0.86 to Expo"
([SDK 57 changelog](https://expo.dev/changelog/sdk-57)).

### How "current" was established

Three independent checks, because a docs page alone can lag:

1. The versioned docs root renders "Reference (v57.0.0)" and lists SDK 57.0.0
   ahead of 56.0.0, 55.0.0 and 54.0.0
   ([SDK reference index](https://docs.expo.dev/versions/latest/)).
2. The npm `dist-tags` for the `expo` package give `latest: 57.0.24` and
   `next: 58.0.0-preview.3`, with 57.0.24 published 2026-09-18 and the newest
   58 preview published 2026-09-16
   ([registry.npmjs.org/expo](https://registry.npmjs.org/expo)). A preview tag
   on `next` means 58 is staged but not shipped.
3. `https://expo.dev/changelog/sdk-58` returns HTTP 404, while
   `https://expo.dev/changelog/sdk-57` returns the release post. Expo publishes
   a changelog post per SDK release, so the absence of an SDK 58 post
   corroborates the npm tags.

This matters for Linger more than usual, because the one `expo-notifications`
feature that would make exact reminders workable ships in SDK 58, not 57. See
section 3.

## 2. Expo Go versus a development build

### Expo's own framing

The distinction is stated the same way on several pages, and it is not neutral.

> Expo Go is a playground for students and learners to quickly try out React
> Native. It does not include all of the native code required to support every
> library, so it's limited and not useful for building production-grade
> projects.
> ([Using libraries](https://docs.expo.dev/workflow/using-libraries/))

> [Expo Go] is a playground app for students and learners to get started
> quickly. It comes with a fixed set of native libraries built in, so you can
> write JavaScript code and see changes instantly without building a native app
> yourself. A development build is a fully featured development environment for
> working on your production-grade Expo apps.
> ([Development builds FAQ](https://docs.expo.dev/develop/development-builds/faq/))

> The Android and iOS app that serves as a sandbox for learning and
> experimenting with React Native. Due to its limitations (such as the inability
> to include custom native code), it's not recommended for building and
> distributing production apps.
> ([Glossary](https://docs.expo.dev/more/glossary-of-terms/))

> A **development build** is essentially **your own version of Expo Go** where
> you are free to use any native libraries and change any native configuration.
> ([Development builds introduction](https://docs.expo.dev/develop/development-builds/introduction/))

### The mechanism, which is the part that predicts everything else

The FAQ explains why the boundary falls where it does, and this single paragraph
answers most of the questions below:

> The **native app** is what you install on your device. Expo Go is a pre-built
> native app that works like a playground, it can't be changed after you install
> it. To add new native libraries or change things like your app name and icon,
> you need to build your own native app (a development build). ... However, only
> APIs and libraries that were bundled in the **native app** can be used.
> ([Development builds FAQ](https://docs.expo.dev/develop/development-builds/faq/))

Two consequences follow directly, and the docs state both:

- **Anything that lives in `AndroidManifest.xml` is out of reach.** Config
  plugins "allow you to configure various properties that cannot be set at
  runtime and require building a new app binary to take effect", a sentence
  repeated verbatim on the `expo-sharing`, `expo-file-system` and
  `expo-document-picker` pages
  ([Expo Sharing](https://docs.expo.dev/versions/latest/sdk/sharing/)).
- **Even a custom URL scheme needs a build.** "After adding a custom scheme to
  your app, you need to create a new development build"
  ([Linking into your app](https://docs.expo.dev/linking/into-your-app/)).
  Android App Links are called out as outright impossible: "This is impossible
  with Expo Go due to the aforementioned native code immutability"
  ([Development builds FAQ](https://docs.expo.dev/develop/development-builds/faq/)).

Continuous Native Generation says the same from the other side: everything Expo
ships supports existing React Native projects, and "the only exception is the
Expo Go app, which can load arbitrary React Native projects only if they include
JavaScript fallbacks for native code absent in the Expo Go runtime"
([CNG](https://docs.expo.dev/workflow/continuous-native-generation/)).

### Is there an official list or a per-library indicator?

Yes, both.

**Per-library badge.** Every Expo SDK API reference page carries platform
compatibility tags at the top, and one of the possible tags is literally
**"Included in Expo Go"**. The docs point at it: "You will see the platform
compatibility tags at the top of each API reference. It tells you which
platforms and environments the library is compatible with"
([Using libraries](https://docs.expo.dev/workflow/using-libraries/)).

Checking the badge across the libraries Linger would need:

| Library | Badge on its SDK 57 page | Source |
| --- | --- | --- |
| `expo-sqlite` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/sqlite/ |
| `expo-file-system` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/filesystem/ |
| `expo-document-picker` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/document-picker/ |
| `expo-sharing` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/sharing/ |
| `@react-native-async-storage/async-storage` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/async-storage/ |
| `expo-secure-store` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/secure-store/ |
| `expo-task-manager` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/task-manager/ |
| `expo-background-task` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/background-task/ |
| `expo-intent-launcher` | Included in Expo Go | https://docs.expo.dev/versions/latest/sdk/intent-launcher/ |
| `expo-notifications` | **no badge**, tags read "Android, iOS" only | https://docs.expo.dev/versions/latest/sdk/notifications/ |
| `expo-calendar` | **no badge**, tags read "Android (device only), iOS (device only)" | https://docs.expo.dev/versions/latest/sdk/calendar/ |
| `expo-maps` | no badge | https://docs.expo.dev/versions/latest/sdk/maps/ |
| `expo-dev-client` | no badge | https://docs.expo.dev/versions/latest/sdk/dev-client/ |

The badge is a reliable negative signal and a mostly reliable positive one. The
one case it under-reports is `expo-notifications`, which carries no badge yet is
documented in prose as partially available in Expo Go. See section 3 and the
ambiguities list.

**Official third-party list.** There is a curated page of non-Expo libraries
compiled into Expo Go: "The Expo Go playground ... supports a curated list of
third-party libraries. ... The difference between libraries listed in this
section, and any other third-party library is that the support for former is
built-in in Expo Go environment"
([Third-party libraries supported in Expo Go](https://docs.expo.dev/versions/latest/sdk/third-party-overview/)).
It names twenty packages, including `react-native-webview`, `react-native-svg`,
`react-native-reanimated`, `@shopify/flash-list` and Async Storage.

**Directory filter.** For anything else: "You can check **React Native
Directory** to find a library compatible with Expo Go by visiting the website
and verifying that it has a '✔️ Expo Go' tag"
([Using libraries](https://docs.expo.dev/workflow/using-libraries/)).

**The rule of thumb Expo gives.** If a library ships `android` or `ios`
directories, mentions linking, requires editing `AndroidManifest.xml`, or has a
config plugin, "then you should create a development build to use the library in
your project" ([Using libraries](https://docs.expo.dev/workflow/using-libraries/)).

### One Expo Go build, one SDK

> Each build of Expo Go includes one Expo SDK version. The `expo` package version
> in **package.json** sets the project's SDK version. The project and Expo Go SDK
> versions must match.
> ([Expo Go version mismatch](https://docs.expo.dev/troubleshooting/expo-go-version-mismatch/))

And the store builds lag: "Expo Go on the Apple App Store stops at SDK 54, and
SDK 55 and later are not available there. The Google Play version can also lag
behind a new SDK release" (same page). The SDK 57 changelog confirms this was
still true at release: "We'd like to release a new version for SDK 57, but we're
still waiting on approval. Expo Go for SDK 57 is available with `eas go` for iOS
devices, and through Expo CLI for Android devices/emulators and iOS simulators"
([SDK 57 changelog](https://expo.dev/changelog/sdk-57)).

The documented workaround on Android is to sideload the matching build: "Visit
[expo.dev/go](https://expo.dev/go), select the SDK version used by your project
and your target platform, then install the compatible Expo Go build", or use the
`expo-go` CLI, "a standalone tool that downloads an Expo Go binary for a platform
and specific SDK version" via `npx expo-go download android latest`
([Expo Go version mismatch](https://docs.expo.dev/troubleshooting/expo-go-version-mismatch/),
[Tools for development](https://docs.expo.dev/develop/tools/)).

## 3. Local notifications on Android, the make-or-break one

### Does `expo-notifications` work in Expo Go on Android?

Expo's current statement, on the first screen of the library page, verbatim:

> Push notifications (remote notifications) functionality provided by
> `expo-notifications` is unavailable in Expo Go on Android from SDK 53. A
> [development build](https://docs.expo.dev/develop/development-builds/introduction/)
> is required to use push notifications. **Local notifications (in-app
> notifications) remain available in Expo Go.**
> ([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/))

So: the change landed in **SDK 53**, and it affected **remote push only**. Local
scheduled notifications were not removed. The FAQ repeats the split: "While
in-app notifications are available in Expo Go, remote push notifications (that
is, sending a push notification from a server to the app) are not"
([Development builds FAQ](https://docs.expo.dev/develop/development-builds/faq/)),
as does the notifications overview: "You must use a development build to use push
notifications since the capability is not built into Expo Go"
([What you need to know](https://docs.expo.dev/push-notifications/what-you-need-to-know/)).

**There is a catch that the docs do not mention.** In `expo-notifications` 57.x,
merely importing the library in Expo Go on Android can throw. The side-effect
module `DevicePushTokenAutoRegistration.fx.ts` registers a module-scope push
token listener, and `warnOfExpoGoPushUsage()` is written to `throw new Error(...)`
rather than warn when `Platform.OS === 'android'`
([warnOfExpoGoPushUsage.ts, sdk-57](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/src/warnOfExpoGoPushUsage.ts)).
PR #49062, "[android][notifications] Fix fatal import in Expo Go from token
listener", describes the failure: the guard assumed Expo Go lacked
`ScopedServerRegistrationModule`, but Expo Go on Android ships it, so the code
fell through to `addPushTokenListener` and threw
([PR #49062](https://github.com/expo/expo/pull/49062)). The fix is recorded under
version 58.0.0 in the package changelog, "[Android] Fixed importing
`expo-notifications` crashing the app in Expo Go"
([CHANGELOG](https://github.com/expo/expo/blob/main/packages/expo-notifications/CHANGELOG.md)),
and the guard `isRunningInExpoGo() && Platform.OS === 'android'` is present in
`main` but absent from the `sdk-57` branch of the same file
([sdk-57](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/src/DevicePushTokenAutoRegistration.fx.ts)).

Read plainly: on SDK 57 plus Expo Go on Android, the documented position is
"local notifications work" and the source tree says the import path that reaches
them was fatal until the fix shipped on the SDK 58 track. This must be tested on
the actual device before any of section 8 is believed. See ambiguities.

### Scheduling precision and trigger types

`scheduleNotificationAsync(request)` takes a trigger. The full set of
schedulable types is
([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/)):

| Trigger type | Platforms | Meaning |
| --- | --- | --- |
| `DATE` | Android, iOS | "delivered once on the specified value of the `date` property" |
| `TIME_INTERVAL` | Android, iOS | "delivered once or many times ... after `seconds` time elapse" |
| `DAILY` | Android, iOS | matches `hour` and `minute` each day |
| `WEEKLY` | Android, iOS | matches `weekday`, `hour`, `minute` |
| `MONTHLY` | Android, iOS | matches day-of-month components |
| `YEARLY` | Android, iOS | matches month and day components |
| `CALENDAR` | **iOS only** | "Corresponds to native `UNCalendarNotificationTrigger`" |

Two things follow for Linger. The Android-usable one-shot trigger is `DATE`,
taking a JavaScript `Date` or a Unix timestamp, so the app computes the absolute
instant itself, which is exactly what `docs/REMINDERS.md` already requires when
it says offsets resolve "in the timezone of the airport, hotel or restaurant, not
the phone". And the only trigger input carrying a `timezone` property is
`CalendarTriggerInput`, which is iOS only, so there is no Android-side timezone
handling to rely on or to fight.

### Does anything map to `setAlarmClock` or `setExactAndAllowWhileIdle`?

This is the crux, and the answer differs between SDK 57 and SDK 58.

**SDK 57: `setExactAndAllowWhileIdle`, with a silent downgrade.** The Android
scheduling delegate reads, in full
([ExpoSchedulingDelegate.kt, sdk-57](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/java/expo/modules/notifications/service/delegates/ExpoSchedulingDelegate.kt)):

```kotlin
private fun setupAlarm(triggerAtMillis: Long, operation: PendingIntent) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
    AlarmManagerCompat.setExactAndAllowWhileIdle(
      alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, operation
    )
  } else {
    AlarmManagerCompat.setAndAllowWhileIdle(
      alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, operation
    )
  }
}
```

There is no `setAlarmClock` anywhere in SDK 57's `expo-notifications`. The best
available precision is `setExactAndAllowWhileIdle`, and if the exact alarm
permission is missing it falls back to `setAndAllowWhileIdle` without telling
the caller. Per the companion research, `setAndAllowWhileIdle` is an inexact
alarm and `setExactAndAllowWhileIdle` is throttled in Doze, with official figures
ranging from once per nine minutes to once per fifteen
(`docs/research/android-alarm-reliability.md`, section 3).

**SDK 58: an opt-in `setAlarmClock`.** The changelog entry for
`expo-notifications` 58.0.0, dated 2026-09-10, verbatim:

> [android] Add a `delivery` option to `DateTriggerInput` and the repeating
> wall-clock triggers (`DailyTriggerInput`, `WeeklyTriggerInput`,
> `MonthlyTriggerInput`, `YearlyTriggerInput`). Set it to `'alarmClock'` to
> deliver the notification via `AlarmManager.setAlarmClock()`, which OEM battery
> policies do not defer. Intended for time-critical, user-facing alarms.
> ([CHANGELOG](https://github.com/expo/expo/blob/main/packages/expo-notifications/CHANGELOG.md))

The unversioned docs document the resulting type
([NotificationDelivery](https://docs.expo.dev/versions/unversioned/sdk/notifications/)):

> - `'bestEffort'`: the default. Uses `setExactAndAllowWhileIdle()`, which
>   delivers at the requested time on most devices. Some OEM Android builds defer
>   these alarms by minutes to save battery. Without the exact alarm permission,
>   the system may deliver the notification later than requested.
> - `'alarmClock'`: uses `setAlarmClock()`. The system delivers these alarms at
>   the requested time and does not defer them for battery optimization. Use only
>   for time-critical alarms, such as alarm clocks or medication reminders. The
>   status bar shows an alarm icon until the notification is delivered. Requires
>   the `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM` permission on Android 12 and
>   higher. Without the permission, the notification is scheduled as
>   `'bestEffort'`.

The status-bar alarm icon is the same visibility cost the companion research
flagged for `setAlarmClock()` directly. Expo is doing the documented thing here,
not hiding it.

This option is only on the `unversioned` docs and only in package version 58.0.0.
It is **not in SDK 57**, and SDK 58 is at `58.0.0-preview.3` on npm. Linger cannot
use it today.

### Exact-alarm permissions

Expo's SDK 57 statement, verbatim:

> Starting from Android 12 (API level 31), to schedule a notification that
> triggers at an exact time, you need to add
> `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>` to
> **AndroidManifest.xml**.
> ([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/))

SDK 57 lists only `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM` in its
Android permission table. The unversioned page adds `USE_EXACT_ALARM` to the
table and the guidance around it:

> The `delivery: 'alarmClock'` trigger option also requires an exact alarm
> permission. On Android 13 (API level 33) and later, alarm clock, timer, and
> calendar apps can declare
> `<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>` instead.
> The system grants that permission without a user prompt. Google Play's exact
> alarm policy restricts `USE_EXACT_ALARM` to those app categories.
> ([Expo Notifications, unversioned](https://docs.expo.dev/versions/unversioned/sdk/notifications/))

That matches the Play policy reading in `docs/research/android-alarm-reliability.md`,
section 2: treat `USE_EXACT_ALARM` as unavailable to a travel app.

**In Expo Go, Linger cannot declare either one, because the manifest is Expo
Go's, not Linger's.** What Expo Go itself declares is checkable. The Expo Go
Android manifest template on the `sdk-57` branch declares
`android.permission.SCHEDULE_EXACT_ALARM` and does **not** declare
`USE_EXACT_ALARM`
([template-files/android/AndroidManifest.xml](https://github.com/expo/expo/blob/sdk-57/template-files/android/AndroidManifest.xml)).
`expo-notifications`'s own library manifest, merged into whichever app includes
it, adds `RECEIVE_BOOT_COMPLETED` and `POST_NOTIFICATIONS`
([expo-notifications AndroidManifest](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/AndroidManifest.xml)).

So a reminder scheduled from Expo Go inherits Expo Go's exact-alarm grant state,
not Linger's. Every consequence in the companion research attaches to the Expo Go
package name: the standby bucket, the battery-optimization allowlist entry, the
Alarms and reminders toggle, the OEM never-sleeping list. Testing reminder
reliability in Expo Go therefore measures Expo Go's reliability, which is the
wrong subject.

### Doze and battery optimization

Expo says very little, and what it says is a pointer back to Android:

> However, the OS may decide not to deliver the notification to your app in some
> cases (e.g. when the device is in Doze mode on Android, or when you send too
> many notifications).
> ([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/),
> on `registerTaskAsync`)

> ... even when the notification is delivered to the device, the OS does not
> guarantee its delivery to your app. This may happen due to a variety of
> reasons, such as when Doze mode is enabled on Android.
> ([What you need to know](https://docs.expo.dev/push-notifications/what-you-need-to-know/))

The only substantive Doze statement Expo makes anywhere is the `NotificationDelivery`
text quoted above, and it is SDK 58. Nothing in Expo's documentation addresses
App Standby buckets, the restricted bucket, force stop, hibernation or OEM
battery managers. All of that stays the responsibility of
`docs/research/android-alarm-reliability.md`, and none of it is softened by using
Expo.

### Surviving app kill and reboot

**Reboot: yes, by design.** The docs:

> On Android, this module requires permission to subscribe to the device boot.
> It's used to set up scheduled notifications when the device (re)starts. The
> `RECEIVE_BOOT_COMPLETED` permission is added automatically through the
> library's **AndroidManifest.xml**.
> ([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/))

The mechanism is visible in the library manifest, whose `NotificationsService`
receiver listens on `android.intent.action.BOOT_COMPLETED`,
`android.intent.action.REBOOT`, `android.intent.action.QUICKBOOT_POWERON`,
`com.htc.intent.action.QUICKBOOT_POWERON` and
`android.intent.action.MY_PACKAGE_REPLACED`
([AndroidManifest](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/AndroidManifest.xml)),
and in `setupScheduledNotifications()`, which replays every request from a
`SharedPreferencesNotificationsStore`
([ExpoSchedulingDelegate.kt](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/java/expo/modules/notifications/service/delegates/ExpoSchedulingDelegate.kt)).
Requests are persisted, not held in memory, which is also why `alarmManager.cancel`
is paired with `store.removeNotificationRequest` on every removal path.

**App kill: yes for an ordinary kill, no for force stop.** The alarm is an
`AlarmManager` `PendingIntent` owned by the system, so it does not need the app
process alive. Expo documents no limit here. The Android-side exception is
already recorded in the companion research: on Android 15 the system "cancels all
pending intents when the app enters the stopped state"
([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state)),
and revoking `SCHEDULE_EXACT_ALARM` deletes every exact alarm silently
([AlarmManager reference](https://developer.android.com/reference/android/app/AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)).

**In Expo Go, one further wrinkle.** Reboot recovery runs inside the Expo Go
process, from Expo Go's shared preferences store, without the development server
being present. Whether Expo Go replays a given project's scheduled notifications
after a reboot is not documented anywhere. Historically Expo Go scoped
notifications per project, per the changelog entry "Notifications from different
experiences in Expo Go can no longer overwrite each other"
([CHANGELOG](https://github.com/expo/expo/blob/main/packages/expo-notifications/CHANGELOG.md)),
which implies per-experience storage exists, but that is an inference. See
ambiguities.

## 4. Getting confirmations into the app

Linger's intake is share sheet and file upload, no typing
(`docs/INGESTION.md`, stage 01).

### Share intents

**No, not in Expo Go.** Receiving an Android share requires an `intent-filter`
on `ACTION_SEND` in `AndroidManifest.xml`, and Expo's own API for this is a
config plugin.

`expo-sharing` gained a receive side, described on its page as "allows you to
share files directly with other compatible applications **and to receive
compatible data shared from other apps**"
([Expo Sharing](https://docs.expo.dev/versions/latest/sdk/sharing/)). Its
configurable properties:

| Property | Description |
| --- | --- |
| `android.enabled` | "A boolean value to enable Android share intent handling. If `true`, adds the necessary `intent-filter` to the **AndroidManifest.xml**." |
| `android.singleShareMimeTypes` | "An array of MIME types to accept for single file sharing (using `ACTION_SEND` intent)." |
| `android.multipleShareMimeTypes` | "An array of MIME types to accept for multiple file sharing (using `ACTION_SEND_MULTIPLE` intent)." |

All three default to off or empty. The same page states the general rule for
config plugin properties: they "cannot be set at runtime and require building a
new app binary to take effect"
([Expo Sharing](https://docs.expo.dev/versions/latest/sdk/sharing/)).

Confirmed against Expo Go's actual manifest: the SDK 57 Expo Go Android manifest
template contains intent filters for `MAIN`, `VIEW`, the `host.exp.exponent`
scheme, Firebase messaging and media sessions, and **no `android.intent.action.SEND`
filter at all**
([template-files/android/AndroidManifest.xml](https://github.com/expo/expo/blob/sdk-57/template-files/android/AndroidManifest.xml)).
Expo Go will not appear in the Android share sheet as a target for a PDF or an
email, and there is no runtime API that could make it.

Expo marks the receive-share feature as not yet stable: "This functionality is
currently experimental"
([Expo Sharing](https://docs.expo.dev/versions/latest/sdk/sharing/)). The
documented handling path is a deep link into the app, using Expo Router's
`+native-intent.ts` or React Navigation's `linking` prop, then the
`useIncomingShare()` hook to read the payload. That is a clean design for a
development build and unreachable from Expo Go.

The generic alternative, `android.intentFilters` in app config, has the same
problem: it is documented as "Configuration for setting an array of custom intent
filters in Android manifest", and for an existing React Native app "This is set
in `AndroidManifest.xml` directly"
([app config](https://docs.expo.dev/versions/latest/config/app/)).

### `expo-document-picker`, `expo-file-system`, `expo-sharing` in Expo Go

All three carry the "Included in Expo Go" badge, so the modules themselves are
present.

**`expo-document-picker`** presents the system document UI:
`getDocumentAsync(options)`, "Display the system UI for choosing a document. By
default, the chosen file is copied to the app's internal cache directory". Its
options include `type`, "The MIME type(s) of the documents that are available to
be picked ... To allow any type of document you can use `'*/*'`", `multiple`, and
`copyToCacheDirectory`, which when true means the file "is copied to
`FileSystem.CacheDirectory`, which allows other Expo APIs to read the file
immediately". The success result gives `uri`, `name`, `mimeType`, `size` and
`lastModified`
([Expo DocumentPicker](https://docs.expo.dev/versions/latest/sdk/document-picker/)).
The library page also warns that without `copyToCacheDirectory`, "it's not always
possible for the file system to read the file immediately after the
`expo-document-picker` picks it". Its config plugin properties are iOS-only
iCloud settings, so nothing about the Android picker needs a native build.

**`expo-file-system`** provides "Both synchronous and asynchronous, read and
write access to file contents", creation, modification and deletion, file
properties including `type` and `size`, "Ability to read and write files as
streams or using the `FileHandle` class", and download and upload via
`downloadFileAsync` or `expo/fetch`
([Expo FileSystem](https://docs.expo.dev/versions/latest/sdk/filesystem/)). Its
two config plugin properties, `supportsOpeningDocumentsInPlace` and
`enableFileSharing`, are both iOS-only, so on Android the library is fully usable
from Expo Go.

**`expo-sharing`** can send files out from Expo Go. Only the inbound direction
needs the manifest.

### Reading a `.eml` or a PDF in Expo Go

**Getting the bytes: yes.** `getDocumentAsync({ type: '*/*' })` or a narrower
MIME list will show `.eml` and `.pdf` files wherever the device's document
providers expose them, and with `copyToCacheDirectory: true` the file lands in
the cache directory where `expo-file-system` can read it
([Expo DocumentPicker](https://docs.expo.dev/versions/latest/sdk/document-picker/)).
Nothing in either page restricts this to a development build.

**Making sense of the bytes: only partly.** A `.eml` is RFC 5322 text, so a
pure-JavaScript MIME parser reading the file through `expo-file-system` works in
Expo Go with no native code. A PDF does not: the Expo SDK has no PDF text
extraction library, and nothing in the SDK index or the Expo Go third-party list
provides one
([SDK reference index](https://docs.expo.dev/versions/latest/),
[Third-party libraries in Expo Go](https://docs.expo.dev/versions/latest/sdk/third-party-overview/)).
A JavaScript PDF parser bundled as an npm dependency would run, since it is JS
only, but that is a claim about the JS ecosystem rather than something Expo
documents, and it is untested here.

Note that `docs/INGESTION.md` stage 02 specifies OCR with Tesseract and a vision
model fallback, which is server-side work in the current design. None of that is
affected by the Expo Go choice.

## 5. Storage

Every storage option Linger needs is available in Expo Go.

| Option | In Expo Go | What Expo says | Source |
| --- | --- | --- | --- |
| `expo-sqlite` | Yes, badge present | "The database is persisted across restarts of your app." | [Store data](https://docs.expo.dev/develop/user-interface/store-data/), [Expo SQLite](https://docs.expo.dev/versions/latest/sdk/sqlite/) |
| `expo-file-system` | Yes, badge present | "Within Expo Go, each project has a separate file system and no access to other Expo projects' files." | [Store data](https://docs.expo.dev/develop/user-interface/store-data/) |
| Async Storage | Yes, badge present | "asynchronous, unencrypted, persistent, key-value storage solution" | [Async Storage](https://docs.expo.dev/versions/latest/sdk/async-storage/) |
| `expo-secure-store` | Yes, badge present | "encrypt and securely store key-value pairs locally on the device ... intended for small values such as tokens, keys, and other secrets" | [Store data](https://docs.expo.dev/develop/user-interface/store-data/) |

Caveats that matter for Linger:

- **SQLCipher is not available.** "SQLCipher is not supported on Expo Go"
  ([Expo SQLite](https://docs.expo.dev/versions/latest/sdk/sqlite/)). Encrypting
  the itinerary database at rest, which `docs/CALENDAR-AND-PASSES.md` implies for
  documents held "encrypted on device behind biometrics", needs a development
  build.
- **Async Storage is unencrypted**, stated on its own page, so it is wrong for
  anything in the document vault.
- **File system isolation in Expo Go is per project**, which is good for
  correctness and means the data lives in Expo Go's sandbox rather than Linger's.
  Uninstalling Expo Go, or Expo Go clearing its data, takes the database with it.
- The file system directories available are `Paths.document`, "a place to store
  files that are safe from being deleted by the system", and `Paths.cache`, "a
  place to store files that can be deleted by the system when the device runs low
  on storage"
  ([Expo FileSystem](https://docs.expo.dev/versions/latest/sdk/filesystem/)).
  Trip packs and passes belong in `document`, picked files land in `cache`.

## 6. Calendar

**No. `expo-calendar` does not work in Expo Go at all**, on Android or iOS. The
statement is on the first screen of the library page, verbatim:

> To provide quicker updates, `expo-calendar` is currently unsupported in Expo Go
> and Snack. To use it, create a
> [development build](https://docs.expo.dev/develop/development-builds/introduction/).
> ([Expo Calendar](https://docs.expo.dev/versions/latest/sdk/calendar/))

Its platform tags read "Android (device only), iOS (device only)", with no
Expo Go badge, which corroborates the prose.

In a development build it does everything `docs/CALENDAR-AND-PASSES.md` route 2
needs. `ExpoCalendar.createEvent(details)`, "Creates a new event in the
calendar", is the current write API;
`Calendar.createEventAsync(calendarId, details)` is marked "Deprecated: Use
`calendar.addEventWithForm()` or import this method from `expo-calendar/legacy`.
This method will throw in runtime". `addEventWithForm(options)` "Presents the
system-provided dialog to create a new event in this calendar, pre-filled with
the provided data", and `Calendar.createCalendar(details)` allows the separate
named `Linger . Tokyo` calendar the design calls for
([Expo Calendar](https://docs.expo.dev/versions/latest/sdk/calendar/)).

Android permissions are `READ_CALENDAR` and `WRITE_CALENDAR`, declared through
`expo.android.permissions` in app config, with one exception: "If you only intend
to use the system-provided calendar UI, you don't need to request any
permissions" (same page). Declaring permissions in app config is itself a
manifest change, so it is another thing Expo Go cannot carry.

Route 1 of the calendar design, the `webcal://` ICS subscription, needs no device
API at all and is unaffected by any of this. Route 3, a one-off `.ics` download,
needs only `expo-file-system` plus `expo-sharing`, both of which are in Expo Go.

## 7. The practical setup on a phone

### What the owner actually does

1. **Install Expo Go from the Play Store.** "Scan the QR code to download the app
   from the Google Play Store, or visit the Expo Go page on the Google Play
   Store", package `host.exp.exponent`
   ([Set up your environment](https://docs.expo.dev/get-started/set-up-your-environment/)).
   If the Play build's SDK does not match the project's, install the matching
   build from [expo.dev/go](https://expo.dev/go) or via
   `npx expo-go download android latest`
   ([Expo Go version mismatch](https://docs.expo.dev/troubleshooting/expo-go-version-mismatch/),
   [Tools for development](https://docs.expo.dev/develop/tools/)).
2. **Run the dev server:** `npx expo start`
   ([Start developing](https://docs.expo.dev/get-started/start-developing/)).
3. **Scan the QR code.** "After running the command above, you will see a QR code
   in your terminal. Scan this QR code to open the app on your device" (same page).

No Expo account is needed on Android. The sign-in requirement is iOS-only: "On a
physical iOS device, Expo Go opens a project from a development server only when
Expo CLI and Expo Go are signed in to the same Expo account"
([Set up your environment](https://docs.expo.dev/get-started/set-up-your-environment/)).
That is moot here, per `docs/adr/0002-android-first.md`.

### Network requirements

The default is a LAN connection, and it needs both devices on the same network:
"Make sure you are on the same Wi-Fi network on your computer and your device"
([Start developing](https://docs.expo.dev/get-started/start-developing/)).

When that fails, the documented escape is a tunnel: "If it still doesn't work, it
may be due to the router configuration, this is common for public networks. You
can work around this by choosing the **Tunnel** connection type when starting the
development server, then scanning the QR code again", via `npx expo start --tunnel`
(same page).

### What `--tunnel` is for

> Restrictive network conditions (common for public Wi-Fi), firewalls (common for
> Windows users), or Emulator misconfiguration can make it difficult to connect a
> remote device to your dev server over lan/localhost. Sometimes it's easier to
> connect to a dev server over a proxy URL that's accessible from any device with
> internet access, this is referred to as **tunneling**. `npx expo start` provides
> built-in support for **tunneling** via [ngrok](https://ngrok.com).
> ([Expo CLI](https://docs.expo.dev/more/expo-cli/))

Details worth knowing before relying on it:

- It needs a separate install: `npm i -g @expo/ngrok`.
- It serves from a public URL of the form
  `https://xxxxxxx.bacon.19000.exp.direct:80`.
- "Tunnel URLs are public and can be accessed by any device with a network
  connection. Expo CLI mitigates the risk of exposure by adding entropy to the
  beginning of the URL."
- "Tunneling is slower than local connections because requests must be forwarded
  to a public URL", and the start-developing page repeats that reloads are
  "considerably slower than on **LAN** or **Local**".
- "Tunnels require a network connection on both devices, meaning this feature
  cannot be used with the `--offline` flag."
- It depends on a third party: "Tunneling requires a third-party hosting service,
  this means it may sometimes experience intermittent issues".

All from [Expo CLI](https://docs.expo.dev/more/expo-cli/). For a trip-testing
workflow, a public proxy carrying the app bundle is worth weighing against
`docs/INGESTION.md`'s "no data out" guardrail, even though the tunnel carries
development traffic rather than traveller data.

Development without any network is also supported: `npx expo start --offline`,
which "will prevent the CLI from making network requests"
([Expo CLI](https://docs.expo.dev/more/expo-cli/)). That is how you test the
offline contract in `docs/CALENDAR-AND-PASSES.md`, though it does not tell you
anything about reminder delivery, which is an OS matter.

### Does Expo Go still exist as a supported product?

Yes, it is maintained and shipping, and Expo pushes people off it for anything
real. Both are true at once.

Evidence it is alive: it is still listed on the Google Play Store with a
documented install path
([Set up your environment](https://docs.expo.dev/get-started/set-up-your-environment/));
Expo builds one per SDK and was actively seeking Play approval for the SDK 57
build ([SDK 57 changelog](https://expo.dev/changelog/sdk-57)); there is a
dedicated `expo-go` CLI for fetching per-SDK binaries
([Tools for development](https://docs.expo.dev/develop/tools/)); and the curated
Expo Go third-party library list is maintained per SDK release, "tested with each
Expo SDK release"
([Third-party libraries in Expo Go](https://docs.expo.dev/versions/latest/sdk/third-party-overview/)).

Evidence of the push away from it: every page that names Expo Go calls it a
playground for students and learners; "Expo Go is limited and not useful for
building production-grade projects. Use development builds instead"
([Tools for development](https://docs.expo.dev/develop/tools/)); "Expo Go is a
learning environment and sandbox. For a production project, create a development
build so the native app and its dependencies are controlled by your project"
([Expo Go version mismatch](https://docs.expo.dev/troubleshooting/expo-go-version-mismatch/));
and on iOS the App Store build has been frozen at SDK 54 (same page).

One further data point on direction of travel: React Native's own docs recommend
Expo as the framework, "if you're building a new app with React Native, we
recommend using a Framework", and name Expo specifically as "a production-grade
React Native Framework"
([React Native environment setup](https://reactnative.dev/docs/environment-setup)).
That recommendation is about Expo, not about Expo Go.

## 8. Verdict per promise

Legend: **Yes** means documented as supported. **No** means documented as
unsupported or structurally impossible. **Partial** means it runs but does not
meet the promise.

| Linger promise | Expo Go | Development build | The gap |
| --- | --- | --- | --- |
| Exact reminders that fire offline | **Partial, and unreliable** | **Partial** | See below |
| Share-sheet intake | **No** | **Yes** | `intent-filter` in the manifest |
| Calendar export | **Partial** | **Yes** | `expo-calendar` is Expo Go excluded |
| Offline-first storage | **Yes** | **Yes** | Encryption at rest only |
| No data out | **Yes, with a caveat** | **Yes** | Tunnel mode and Expo Go's sandbox |

### Exact reminders that fire offline

**Expo Go: partial, and what it does deliver is not measurable.** Local scheduled
notifications are documented as available
([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/)),
`DATE` triggers exist on Android, and they fire with the radio off because
`AlarmManager` is an on-device mechanism. Three things break the promise.

First, precision. SDK 57 schedules through `setExactAndAllowWhileIdle` at best,
never `setAlarmClock`
([ExpoSchedulingDelegate.kt](https://github.com/expo/expo/blob/sdk-57/packages/expo-notifications/android/src/main/java/expo/modules/notifications/service/delegates/ExpoSchedulingDelegate.kt)),
and the companion research establishes that `setExactAndAllowWhileIdle` is the
throttled path and `setAlarmClock` is the only API Android documents as never
delayed (`docs/research/android-alarm-reliability.md`, section 3).

Second, ownership. Permissions, standby bucket, battery-optimization allowlisting
and OEM exemption lists all attach to Expo Go's package, not Linger's, because the
manifest is Expo Go's
([template-files/android/AndroidManifest.xml](https://github.com/expo/expo/blob/sdk-57/template-files/android/AndroidManifest.xml)).
Whatever reliability you measure in Expo Go is Expo Go's reliability under Expo
Go's usage pattern, and a developer opens Expo Go constantly, which keeps it in a
healthy bucket. The eight-day idle case that
`docs/adr/0002-android-first.md` says is the whole reason for going Android first
cannot be reproduced in Expo Go at all.

Third, the SDK 57 import defect described in section 3, which may prevent the
library loading in Expo Go on Android before any of the above matters
([PR #49062](https://github.com/expo/expo/pull/49062)).

**Development build: partial, and it is the best available.** It gets Linger its
own package, its own manifest, its own `SCHEDULE_EXACT_ALARM` declaration, its own
bucket and its own allowlist entry. On SDK 57 it still only reaches
`setExactAndAllowWhileIdle`. On SDK 58, once released, `delivery: 'alarmClock'`
reaches `setAlarmClock()` directly
([CHANGELOG](https://github.com/expo/expo/blob/main/packages/expo-notifications/CHANGELOG.md)).
It remains partial because everything in the companion research still applies:
permission revocation without notice, force stop on Android 15, hibernation, the
restricted bucket after eight idle days, and OEM restriction managers. Expo
changes none of that, and the residual gap after a development build on SDK 58 is
the same gap a native Kotlin app would have.

**Gap:** no `setAlarmClock` until SDK 58; no control of the manifest, the
permission or the package identity in Expo Go; and no way to reproduce the idle
device conditions the product actually fails under.

### Share-sheet intake

**Expo Go: no.** The Expo Go manifest declares no `ACTION_SEND` intent filter
([template-files/android/AndroidManifest.xml](https://github.com/expo/expo/blob/sdk-57/template-files/android/AndroidManifest.xml)),
and the only Expo API for receiving shares is a config plugin that "adds the
necessary `intent-filter` to the **AndroidManifest.xml**"
([Expo Sharing](https://docs.expo.dev/versions/latest/sdk/sharing/)). There is no
runtime path.

**Development build: yes, with a caveat.** `expo-sharing`'s `android.enabled`,
`singleShareMimeTypes` and `multipleShareMimeTypes`, plus `useIncomingShare()`,
cover exactly this case, but Expo marks the feature experimental
([Expo Sharing](https://docs.expo.dev/versions/latest/sdk/sharing/)).

**Partial fallback in Expo Go:** the file-upload half of the intake works.
`expo-document-picker` plus `expo-file-system` are both in Expo Go, so the owner
can pick a saved PDF or `.eml` from inside the app. That is the second-best
intake path in `docs/INGESTION.md` stage 01, and it is enough to exercise the
pipeline. What it cannot do is prove the share-sheet flow, which is the one the
product is actually designed around.

**Gap:** the whole inbound share surface, which is a manifest feature.

### Calendar export

**Expo Go: partial.** `expo-calendar` is explicitly unsupported
([Expo Calendar](https://docs.expo.dev/versions/latest/sdk/calendar/)), so route
2 of `docs/CALENDAR-AND-PASSES.md`, direct writes into a named `Linger . Tokyo`
calendar, is out. Routes 1 and 3 are unaffected: a `webcal://` subscription needs
no device API, and generating an `.ics` file needs only `expo-file-system` and
`expo-sharing`, both in Expo Go. Since the design already ranks the live ICS feed
first, this is the least damaging of the gaps.

**Development build: yes.** `createEvent`, `createCalendar`, `addEventWithForm`
and `READ_CALENDAR` / `WRITE_CALENDAR` all become available.

**Gap:** direct calendar writes only.

### Offline-first storage

**Expo Go: yes.** SQLite, file system, Async Storage and SecureStore all carry the
"Included in Expo Go" badge, and the SQLite database "is persisted across restarts
of your app"
([Store data](https://docs.expo.dev/develop/user-interface/store-data/)).

**Development build: yes**, plus SQLCipher, which Expo Go excludes
([Expo SQLite](https://docs.expo.dev/versions/latest/sdk/sqlite/)).

**Gap:** encryption at rest. `docs/CALENDAR-AND-PASSES.md` promises documents
"encrypted on device behind biometrics", which needs either SQLCipher or an
encryption layer above `expo-file-system`. Also worth noting that data written in
Expo Go lives in Expo Go's sandbox and does not migrate to a development build.

### No data out

**Expo Go: yes, with a caveat.** Everything Linger stores stays on device, and
Expo Go isolates it: "Within Expo Go, each project has a separate file system and
no access to other Expo projects' files"
([Store data](https://docs.expo.dev/develop/user-interface/store-data/)). The
caveat is the development workflow rather than the app: `--tunnel` routes the dev
server through a public ngrok URL, "public and can be accessed by any device with
a network connection"
([Expo CLI](https://docs.expo.dev/more/expo-cli/)). That exposes the bundle and
the dev server, not the traveller's data, but it is a real exposure and it is
avoidable by staying on LAN.

A second caveat is push. `docs/REMINDERS.md` lists "Push" as the channel for
nearly every rule. If any of those become genuinely remote rather than local,
push is unavailable in Expo Go on Android from SDK 53
([Expo Notifications](https://docs.expo.dev/versions/latest/sdk/notifications/))
and would need FCM credentials in a development build, which is a data-out
decision of its own.

**Development build: yes.** Nothing about a development build weakens this.

**Gap:** none in the app. The gap is in the workflow, and only when tunnelling.

## Ambiguities and unconfirmed items

1. **Whether `expo-notifications` actually imports in Expo Go on Android under
   SDK 57.** The docs say local notifications remain available; the `sdk-57`
   source still has the unguarded module-scope push token listener that PR #49062
   identified as fatal, and the fix is recorded only against version 58.0.0.
   These two cannot both be right for a real device. This is the single most
   important thing to test first, and it takes ten minutes. If it does throw, the
   entire Expo Go plan collapses at step one.
2. **Why `expo-notifications` carries no "Included in Expo Go" badge while the
   prose says local notifications work.** The badge is the documented per-library
   indicator, so its absence here is either a deliberate signal of partial support
   or an oversight. Expo does not say which.
3. **Whether Expo Go replays a project's scheduled notifications after a device
   reboot.** `RECEIVE_BOOT_COMPLETED` and `setupScheduledNotifications()` exist in
   the library, and the store is per experience, but no documentation covers the
   Expo Go case, where the boot receiver fires with no development server present
   and possibly several projects' notifications in the store.
4. **Whether Expo Go's exact-alarm state is even observable from JavaScript.**
   `AlarmManager.canScheduleExactAlarms()` is consulted natively in
   `setupAlarm()`, but `expo-notifications` exposes no API reporting the result,
   so a caller cannot tell whether a given schedule landed on
   `setExactAndAllowWhileIdle` or silently degraded to `setAndAllowWhileIdle`.
   This appears to be true in a development build as well as in Expo Go.
5. **SDK 58's release date.** `expo@58.0.0-preview.3` is on npm and the
   unversioned docs describe SDK 58 behaviour, but there is no SDK 58 changelog
   post and no announced date. Linger's access to `delivery: 'alarmClock'`
   depends on it.
6. **Whether `delivery: 'alarmClock'` will work in Expo Go once SDK 58 ships.**
   The option needs `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`. Expo Go's
   manifest declares the first and not the second on the `sdk-57` branch, and the
   SDK 58 Expo Go manifest is not yet published. If it declares
   `SCHEDULE_EXACT_ALARM`, whether the Expo Go install on a given phone has been
   granted it through Alarms and reminders is a per-device question no
   documentation can answer.
7. **Whether reading PDF text in pure JavaScript is practical inside Expo Go.**
   Nothing in the Expo SDK does it, a JS-only npm parser should bundle and run
   since Metro handles it like any dependency, but this is untested and Expo
   documents nothing about it.
8. **What the SDK 57 Expo Go build on the Play Store actually is right now.** The
   changelog said Expo was "still waiting on approval" as of the SDK 57 post, and
   the troubleshooting page says the Play version "can also lag behind a new SDK
   release". Whether a Play install today matches an SDK 57 project has to be
   checked on the phone, not from the docs.
9. **Whether `expo-sharing`'s incoming-share feature is stable enough to build
   on.** Expo marks it experimental and warns about the iOS side specifically.
   The Android side is not flagged, but "experimental" is a status across the
   feature, per
   [release statuses](https://docs.expo.dev/more/release-statuses/).
10. **Whether anything measured in Expo Go transfers to a development build.**
    Bucket placement, permission grants and OEM restriction all attach to the
    package name, so a reminder that fires reliably in Expo Go tells you nothing
    about the same reminder in a Linger build, and vice versa. This is an
    inference from how Android scopes these mechanisms, not an Expo statement.

## Bottom line

Expo Go can carry roughly half of Linger and none of the interesting half.

What works in Expo Go today: storage in all four forms, the file-upload half of
intake, `.ics` generation and sharing out, the `webcal://` route entirely, and
the UI. That is enough to build and feel the product on a real phone, which is
what the owner asked for.

What does not: the share sheet, direct calendar writes, and, most importantly,
any honest test of the reminder promise. The reminder gap is not a matter of
degree. In Expo Go, Linger has no manifest, so it has no exact-alarm permission,
no package identity, no standby bucket of its own and no battery-optimization
entry, and every mitigation in `docs/research/android-alarm-reliability.md`
section 4 is addressed to an app that has those things. Add that SDK 57 reaches
only `setExactAndAllowWhileIdle`, and the option that reaches `setAlarmClock()`
ships in an SDK that is still in preview, and the position is clear: Expo Go
cannot validate the one thing `docs/adr/0002-android-first.md` says Android was
chosen to validate.

The sequencing that follows from this is not "Expo Go or development build". It
is Expo Go first for the fast loop on screens, timeline, ingestion plumbing and
storage, then a development build before the first reminder is trusted. The move
between them is documented and small: `npx expo install expo-dev-client` followed
by `npx expo run:android --device`, with no Expo account required
([Development builds introduction](https://docs.expo.dev/develop/development-builds/introduction/)).
A development build is "essentially your own version of Expo Go", so the
JavaScript, the QR-code loop and the fast refresh all survive the move unchanged.

The one thing to do before any of this is to confirm ambiguity 1, that
`expo-notifications` imports at all in Expo Go on Android under SDK 57. If it
does not, the Expo Go phase shrinks to the parts of Linger that never touch
notifications, and the development build stops being a second step and becomes
the first one.
