# Player Fitness Tracking Status Milestone

This milestone turns the existing fitness diagnostics into a concise player-facing tracking-status card on Home while keeping the detailed counters in Diagnostics.

## Home tracking card

The Home screen now shows a **Fitness tracking** card with:

- the current step-source mode;
- Health Connect connection state;
- the last successful Health Connect sync time;
- live hardware step-sensor status;
- Activity Recognition sampler status;
- the most recent activity classification, confidence, and freshness;
- direct Health Connect / Activity Recognition permission buttons when either permission is missing.

The card is intentionally status-oriented rather than debug-oriented. Raw reconciliation counters remain in Diagnostics.

## Health Connect sync timestamp

A successful Health Connect refresh for an existing character now records `fitness_last_health_sync_epoch_ms` in the core `path_of_the_wild_save` preferences.

The timestamp is updated only after the Health Connect read/reconciliation path completes successfully. It is cleared when a new character fitness epoch is created.

Because it lives in the core save store, the timestamp is automatically covered by the existing Android backup and manual `.potw` save export/import system.

Diagnostics also exposes the exact persisted timestamp as **Health Connect last successful sync**.

## Activity Recognition boundary

Activity classification remains supporting evidence only. The Home card can show walking, running, on-foot, in-vehicle, bicycle, still, or unknown observations with confidence/freshness, but none of those classifications currently reject, subtract, or alter eligible steps or fitness rewards.

That deliberate boundary avoids false step rejection while real-device signal behavior is still being observed.

## Architecture

`FitnessTrackingPanel.kt` owns the compact Home presentation. `DestinationContent` supplies the existing health/sensor/activity state as one composable Home block instead of expanding `HomeScreen` with a large set of new tracking parameters.

`FitnessLedgerStore` owns persistence of the last successful Health Connect sync timestamp alongside the existing reconciliation/reward ledger.

## Validation

The integration candidate passed the Android CI build, JVM unit-test suite, and debug APK assembly/upload before this milestone was documented.

## Deliberately deferred

- Automatic rejection of vehicle-associated or suspicious steps.
- Confidence thresholds that change rewards.
- Long-term activity history or charts.
- Background Health Connect polling beyond the existing refresh lifecycle.
