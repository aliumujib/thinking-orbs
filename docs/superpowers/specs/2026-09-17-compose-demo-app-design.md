# thinking-orbs — Compose demo app + buildable Android project

Date: 2026-09-17
Base: fork of `kvnloo/thinking-orbs` (`aliumujib/thinking-orbs`), directory `ports/android/`.

## Goal

Make the existing golden-verified Kotlin engine port **actually run on a
device** and provide a runnable demo. The upstream fork ships the engine and a
`ThinkingOrb.kt` composable, but they are wired to a **plain Kotlin/JVM**
Gradle project — the Compose renderer has never been compiled and there is no
app. This work stands up a real Android project around the existing code and
adds the demo screen, without altering the engine math.

Not in scope (deferred, decided per user): upstream PR and a publish workflow —
only after the user sees it working and decides.

## Current state of the fork (verified 2026-09-17)

- `ports/android/build.gradle.kts` = `kotlin("jvm") 2.0.21`, no Android Gradle
  Plugin. `./gradlew test` **passes** (golden-vector JVM test, ε=1e-4) — the
  engine transcription is correct.
- `ports/android/src/main/kotlin/thinking/orbs/` — the engine (`Core`,
  `Lattice`, `Orbits`, `Web`, `Braid`, `Ribbon`, `Morph`, `Profiles`,
  `Engine.kt`). Public surface used by the renderer: `frameFor(state, size, t)`,
  `resolvePreset`, `inkGrey(white, dark)`, `LABELS`, `REDUCED_MOTION_T`,
  `OrbFrame/Dot/Line`.
- `ports/android/compose/.../ThinkingOrb.kt` — Compose composable. Imports
  `androidx.compose.*` and `android.provider.Settings`. **Not part of any
  build** — an orphan file. Never compiled.
- Golden test + `orbs-golden.json` resource under `src/test`.

### Latent issue to fix while wiring

`ThinkingOrb.kt` sizes the canvas `sizePx.dp` but feeds geometry computed for
`size = sizePx` **pixels** straight into `drawCircle`/`drawLine`. On any device
with density ≠ 1.0 the orb renders at `sizePx` px inside a `sizePx*density` px
box — i.e. small and top-left. Fix: compute the frame at the canvas's actual
pixel size and draw in px. Concretely, size the Canvas in dp for layout but call
`frameFor(state, canvasWidthPx, t)` using the real px width (from
`LocalDensity`), keeping the preset selection keyed to the logical `64`/`20`.
This preserves the two tuned presets while drawing crisp at native density.
(Detail confirmed during implementation; the DPR-cap-2 note in PORT_PLAN maps to
capping effective px scale at 2× the logical size.)

## Architecture

Convert `ports/android/` into a 3-module Android Gradle build. Keep the engine a
pure Kotlin/JVM module so the golden test keeps running with **no emulator**.

```
ports/android/
  settings.gradle.kts          includes the 3 modules + version catalog
  build.gradle.kts             root: plugin versions via catalog, no code
  gradle/libs.versions.toml    AGP, Kotlin, Compose BOM, activity-compose
  local.properties             sdk.dir (generated, gitignored)
  engine/                      (was src/) — kotlin("jvm") library
    src/main/kotlin/thinking/orbs/…   the engine, MOVED here unchanged
    src/test/…/GoldenVectorTest.kt    unchanged; still `:engine:test`
    src/test/resources/orbs-golden.json
  orbs-compose/                com.android.library + compose
    src/main/kotlin/…/ThinkingOrb.kt  MOVED from compose/, density fix applied
    depends on :engine
  demo/                        com.android.application
    src/main/kotlin/…/MainActivity.kt   the demo screen
    src/main/AndroidManifest.xml
    depends on :orbs-compose
```

Rationale for 3 modules (vs folding engine into the Android lib): the JVM golden
test is the parity guarantee and must not require the Android toolchain to run.
Keeping `:engine` as `kotlin("jvm")` preserves `./gradlew :engine:test` exactly
as it works today.

## Module details

### `:engine` (kotlin jvm library) — minimal change
- Move existing `src/` here verbatim. Same `build.gradle.kts` as today
  (`kotlin("jvm")`, jvmToolchain 17, `kotlin("test")`, JUnit platform).
- No source edits. Golden test must stay green — it's the regression guard.

### `:orbs-compose` (android library)
- `com.android.library`, `org.jetbrains.kotlin.android`, compose enabled.
- `ThinkingOrb.kt` moved here; package unchanged (`thinking.orbs.compose`).
- Density fix (above). Everything else — auto theme via `isSystemInDarkTheme`,
  reduced motion via `ANIMATOR_DURATION_SCALE == 0` → static `t=0.6`,
  `withFrameNanos` shared clock, `paused`, a11y `contentDescription`, line
  strokes for `connecting` — kept as authored.
- minSdk 24, compileSdk current stable, no manifest permissions.

### `:demo` (android app)
- Single `MainActivity` with `setContent`. A scrollable screen:
  - All 9 states, each shown at size 64 and 20 side by side, labelled.
  - Global controls: light/dark/auto theme toggle; speed slider (0.25×–3×);
    pause toggle.
  - Background follows the chosen theme so both ink polarities are visible.
- Material 3, `activity-compose`. No nav, no DI, no persistence (YAGNI).

## Verification

1. `./gradlew :engine:test` — golden vectors stay green (unchanged).
2. `./gradlew :demo:assembleDebug` — proves the Compose renderer and app
   actually compile (they never have before).
3. Launch on an emulator/device via the android-ninja `run` flow; confirm all
   9 states animate, theme toggle flips ink polarity, speed/pause work,
   `connecting` shows edges. Capture a screenshot/GIF as the "it works" artifact.

## Risks

- **Compose/AGP/Kotlin alignment.** Kotlin is pinned 2.0.21 by the engine; the
  Compose compiler must match. Use the Kotlin 2.0 built-in Compose plugin
  (`org.jetbrains.kotlin.plugin.compose`) pinned to the same Kotlin version, and
  a matching Compose BOM. Verify with `assembleDebug` after wiring.
- **Density/scale.** Addressed above; verify visually — the orb must fill its
  box on a ≠1.0-density device.
- **Emulator availability.** SDK + emulator present locally; if no AVD exists,
  create one or run on a connected device.

## Explicitly deferred (user decides later)

- PR to upstream `kvnloo` / `Jakubantalik`.
- Maven publish workflow (GitHub Actions).
