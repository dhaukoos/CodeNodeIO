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
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:${LocalVersions.activityCompose}")
            // fbpDsl is JVM-only, so we include it only for Android
            implementation(project(":fbpDsl"))
        }

        androidUnitTest.dependencies {
            implementation(libs.junit5.all)
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
}
