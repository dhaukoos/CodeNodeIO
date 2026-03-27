/*
 * preview-api Module
 * Shared PreviewRegistry for composable preview dispatch
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Minimal Compose dependencies for @Composable and Modifier types
                implementation(compose.runtime)
                implementation(compose.ui)
            }
        }
    }
}
