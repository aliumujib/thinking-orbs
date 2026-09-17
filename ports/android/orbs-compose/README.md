# thinking-orbs Compose UI

`ThinkingOrb.kt` is the Jetpack Compose renderer. The JVM golden-vector tests
live in the parent `ports/android` engine module and do not compile this
directory (no emulator, no Android SDK required to merge).

Drop `ThinkingOrb.kt` into an app module that already depends on Compose UI /
Foundation, and depend on the engine sources (or publish them as a JVM/Android
library).
