# Compass — BTC Market Temperature Gauge (Android)

Compass is an Android app that aggregates Bitcoin on-chain and market indicators into a single **market temperature score** from 0 (deep accumulation) to 100 (euphoria). It's the mobile front-end for the [Compass API](https://github.com/bogdanp-prog) (private backend on Vercel + Supabase).

The score is a weighted composite of:
- **CBBI** (Colin Talks Crypto Bitcoin Bull Run Index) — 45%
- **Funding Rate** (7-day average, OKX) — 22%
- **Fear & Greed Index** (Alternative.me) — 17%
- **Mayer Multiple** (price / 200-day MA) — 16%

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 (dynamic colors) |
| Networking | Ktor 3 + OkHttp |
| DI | Koin 4 |
| Async | Kotlin Coroutines + StateFlow |
| Min SDK | 26 (Android 8.0) |

Future: Glance widget, WorkManager background refresh, Vico charts, Room cache.

## Build

```bash
# Requires Android Studio's JDK and Android SDK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Screenshot

*(coming soon)*

## License

MIT — see [LICENSE](LICENSE)
