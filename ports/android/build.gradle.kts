plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// JitPack coordinates. JitPack builds this repo on a git tag and derives the
// group from the repo owner: com.github.aliumujib.thinking-orbs:<module>:<tag>.
// The version is overridden by JitPack at build time (-Pversion=<tag>); the
// default here is just for local `publishToMavenLocal` testing.
subprojects {
    group = "com.github.aliumujib.thinking-orbs"
    version = findProperty("version")?.takeIf { it != "unspecified" } ?: "0.1.0-local"
}
