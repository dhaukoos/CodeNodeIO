/*
 * KMPMobileApp Module
 * Kotlin Multiplatform mobile app targeting Android and iOS using Compose Multiplatform
 * License: Apache 2.0
 *
 * This module uses localized Compose Multiplatform version (1.7.3) which differs from
 * the main project's version to ensure compatibility with Kotlin 2.1.21.
 * Dependencies are defined explicitly rather than using the compose plugin.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)  // Compose compiler plugin (tied to Kotlin version)
    alias(libs.plugins.android.application)
}

// Localized version constants for this module (differs from main project)
object LocalVersions {
    const val compose = "1.7.3"
    const val activityCompose = "1.9.0"
    const val datetime = "0.6.1"
}

kotlin {
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KMPMobileApp"
            isStatic = true
        }
    }

    // Use the default hierarchy template for iOS source sets
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // Localized Compose dependencies (version 1.7.3 for Kotlin 2.1.21 compatibility)
            implementation("org.jetbrains.compose.runtime:runtime:${LocalVersions.compose}")
            implementation("org.jetbrains.compose.foundation:foundation:${LocalVersions.compose}")
            implementation("org.jetbrains.compose.material3:material3:${LocalVersions.compose}")
            implementation("org.jetbrains.compose.ui:ui:${LocalVersions.compose}")
            implementation(libs.coroutines.core)
            // Multiplatform datetime support
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:${LocalVersions.datetime}")
            // FBP DSL and generated StopWatch module (KMP - works on all platforms)
            implementation(project(":fbpDsl"))
            implementation(project(":StopWatch"))
            // JetBrains Multiplatform ViewModel
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":fbpDsl"))
            implementation(project(":StopWatch"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:${LocalVersions.activityCompose}")
            // Compose tooling for previews
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:${LocalVersions.compose}")
        }

        androidUnitTest.dependencies {
            implementation("junit:junit:4.13.2")
        }

        iosMain.dependencies {
            // iOS-specific dependencies can be added here
        }
    }
}

android {
    namespace = "io.codenode.mobileapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.codenode.mobileapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    dependencies {
        debugImplementation("org.jetbrains.compose.ui:ui-tooling:${LocalVersions.compose}")
    }
}
