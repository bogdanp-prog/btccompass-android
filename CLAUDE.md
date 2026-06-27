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
| Local cache (v1.1) | Room (dependency added, not used yet) |
| Widget (later) | Jetpack Glance |
| Background (later) | WorkManager |
| Charts (later) | Vico |
| Crash reporting | Sentry (dependency added; init when DSN is available) |

## Architecture

```
app/src/main/kotlin/app/btccompass/android/
├── CompassApp.kt          — Application class, Koin init
├── MainActivity.kt        — Compose entry point
├── data/
│   └── api/
│       ├── ScoreApi.kt    — Ktor HTTP client, GET /api/score
│       └── dto/
│           └── ScoreDto.kt — Kotlinx Serialization DTOs
├── domain/
│   └── repository/
│       └── ScoreRepository.kt — interface + impl
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt  — Composable
│   │   └── HomeViewModel.kt — StateFlow<HomeUiState>
│   └── theme/
│       └── Theme.kt       — Material 3 + dynamic colors
├── di/
│   └── AppModule.kt       — Koin module
├── widget/                — (placeholder, Glance widget later)
└── work/                  — (placeholder, WorkManager later)
```

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

## Current state (v0 skeleton — 2026-06-27)

- Single HomeScreen showing: score (2 decimals), band name (in band color), BTC price, stale_minutes
- Loading and error states with retry
- No local caching (Room wired but unused)
- No widget
- No chart
- Sentry not initialized (no DSN yet)
- Launcher icons: placeholder blue squares

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

## Backlog (next sessions)

- Replace placeholder icons with real Compass icon
- Add Sentry DSN + init
- HomeScreen redesign (gauge arc, animations)
- `/api/history` integration + Vico chart
- Glance widget (score + band color)
- WorkManager background refresh every 6h
- Room cache for offline mode
- `/api/price` endpoint integration for widget
