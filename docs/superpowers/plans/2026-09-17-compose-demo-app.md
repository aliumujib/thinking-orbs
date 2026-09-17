# thinking-orbs Compose Demo App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the fork's unwired Kotlin engine + orphan `ThinkingOrb.kt` into a buildable 3-module Android project with a runnable demo app that renders all 9 orb states on-device.

**Architecture:** Convert `ports/android/` into a Gradle build with three modules: `:engine` (pure Kotlin/JVM, holds the existing engine + golden test, runs with no emulator), `:orbs-compose` (Android library, holds the Compose renderer, depends on `:engine`), and `:demo` (Android app, the showcase screen, depends on `:orbs-compose`). The engine math is not touched; only build wiring, one density fix in the renderer, and new demo UI are added.

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.3, Gradle 8.10, Jetpack Compose (BOM 2024.09.03), Material 3, compileSdk 35 / minSdk 24, `org.jetbrains.kotlin.plugin.compose`.

## Global Constraints

- **Do not modify engine math.** Files under the engine source set (`Core.kt`, `Lattice.kt`, `Orbits.kt`, `Web.kt`, `Braid.kt`, `Ribbon.kt`, `Morph.kt`, `Profiles.kt`, `Engine.kt`) keep their current contents. They are moved, not edited.
- **The golden-vector JVM test must stay green** after every task: `./gradlew :engine:test` passes (72 cases, ε=1e-4).
- **Kotlin pinned to 2.0.21** across all modules (matches the existing engine module and the Compose compiler plugin version).
- **All work lives under `ports/android/`** in the fork at `/Users/aliumujib/Desktop/Android-Projects/amjb_apps/thinking-orbs-fork`. Paths below are relative to that repo root.
- **No new runtime deps beyond Compose/Material3/activity-compose.** No Hilt, Room, network, nav — YAGNI.
- **Engine public API consumed by the renderer** (already exists, do not rename): `frameFor(state: String, size: Int, t: Double): OrbFrame`, `resolvePreset(state: String, size: Int): Resolved`, `inkGrey(white: Double, dark: Boolean): Int`, `LABELS: Map<String,String>`, `REDUCED_MOTION_T: Double` (= 0.6), and `OrbFrame(dots: List<Dot>, lines: List<Line>)` with `Dot(x,y,z,r,white,a)` / `Line(x1,y1,x2,y2,white,a,w)`.
- **The 9 states, in demo order:** working, searching, solving, listening, connecting, weaving, composing, breathing, shaping. **Two sizes:** 64 and 20.

---

### Task 1: Restructure into a 3-module Gradle build (engine unchanged)

Moves the existing JVM project into an `:engine` module and creates the root build files + version catalog. No Android code yet; this task's deliverable is "the engine module still builds and its golden test still passes under the new structure."

**Files:**
- Create: `ports/android/gradle/libs.versions.toml`
- Create: `ports/android/build.gradle.kts` (root, replaces existing)
- Create: `ports/android/settings.gradle.kts` (replaces existing)
- Create: `ports/android/engine/build.gradle.kts`
- Move: `ports/android/src/` → `ports/android/engine/src/` (git mv, unchanged)
- Delete: old `ports/android/build.gradle.kts` content (replaced above)
- Modify: `ports/android/gradle.properties` (add AndroidX flags)
- Create/verify: `ports/android/local.properties` (sdk.dir; gitignored)

**Interfaces:**
- Produces: a `:engine` Gradle module exposing all `thinking.orbs.*` symbols; consumed by Task 2 via `implementation(project(":engine"))`.

- [ ] **Step 1: Move the engine sources into the `:engine` module**

```bash
cd ports/android
git mv src engine/src 2>/dev/null || { mkdir -p engine && git mv src engine/src; }
ls engine/src/main/kotlin/thinking/orbs/   # expect Core.kt … Engine.kt
ls engine/src/test/kotlin/thinking/orbs/   # expect GoldenVectorTest.kt
ls engine/src/test/resources/              # expect orbs-golden.json
```

- [ ] **Step 2: Write the version catalog**

Create `ports/android/gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
coreKtx = "1.13.1"
activityCompose = "1.9.3"
composeBom = "2024.09.03"
lifecycleRuntimeCompose = "2.8.7"
compileSdk = "35"
minSdk = "24"
targetSdk = "35"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 3: Write the root build + settings**

Create `ports/android/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "thinking-orbs-android"
include(":engine", ":orbs-compose", ":demo")
```

Create `ports/android/build.gradle.kts` (root):

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- [ ] **Step 4: Write the `:engine` module build**

Create `ports/android/engine/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    testImplementation(kotlin("test"))
}
tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 5: Update gradle.properties and local.properties**

