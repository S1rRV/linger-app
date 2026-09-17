# Android exact-alarm reliability for time-critical local notifications

Research date: 2026-09-17. API 31 (Android 12) through API 36 (Android 16).

Question: can Linger promise that a reminder fires at the right local wall-clock
time with no network and the app closed?

Every claim below carries a URL. Sources are limited to developer.android.com,
source.android.com, Google Play Console help, developer.samsung.com and
developer.apple.com. Anything the docs leave open is collected in
"Ambiguities and unconfirmed items" at the end.

## 1. SCHEDULE_EXACT_ALARM vs USE_EXACT_ALARM

| | `SCHEDULE_EXACT_ALARM` | `USE_EXACT_ALARM` |
| --- | --- | --- |
| Added | API level 31 | API level 33 |
| Grant model | "Granted by the user" | "Granted automatically" |
| Revocable | "This is a special access permission that can be revoked by the system or the user." | "Cannot be revoked by the user" |
| Min target SDK to request | `S` (31) | `TIRAMISU` (33) |
| Play review | not restricted | "restricted permission", subject to review |

Sources:
[SCHEDULE_EXACT_ALARM reference](https://developer.android.com/reference/android/Manifest.permission#SCHEDULE_EXACT_ALARM),
[USE_EXACT_ALARM reference](https://developer.android.com/reference/android/Manifest.permission#USE_EXACT_ALARM),
[Schedule alarms guide](https://developer.android.com/develop/background-work/services/alarms/schedule).

Only one of the two should be declared on a given device. The reference gives
the exact pattern: "Note that only one of `USE_EXACT_ALARM` or
`SCHEDULE_EXACT_ALARM` should be requested on a device. If your app is already
using `SCHEDULE_EXACT_ALARM` on older SDKs but needs `USE_EXACT_ALARM` on SDK 33
and above, then `SCHEDULE_EXACT_ALARM` should be declared with a max-sdk
attribute, like: `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32" />`"
([USE_EXACT_ALARM reference](https://developer.android.com/reference/android/Manifest.permission#USE_EXACT_ALARM)).

### What changed at API 34 (Android 14)

`SCHEDULE_EXACT_ALARM` "is no longer being pre-granted to most newly installed
apps targeting Android 13 and higher (will be set to denied by default)"
([Android 14 change](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)).
The trigger is the app's target SDK being 33+, not the device being on 14.

Follow-on details:

- Upgrade path is preserved: "if an existing app already has this permission, it
  will be pre-granted when the device upgrades to Android 14."
- Backup and restore is not: "If a user transfers app data to a device running
  Android 14 through a backup-and-restore operation, the
  `SCHEDULE_EXACT_ALARM` permission will be denied on the new device."
  ([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule))
- Always-allowed to call `setExact()` / `setExactAndAllowWhileIdle()`: apps
  signed with the platform certificate, privileged apps, and apps on the power
  allowlist. Holders of the `SYSTEM_WELLBEING` role are pre-granted
  `SCHEDULE_EXACT_ALARM`.
  ([Android 14 change](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms))

### Revocation is destructive, not just a gate

"When the user revokes the `Manifest.permission.SCHEDULE_EXACT_ALARM`
permission, all alarms scheduled with `setExact(int, long, PendingIntent)`,
`setExactAndAllowWhileIdle(int, long, PendingIntent)` and
`setAlarmClock(AlarmClockInfo,PendingIntent)` will be deleted."
([ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED](https://developer.android.com/reference/android/app/AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))

The guide is blunter: "When the `SCHEDULE_EXACT_ALARM` permission is revoked for
your app, your app stops, and all future exact alarms are canceled."
([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule))

The grant broadcast exists but the revoke broadcast does not: "This broadcast
will not be sent when the user revokes the permission."
([reference](https://developer.android.com/reference/android/app/AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))
So an app on `SCHEDULE_EXACT_ALARM` can have its entire reminder set silently
deleted and gets no callback telling it so. It can only discover this by calling
`canScheduleExactAlarms()` the next time it runs.

### The standby-bucket difference, which matters more than the prompt

These two sentences are the most consequential difference for Linger:

- `USE_EXACT_ALARM`: "Apps that hold this permission, always stay in the
  `WORKING_SET` or lower standby bucket."
  ([reference](https://developer.android.com/reference/android/Manifest.permission#USE_EXACT_ALARM))
- `SCHEDULE_EXACT_ALARM`: "Apps that hold this permission and target API
  `Build.VERSION_CODES.TIRAMISU` and below always stay in the `WORKING_SET` or
  lower standby bucket."
  ([reference](https://developer.android.com/reference/android/Manifest.permission#SCHEDULE_EXACT_ALARM))

The target-SDK qualifier on the second one means an app targeting API 34+ with
`SCHEDULE_EXACT_ALARM` loses the bucket floor. Separately, the exemption list
for the restricted bucket names `USE_EXACT_ALARM` and `ACCESS_BACKGROUND_LOCATION`
and does not name `SCHEDULE_EXACT_ALARM`
([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)).
See section 3 for why that is severe.

### Also required, and separately revocable

- `POST_NOTIFICATIONS`, added API 33, "Protection level: dangerous", so it is a
  runtime permission the user can deny or later revoke
  ([reference](https://developer.android.com/reference/android/Manifest.permission#POST_NOTIFICATIONS)).
- `RECEIVE_BOOT_COMPLETED`, "Protection level: normal"
  ([reference](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_BOOT_COMPLETED)),
  needed because "By default, all alarms are canceled when a device shuts down."
  ([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule))

## 2. Google Play policy on USE_EXACT_ALARM

Verbatim, from the "Exact Alarm Permission" section of
[Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/9888170?hl=en):

> **Policy Summary**
>
> USE_EXACT_ALARM permission on Android 13+ is a highly restricted permission
> used only for apps whose core, user-facing functionality genuinely requires
> precise timing, like dedicated alarm, timer, or calendar applications with
> event notifications. If your app does not have this specific core need,
> consider using SCHEDULE_EXACT_ALARM permission instead. It provides the same
> functionality but access must be granted by the user. This policy prevents
> misuse that impacts system resources. Please review the full policy to ensure
> compliance.
>
> **Full Policy**
>
> A new permission, USE_EXACT_ALARM, will be introduced that will grant access
> to exact alarm functionality in apps starting with Android 13 (API target
> level 33).
>
> USE_EXACT_ALARM is a restricted permission and apps must only declare this
> permission if their core functionality supports the need for an exact alarm.
> Apps that request this restricted permission are subject to review, and those
> that do not meet the acceptable use case criteria will be disallowed from
> publishing on Google Play.
>
> **Acceptable use cases for using the Exact Alarm Permission**
>
> Your app must use the USE_EXACT_ALARM functionality only when your app's core,
> user facing functionality requires precisely-timed actions, such as:
>
> - The app is an alarm or timer app.
> - The app is a calendar app that shows event notifications.
>
> If you have a use case for exact alarm functionality that's not covered above,
> you should evaluate if using SCHEDULE_EXACT_ALARM as an alternative is an
> option.

And the "Key Considerations" table, verbatim:

> **Do**
> - Request the auto granted version of the permission, USE_EXACT_ALARM, only if
>   your app's core functionality is of alarm or calendar.
> - Use SCHEDULE_EXACT_ALARM instead if the above criteria is not met.
> - Complete Play Console declaration to indicate app functionality.
>
> **Don't**
> - Don't use this permission for non-critical features that do not directly
>   contribute to the app's main purpose.

The identical text appears on the forward-looking
[Preview: Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/16909972?hl=en),
so no change to this policy is staged.

### Verdict for a travel itinerary / reminder app

Outside the permitted set, on the literal reading. The list has exactly two
entries. A travel companion app is not "an alarm or timer app". It is also not
"a calendar app that shows event notifications" in the sense the policy means,
though this is the only arguable door: the product does hold dated itinerary
events and does show notifications for them. The Android 14 changes page frames
the same carve-out around app identity rather than feature presence: "Calendar
or alarm clock apps need to send calendar reminders, wake-up alarms, or alerts
when the app is no longer running. These apps can request the `USE_EXACT_ALARM`
normal permission"
([Android 14 change](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)).

Treat `USE_EXACT_ALARM` as unavailable. Plan on `SCHEDULE_EXACT_ALARM` plus a
user-facing grant flow. This is a policy judgment, not a documented ruling; see
the ambiguities section.

### The battery-optimization allowlist is not a way around it either

Being on the power allowlist would let Linger call `setExact()` without
`SCHEDULE_EXACT_ALARM`
([Android 14 change](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)),
but Play restricts asking for it directly. Google Play policies prohibit apps
from requesting direct exemption from Doze and App Standby on Android 6.0 and
above unless the core function of the app is adversely affected (paraphrased to
keep house style; the note is on
[Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)).

The acceptable-use-case table on that page lists, verbatim, only: "Instant
messaging, chat, or calling app; enterprise VOIP apps" where FCM is unusable,
"Safety app", "Task automation app" whose "core function is scheduling automated
actions, such as for instant messaging, voice calling, or new photo management",
and "Peripheral device companion app" maintaining a persistent connection. A
travel reminder app is not on that list. Apps not meeting a case can still use
`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which only opens the settings
screen rather than prompting directly
([same page](https://developer.android.com/training/monitoring-device-state/doze-standby)).

## 3. Doze mode and App Standby

### What Doze does

From [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby),
the system in Doze:

> - Suspends network access.
> - Ignores wake locks.
> - Defers standard `AlarmManager` alarms, including `setExact()` and
>   `setWindow()`, to the next maintenance window.
> - Doesn't perform Wi-Fi scans.
> - Doesn't let sync adapters run.
> - Doesn't let `JobScheduler` run. `WorkManager` uses `JobScheduler`
>   internally, so `WorkManager` tasks don't run.

So `setExact()` alone does not satisfy the product promise. It is deferred in
Doze like any ordinary alarm.

### The three APIs, in Doze

**`setAlarmClock()`** is the only one the docs describe as unconditionally
punctual. "Alarms set with `setAlarmClock()` continue to fire normally. The
system exits Doze shortly before those alarms fire."
([doze-standby](https://developer.android.com/training/monitoring-device-state/doze-standby)).
The schedule guide: "Invoke an alarm at a precise time in the future. Because
these alarms are highly visible to users, the system never adjusts their
delivery time. The system identifies these alarms as the most critical ones and
leaves low-power modes if necessary to deliver the alarms."
([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule)).
No minimum interval or throttle is documented for it.

**`setExactAndAllowWhileIdle()`** fires in Doze but is throttled. The reference
says: "Under normal system operation, it will not dispatch these alarms more
than about every minute (at which point every such pending alarm is
dispatched); when in low-power idle modes this duration may be significantly
longer, such as 15 minutes." It adds: "Note that the OS will allow itself more
flexibility for scheduling these alarms than regular exact alarms, since the
application has opted into this behavior. When the device is idle it may take
even more liberties with scheduling in order to optimize for battery life."
([setExactAndAllowWhileIdle](https://developer.android.com/reference/android/app/AlarmManager#setExactAndAllowWhileIdle(int,%20long,%20android.app.PendingIntent))).
On dispatch, "the app will also be added to the system's temporary power
exemption list for approximately 10 seconds."

**Inexact alarms** are unusable for this product:

- `setWindow()`: "If your app targets Android 12 or higher, the system can delay
  the invocation of a time-windowed inexact alarm by at least 10 minutes. For
  this reason, `windowLengthMillis` parameter values under `600000` are
  typically clipped to `600000`."
- `setInexactRepeating()`: "On Android 12 (API level 31) and higher, the system
  invokes the alarm within one hour of the supplied trigger time."
  ([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule))

### Documented minimum intervals, which do not agree

Three official pages give three different numbers for the same throttle:

| Source | Stated limit |
| --- | --- |
| [doze-standby](https://developer.android.com/training/monitoring-device-state/doze-standby) | "Neither `setAndAllowWhileIdle()` nor `setExactAndAllowWhileIdle()` can fire alarms more than once per nine minutes, per app." |
| [AlarmManager reference](https://developer.android.com/reference/android/app/AlarmManager#setExactAndAllowWhileIdle(int,%20long,%20android.app.PendingIntent)) | "not ... more than about every minute" normally; "such as 15 minutes" in low-power idle |
| [Power management resource limits](https://developer.android.com/topic/performance/power/power-details) | "While-idle alarms: Limited to 7 per hour" when screen off and doze active |

Flagged as ambiguous. Nine minutes is the most conservative figure and roughly
consistent with 7 per hour; the reference's 15 minutes is the pessimistic
bound. Design for at least 15 minutes between while-idle alarms, or avoid the
issue entirely by using `setAlarmClock()`, which none of these rows cover.

### App Standby buckets: alarm frequency caps

From [Power management resource limits](https://developer.android.com/topic/performance/power/power-details):

| Bucket | Alarms |
| --- | --- |
| Active | "No execution limits" |
| Working set | "Limited to 10 per hour" |
| Frequent | "Limited to 2 per hour" |
| Rare | "Limited to 1 per hour" |
| Restricted | "One alarm per day, either an exact alarm or an inexact alarm" |

Device-state rows from the same page: when "Screen off and doze is active",
"Regular alarms: Deferred to doze maintenance window. While-idle alarms: Limited
to 7 per hour". When charging, "No execution limits for all standby buckets and
process states, except if user manually restricts app battery".

### The restricted bucket is the sharpest risk for a travel app

From [App Standby Buckets](https://developer.android.com/topic/performance/appstandby):

> On Android 13 (API level 33) and higher, unless your app qualifies for an
> exemption, the system places your app in the restricted bucket in the
> following situations:
>
> - The user doesn't interact with your app for a specific number of days. On
>   Android 12 (API level 31) and 12L (API level 32), the number of days is 45.
>   Android 13 reduces the number of days to 8.
> - Your app invokes an excessive number of broadcasts or bindings during a
>   24-hour period.

Restricted means "Your app can invoke one alarm per day. This alarm can be
either an exact alarm or an inexact alarm." And: "Unlike other buckets, these
power management restrictions apply to the restricted bucket even when the
device is charging."

Eight days of non-interaction is squarely inside a normal Linger usage pattern:
book a trip, ignore the app, then need reminders during travel. The exemption
list includes "Apps that are granted at least one of the following permissions:
`USE_EXACT_ALARM`, `ACCESS_BACKGROUND_LOCATION`". `SCHEDULE_EXACT_ALARM` is not
on that list. Whether `setAlarmClock()` is itself counted against the
one-alarm-per-day cap is not stated anywhere I could find; see ambiguities.

Also: "Every manufacturer can set their own criteria for how non-active apps are
assigned to buckets. Don't try to influence which bucket your app is assigned
to."
([App Standby Buckets](https://developer.android.com/topic/performance/appstandby))

### Two more platform-level kill switches

**Force stop cancels everything, Android 15+.** "the system also cancels all
pending intents when the app enters the stopped state on a device running
Android 15. When the user's actions remove the app from the stopped state, the
`ACTION_BOOT_COMPLETED` broadcast is delivered to the app providing an
opportunity to re-register any pending intents."
([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state))
Users reach this state by long-pressing an app icon and selecting Force Stop.
`ApplicationStartInfo.wasForceStopped()` detects it after the fact.

**App hibernation.** "The system places your app in a hibernation state if your
app targets Android 11 (API level 30) or higher" after "a few months" of no
interaction. On Android 12+, runtime permissions are reset, cached files are
removed, and the app "can't run jobs or alerts from the background". Running
jobs, receiving implicit broadcasts and scheduling alarms do not count as usage
and will not prevent hibernation. Exemption is requested via
`PackageManagerCompat.getUnusedAppRestrictionsStatus()` and
`IntentCompat.createManageUnusedAppRestrictionsIntent()`, launched with
`startActivityForResult()`.
([App hibernation](https://developer.android.com/topic/performance/app-hibernation))
CDD 3.5.2 constrains this to roughly a month or longer of non-use
([CDD](https://source.android.com/docs/compatibility/16/android-16-cdd)).

## 4. OEM aggressiveness

### What AOSP itself sanctions

AOSP explicitly blesses OEM-specific restriction mechanisms. From
[App power management](https://source.android.com/docs/core/power/app_mgmt):
"Device implementers can continue to use their custom methods to apply
restrictions on the apps," with the caveat "Future releases may break device
implementers' customizations." Restricted apps cannot "run jobs/alarms or use
the network in the background" and cannot "run foreground services," while
remaining "fully functional when the user launches the apps."

### What the CDD requires of OEMs that do this

[CDD section 3.5.1, Application Restriction](https://source.android.com/docs/compatibility/16/android-16-cdd)
applies when a device implements "a proprietary mechanism to restrict apps ...
that mechanism is more restrictive than the Restricted App Standby Bucket". Key
clauses, verbatim:

> - [C-1-1] MUST allow the user to see the list of restricted apps.
> - [C-1-2] MUST provide user affordance to turn on / off all of these
>   proprietary restrictions on each app.
> - [C-1-3] MUST not automatically apply these proprietary restrictions without
>   evidence of poor system health behavior, but MAY apply the restrictions on
>   apps upon detection of poor system health behavior like stuck wakelocks,
>   long running services, and other criteria. ... Other criteria that are not
>   purely related to the system health, such as the app's lack of popularity in
>   the market, MUST NOT be used as criteria.
> - [C-1-5] MUST inform users if these proprietary restrictions are applied to
>   an app automatically. Such information MUST be provided in the 24-hour
>   period preceding the application of these proprietary restrictions.
> - [C-1-6] MUST return true for the `ActivityManager.isBackgroundRestricted()`
>   method for any API calls from an app.
> - [C-1-8] MUST suspend these proprietary restrictions on an app whenever a
>   user starts to explicitly use the app, making it the top foreground
>   application.
> - [C-1-10] MUST provide a public and clear document or website that describes
>   how proprietary restrictions are applied. ... MUST include: Triggering
>   conditions for proprietary restrictions. What and how an app can be
>   restricted. How an app can be exempted from such restrictions. How an app
>   can request an exemption from proprietary restrictions.

Section 3.5 also states: "Devices MUST NOT alter the limitations enforced on
background applications."

Two usable engineering consequences: `ActivityManager.isBackgroundRestricted()`
should return true on a CDD-compliant device that has restricted Linger, which
gives the app a way to detect the condition and warn the user. And C-1-8 means
the restriction lifts when the user opens the app.

### Samsung: the one OEM with a primary-source document

[Samsung Application Management](https://developer.samsung.com/mobile/app-management.html):

- Purpose: "Samsung's application management is a solution that helps prevent
  battery consumption due to unintended background operations of applications."
- Sleeping mode: applications unused for "about 3 days" and causing poor system
  health enter it. "A bucket restriction applies to any sleeping applications,
  and features such as Job, Alarm, and Foreground-service are restricted."
- Deep sleeping mode: entered after an extended unused period, documented as 16
  days. "Deep sleeping applications only become active when the user opens them,
  and become inactive when they go into the background. Inactive applications
  can't perform any activities, including notifications or updates."
- Mitigation: the user adds the app to Never sleeping apps at
  "Settings > Device care > Battery > Background usage limits". Samsung
  publishes a deeplink so the app can send the user straight there:
  `intent.setAction("com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY")`
  with `intent.putExtra("activity_type", 2)` for the never-sleeping list.

Three days is much tighter than AOSP's eight, and deep sleeping is fatal to the
product promise.

### Xiaomi, OnePlus, Oppo

Could not confirm from any primary source. I found no developer-facing document
on a Xiaomi, OnePlus or Oppo domain describing their background restrictions,
trigger conditions or exemption paths, which is what CDD 3.5.1 [C-1-10] requires
of them. The widely cited community catalogue (dontkillmyapp.com) is not a
primary source and is deliberately excluded here.

Treat the following as untested hypotheses to verify on real hardware rather
than as findings: MIUI/HyperOS Autostart being off by default, a separate
HyperOS background-autostart permission, and per-app battery presets that must
be set to unrestricted. Oppo and OnePlus both ship ColorOS, so they likely share
one mechanism, but I could not confirm that either.

### Mitigations that are documented

1. Prefer `setAlarmClock()`, the only API the docs say the system never delays
   and will leave low-power mode for.
2. Detect restriction with `ActivityManager.isBackgroundRestricted()` (CDD
   C-1-6) and `getAppStandbyBucket()`
   ([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)),
   then tell the user their reminders are at risk.
3. Send the user to the OEM exemption screen where a documented deeplink exists,
   as with Samsung's never-sleeping list.
4. Reschedule on every re-entry point: `ACTION_BOOT_COMPLETED`,
   `ACTION_LOCKED_BOOT_COMPLETED`, `ACTION_TIMEZONE_CHANGED` and
   `ACTION_TIME_SET` are all on the implicit broadcast exceptions list, so
   manifest-declared receivers still get them
   ([broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)).
   Note `ACTION_MY_PACKAGE_REPLACED` and other package broadcasts are "not
   exempted".
5. Implement `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, and on
   receipt "Confirms that your app still has the special app access. To do so,
   call `canScheduleExactAlarms()`" then reschedule everything
   ([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule)).
6. Nudge the user to open the app at least every 8 days, or accept the
   restricted bucket. There is no documented API-side alternative for an app
   holding only `SCHEDULE_EXACT_ALARM`.

## 5. Recommended API for "exact wall-clock time in a specific timezone, offline, app closed"

**Use `AlarmManager.setAlarmClock()` with a `PendingIntent`, having computed the
absolute UTC instant yourself from the destination timezone.**

Why `setAlarmClock()`:

- It is the only method documented as never delayed: "the system never adjusts
  their delivery time. The system identifies these alarms as the most critical
  ones and leaves low-power modes if necessary to deliver the alarms."
- It survives Doze: "Alarms set with `setAlarmClock()` continue to fire
  normally. The system exits Doze shortly before those alarms fire."
- No throttle is documented for it, unlike `setExactAndAllowWhileIdle()`.
- "Alarms scheduled via this API will be allowed to start a foreground service
  even if the app is in the background."
- It "implies `RTC_WAKEUP`", which is "Alarm time in `System.currentTimeMillis()`
  (wall clock time in UTC), which will wake up the device when it goes off."
  ([setAlarmClock](https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock(android.app.AlarmManager.AlarmClockInfo,%20android.app.PendingIntent)),
  [RTC_WAKEUP](https://developer.android.com/reference/android/app/AlarmManager#RTC_WAKEUP))

Costs to accept:

- It needs `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`: "Starting with
  `Build.VERSION_CODES.S`, apps targeting SDK level 31 or higher need to request
  the `SCHEDULE_EXACT_ALARM` permission to use this API."
- It is deliberately loud. "The expectation is that when this alarm triggers,
  the application will further wake up the device to tell the user about the
  alarm, turning on the screen, playing a sound, vibrating, etc. As such, the
  system will typically also use the information supplied here to tell the user
  about this upcoming alarm if appropriate." Any app can read the next one via
  `getNextAlarmClock()`. In practice every Linger reminder becomes a
  system-surfaced upcoming alarm, which is a real product decision, not just an
  implementation detail.
- The docs warn it is expensive: "these types of alarms can be extremely
  expensive on battery use and should only be used for their intended purpose."

Do not use `setExact()` with an `OnAlarmListener`. It needs no
`SCHEDULE_EXACT_ALARM` permission, which is tempting, but: "Starting with
android version `Build.VERSION_CODES.UPSIDE_DOWN_CAKE`, the system will
explicitly drop any alarms set via this API when the calling app goes out of
lifecycle."
([setExact listener variant](https://developer.android.com/reference/android/app/AlarmManager#setExact(int,%20long,%20java.lang.String,%20android.app.AlarmManager.OnAlarmListener,%20android.os.Handler)))
That is the exact opposite of "app closed".

### Timezone handling

`RTC` alarms are absolute UTC instants, so an alarm's firing instant does not
shift when the device's timezone changes. Linger must therefore resolve
"09:00 in Asia/Tokyo" to a UTC instant at schedule time and re-resolve it if the
itinerary's timezone or offset data changes. The docs say nothing about
rescheduling on timezone change; `ACTION_TIMEZONE_CHANGED` is available as a
manifest-registrable broadcast
([broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)),
and a receiver on it is the standard hook, but this is an inference rather than
documented guidance.

### WorkManager: not suitable

Not suitable for the user-visible fire moment. Two documented blockers.

First, Doze stops it outright: "Doesn't let `JobScheduler` run. `WorkManager`
uses `JobScheduler` internally, so `WorkManager` tasks don't run."
([doze-standby](https://developer.android.com/training/monitoring-device-state/doze-standby))

Second, the API itself disclaims exactness. From
[PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest):
"note that execution may be delayed because WorkManager is subject to OS battery
optimizations, such as doze mode"; "Periodic work has a minimum interval of 15
minutes"; "Periodic work is intended for use cases where you want a fairly
consistent delay between consecutive runs, and you are willing to accept
inexactness due to battery optimizations and doze mode."

The alarms guide positions the two correctly: WorkManager is for "Scheduled
background work, such as updating your app and uploading logs", with a
"`flexInterval` (15 minutes minimum)"
([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule)).
Android 16 tightens job quotas further: "in Android 16, active standby buckets
will start being enforced by a generous runtime quota", and jobs running
alongside a foreground service now "adhere to the job runtime quota"
([Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-all)).

WorkManager remains the right tool for the work behind the alarm: "To perform
longer work, schedule it using `WorkManager` or `JobScheduler` from your alarm's
`BroadcastReceiver`."
([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule))

## 6. Comparison with iOS UNCalendarNotificationTrigger

`UNCalendarNotificationTrigger` is "A trigger condition that causes a
notification the system delivers at a specific date and time"
([reference](https://developer.apple.com/documentation/usernotifications/uncalendarnotificationtrigger)).
You hand the system `DateComponents` and it owns delivery: "The system handles
delivery of notifications based on a time or location that you specify. If the
delivery of the notification occurs when your app isn't running or in the
background, the system interacts with the user for you."
([Scheduling a notification locally from your app](https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app))

Structural differences that matter:

| | Android `setAlarmClock()` | iOS `UNCalendarNotificationTrigger` |
| --- | --- | --- |
| Who owns the fire moment | the app, via an alarm that runs app code | the system, which renders the notification itself |
| App code runs at fire time | yes, a `BroadcastReceiver` | no, unless the app is foregrounded |
| Survives reboot | no, must reschedule on `ACTION_BOOT_COMPLETED` | no documented reschedule requirement |
| Power-saving deferral | documented in detail, with buckets and throttles | none documented |
| OEM variance | sanctioned by AOSP and real | none |
| Permission friction | notification permission plus an exact-alarm special access plus Play review of the auto-granted variant | one notification authorization prompt |

Permission friction is the clearest contrast. iOS asks once:
"requestAuthorization(options:)" and "The first time your app makes this
authorization request, the system prompts the person to grant or deny the
request and records that response. Subsequent authorization requests don't
prompt the person."
([Asking permission to use notifications](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications))
There is no iOS analogue of `SCHEDULE_EXACT_ALARM`, no store policy gating
precise timing, and no documented Doze equivalent that defers a scheduled
notification. iOS also offers `provisional` authorization to avoid an upfront
prompt entirely, at the cost of quiet delivery: provisional notifications "only
appear in the notification center's history."

Reliability assessment: iOS is materially more reliable because the app is not
in the delivery path at all, so nothing the OS does to the app's background
execution can suppress the notification. Android's promise depends on the app
process being woken and a receiver running, which is exactly the surface that
Doze, standby buckets, force stop, hibernation and OEM managers all attack.

One iOS constraint to plan around: the platform is widely understood to keep
only the soonest 64 pending local notification requests per app. I could not
confirm this in current Apple documentation. `UNUserNotificationCenter`,
`add(_:)`, `UNNotificationRequest` and
`getPendingNotificationRequests(completionHandler:)` state no limit. Verify
empirically before relying on either the presence or absence of the cap.

## Ambiguities and unconfirmed items

1. **Is a travel itinerary app inside the `USE_EXACT_ALARM` permitted set?** Not
   resolvable from the policy text. The list names two app types and the app is
   neither, but "a calendar app that shows event notifications" is a description
   a dated-itinerary product partially fits. No Play documentation defines
   "calendar app". Requires a Play Console declaration and review to settle.
2. **The while-idle throttle number.** Nine minutes, about one minute, 15
   minutes and 7 per hour all appear in current official docs for the same
   mechanism. Unresolved.
3. **Does `setAlarmClock()` count against standby-bucket alarm caps?** Not
   stated. The restricted bucket's "one alarm per day, either an exact alarm or
   an inexact alarm" does not say whether alarm-clock alarms are included or
   exempt. This is the single most important open question for the product,
   because a travel app will routinely be 8+ days idle. Must be measured.
4. **Does `setAlarmClock()` bypass the restricted bucket at all?** Same gap. The
   doze exemption is documented; the bucket exemption is not.
5. **What `SCHEDULE_EXACT_ALARM` does for bucket placement at target 34+.** The
   reference's bucket floor is explicitly conditioned on targeting API 33 or
   below, and no replacement statement covers higher targets. Whether holders
   targeting 34+ get no floor, or an undocumented one, is unclear.
6. **Timezone-change rescheduling.** No documentation recommends or describes
   rescheduling alarms on `ACTION_TIMEZONE_CHANGED`. The receiver approach is an
   inference.
7. **Xiaomi, OnePlus and Oppo behavior.** No primary source found, despite CDD
   3.5.1 [C-1-10] requiring each to publish one. Everything commonly said about
   these OEMs is unverified here.
8. **Whether `isBackgroundRestricted()` actually returns true on non-compliant
   OEM builds.** CDD C-1-6 requires it; compliance is not something the docs can
   confirm.
9. **The iOS 64 pending-request limit.** Absent from current Apple docs, as
   noted above.
10. **Android 15 force-stop recovery.** Docs say `ACTION_BOOT_COMPLETED` is
    delivered when the user brings the app out of the stopped state, but do not
    say whether this happens before or independently of the user opening the
    app's UI, so the reschedule window is unclear.

## Bottom line

Exact-alarm scheduling on Android is a workable foundation, with
`setAlarmClock()` as the only API whose documented contract matches the product
promise. It is not a foundation that delivers the promise by itself. The
promise survives only if the app also handles permission revocation without
notice, reboot, force stop, hibernation, standby-bucket demotion after 8 days
idle (3 days on Samsung), and per-OEM restriction managers that AOSP explicitly
allows and that three of the four named vendors do not document.

The largest risk is not the permission prompt. It is the restricted App Standby
Bucket after 8 days of non-interaction, which caps the app at one alarm per day
even while charging, and from which only `USE_EXACT_ALARM` grants exemption, the
permission Play policy most likely denies this app.
