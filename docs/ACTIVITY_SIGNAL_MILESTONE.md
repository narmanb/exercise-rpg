# Activity Recognition Signal Foundation

This milestone adds a persisted Activity Recognition signal and exposes it in Fitness Diagnostics without using it to reject or modify steps yet.

## Data source

Path of the Wild now uses Google Play services `ActivityRecognitionClient` through `play-services-location:21.4.0`.

- Raw activity samples are requested approximately every 60 seconds while a character exists and Activity Recognition permission is available.
- Google Play services may deliver samples faster or slower than the requested interval and may pause reporting while the device remains still for an extended period.
- Sampling is used instead of the transition-only API because the development diagnostics need the most probable activity together with its confidence value.

## Supported activity mapping

The most probable detected activity is normalized into one of these stable game-side values:

- Walking
- Running
- On foot
- In vehicle
- On bicycle
- Still
- Unknown

Unsupported/raw classifications such as tilting remain `Unknown` instead of being guessed into another activity.

## Persistence

The latest activity signal is stored inside the existing `path_of_the_wild_save` preferences:

- normalized activity kind;
- confidence from 0–100%;
- UTC observation timestamp.

Because it lives in the existing core save store, manual `.potw` backups automatically include the latest signal without adding another backup store or changing backup format version 1.

## Android integration

- Android 10+ uses `android.permission.ACTIVITY_RECOGNITION`.
- Android 9 compatibility includes `com.google.android.gms.permission.ACTIVITY_RECOGNITION` as required by Google Play services on that API level.
- Results are delivered to an explicit, non-exported `ActivitySignalReceiver`.
- The Android 12+ PendingIntent is mutable only because Google Play services must attach result extras; the intent has an explicit receiver component and is not exposed as a generic implicit callback.
- Re-registering the same PendingIntent replaces the previous sampling request rather than stacking duplicate registrations.

## Fitness Diagnostics

Diagnostics now shows:

- current step tracking mode:
  - Health Connect + live sensor;
  - Health Connect only;
  - live sensor only;
  - no active step source;
- Activity Recognition permission state;
- sampler registration state;
- most probable recent activity;
- confidence;
- signal freshness (`Fresh`, `Recent`, `Stale`);
- UTC observation time.

Activity/step permission buttons are labeled consistently as Activity permission/access because the Android permission gates both the hardware step-counter path and Activity Recognition on supported Android versions.

## Signal age policy

- Fresh: up to 5 minutes old.
- Recent: over 5 and up to 30 minutes old.
- Stale: over 30 minutes old.

A future-dated signal caused by small wall-clock correction is treated as fresh rather than producing a negative age.

## Explicitly not implemented yet

This milestone does **not** subtract, hold, reject, or alter steps based on activity classification.

`IN_VEHICLE` is visible to the diagnostics layer, but it is observation-only. Raw Activity Recognition samples can be noisy, and the roadmap specifically prefers preserving legitimate exercise over aggressively rejecting uncertain steps. A later filtering milestone can use collected evidence and confidence thresholds once the behavior has been tested on real devices.

## Tests

`ActivitySignalRulesTest` covers:

- confidence/timestamp normalization;
- fresh/recent/stale boundaries;
- wall-clock skew handling;
- step-source tracking-mode selection.

`ActivitySignalMappingTest` covers the stable mapping of Google detected activity constants, including explicit `IN_VEHICLE`, and confirms unsupported classifications remain `Unknown`.