Overwrite `ports/android/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
kotlin.code.style=official
android.useAndroidX=true
android.nonTransitiveRClass=true
org.gradle.configuration-cache=false
```

Create `ports/android/local.properties` (do not commit):

```bash
cd ports/android
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
grep -q "^local.properties" ../../.gitignore 2>/dev/null || echo "local.properties" >> .gitignore
```

- [ ] **Step 6: Verify the engine module builds and golden test passes**

Run:
```bash
cd ports/android
./gradlew :engine:test --console=plain
```
Expected: `BUILD SUCCESSFUL`, task `:engine:test` executed (golden vectors green).

- [ ] **Step 7: Commit**

```bash
cd /Users/aliumujib/Desktop/Android-Projects/amjb_apps/thinking-orbs-fork
git add -A ports/android
git commit -m "build: restructure ports/android into 3-module Gradle build (engine intact)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: `:orbs-compose` Android library — compile the renderer for the first time, fix density

Wires `ThinkingOrb.kt` into an Android library so it actually compiles, and fixes the px/dp density mismatch so the orb fills its box on real devices.

**Files:**
- Create: `ports/android/orbs-compose/build.gradle.kts`
- Create: `ports/android/orbs-compose/src/main/AndroidManifest.xml`
- Move: `ports/android/compose/src/main/kotlin/thinking/orbs/compose/ThinkingOrb.kt` → `ports/android/orbs-compose/src/main/kotlin/thinking/orbs/compose/ThinkingOrb.kt`
- Modify: the moved `ThinkingOrb.kt` (density fix only)

**Interfaces:**
- Consumes: `:engine` symbols (see Global Constraints).
- Produces: `@Composable fun ThinkingOrb(state: String = "working", sizeDp: Int = 64, theme: OrbTheme = OrbTheme.Auto, speed: Float = 1f, paused: Boolean = false, contentDescription: String? = null, modifier: Modifier = Modifier)` and `enum class OrbTheme { Auto, Dark, Light }`, package `thinking.orbs.compose`. Consumed by Task 3.

- [ ] **Step 1: Move the composable into the new module path**

```bash
cd ports/android
mkdir -p orbs-compose/src/main/kotlin/thinking/orbs/compose
git mv compose/src/main/kotlin/thinking/orbs/compose/ThinkingOrb.kt \
       orbs-compose/src/main/kotlin/thinking/orbs/compose/ThinkingOrb.kt
rmdir -p compose/src/main/kotlin/thinking/orbs/compose 2>/dev/null || true
```

- [ ] **Step 2: Write the library build file**

Create `ports/android/orbs-compose/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "thinking.orbs.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 3: Write the library manifest**

Create `ports/android/orbs-compose/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

- [ ] **Step 4: Apply the density fix to `ThinkingOrb.kt`**

The current file sizes the Canvas `sizePx.dp` but computes geometry at `size = sizePx` (pixels) and draws in px — on density ≠ 1.0 the orb is small and top-left. Change the public param to `sizeDp` (the logical preset 64/20), keep preset selection on that logical value, but compute the frame at the Canvas's real pixel width and draw in px.

Replace the `Canvas(...) { ... }` block and the two lines above it (the `frame`/`label`/`dp` vals) with:

```kotlin
    val label = contentDescription ?: LABELS[state] ?: "Thinking…"

    Canvas(
        modifier
            .size(sizeDp.dp)
            .semantics { this.contentDescription = label },
    ) {
        // Geometry is tuned in the logical preset unit (64/20). Draw at the
        // canvas's real pixel size so the orb fills the box at any density,
        // capping the effective scale at 2x to match the web DPR cap.
        val px = size.minDimension
        val scale = (px / sizeDp.toFloat()).coerceAtMost(2f)
        val drawSize = (sizeDp * scale).toInt()
        val frame: OrbFrame = frameFor(state, drawSizePreset(sizeDp), t)
        val k = drawSize.toFloat() / sizeDp.toFloat()  // px-per-logical-unit actually drawn
        val pad = (px - drawSize) / 2f

        for (l in frame.lines) {
            val g = inkGrey(l.white, dark) / 255f
            drawLine(
                color = Color(g, g, g, l.a.toFloat()),
                start = Offset(pad + l.x1.toFloat() * k, pad + l.y1.toFloat() * k),
                end = Offset(pad + l.x2.toFloat() * k, pad + l.y2.toFloat() * k),
                strokeWidth = (l.w.toFloat() * k).coerceAtLeast(1f),
                cap = StrokeCap.Butt,
            )
        }
        for (d in frame.dots) {
            val g = inkGrey(d.white, dark) / 255f
            drawCircle(
                color = Color(g, g, g, d.a.toFloat()),
                radius = d.r.toFloat() * k,
                center = Offset(pad + d.x.toFloat() * k, pad + d.y.toFloat() * k),
            )
        }
    }
}

