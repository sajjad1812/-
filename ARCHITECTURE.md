# Architecture

Single module `app` (Kotlin, Compose, DataStore).
- `AppSettings.kt`: settings model + DataStore repository
- `LauncherActivity.kt`: HOME screen
- `MainActivity.kt`: dashboard
- `island/IslandService.kt`: foreground service drawing the overlay with classic Views
- `AppFilter.kt`: pure search helper (unit-tested)
Planned split: `core-*` and `feature-*` modules once features grow.
