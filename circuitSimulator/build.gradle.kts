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
                implementation(libs.coroutines.core)
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
                implementation(libs.junit5.all)
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