/** Snap an arbitrary logical size to the nearest shipped preset (64 or 20). */
private fun drawSizePreset(sizeDp: Int): Int = if (sizeDp <= 42) 20 else 64
```

Note: geometry is computed with `frameFor(state, preset, t)` where `preset ∈ {20,64}`, then linearly scaled by `k` to the actual pixels drawn. This keeps the two tuned presets exact and only scales the finished draw list — the same principle the engine's `finalizeFrame` output supports.

Also rename the function parameter: change the signature line `sizePx: Int = 64,` to `sizeDp: Int = 64,` and remove any now-unused `remember(t, state, sizePx)` for `frame` (the frame is now computed inside the draw scope). Keep the `resolvePreset(state, sizePx)`-based `effSpeed`/clock logic but key it to `sizeDp`: replace `resolvePreset(state, sizePx)` with `resolvePreset(state, drawSizePreset(sizeDp))` and every other `sizePx` reference with `sizeDp`.

- [ ] **Step 5: Verify the library compiles (renderer's first-ever compile)**

Run:
```bash
cd ports/android
./gradlew :orbs-compose:assembleDebug --console=plain
```
Expected: `BUILD SUCCESSFUL`. If the Compose compiler complains about Kotlin version, confirm `libs.plugins.kotlin.compose` version matches `kotlin = "2.0.21"`.

- [ ] **Step 6: Commit**

```bash
cd /Users/aliumujib/Desktop/Android-Projects/amjb_apps/thinking-orbs-fork
git add -A ports/android
git commit -m "feat(orbs-compose): compile ThinkingOrb as Android library; fix density scaling

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: `:demo` app — the showcase screen

The runnable app: a scrollable grid of all 9 states at 64 and 20, with theme toggle, speed slider, and pause.

**Files:**
- Create: `ports/android/demo/build.gradle.kts`
- Create: `ports/android/demo/src/main/AndroidManifest.xml`
- Create: `ports/android/demo/src/main/kotlin/thinking/orbs/demo/MainActivity.kt`
- Create: `ports/android/demo/src/main/res/values/strings.xml`
- Create: `ports/android/demo/src/main/res/values/themes.xml`

**Interfaces:**
- Consumes: `ThinkingOrb`, `OrbTheme` from `:orbs-compose`.
- Produces: launchable APK `thinking.orbs.demo/.MainActivity`.

- [ ] **Step 1: Write the app build file**

Create `ports/android/demo/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "thinking.orbs.demo"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "thinking.orbs.demo"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":orbs-compose"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 2: Write the manifest**

Create `ports/android/demo/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.ThinkingOrbs">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Write resources**

Create `ports/android/demo/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Thinking Orbs</string>
</resources>
```

Create `ports/android/demo/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.ThinkingOrbs" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 4: Write the demo screen**

Create `ports/android/demo/src/main/kotlin/thinking/orbs/demo/MainActivity.kt`:

```kotlin
package thinking.orbs.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import thinking.orbs.compose.OrbTheme
import thinking.orbs.compose.ThinkingOrb

private val STATES = listOf(
    "working", "searching", "solving", "listening", "connecting",
    "weaving", "composing", "breathing", "shaping",
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DemoScreen() }
    }
}

@Composable
fun DemoScreen() {
    var dark by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var paused by remember { mutableStateOf(false) }

    val bg = if (dark) Color(0xFF0B0B0C) else Color(0xFFF7F7F8)
    val fg = if (dark) Color(0xFFEDEDED) else Color(0xFF1A1A1A)
    val theme = if (dark) OrbTheme.Dark else OrbTheme.Light

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Thinking Orbs", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark", color = fg)
            Switch(checked = dark, onCheckedChange = { dark = it })
            Spacer(Modifier.width(16.dp))
            Text("Paused", color = fg)
            Switch(checked = paused, onCheckedChange = { paused = it })
        }
        Text("Speed ${"%.2f".format(speed)}x", color = fg)
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.25f..3f)

        Spacer(Modifier.padding(4.dp))

        for (state in STATES) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state,
                    color = fg,
                    modifier = Modifier.width(110.dp),
                )
                ThinkingOrb(
                    state = state,
                    sizeDp = 64,
                    theme = theme,
                    speed = speed,
                    paused = paused,
                )
                Spacer(Modifier.width(24.dp))
                ThinkingOrb(
                    state = state,
                    sizeDp = 20,
                    theme = theme,
                    speed = speed,
                    paused = paused,
                )
            }
        }
    }
}
```

- [ ] **Step 5: Verify the app compiles**

Run:
```bash
cd ports/android
./gradlew :demo:assembleDebug --console=plain
```
Expected: `BUILD SUCCESSFUL`, an APK at `demo/build/outputs/apk/debug/demo-debug.apk`.

- [ ] **Step 6: Commit**

```bash
cd /Users/aliumujib/Desktop/Android-Projects/amjb_apps/thinking-orbs-fork
git add -A ports/android
git commit -m "feat(demo): runnable Compose showcase app for all 9 orb states

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Run on emulator and capture a GIF

