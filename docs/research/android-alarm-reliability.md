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

## Follow-up: delivery architecture

Three narrow questions on whether a remote backstop rescues the restricted
bucket case. Same sourcing rules as above.

### 1. High-priority FCM in the restricted bucket

**Since Android 13, standby buckets no longer cap high-priority FCM at all.**
This is the single most useful finding in this follow-up. From
[Power management resource limits](https://developer.android.com/topic/performance/power/power-details),
stated twice on the page:

> Note that starting in Android 13, the app's standby bucket no longer
> determines how many high priority FCMs an app can use.

And in the Android 13 changes list on the same page, verbatim:

> **High Priority Firebase Cloud Message (FCM) Quotas behavior change**
>
> - App Standby Buckets no longer determine how many high priority FCMs an app
>   can use.
> - System now downgrades the high priority messages if it detects an app
>   consistently sending high-priority messages that don't result in a
>   notification
> - For current guidelines on high priority messages, refer to firebase
>   documentation on set and manage message priority.

So there are **no per-bucket high-priority FCM numbers to report for Android 13
and above, because the mechanism was removed.** The pre-13 per-bucket FCM quotas
are gone from current documentation and I did not find them stated anywhere
current. Anyone quoting per-bucket FCM numbers today is quoting a retired
mechanism.

**Doze.** The device-state table on the same page gives, for "Screen off and
doze is active", FCM behavior of "High priority: No execution limits" against
"Normal priority: Deferred to doze maintenance window"
([power-details](https://developer.android.com/topic/performance/power/power-details)).
Firebase states the mechanism: "FCM attempts to deliver high priority messages
immediately, allowing FCM to wake a sleeping device when necessary and to run
some limited processing (including very limited network access)"
([Message priority](https://firebase.google.com/docs/cloud-messaging/android/message-priority)).
This is a Firebase-domain source, but developer.android.com explicitly delegates
to it for current guidance, so the referral chain is primary.

So high-priority FCM is exempt, not capped, on both axes that matter: Doze and
standby bucket. That is a genuinely better position than local alarms are in.

**The throttle that does exist is behavioral, not numeric.** Firebase: "If FCM
detects a pattern in which messages don't result in user-facing notifications,
your messages may be deprioritized to normal priority"
([Message priority](https://firebase.google.com/docs/cloud-messaging/android/message-priority)).
The Android side puts it more strongly: "the only intended use for high priority
FCM messages is to push a notification to the user, so this situation must not
occur"
([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)).
Every high-priority push Linger sends must produce a visible notification. A
silent sync push must be normal priority.

**Does receiving an FCM message promote the app out of restricted?** No.
Receiving is not on the documented promotion list; only the user's reaction is.
From [App Standby Buckets](https://developer.android.com/topic/performance/appstandby):

> If the app doesn't show a notification upon receiving a high-priority Firebase
> Cloud Messaging (FCM) message, the user can't interact with the app and thus
> promote it to the active bucket.

The promotion is therefore a three-step chain, not a one-step one: message
arrives, app posts a notification, **user taps it**, app becomes active. Arrival
alone leaves the app in restricted.

**Two limits on the backstop that must be designed around.**

First, the app's own network is off in restricted. The bucket table gives
Network as "Disabled" for both Rare and Restricted
([power-details](https://developer.android.com/topic/performance/power/power-details)).
Firebase says high priority allows "very limited network access", so the message
itself arrives, but the handler should assume it cannot fetch itinerary detail
and must render the notification from locally cached data. Whether the "very
limited network access" grant overrides the bucket's disabled-network state is
not stated on either page. Unconfirmed.

Second, and decisively, **hibernation kills the FCM backstop too.** From
[App hibernation](https://developer.android.com/topic/performance/app-hibernation),
verbatim: "Your app can't receive push notifications, including high-priority
Firebase Cloud Messaging messages." Alongside "Your app can't run jobs or alerts
from the background." Hibernation triggers after a few months of no interaction,
so it is outside the 10-day scenario, but it means FCM is not a permanent
backstop, only a medium-term one.

Android 15 force stop is the other case where FCM is no help for alarms
specifically: it cancels PendingIntents, and an FCM handler can reschedule them,
but only if the app is not also in the stopped state that blocks it from
running. Interaction between force stop and FCM delivery is not documented.
Unconfirmed.

### 2. Does setAlarmClock() count against the restricted one-per-day cap?

**Undocumented. Stated plainly: no Google page I could find answers this.**

What I checked, and what each says:

- [Power management resource limits](https://developer.android.com/topic/performance/power/power-details)
  never uses the words `setAlarmClock` or "alarm clock". Its alarm vocabulary is
  only "Regular alarms", "While-idle alarms", "exact alarm" and "inexact alarm".
- The [AlarmManager reference](https://developer.android.com/reference/android/app/AlarmManager)
  never mentions standby buckets, the restricted bucket, or alarm quotas in any
  method's documentation, including `setAlarmClock()`.
- The [schedule alarms guide](https://developer.android.com/develop/background-work/services/alarms/schedule)
  never mentions standby buckets either.
- [App Standby Buckets](https://developer.android.com/topic/performance/appstandby)
  never mentions `setAlarmClock()`.

**The strongest available inference points to "counted", not "exempt".** The
restricted bucket row reads "One alarm per day, either an exact alarm or an
inexact alarm", and its "exact alarm" link resolves to the section of the
schedule guide headed "Ways to set an exact alarm", which lists exactly three
methods: `setExact()`, `setExactAndAllowWhileIdle()` and `setAlarmClock()`
([schedule guide](https://developer.android.com/develop/background-work/services/alarms/schedule)).
By Google's own taxonomy on the page the cap links to, `setAlarmClock()` is an
exact alarm. Nothing carves it out.

This is an inference from a link target, not a documented rule. Do not build the
product on the optimistic reading.

**One documented escape does exist,** and it is the most important sentence
found in this follow-up round. From
[App Standby Buckets](https://developer.android.com/topic/performance/appstandby):

> Note: Apps that are on the Doze exemption list are exempted from the App
> Standby Bucket-based restrictions.

The Doze exemption list is the battery-optimization allowlist. So being
allowlisted removes the restricted bucket's one-alarm-per-day cap entirely,
without `USE_EXACT_ALARM`. Section 2 of this document covers the Play
constraint: Linger almost certainly cannot use
`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to prompt directly, but "Most apps
can invoke an intent that contains the
`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`"
([doze-standby](https://developer.android.com/training/monitoring-device-state/doze-standby)),
which opens the system screen and lets the user do it themselves. That is a
documented, policy-safe, user-consented path to full bucket exemption.

**Experiment to settle the cap question.** Nothing here needs guessing; it is a
half-day measurement.

Setup. Debug build targeting API 35 or 36, declaring `SCHEDULE_EXACT_ALARM` only
and not `USE_EXACT_ALARM`, granted via the Alarms and reminders settings screen.
Physical devices, since bucket behavior is manufacturer-dependent per the
"Every manufacturer can set their own criteria" note; at minimum one Pixel and
one Samsung.

Force the state:

```
adb shell dumpsys battery unplug
adb shell am set-standby-bucket PACKAGE_NAME restricted
adb shell am get-standby-bucket PACKAGE_NAME
```

The `set-standby-bucket` command is documented for exactly this purpose on the
[Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-all)
page and the bucket values are listed there as
`active|working_set|frequent|rare|restricted`. Unplugging matters because bucket
alarm limits "apply only while the device is on battery power. While the device
is charging, the system doesn't impose these restrictions"
([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)),
with the restricted bucket being the documented exception, so measuring on a
charging device would prove nothing.

Instrument: schedule N `setAlarmClock()` alarms at known instants 20 minutes
apart over several hours, each with a distinct request code, each receiver
logging its scheduled instant and `System.currentTimeMillis()` on fire. Record
`getAppStandbyBucket()` and `isBackgroundRestricted()` at schedule time and at
each fire.

Measure three things. Does alarm number 2 within the same day fire at all, which
answers counted versus exempt. If it fires, what is the delivery skew from the
requested instant. And run the same N with `setExactAndAllowWhileIdle()` as a
control, to separate the bucket cap from the while-idle throttle.

Then repeat the whole matrix with the app on the battery-optimization allowlist,
to confirm the exemption note holds in practice, and force Doze with
`adb shell dumpsys deviceidle force-idle`
([doze-standby](https://developer.android.com/training/monitoring-device-state/doze-standby))
to check the two mechanisms compose.

### 3. Legitimate promotion out of restricted

The documented signals are short and specific. An app is in the active bucket
"while it is used, is very recently used, or when it does any of the following",
verbatim:

> - Launches an activity.
> - Runs a long running foreground service.
> - Is tapped by the user from a notification.

And, on Android 9 and higher, the system "temporarily places your app into the
active bucket" on these interactions, verbatim:

> - The user taps on a notification that your app sends.
>
>   Note: If the user swipes away the notification without tapping on it, the
>   system doesn't consider that action to be an interaction with your app.
>
> - The user interacts with a foreground service in your app by tapping a media
>   button.
> - The user connects to your app while interacting with Android Automotive OS,
>   where your app uses either a foreground service or
>   `CONNECTION_TYPE_PROJECTION`.

Both lists from [App Standby Buckets](https://developer.android.com/topic/performance/appstandby).
Promotion is temporary: "After the user stops interacting with your app, the
system places it into a bucket based on usage history."

Answering the three specific questions:

- **Does a tapped notification count?** Yes, explicitly, and it is the only
  promotion signal Linger can realistically trigger without the user opening the
  app first. A swipe-away does not count.
- **Does a home-screen widget count?** Not as a promotion signal. "Apps with
  active widgets" appear on a different list, the exemptions from *entering* the
  restricted bucket
  ([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)).
  That is arguably better than promotion, since it prevents the demotion rather
  than reversing it, but the two are not the same mechanism and the widget is
  not documented as promoting anything.
- **Does a foreground service count?** Only in two narrow forms. "Runs a long
  running foreground service" is a direct active-bucket condition, and tapping a
  media button on one is an interaction. A short foreground service, or one the
  user never touches, is not a documented promotion signal. Android 14 and later
  also constrain which foreground service types an app may declare, which is
  outside this document's scope and would need its own check.

**What Linger could plausibly use, without being user-hostile.**

Defensible:

1. **A home-screen widget**, offering the next itinerary item. This is the best
   option found: it exempts the app from entering the restricted bucket at all,
   it is opt-in, it is genuinely useful for a travel app, and it costs the user
   nothing after placement. Caveat from section 3 of the main document: Android
   15 force stop cancels widget PendingIntents and "the system disables the
   app's widgets", so the widget is not self-healing.
2. **The battery-optimization settings deeplink**, once, in context, with an
   honest explanation. Per the note quoted above this exempts the app from
   bucket restrictions entirely, which is a stronger outcome than promotion.
3. **Making reminder notifications tappable and worth tapping**, which the docs
   actively recommend: "If the users can't interact with app notifications,
   users are unable to trigger the app's promotion to the active bucket. In this
   case, consider redesigning some notifications that let users interact"
   ([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)).
   Note this is circular as a fix for a missed reminder: the tap that promotes
   the app requires a notification that already fired.
4. **A pre-trip re-engagement notification** a day or two before departure,
   while the app is still in a healthy bucket, sized to get one tap. This
   converts the 8-day idle window into a bounded problem.

Not defensible, and documented as such:

- Notification spam to hold the active bucket. Verbatim: "Note: If the user
  repeatedly dismisses a notification, the system gives the user the option to
  block that notification in the future. Don't spam the user with notifications
  to try to keep your app in the active bucket."
- Any bucket manipulation. Verbatim: "Don't try to manipulate the system into
  putting your app into a certain bucket."
- A long-running foreground service purely to hold the bucket. It would work as
  a mechanism but it is a persistent notification and a battery cost for no
  user-visible purpose, and it invites the poor-system-health detection that CDD
  3.5.1 [C-1-3] lets OEMs act on
  ([CDD](https://source.android.com/docs/compatibility/16/android-16-cdd)).

### Verdict: is local setAlarmClock plus high-priority FCM sufficient at day 10?

**Not on its own, but it is close, and the gap is closable without
`USE_EXACT_ALARM`.**

Walking the day-10 case. The user has not opened the app for 10 days, which on
Android 13+ is past the 8-day threshold, so assume the restricted bucket, and on
Samsung assume sleeping mode after 3 days.

- The local `setAlarmClock()` path is at risk. If the cap counts alarm-clock
  alarms, which is the strongest available reading though undocumented, the app
  gets one alarm that day, enforced even while charging. A travel day with three
  reminders loses two of them.
- The FCM path holds up better than expected. High-priority FCM is bucket-exempt
  since Android 13 and Doze-exempt, so the message arrives. But it is not
  offline. The core product promise is "no network", and FCM is by definition a
  network path. It backstops the connected case only.
- Neither path survives Samsung deep sleeping, where "Inactive applications
  can't perform any activities, including notifications or updates"
  ([Samsung](https://developer.samsung.com/mobile/app-management.html)), though
  that needs 16 days rather than 10.

So the answer to the literal question is no: `setAlarmClock` plus FCM does not
by itself guarantee an offline reminder at day 10 to a restricted-bucket app.

**But there is a reliable path, and it is not `USE_EXACT_ALARM`.** The
documented note that apps on the Doze exemption list "are exempted from the App
Standby Bucket-based restrictions" is the lever. Combining:

1. `setAlarmClock()` as the delivery mechanism, offline and app-closed.
2. `SCHEDULE_EXACT_ALARM` granted through the Alarms and reminders screen.
3. Battery-optimization allowlisting, offered through
   `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which removes bucket
   restrictions entirely.
4. A widget, which independently exempts the app from entering restricted.
5. High-priority FCM as a connected-only backstop and a self-heal trigger for
   reschedule after reboot, force stop or permission change.
6. The Samsung never-sleeping deeplink on Samsung hardware.

Each of those is documented and policy-safe. None is auto-granted. The honest
framing is that Android can deliver this promise reliably **only for users who
complete two or three permission flows**, and degrades for users who do not.
That is a product decision about onboarding, not a platform blocker.

### New ambiguities from this round

11. **Per-bucket high-priority FCM quotas for Android 12 and below.** Removed
    from current docs. If Linger supports API 31 and 32, the old quotas applied
    there and I could not find them stated in any current primary source.
12. **Whether FCM's "very limited network access" overrides the restricted
    bucket's disabled network.** The two pages do not reconcile. Determines
    whether an FCM handler can fetch anything or must render from cache.
13. **Whether a force-stopped app on Android 15 receives high-priority FCM.**
    Not documented. Determines whether FCM can self-heal cancelled
    PendingIntents.
14. **Whether the Doze exemption list also exempts an app from the Samsung
    sleeping and deep sleeping mechanisms,** which are proprietary and separate
    from AOSP buckets. Samsung's page does not say.
15. **Whether widget presence exempts from the restricted bucket on OEM builds**
    that implement their own bucketing, given the "Every manufacturer can set
    their own criteria" note.
