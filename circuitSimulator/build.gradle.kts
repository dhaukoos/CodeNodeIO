/*
 * circuitSimulator Module
 * Debugging and simulation tool for flow-based programs
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation(project(":fbpDsl"))
                implementation(project(":graphEditor"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                // Compose dependencies removed - use fbpDsl and graphEditor instead
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.10.1")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

