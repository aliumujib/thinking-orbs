plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}
kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

dependencies {
    testImplementation(kotlin("test"))
}
tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "engine"
            afterEvaluate {
                from(components["java"])
            }
            pom {
                name.set("thinking-orbs engine")
                description.set("Pure-Kotlin geometry engine for the thinking-orbs loading indicators")
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
