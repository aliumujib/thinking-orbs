# thinking-orbs — Jetpack Compose / Android

Kotlin transcription of the thinking-orbs geometry (same approach as the
Swift plan in `PORT_PLAN.md`): math from `spec/orbs-spec.json`, Compose
`Canvas` filled circles plus strokes for the `connecting` web.

## Layout

A 3-module Gradle build. The engine stays a pure Kotlin/JVM module so its
golden-vector test runs with no Android toolchain and no emulator.

```
ports/android/
  engine/        kotlin("jvm") — the pure geometry (all 9 states) + golden test
    src/main/kotlin/thinking/orbs/    Core, Lattice, Orbits, Web, Braid,
                                      Ribbon, Morph, Profiles, Engine
    src/test/…/GoldenVectorTest.kt    + orbs-golden.json
  orbs-compose/  com.android.library — the Compose renderer (ThinkingOrb)
  demo/          com.android.application — runnable showcase app
```

## API

```kotlin
ThinkingOrb(
    state = "searching",  // working, searching, solving, listening,
                          // connecting, weaving, composing, breathing, shaping
    sizeDp = 64,          // 64 or 20 — separate tunings, not a scale factor;
                          // any other value snaps to the nearest preset
    theme = OrbTheme.Auto,
    speed = 1f,
    paused = false,
)
```

Theme `Auto` follows `isSystemInDarkTheme()`. Reduced motion (animator
duration scale = 0) freezes at `t = 0.6`, matching the web spec. Geometry is
computed at the shipped preset (64/20) and the finished draw list is scaled to
the Canvas's real pixel size, so the orb fills its box at any display density
(effective scale capped at 2× to match the web DPR cap).

## Golden-vector tests

Geometry is checked against `spec/orbs-golden.json` (9 states × sizes 64/20
× 4 timestamps; ε = 1e-4). Theme is ink-mirroring at paint time
(`grey = round((dark ? 1 - white : white) * 255)`). Tests are JVM unit
tests — no emulator.

```bash
cd ports/android
./gradlew :engine:test
```

## Build the app

```bash
cd ports/android
./gradlew :demo:assembleDebug        # APK at demo/build/outputs/apk/debug/
```

## Run the demo

Needs an Android SDK on the machine (`ANDROID_HOME` / `local.properties`
`sdk.dir`) and a running device or emulator.

```bash
# 1. start an emulator (or plug in a device)
"$ANDROID_HOME/emulator/emulator" -avd <your_avd> -no-snapshot -no-boot-anim &
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done

# 2. install + launch
cd ports/android
./gradlew :demo:installDebug
adb shell am start -n thinking.orbs.demo/.MainActivity

# 3. (optional) record a GIF of the states animating
adb shell screenrecord --time-limit 6 --size 720x1280 /sdcard/orbs.mp4
adb pull /sdcard/orbs.mp4
ffmpeg -i orbs.mp4 -vf "fps=15,scale=360:-1:flags=lanczos" orbs.gif
```

The demo shows all nine states at sizes 64 and 20, with a dark/light toggle,
a speed slider, and a pause switch.

> **Note on some OEM devices:** aggressive "focus mode" / background-management
> software (seen on ColorOS/OxygenOS) can bounce a freshly sideloaded activity
> straight back to the launcher. If the app opens and immediately backgrounds,
> disable focus-mode/app-timers for it, or run on an emulator.