Prove it actually renders and animates. Produces the "it works" artifact.

**Files:**
- Create: `ports/android/README.md` update (a "Run the demo" section) — optional doc, folded into this task.

- [ ] **Step 1: Boot the emulator**

```bash
"$HOME/Library/Android/sdk/emulator/emulator" -avd lab_pixel_api34 -no-snapshot -no-boot-anim &
adb wait-for-device
# wait until boot completes
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
echo "booted"
```

- [ ] **Step 2: Install and launch**

```bash
cd ports/android
./gradlew :demo:installDebug --console=plain
adb shell am start -n thinking.orbs.demo/.MainActivity
sleep 3
```

- [ ] **Step 3: Sanity screenshot**

```bash
adb exec-out screencap -p > /tmp/orbs-demo.png
echo "saved /tmp/orbs-demo.png"
```
Read `/tmp/orbs-demo.png` and confirm: nine labelled rows, each with a large + small orb, dark background, dots visible (the `connecting` row should show faint edges).

- [ ] **Step 4: Capture an animated GIF**

Record ~5s of screen, pull it, convert to GIF with the ffmpeg on PATH (fallback: keep the mp4 if ffmpeg absent).

```bash
adb shell screenrecord --time-limit 5 --size 720x1280 /sdcard/orbs.mp4
sleep 6
adb pull /sdcard/orbs.mp4 /tmp/orbs.mp4
if command -v ffmpeg >/dev/null; then
  ffmpeg -y -i /tmp/orbs.mp4 -vf "fps=15,scale=360:-1:flags=lanczos" /tmp/orbs.gif
  echo "GIF at /tmp/orbs.gif"
else
  echo "ffmpeg missing; mp4 at /tmp/orbs.mp4"
fi
```

- [ ] **Step 5: Toggle verification (theme flips ink polarity)**

Confirm live theme switch works by flipping the Dark switch in the running app (tap via adb) and screenshotting again:

```bash
# tap the Dark switch (coordinates approximate; adjust from the screenshot)
adb shell input tap 200 170
sleep 2
adb exec-out screencap -p > /tmp/orbs-demo-light.png
echo "saved /tmp/orbs-demo-light.png"
```
Read `/tmp/orbs-demo-light.png`; confirm background is light and dots are now dark ink (polarity mirrored).

- [ ] **Step 6: Commit the run notes / README update**

Add a "Run the demo" section to `ports/android/README.md` documenting the emulator + install + record commands above, then:

```bash
cd /Users/aliumujib/Desktop/Android-Projects/amjb_apps/thinking-orbs-fork
git add -A ports/android/README.md
git commit -m "docs(android): how to run the Compose demo on an emulator

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Deferred (not in this plan; user decides after seeing it run)

- PR upstream to `kvnloo` and/or `Jakubantalik`.
- Maven publish workflow (GitHub Actions) for `:engine` + `:orbs-compose`.

## Self-Review Notes

- **Spec coverage:** 3-module structure (Task 1) ✓; engine untouched + golden stays green (Task 1 step 6, Global Constraints) ✓; renderer compiled for the first time (Task 2) ✓; density fix (Task 2 step 4) ✓; demo screen 9×2 + theme/speed/pause (Task 3) ✓; run + GIF (Task 4) ✓; PR/publish deferred ✓.
- **Types:** `ThinkingOrb(sizeDp=…)` and `OrbTheme` defined in Task 2 Produces, consumed identically in Task 3. `frameFor/resolvePreset/inkGrey/LABELS/OrbFrame` per Global Constraints, used in Task 2.
- **Density approach:** compute geometry at the exact shipped preset (20/64), scale the finished draw list by `k` to actual pixels — presets stay exact, only rasterization scales. Effective scale capped at 2× (web DPR cap).
- **Version risk:** if AGP 8.7.3 needs a newer Gradle than 8.10, bump the wrapper to 8.11 in Task 1 (Gradle 8.10 supports AGP 8.7). Compose compiler pinned via `kotlin.plugin.compose` = 2.0.21, matching the engine's Kotlin.
