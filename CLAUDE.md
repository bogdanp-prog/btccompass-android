# Compass Android — CLAUDE.md

## Project context

**Compass** is a BTC market temperature gauge. The backend (`bogdanp-prog/btccompass-api`, private) aggregates on-chain and market indicators into a composite score (0–100): 0 = deep accumulation, 100 = euphoria.

This repo is the Android app (and future home screen widget).

**Sister repo:** `bogdanp-prog/btccompass-api` (private) — see its CLAUDE.md for the full formula, data sources, and API contract.

## Language convention

All code, comments, commit messages in **English**. Romanian only in user conversations.

## Package

`app.btccompass.android`

## Tech stack

| Layer | Library |
|---|---|
| Language | Kotlin 2.1.x |
| UI | Jetpack Compose + Material 3 |
| Networking | Ktor 3.x (OkHttp engine) |
| Serialization | Kotlinx Serialization JSON |
| Async | Kotlin Coroutines + StateFlow |
| DI | Koin 4.x |
| Score cache | DataStore Preferences (`score_cache`) — single source of truth |
| Local cache (deferred) | Room (dependency added; not used until History/heatmap screen) |
| Widget | Jetpack Glance 1.1.1 (2×2 live score widget, shipped) |
| Background refresh | WorkManager 2.10.0 (4 h periodic, no network constraint) |
| Charts (later) | Vico |
| Crash reporting | Sentry (dependency added; init when DSN is available) |

## Architecture

```
app/src/main/kotlin/app/btccompass/android/
├── CompassApp.kt          — Application class, Koin init, periodic WorkManager registration
├── MainActivity.kt        — Compose entry point
├── data/
│   ├── api/
│   │   ├── ScoreApi.kt    — Ktor HTTP client, GET /api/score
│   │   └── dto/
│   │       └── ScoreDto.kt — Kotlinx Serialization DTOs
│   └── cache/
│       └── ScoreCache.kt  — DataStore Preferences; write(ScoreDto), observe(): Flow<ScoreSnapshot?>
├── domain/
│   ├── model/
│   │   └── ScoreSnapshot.kt — UI-facing value object (score, band, price, dataAsOfEpochMillis)
│   └── repository/
│       └── ScoreRepository.kt — interface + impl (write-through: getScore() → cache + return)
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt  — Composable; cache-first render
│   │   └── HomeViewModel.kt — StateFlow<HomeUiState>; observes cache Flow, fires network refresh
│   └── theme/
│       └── Theme.kt       — Material 3 + dynamic colors
├── di/
│   └── AppModule.kt       — Koin module
└── widget/
    ├── CompassWidget.kt       — GlanceAppWidget (2×2); reads Glance-state DataStore at render time
    ├── CompassWidgetReceiver.kt — GlanceAppWidgetReceiver; triggers one-time + periodic refresh on first add
    └── ScoreRefreshWorker.kt  — CoroutineWorker; fetches → writes ScoreCache → projects to Glance state → update()
```

## Data layer

### Single source of truth

`ScoreCache` (DataStore Preferences, file `score_cache`) is the authoritative store for the latest
score snapshot. Every successful network fetch in `ScoreRepositoryImpl.getScore()` writes through to
it; the read path is `observeCachedScore(): Flow<ScoreSnapshot?>`.

**Why Preferences DataStore, not Room:** the snapshot is a flat 5-field value — score, band name,
band color, price, and `dataAsOfEpochMillis`. Room is deferred until the History/heatmap screen
requires multi-row time-series storage. Proto DataStore was also ruled out (code-gen overhead
unjustified for a single flat record).

### Cache-first rendering

`HomeViewModel` launches two coroutines on `init`:
1. Collects `observeCachedScore()` — emits immediately from disk, drives `HomeUiState.Success`.
2. Fires `getScore()` — write-through updates the cache Flow, which updates the UI in turn.

A failed refresh **must not clobber a cached score**. `HomeUiState.Error` is emitted only when the
cache is empty and the network call also fails. This invariant must not regress.

## Widget

### Architecture (Glance 2×2)

`ScoreRefreshWorker` owns the data path for the widget:
1. Calls `repository.getScore()` — fetches live, writes through to `ScoreCache`.
2. Projects a render subset (`score`, `bandName`, `bandColor`, `priceUsd`, `dataAsOfEpochMillis`)
   into the widget's own Glance-state DataStore via `updateAppWidgetState` + `persistScoreSnapshot`.
3. Calls `CompassWidget().update()` on every registered instance.

