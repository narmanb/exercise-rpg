# Foreground Fitness Lifecycle Milestone

This milestone tightens fitness synchronization around the Android activity lifecycle without changing exercise reward rules or Activity Recognition filtering.

## Health Connect refresh on return

The app now observes its Activity lifecycle. Each `ON_RESUME` event creates a foreground refresh opportunity.

When Health Connect is available and a client exists, returning to the app refreshes Health Connect step data and runs the existing reconciliation path. This means activity accumulated while the game was in the background can be reflected promptly when the player comes back instead of waiting for a manual refresh or app restart.

The existing successful-sync timestamp is updated only when the Health Connect refresh itself succeeds.

## Direct step sensor lifecycle

The hardware `TYPE_STEP_COUNTER` listener is now registered only while the app is in the foreground and Activity Recognition permission is available.

At `ON_STOP`, Compose disposes the foreground registration and unregisters the listener. When the app returns, the listener reconnects.

No walking progress is intentionally discarded by this change: Android's step counter is cumulative since its current sensor/reboot epoch, so the next foreground reading is reconciled as a delta from the last stored raw observation. Existing reboot handling remains authoritative if the raw counter itself resets.

## Permission refresh

`ACTIVITY_RECOGNITION` permission is re-checked on `ON_RESUME`. If the player changed that permission in Android Settings while the app was away, the in-app sensor/activity state updates on return.

Health Connect permission state is re-checked as part of the foreground Health Connect refresh.

## Policy layer

`FitnessForegroundPolicy` keeps two decisions explicit and unit-testable:

- whether the live hardware step listener should be registered;
- whether a foreground Health Connect refresh should run.

The direct sensor requires foreground state, permission, and hardware. Health refresh requires foreground state, an available Health Connect SDK, and an initialized Health Connect client.

## Deliberately unchanged

- Walking XP, Adventure Point, and Momentum conversion rates.
- The persistent reconciliation/reward ledgers.
- Activity Recognition remains observation-only; it still does not reject or subtract steps.
- No continuous GPS or new background service was added.
- No class, subclass, skill, capture, or Bond/Mastery design choices were introduced.

## Validation

`FitnessForegroundPolicyTest` covers the required foreground/permission/hardware combinations for sensor registration and Health Connect refresh eligibility.

The integration candidate compiled successfully, passed the JVM unit-test suite, assembled the debug APK, and uploaded the APK artifact before this milestone was documented.
