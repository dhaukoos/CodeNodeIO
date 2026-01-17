/*
 * CodeNodeIO IDE Plugin Platform
 * Root build configuration for Flow-Based Programming visual editor
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

// Common version constraints across all modules
allprojects {
    group = "io.codenode"
    version = "0.1.0-SNAPSHOT"
}

// Shared dependency versions
object Versions {
    const val KOTLIN = "2.1.21"
    const val COMPOSE = "1.10.0"
    const val KOTLIN_POET = "2.2.0"
    const val COROUTINES = "1.8.0"
    const val SERIALIZATION = "1.6.2"
    const val JUNIT5 = "5.10.1"
    const val INTELLIJ_PLATFORM_SDK = "2024.1"
}

subprojects {

    // Enforce Kotlin 2.1.21 across all configurations
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
            force("org.jetbrains.kotlin:kotlin-reflect:${Versions.KOTLIN}")
        }
    }
}

