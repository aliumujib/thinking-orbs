plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "orbs-compose"
            // from(components["release"]) maps the project(":engine") dependency
            // to its published coordinates in the POM, so it resolves transitively.
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("thinking-orbs Compose")
                description.set("Jetpack Compose renderer for the thinking-orbs loading indicators")
                url.set("https://github.com/aliumujib/thinking-orbs")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("aliumujib")
                        name.set("Aliu Abdul-Mujeeb")
                    }
                }
                scm {
                    url.set("https://github.com/aliumujib/thinking-orbs")
                }
            }
        }
    }
}
