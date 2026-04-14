/*
 * iptypes Module
 * Project-tier IP type definitions for architecture flow graph connections
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":fbpDsl"))
                implementation(project(":flowGraph-persist"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":flowGraph-persist"))
                implementation(project(":flowGraph-execute"))
            }
        }
    }
}
