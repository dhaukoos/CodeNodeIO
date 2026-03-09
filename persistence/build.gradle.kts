/*
 * Shared Persistence Module
 * Provides Room database infrastructure and entity definitions
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "persistence"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":UserProfiles"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                // Room KMP persistence — exposed as api since public types extend RoomDatabase
                api("androidx.room:room-runtime:2.8.4")
                implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "io.codenode.persistence"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