Two physical DataStores exist (app-level `score_cache` + Glance's per-widget store). This is
inherent to Glance running in a separate process; they are consistent by construction (both derived
from the same fetch) and not independent duplicates.

### Staleness label

`dataAsOfEpochMillis = fetchTimeMillis − staleMinutes × 60 000` is stored as an absolute instant.
Age is computed at render time:

```
ageMinutes = (System.currentTimeMillis() − dataAsOfEpochMillis) / 60_000
```

This ensures the label advances correctly between refreshes (a frozen `stale_minutes` would not).
`STALE_THRESHOLD_MINUTES = 8 * 60L` is defined in both `CompassWidget` and `HomeScreen`; both must
stay in sync. Above the threshold the label reads **"⚠ Data stale"**.

### Self-escalation on failure

`ScoreRefreshWorker` calls `CompassWidget().update()` even in the `catch` block (after exhausting
retries). The snapshot on disk is left unchanged; the re-render advances the age label so it can
cross the stale threshold while the device is offline. **Do not remove this call.**

### Refresh scheduling

- **Periodic:** `enqueueUniquePeriodicWork(UPDATE, 4 h, no network constraint)` — runs every 4 h
  regardless of connectivity. Registered in both `Application.onCreate` (survives `pm clear` /
  WorkManager DB wipe) and `CompassWidgetReceiver.onEnabled`.
- **One-time:** `enqueueUniqueWork(REPLACE, NetworkType.CONNECTED)` — triggered on widget first-add
  (`onEnabled`) to populate the snapshot immediately.

## API contract

Production endpoint: `https://api.btccompass.app/api/score`

Response shape:
```json
{
  "ts": "2026-06-27T14:04:23.334238+00:00",
  "score": 18.56,
  "band": { "name": "Value", "color": "#3b82f6", "range": [16, 35] },
  "trend_7d": null,
  "price": { "usd": 60519, "change_24h": 1.57, "source": "live", "ts": "..." },
  "components": {
    "cbbi":    { "raw": 0.2759, "norm": 27.59, "weight": 0.45, "contribution": 12.42 },
    "fg":      { "raw": 15,     "norm": 15,    "weight": 0.17, "contribution": 2.55 },
    "funding": { "raw": 0,      "norm": 12.56, "weight": 0.22, "contribution": 2.76 },
    "mayer":   { "raw": 0.79,   "norm": 5.23,  "weight": 0.16, "contribution": 0.84 },
    "etf":     { "raw": null,   "norm": null,  "weight": 0,    "contribution": null }
  },
  "sources_ok": { "cbbi": true, "fg": true, "funding": true, "mayer": true, "etf": false },
  "stale_minutes": 31,
  "is_stale": false
}
```

Bands:
| Name | Score range | Color |
|---|---|---|
| Deep Value | 0–15 | `#1e3a8a` |
| Value | 16–35 | `#3b82f6` |
| Neutral | 36–60 | `#6b7280` |
| Frothy | 61–80 | `#f97316` |
| Euphoria | 81–100 | `#dc2626` |

## Build

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## SDK requirements

- compileSdk: 36
- minSdk: 26 (Android 8.0)
- targetSdk: 36
- Java: 17 (bundled in Android Studio JBR)
- Gradle: 8.10.2

## Current state (v1.1 — 2026-06-28)

- HomeScreen: score (2 decimals), band name (in band color), BTC price, staleness label
- Loading and error states with retry
- Cache-first rendering via DataStore Preferences (`score_cache`)
- Glance 2×2 home-screen widget: score, band name, price, staleness label
- WorkManager 4 h periodic refresh (no network constraint; self-escalates stale label offline)
- Sentry not initialized (no DSN yet)
- Launcher icons: placeholder blue squares
- No chart

## Emulator

AVD name: `compass_pixel7` (Pixel 7, API 34, ARM64)
Create with:
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
  -n compass_pixel7 -k "system-images;android-34;google_apis;arm64-v8a" \
  -d pixel_7
```
Launch: `$ANDROID_HOME/emulator/emulator -avd compass_pixel7`
Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Start: `adb shell am start -n app.btccompass.android/.MainActivity`

Kill connectivity (airplane mode is ignored by the emulator):
```bash
adb shell svc wifi disable && adb shell svc data disable
adb shell svc wifi enable  && adb shell svc data enable
```

## Backlog (next sessions)

- Replace placeholder icons with real Compass icon
- Add Sentry DSN + init
- HomeScreen redesign (gauge arc, animations)
- `/api/history` integration + Vico chart
- Room cache (multi-row time-series for History/heatmap screen)
- `/api/price` endpoint integration for widget
